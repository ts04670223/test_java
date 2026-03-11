import React, { useState, useEffect, useCallback } from 'react';
import toast from 'react-hot-toast';
import { useAuthStore } from '../stores/authStore';
import { passkeyAPI } from '../services/api';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

/**
 * 將 ArrayBuffer / Uint8Array 轉換為 Base64URL 字串（WebAuthn 需要）
 */
function bufferToBase64url(buffer) {
  const bytes = new Uint8Array(buffer);
  let str = '';
  for (const b of bytes) str += String.fromCharCode(b);
  return btoa(str).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

/**
 * 將 Base64URL 字串轉換為 ArrayBuffer
 */
function base64urlToBuffer(base64url) {
  const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
  const binary = atob(padded);
  const buffer = new ArrayBuffer(binary.length);
  const view = new Uint8Array(buffer);
  for (let i = 0; i < binary.length; i++) view[i] = binary.charCodeAt(i);
  return buffer;
}

/**
 * 在傳送至後端前，將 WebAuthn 回應中的 ArrayBuffer 欄位轉為 Base64URL
 */
function prepareCredentialForTransport(credential) {
  const obj = {
    id: credential.id,
    rawId: bufferToBase64url(credential.rawId),
    type: credential.type,
    response: {},
  };
  for (const key of Object.keys(credential.response)) {
    const val = credential.response[key];
    if (val instanceof ArrayBuffer) {
      obj.response[key] = bufferToBase64url(val);
    } else if (typeof val === 'function') {
      // 跳過方法（如 getTransports）
    } else {
      obj.response[key] = val;
    }
  }
  // 包含 transport 資訊（如果有的話）
  if (typeof credential.response.getTransports === 'function') {
    obj.response.transports = credential.response.getTransports();
  }
  if (typeof credential.response.getAuthenticatorData === 'function') {
    obj.response.authenticatorData = bufferToBase64url(credential.response.getAuthenticatorData());
  }
  if (typeof credential.response.getPublicKey === 'function') {
    const pk = credential.response.getPublicKey();
    if (pk) obj.response.publicKey = bufferToBase64url(pk);
  }
  if (typeof credential.response.getPublicKeyAlgorithm === 'function') {
    obj.response.publicKeyAlgorithm = credential.response.getPublicKeyAlgorithm();
  }
  return obj;
}

/**
 * 將後端回傳的 creation options 中的 Base64URL 欄位轉為 ArrayBuffer
 */
function prepareCreationOptions(options) {
  const prepared = { ...options };
  prepared.challenge = base64urlToBuffer(options.challenge);
  prepared.user = {
    ...options.user,
    id: base64urlToBuffer(options.user.id),
  };
  if (options.excludeCredentials) {
    prepared.excludeCredentials = options.excludeCredentials.map((c) => ({
      ...c,
      id: base64urlToBuffer(c.id),
    }));
  }
  return prepared;
}

/**
 * 將後端回傳的 request options 中的 Base64URL 欄位轉為 ArrayBuffer
 */
function prepareRequestOptions(options) {
  const prepared = { ...options };
  // PublicKeyCredentialRequestOptions 放在 publicKeyCredentialRequestOptions 或頂層
  const pkOpts = options.publicKeyCredentialRequestOptions || options;
  prepared.challenge = base64urlToBuffer(pkOpts.challenge);
  if (pkOpts.allowCredentials) {
    prepared.allowCredentials = pkOpts.allowCredentials.map((c) => ({
      ...c,
      id: base64urlToBuffer(c.id),
    }));
  }
  return prepared;
}

// ──────────────────────────────────────────────────────────────────────────────

export default function PasskeyManager() {
  const { user, isAuthenticated } = useAuthStore();
  const [passkeys, setPasskeys] = useState([]);
  const [loading, setLoading] = useState(false);
  const [registering, setRegistering] = useState(false);
  const [displayName, setDisplayName] = useState('');
  const [supported, setSupported] = useState(true);
  const [unsupportedReason, setUnsupportedReason] = useState('');
  // 是否有本機平台驗證器（Windows Hello / Touch ID）
  const [hasPlatformAuth, setHasPlatformAuth] = useState(null);

  useEffect(() => {
    if (!window.isSecureContext) {
      setSupported(false);
      setUnsupportedReason('insecure');
      return;
    }
    if (!window.PublicKeyCredential) {
      setSupported(false);
      setUnsupportedReason('browser');
      return;
    }
    // 偵測是否有本機生物辨識（平台驗證器）
    window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable?.()
      .then(setHasPlatformAuth)
      .catch(() => setHasPlatformAuth(false));
  }, []);

  const loadPasskeys = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      setLoading(true);
      const res = await passkeyAPI.listPasskeys();
      setPasskeys(res.data.data || []);
    } catch {
      // 靜默失敗
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    loadPasskeys();
  }, [loadPasskeys]);

  const handleRegister = async () => {
    if (!user?.username) return;
    setRegistering(true);
    try {
      // Step 1: 取得 challenge
      const startRes = await passkeyAPI.startRegistration(
        user.username,
        displayName || `${user.username} 的 Passkey`
      );
      const rawOptions = startRes.data.data;
      const creationOptions = prepareCreationOptions(rawOptions);

      // Step 2: 呼叫瀏覽器 WebAuthn API
      const credential = await navigator.credentials.create({
        publicKey: creationOptions,
      });

      if (!credential) {
        toast.error('未能取得驗證器回應，請再試一次');
        return;
      }

      // Step 3: 傳送至後端完成驗證
      const credJson = JSON.stringify(prepareCredentialForTransport(credential));
      await passkeyAPI.finishRegistration(
        user.username,
        displayName || `${user.username} 的 Passkey`,
        credJson
      );

      toast.success('Passkey 已成功登錄！下次可直接使用生物辨識登入');
      setDisplayName('');
      loadPasskeys();
    } catch (err) {
      if (err.name === 'NotAllowedError') {
        toast.error('使用者取消了生物辨識驗證');
      } else if (err.name === 'InvalidStateError') {
        toast.error('此裝置已有相同的 Passkey，請先刪除舊的再重新登錄');
      } else {
        const msg = err.response?.data?.message || err.message || 'Passkey 登錄失敗';
        toast.error(msg);
      }
    } finally {
      setRegistering(false);
    }
  };

  const handleDelete = async (credentialId, name) => {
    if (!window.confirm(`確定要刪除「${name}」嗎？`)) return;
    try {
      await passkeyAPI.deletePasskey(credentialId);
      toast.success('Passkey 已刪除');
      loadPasskeys();
    } catch (err) {
      toast.error(err.response?.data?.message || '刪除失敗');
    }
  };

  if (!supported) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-2xl">
        <Card>
          <CardContent className="pt-6 space-y-3">
            {unsupportedReason === 'insecure' ? (
              <>
                <p className="font-semibold text-red-600 text-center">
                  ⚠️ 需要 HTTPS 才能使用 Passkeys
                </p>
                <p className="text-sm text-muted-foreground text-center">
                  WebAuthn / Passkeys 只能在 <strong>安全情境（Secure Context）</strong> 下運作：
                </p>
                <ul className="text-sm text-muted-foreground list-disc ml-6 space-y-1">
                  <li>使用 <code className="bg-muted px-1 rounded">https://</code> 存取網站（需設定 SSL 憑證）</li>
                  <li>或改用 <code className="bg-muted px-1 rounded">localhost</code> / <code className="bg-muted px-1 rounded">127.0.0.1</code> 進行本機測試</li>
                </ul>
                <p className="text-xs text-muted-foreground text-center mt-2">
                  目前網址：<code className="bg-muted px-1 rounded">{window.location.origin}</code>
                </p>
              </>
            ) : (
              <p className="text-muted-foreground text-center">
                您的瀏覽器不支援 WebAuthn / Passkeys。請使用 Chrome 108+、Safari 16+ 或 Firefox 119+。
              </p>
            )}
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-2xl">
      <h1 className="text-2xl font-bold mb-6">🔑 Passkey 管理</h1>

      {/* 登錄新 Passkey */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>登錄新的 Passkey</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* 根據平台驗證器支援情況顯示不同說明 */}
          {hasPlatformAuth === false ? (
            <div className="rounded-md border border-yellow-300 bg-yellow-50 p-3 text-sm text-yellow-800">
              <p className="font-medium mb-1">⚠️ 此裝置未偵測到生物辨識感應器</p>
              <p>您仍可使用以下方式登錄 Passkey：</p>
              <ul className="list-disc ml-4 mt-1 space-y-1">
                <li>插入 <strong>YubiKey</strong> 等 USB 硬體安全金鑰</li>
                <li>用手機掃描 QR Code（瀏覽器會自動提示）</li>
                <li>在 Windows 設定中啟用 <strong>Windows Hello</strong> PIN 碼</li>
              </ul>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              Passkey 使用裝置的生物辨識（指紋、臉部識別、Windows Hello）或安全金鑰代替密碼，更安全也更方便。
              <strong>不需要手機</strong>，電腦上的 Touch ID / Windows Hello 也可以使用。
            </p>
          )}
          <div className="space-y-2">
            <Label htmlFor="displayName">Passkey 名稱（選填）</Label>
            <Input
              id="displayName"
              placeholder="例如：我的 iPhone 15、MacBook Touch ID"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
          </div>
          <Button
            onClick={handleRegister}
            disabled={registering || !isAuthenticated}
            className="w-full"
          >
            {registering ? '正在啟動生物辨識...' : '+ 登錄此裝置的 Passkey'}
          </Button>
        </CardContent>
      </Card>

      {/* 已登錄的 Passkeys 列表 */}
      <Card>
        <CardHeader>
          <CardTitle>已登錄的 Passkeys</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <p className="text-muted-foreground text-center py-4">讀取中...</p>
          ) : passkeys.length === 0 ? (
            <p className="text-muted-foreground text-center py-4">
              尚未登錄任何 Passkey
            </p>
          ) : (
            <ul className="space-y-3">
              {passkeys.map((pk) => (
                <li
                  key={pk.id}
                  className="flex items-center justify-between p-3 border rounded-lg"
                >
                  <div>
                    <p className="font-medium">{pk.displayName || 'Passkey'}</p>
                    <p className="text-xs text-muted-foreground">
                      登錄時間：{pk.createdAt ? new Date(pk.createdAt).toLocaleString('zh-TW') : '—'}
                    </p>
                    {pk.lastUsedAt && (
                      <p className="text-xs text-muted-foreground">
                        上次使用：{new Date(pk.lastUsedAt).toLocaleString('zh-TW')}
                      </p>
                    )}
                  </div>
                  <Button
                    variant="destructive"
                    size="sm"
                    onClick={() => handleDelete(pk.credentialId, pk.displayName || 'Passkey')}
                  >
                    刪除
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

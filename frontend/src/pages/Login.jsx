import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuthStore } from '../stores/authStore';
import { authAPI, passkeyAPI } from '../services/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import '../styles/Auth.css';

// ── WebAuthn 工具函式 ────────────────────────────────────────────────────────

function base64urlToBuffer(base64url) {
  const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
  const binary = atob(padded);
  const buffer = new ArrayBuffer(binary.length);
  const view = new Uint8Array(buffer);
  for (let i = 0; i < binary.length; i++) view[i] = binary.charCodeAt(i);
  return buffer;
}

function bufferToBase64url(buffer) {
  const bytes = new Uint8Array(buffer);
  let str = '';
  for (const b of bytes) str += String.fromCharCode(b);
  return btoa(str).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

function prepareRequestOptions(serverOptions) {
  const pkOpts = serverOptions.publicKeyCredentialRequestOptions || serverOptions;
  const prepared = { ...pkOpts };
  prepared.challenge = base64urlToBuffer(pkOpts.challenge);
  if (pkOpts.allowCredentials?.length) {
    prepared.allowCredentials = pkOpts.allowCredentials.map((c) => ({
      ...c,
      id: base64urlToBuffer(c.id),
    }));
  }
  return prepared;
}

function prepareAssertionForTransport(credential) {
  return {
    id: credential.id,
    rawId: bufferToBase64url(credential.rawId),
    type: credential.type,
    response: {
      authenticatorData: bufferToBase64url(credential.response.authenticatorData),
      clientDataJSON: bufferToBase64url(credential.response.clientDataJSON),
      signature: bufferToBase64url(credential.response.signature),
      userHandle: credential.response.userHandle
        ? bufferToBase64url(credential.response.userHandle)
        : null,
    },
  };
}

// ────────────────────────────────────────────────────────────────────────────

function Login() {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [passkeyLoading, setPasskeyLoading] = useState(false);
  const [passkeySupported, setPasskeySupported] = useState(false);
  const navigate = useNavigate();
  const { login, isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (isAuthenticated) navigate('/shop');
    // 非同步確認平台驗證器可用性
    if (window.PublicKeyCredential) {
      window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable?.()
        .then((available) => setPasskeySupported(available))
        .catch(() => setPasskeySupported(true)); // 無法偵測時樂觀顯示
    }
  }, [isAuthenticated, navigate]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const response = await authAPI.login(formData);
      const loginData = response.data.data;
      login(loginData.user, loginData.token);
      toast.success('登入成功！');
      navigate('/shop');
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.response?.data?.error || '登入失敗，請稍後再試';
      setError(errorMessage);
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handlePasskeyLogin = async () => {
    setError('');
    setPasskeyLoading(true);
    try {
      // Step 1: 取得 challenge（username 可選，支援 discoverable credential）
      const username = formData.username || null;
      const startRes = await passkeyAPI.startAssertion(username);
      const serverOptions = startRes.data.data;
      const requestOptions = prepareRequestOptions(serverOptions);

      // Step 2: 呼叫瀏覽器 WebAuthn API
      const credential = await navigator.credentials.get({ publicKey: requestOptions });
      if (!credential) {
        toast.error('未取得驗證器回應，請再試一次');
        return;
      }

      // Step 3: 傳送至後端完成驗證
      const credJson = JSON.stringify(prepareAssertionForTransport(credential));
      const finishRes = await passkeyAPI.finishAssertion(credJson, username);
      const loginData = finishRes.data.data;

      login(loginData.user, loginData.token);
      toast.success('Passkey 驗證成功！');
      navigate('/shop');
    } catch (err) {
      if (err.name === 'NotAllowedError') {
        toast.error('使用者取消了生物辨識驗證');
      } else {
        const msg = err.response?.data?.message || err.message || 'Passkey 登入失敗';
        setError(msg);
        toast.error(msg);
      }
    } finally {
      setPasskeyLoading(false);
    }
  };

  return (
    <div className="container mx-auto flex items-center justify-center min-h-[80vh] px-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl font-bold"> 現代電商</CardTitle>
          <p className="text-sm text-muted-foreground">歡迎回來</p>
        </CardHeader>
        <CardContent>
          {error && (
            <Alert variant="destructive" className="mb-4">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="username">用戶名或電子郵件</Label>
              <Input
                id="username"
                name="username"
                type="text"
                placeholder="請輸入用戶名"
                autoFocus
                value={formData.username}
                onChange={handleChange}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">密碼</Label>
              <Input
                id="password"
                name="password"
                type="password"
                placeholder="請輸入密碼"
                value={formData.password}
                onChange={handleChange}
              />
            </div>

            <Button className="w-full" type="submit" disabled={loading || passkeyLoading}>
              {loading ? '登入中...' : '登入'}
            </Button>
          </form>

          {/* Passkey 登入區塊 */}
          {passkeySupported && (
            <>
              <div className="relative my-4">
                <div className="absolute inset-0 flex items-center">
                  <span className="w-full border-t" />
                </div>
                <div className="relative flex justify-center text-xs text-muted-foreground">
                  <span className="bg-card px-2">或使用無密碼登入</span>
                </div>
              </div>

              <Button
                variant="outline"
                className="w-full gap-2"
                onClick={handlePasskeyLogin}
                disabled={loading || passkeyLoading}
                type="button"
              >
                {passkeyLoading ? (
                  '正在驗證生物辨識...'
                ) : (
                  <>
                    <span>🔑</span>
                    使用 Passkey（指紋 / 臉部識別）登入
                  </>
                )}
              </Button>
              <p className="text-xs text-muted-foreground text-center mt-2">
                可選填帳號以使用特定 Passkey，或留空使用任何已登錄的 Passkey
              </p>
            </>
          )}
        </CardContent>
        <CardFooter className="flex justify-center">
          <div className="text-sm text-muted-foreground">
            還沒有帳號?{' '}
            <Link to="/register" className="text-primary hover:underline">
              立即註冊
            </Link>
          </div>
        </CardFooter>
      </Card>
    </div>
  );
}

export default Login;

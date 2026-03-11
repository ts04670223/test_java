#!/bin/bash
# 為 test6.test 設定 HTTPS 自簽憑證（使用 mkcert，瀏覽器可信任）
# 用法：bash /vagrant/scripts/nginx/setup-ssl.sh
#
# 執行後需要在 Windows 端匯入根憑證：
#   /vagrant/scripts/nginx/ssl/rootCA.pem

set -e

DOMAIN="test6.test"
SSL_DIR="/vagrant/scripts/nginx/ssl"
NGINX_SSL_DIR="/etc/nginx/ssl"

echo "=== 設定 HTTPS for $DOMAIN ==="

# ── 1. 安裝 mkcert ──────────────────────────────────────────────
if ! command -v mkcert &>/dev/null; then
  echo "→ 安裝 mkcert..."
  sudo apt-get update -qq
  sudo apt-get install -y libnss3-tools wget

  MKCERT_VER="v1.4.4"
  ARCH=$(dpkg --print-architecture)
  if [ "$ARCH" = "amd64" ]; then
    BIN="mkcert-${MKCERT_VER}-linux-amd64"
  else
    BIN="mkcert-${MKCERT_VER}-linux-arm64"
  fi

  wget -qO /tmp/mkcert "https://github.com/FiloSottile/mkcert/releases/download/${MKCERT_VER}/${BIN}"
  chmod +x /tmp/mkcert
  sudo mv /tmp/mkcert /usr/local/bin/mkcert
fi

echo "✓ mkcert $(mkcert --version)"

# ── 2. 安裝本機 CA ─────────────────────────────────────────────
mkcert -install

# ── 3. 找到 rootCA 位置 ────────────────────────────────────────
CAROOT=$(mkcert -CAROOT)
echo "→ CA 根目錄：$CAROOT"

# ── 4. 產生 test6.test 憑證 ────────────────────────────────────
mkdir -p "$SSL_DIR"
cd "$SSL_DIR"
mkcert -key-file test6.test-key.pem \
       -cert-file test6.test-cert.pem \
       "$DOMAIN"

# ── 5. 複製 rootCA 到共享資料夾（供 Windows 匯入）──────────────
if [ -f "$CAROOT/rootCA.pem" ]; then
  cp "$CAROOT/rootCA.pem" "$SSL_DIR/rootCA.pem"
  echo "✓ rootCA.pem 已複製到 $SSL_DIR"
fi

# ── 6. 複製憑證到 nginx ssl 目錄 ───────────────────────────────
sudo mkdir -p "$NGINX_SSL_DIR"
sudo cp "$SSL_DIR/test6.test-cert.pem" "$NGINX_SSL_DIR/test6.test-cert.pem"
sudo cp "$SSL_DIR/test6.test-key.pem"  "$NGINX_SSL_DIR/test6.test-key.pem"
sudo chmod 640 "$NGINX_SSL_DIR/test6.test-key.pem"
echo "✓ 憑證已安裝至 $NGINX_SSL_DIR"

# ── 7. 套用更新的 nginx 設定 ────────────────────────────────────
bash /vagrant/scripts/nginx/update.sh

echo ""
echo "════════════════════════════════════════════════════════"
echo "  HTTPS 設定完成！"
echo ""
echo "  下一步：在 Windows 匯入根憑證（只需做一次）"
echo "  憑證位置：C:\\JOHNY\\test\\scripts\\nginx\\ssl\\rootCA.pem"
echo "  方法：雙擊 rootCA.pem → 安裝憑證 → 本機電腦"
echo "        → 將所有憑證放入下列存放區 → 受信任的根憑證授權單位"
echo ""
echo "  完成後用 https://test6.test 存取網站"
echo "════════════════════════════════════════════════════════"

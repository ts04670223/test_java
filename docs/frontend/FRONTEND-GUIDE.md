# 🎨 前端啟動指南

完整的 React + Vite 前端應用啟動和部署說明。

## 📋 目錄

1. [前端技術棧](#前端技術棧)
2. [在 Windows 啟動](#在-windows-啟動)
3. [在 Vagrant VM 啟動](#在-vagrant-vm-啟動)
4. [訪問前端應用](#訪問前端應用)
5. [故障排查](#故障排查)

---

## 前端技術棧

### 框架和工具
- **React 18** - UI 框架
- **Vite 5** - 構建工具
- **React Router 6** - 路由管理
- **Material-UI (MUI)** - UI 組件庫
- **Zustand** - 狀態管理
- **Axios** - HTTP 客戶端

### 功能特點
✅ 會員註冊和登入系統  
✅ 商品瀏覽和搜索  
✅ 購物車管理  
✅ 訂單管理  
✅ 即時聊天功能  
✅ 個人資料管理  
✅ 響應式設計

### 端口配置
- **開發服務器**: http://localhost:3000
- **API 代理**: `/api` → `http://localhost:8080`

---

## 在 Windows 啟動

### 前置需求

✅ **Node.js 18+** 已安裝  
✅ **npm 或 yarn** 已安裝  
✅ **後端應用運行中** (http://localhost:8080)

### 步驟 1: 檢查 Node.js 版本

```powershell
# 檢查 Node.js 版本
node --version
# 應該顯示 v18.x.x 或更高

# 檢查 npm 版本
npm --version
# 應該顯示 9.x.x 或更高
```

### 步驟 2: 安裝依賴

```powershell
# 切換到前端目錄
cd C:\JOHNY\test\frontend

# 安裝依賴（首次運行需要）
npm install
```

### 步驟 3: 啟動開發服務器

```powershell
# 啟動開發服務器
npm run dev
```

### 步驟 4: 訪問應用

瀏覽器會自動打開，或手動訪問：
```
http://localhost:3000
```

### 常用命令

```powershell
# 開發模式（熱重載）
npm run dev

# 構建生產版本
npm run build

# 預覽生產版本
npm run preview

# 清理 node_modules 重新安裝
Remove-Item -Recurse -Force node_modules
npm install
```

---

## 在 Vagrant VM 啟動

### 為什麼在 VM 內啟動？

- ✅ 前後端在同一環境中
- ✅ 避免 Windows 和 VM 之間的網路問題
- ✅ 更接近生產環境

### 步驟 1: 登入 VM

```powershell
# 在 Windows 執行
cd C:\JOHNY\test
vagrant ssh
```

### 步驟 2: 安裝 Node.js（如果未安裝）

```bash
# 在 VM 內執行

# 安裝 Node.js 18.x
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# 驗證安裝
node --version
npm --version
```

### 步驟 3: 安裝前端依賴

```bash
# 切換到前端目錄
cd /vagrant/frontend

# 安裝依賴
npm install
```

### 步驟 4: 啟動開發服務器

```bash
# 啟動服務器（監聽所有網路介面）
npm run dev -- --host 0.0.0.0
```

### 步驟 5: 在 Windows 訪問

```
http://localhost:3000
```

### 後台運行（選用）

```bash
# 使用 nohup 後台運行
nohup npm run dev -- --host 0.0.0.0 > /tmp/frontend.log 2>&1 &

# 查看日誌
tail -f /tmp/frontend.log

# 停止後台服務
pkill -f "vite"
```

---

## 訪問前端應用

### 🏠 主頁面

```
http://localhost:3000
```

### 📄 主要頁面路由

| 頁面 | 路徑 | 說明 |
|------|------|------|
| 首頁 | `/` | 商品列表 |
| 登入 | `/login` | 會員登入 |
| 註冊 | `/register` | 新用戶註冊 |
| 商城 | `/shop` | 商品瀏覽 |
| 購物車 | `/cart` | 購物車管理 |
| 訂單 | `/orders` | 訂單查詢 |
| 個人資料 | `/profile` | 用戶資料 |
| 聊天 | `/chat` | 即時聊天 |
| 結帳 | `/checkout` | 結帳頁面 |

### 🔐 測試帳號

```
用戶名: user1
密碼: password123
```

或使用 [測試數據文檔](TEST_DATA.md) 中的其他帳號。

---

## 配置說明

### vite.config.js 配置

```javascript
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,              // 前端端口
    host: '0.0.0.0',        // 監聽所有網路介面（VM 需要）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // 後端 API 地址
        changeOrigin: true
      }
    }
  }
})
```

### API 請求配置

前端的 API 請求會自動代理到後端：

```javascript
// 前端請求
axios.get('/api/products')

// 實際請求
http://localhost:8080/api/products
```

---

## 故障排查

### 問題 1: npm install 失敗

#### 症狀
```
npm ERR! code EACCES
npm ERR! syscall mkdir
```

#### 解決方法
```powershell
# Windows: 以管理員權限執行
# 或清理 npm 緩存
npm cache clean --force
Remove-Item -Recurse -Force node_modules
npm install
```

### 問題 2: 端口 3000 已被占用

#### 症狀
```
Port 3000 is in use
```

#### 解決方法
```powershell
# Windows: 查找並終止占用進程
netstat -ano | findstr :3000
taskkill /PID <PID> /F

# 或修改 vite.config.js 使用其他端口
server: {
  port: 3001
}
```

### 問題 3: 無法連接到後端 API

#### 症狀
```
Network Error
ERR_CONNECTION_REFUSED
```

#### 檢查清單
```powershell
# 1. 確認後端運行
curl http://localhost:8080/actuator/health

# 2. 檢查後端日誌
vagrant ssh -c "docker logs demo-app"

# 3. 確認代理配置
# 查看 vite.config.js 的 proxy 設置
```

### 問題 4: 熱重載不工作

#### 解決方法
```bash
# 在 VM 內需要配置輪詢
# 修改 vite.config.js
export default defineConfig({
  server: {
    watch: {
      usePolling: true  // 啟用輪詢（VM 共享文件夾需要）
    }
  }
})
```

### 問題 5: 在 VM 內無法訪問

#### 症狀
從 Windows 無法訪問 VM 內的前端服務

#### 解決方法
```bash
# 確保使用 --host 0.0.0.0
npm run dev -- --host 0.0.0.0

# 或修改 package.json
"scripts": {
  "dev": "vite --host 0.0.0.0"
}
```

---

## 🚀 生產部署

### 構建生產版本

```bash
# 構建
npm run build

# 會生成 dist/ 目錄
```

### 使用 Nginx 服務

```nginx
# /etc/nginx/sites-available/frontend
server {
    listen 80;
    server_name localhost;
    
    root /vagrant/frontend/dist;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 使用 Docker 部署

```dockerfile
# frontend/Dockerfile
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## 📝 開發建議

### 推薦的開發工作流

#### Windows 開發（推薦新手）

```powershell
# 終端 1: 啟動後端
cd C:\JOHNY\test
.\install.bat

# 終端 2: 啟動前端
cd C:\JOHNY\test\frontend
npm run dev
```

**優點**：
- ✅ 熱重載快速
- ✅ 調試方便
- ✅ 編輯器支持好

#### VM 開發（推薦進階）

```bash
# 登入 VM
vagrant ssh

# 終端 1: 後端已通過 Docker 運行

# 終端 2: 啟動前端
cd /vagrant/frontend
npm run dev -- --host 0.0.0.0
```

**優點**：
- ✅ 環境一致
- ✅ 接近生產環境
- ✅ 避免跨平台問題

---

## 🔧 進階配置

### 環境變量配置

創建 `.env` 文件：

```env
# .env.development
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws

# .env.production
VITE_API_BASE_URL=https://api.example.com
VITE_WS_URL=wss://api.example.com/ws
```

使用環境變量：

```javascript
// src/config.js
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
export const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws';
```

### VS Code 推薦插件

```json
{
  "recommendations": [
    "dbaeumer.vscode-eslint",
    "esbenp.prettier-vscode",
    "bradlc.vscode-tailwindcss",
    "dsznajder.es7-react-js-snippets"
  ]
}
```

---

## 📚 相關文檔

- [前端 README](../frontend/README.md) - 詳細前端說明
- [API 文檔](SHOPPING_API.md) - 後端 API 參考
- [聊天 API](CHAT_API.md) - WebSocket API
- [測試數據](TEST_DATA.md) - 測試帳號和數據

---

## 💡 快速命令總結

### Windows 快速啟動

```powershell
cd C:\JOHNY\test\frontend
npm install    # 首次運行
npm run dev    # 啟動開發服務器
```

### VM 快速啟動

```bash
vagrant ssh
cd /vagrant/frontend
npm install    # 首次運行
npm run dev -- --host 0.0.0.0
```

### 訪問地址

```
前端: http://localhost:3000
後端: http://localhost:8080
Dashboard: http://localhost:8001/...
```

---

**記住**：前端需要後端 API 支持，確保後端服務已啟動！

**快速開始**：
```powershell
cd frontend
npm install
npm run dev
```

**就是這麼簡單！** 🎉

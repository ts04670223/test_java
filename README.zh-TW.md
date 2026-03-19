# Spring Boot 電商平台 - Docker 部署版

完整的 Spring Boot 3.2.0 電商應用程式，整合 MySQL、Redis，支持 Docker 容器化部署。

## 📦 快速開始

### 前置需求

- **Windows 10/11**
- **VirtualBox 7.0+** - [下載](https://www.virtualbox.org/wiki/Downloads)
- **Vagrant 2.4+** - [下載](https://www.vagrantup.com/downloads)

### 一鍵安裝

```cmd
# 1. 安裝前置軟體（管理員權限執行）
.\tools\install-prerequisites.ps1

# 2. 啟動環境
.\tools\install-vagrant-env.bat

# 3. 等待應用啟動（約 5-10 分鐘）
.\tools\wait-for-app.bat

# 4. 驗證部署
.\tools\verify-deployment.bat
```

### 訪問服務

部署成功後可訪問：

- **主應用**: http://localhost:8080
- **API 文檔**: http://localhost:8080/swagger-ui.html
- **健康檢查**: http://localhost:8080/actuator/health
- **MySQL**: localhost:3307 (用戶: springboot/springboot123)
- **Redis**: localhost:6379

### Kubernetes Dashboard（可選）

如需使用 K8s Web 管理頁面：

```cmd
# 安裝 Dashboard
cd k8s-dashboard
install-dashboard.bat

# 啟動 Dashboard
start-dashboard.bat
```

**訪問**: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/

**功能**：
- 📊 Web 圖形界面管理 Kubernetes 集群
- 🚀 查看和管理 Pods、Deployments、Services
- 📝 實時查看容器日誌
- 💻 直接進入容器執行命令
- ⚙️ 管理 ConfigMaps 和 Secrets

詳細說明請查看：
- [k8s-dashboard/README.md](k8s-dashboard/README.md) - 完整使用說明
- [docs/infrastructure/K8S-DASHBOARD.md](docs/infrastructure/K8S-DASHBOARD.md) - 詳細操作指南

## 📚 文檔

- **[快速開始指南](docs/getting-started/QUICKSTART.md)** - 5 分鐘快速部署
- **[安裝詳細說明](docs/getting-started/INSTALL.md)** - 完整安裝步驟
- **[Docker 使用指南](docs/infrastructure/DOCKER-GUIDE.md)** - Docker 操作說明
- **[容器訪問指南](docs/infrastructure/DOCKER-ACCESS.md)** - 進入容器和調試
- **[K8s Dashboard 指南](docs/infrastructure/K8S-DASHBOARD.md)** - Kubernetes 管理頁面
- **[故障排查](docs/troubleshooting/TROUBLESHOOTING.md)** - 常見問題解決

## 🛠️ 常用命令

### Docker 管理

```cmd
# 啟動所有服務
.\start-docker.bat

# 重啟服務
.\fix-and-restart.bat

# 進入容器
.\docker-access.bat

# 診斷問題
.\diagnose.bat

# 查看日誌
vagrant ssh -c "docker compose logs -f app"
```

### Vagrant 管理

```cmd
# 啟動 VM
vagrant up

# 停止 VM
vagrant halt

# 重啟 VM
vagrant reload

# SSH 連接
vagrant ssh

# 查看狀態
vagrant status
```

## 🏗️ 專案結構

```
test/
├── src/                          # Java 源代碼
│   ├── main/
│   │   ├── java/                 # Java 源文件
│   │   └── resources/            # 配置文件
│   └── test/                     # 測試代碼
├── frontend/                     # React 前端
├── scripts/                      # 安裝腳本
│   ├── install-docker.sh         # Docker 安裝
│   └── install-k8s.sh           # Kubernetes 安裝
├── data/                         # 數據庫初始化腳本
├── Dockerfile                    # Docker 構建文件
├── docker-compose.yml           # Docker Compose 配置
├── Vagrantfile                  # Vagrant 配置
├── pom.xml                      # Maven 配置
└── *.bat                        # Windows 工具腳本
```

## 🔧 技術棧

### 後端
- **Spring Boot 3.2.0** - 應用框架
- **Spring Security** - JWT 認證
- **Spring Data JPA** - 數據訪問
- **MySQL 8.0** - 關係型數據庫
- **Redis 7** - 緩存層
- **Maven** - 構建工具

### 前端
- **React 18** - UI 框架
- **Vite** - 構建工具
- **Zustand** - 狀態管理

### 部署
- **Docker** - 容器化
- **Docker Compose** - 服務編排
- **Vagrant** - 開發環境
- **VirtualBox** - 虛擬化

## 📊 功能特性

### 用戶管理
- 用戶註冊/登入
- JWT Token 認證
- 個人資料管理
- 密碼加密存儲

### 商品管理
- 商品 CRUD 操作
- 商品分類
- 商品圖片
- 商品變體（SKU）
- 商品標籤
- 商品評論

### 購物車
- 添加/刪除商品
- 數量調整
- 小計計算
- 購物車持久化

### 訂單系統
- 訂單創建
- 訂單查詢
- 訂單狀態追蹤
- 訂單項目管理

### 即時聊天
- WebSocket 支持
- 實時消息推送
- 消息歷史記錄

## 🚀 開發指南

### 本地開發

```cmd
# 啟動數據庫（僅 MySQL + Redis）
vagrant ssh -c "cd /vagrant && docker compose -f docker-compose.dev.yml up -d"

# 在 IDE 中運行 Spring Boot
# 或使用 Maven
mvn spring-boot:run
```

### 構建部署

```cmd
# 完整構建
vagrant ssh -c "cd /vagrant && docker compose build --no-cache"

# 啟動服務
vagrant ssh -c "cd /vagrant && docker compose up -d"
```

### 測試

```cmd
# 運行測試
mvn test

# 整合測試
.\test-integration.bat
```

## 📝 API 文檔

### 用戶 API
- `POST /api/auth/register` - 用戶註冊
- `POST /api/auth/login` - 用戶登入
- `GET /api/users/me` - 獲取當前用戶信息

### 商品 API
- `GET /api/products` - 獲取商品列表
- `GET /api/products/{id}` - 獲取商品詳情
- `POST /api/products` - 創建商品（需認證）
- `PUT /api/products/{id}` - 更新商品（需認證）
- `DELETE /api/products/{id}` - 刪除商品（需認證）

### 購物車 API
- `GET /api/cart` - 獲取購物車
- `POST /api/cart/items` - 添加商品到購物車
- `PUT /api/cart/items/{id}` - 更新購物車項目
- `DELETE /api/cart/items/{id}` - 刪除購物車項目

### 訂單 API
- `GET /api/orders` - 獲取訂單列表
- `GET /api/orders/{id}` - 獲取訂單詳情
- `POST /api/orders` - 創建訂單
- `PUT /api/orders/{id}/status` - 更新訂單狀態

詳細 API 說明請查看：
- [購物 API 文檔](SHOPPING_API.md)
- [聊天 API 文檔](CHAT_API.md)
- [測試數據](TEST_DATA.md)

## 🔍 故障排查

常見問題及解決方案請參考 [故障排查指南](TROUBLESHOOTING.md)

### 快速診斷

```cmd
# 運行診斷工具
.\diagnose.bat

# 查看容器狀態
vagrant ssh -c "docker compose ps"

# 查看應用日誌
vagrant ssh -c "docker compose logs -f app"
```

## 📄 許可證

MIT License

## 👥 貢獻

歡迎提交 Issue 和 Pull Request！

## 📮 聯繫方式

如有問題請提交 GitHub Issue。

---

**Version**: 1.0.0  
**Last Updated**: 2025-12-19

# Spring Boot 電商平台

完整的 Spring Boot 3.2.0 電商應用程式，整合 MySQL、Redis，支持 Docker 容器化部署。

> **📦 已整理為安裝包** - 所有文檔和工具已分類整理到 `docs/` 和 `tools/` 資料夾

## 🚀 快速開始

### 🔑 登入 K8s 機器

如果需要在 Kubernetes 機器內執行命令，使用以下方式登入：

```cmd
# 在專案根目錄執行
vagrant ssh
```

登入後看到 `vagrant@k8s-master:~$` 表示已進入 K8s 機器，可以執行：
```bash
kubectl get nodes           # 查看節點狀態
kubectl get pods -A         # 查看所有 Pods
docker ps                   # 查看容器
exit                        # 退出 VM
```

📖 詳細說明請參閱 [Vagrant 使用指南](docs/getting-started/VAGRANT-GUIDE.md)

---

### 三步驟安裝

```powershell
# 1. 安裝前置軟體（管理員權限）
.\tools\install-prerequisites.ps1

# 2. 啟動環境
.\tools\install-vagrant-env.bat

# 3. 驗證部署
.\tools\verify-deployment.bat
```

訪問：http://localhost:8080

📖 詳細步驟請參閱 [快速開始指南](docs/getting-started/QUICKSTART.md)

## 📚 文檔

### 🚀 入門
- **[快速開始](docs/getting-started/QUICKSTART.md)** - 5 分鐘快速部署
- **[快速安裝](docs/getting-started/INSTALL_QUICK.md)** - 簡易安裝步驟
- **[完整安裝指南](docs/getting-started/INSTALL.md)** - 詳細安裝說明
- **[Vagrant 使用指南](docs/getting-started/VAGRANT-GUIDE.md)** - Vagrant 操作說明

### 🏗️ 基礎設施
- **[Docker 使用指南](docs/infrastructure/DOCKER-GUIDE.md)** - Docker 操作說明
- **[容器訪問指南](docs/infrastructure/DOCKER-ACCESS.md)** - 進入容器和調試
- **[K8s Dashboard 指南](docs/infrastructure/K8S-DASHBOARD.md)** - Kubernetes 管理頁面
- **[VM 存取指南](docs/infrastructure/VM-ACCESS.md)** - VM 登入與操作

### 🎨 前端
- **[前端啟動指南](docs/frontend/FRONTEND-GUIDE.md)** - 前端運行方式

### 🗄️ 資料庫
- **[MySQL 配置](docs/database/MYSQL_SETUP.md)** - 數據庫配置說明
- **[測試數據](docs/database/TEST_DATA.md)** - 測試數據說明

### 🔧 故障排除
- **[故障排查](docs/troubleshooting/TROUBLESHOOTING.md)** - 常見問題解決
- **[Flannel 修復](docs/troubleshooting/FLANNEL-FIX.md)** - K8s 網路問題修復
- **[Kong 路由修復](docs/troubleshooting/KONG-ROUTE-FIX.md)** - Kong Gateway 路由問題
- **[重啟問題分析](docs/troubleshooting/RESTART-ISSUES-ANALYSIS.md)** - VM/K8s 重啟問題根因
- **[重啟快速修復](docs/troubleshooting/RESTART-QUICK-FIX.md)** - 重啟後一鍵修復

### 📡 API 文檔
- **[購物 API](docs/api/SHOPPING_API.md)** - 電商功能 API
- **[聊天 API](docs/api/CHAT_API.md)** - WebSocket 聊天 API

## 🛠️ 技術棧

### 後端
- **Spring Boot 3.2.0** - 應用框架
- **Spring Security** - JWT 認證
- **Spring Data JPA** - 數據訪問
- **MySQL 8.0** - 關係型數據庫
- **Redis 7** - 緩存層
- **Maven 3.9** - 構建工具

### 前端
- **React 18** - UI 框架
- **Vite** - 構建工具
- **Zustand** - 狀態管理

### 部署
- **Docker** - 容器化
- **Docker Compose** - 服務編排
- **Vagrant** - 開發環境
- **VirtualBox** - 虛擬化
- **Kubernetes** - 容器編排（可選）
- **Kubernetes Dashboard** - K8s Web 管理界面

## 📦 安裝包內容

本專案包含三個主要安裝包：

### 1. 🐳 Docker 部署包（主要）
位置：根目錄 + `tools/` + `docs/`

**快速開始**：
```cmd
.\tools\install-prerequisites.ps1  # 安裝 VirtualBox + Vagrant
.\tools\install-vagrant-env.bat                      # 部署應用
.\tools\verify-deployment.bat      # 驗證部署
```

**訪問**：http://localhost:8080

### 2. 🎛️ Kubernetes Dashboard 包
位置：`k8s-dashboard/`

**快速開始**：
```cmd
cd k8s-dashboard
install-dashboard.bat    # 安裝 Dashboard
start-dashboard.bat      # 啟動並訪問
```

**訪問**：http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/

**文檔**：
- [k8s-dashboard/README.md](k8s-dashboard/README.md) - 完整使用說明
- [k8s-dashboard/PACKAGE_INFO.md](k8s-dashboard/PACKAGE_INFO.md) - 安裝包詳情
- [docs/infrastructure/K8S-DASHBOARD.md](docs/infrastructure/K8S-DASHBOARD.md) - 詳細操作指南

### 3. 📚 完整文檔包
位置：`docs/`

包含所有操作指南、API 文檔、故障排查等 13 個文檔文件。

## 📁 專案結構

```
test/                                     # 專案根目錄
├── docs/                                 # 📚 文檔資料夾（11 個文檔）
│   ├── getting-started/                  # 🚀 入門指南
│   │   ├── QUICKSTART.md
│   │   ├── INSTALL.md
│   │   ├── INSTALL_QUICK.md
│   │   └── VAGRANT-GUIDE.md
│   ├── api/                              # 📡 API 文件
│   │   ├── SHOPPING_API.md
│   │   └── CHAT_API.md
│   ├── infrastructure/                   # 🏗️ 基礎設施
│   │   ├── DOCKER-GUIDE.md
│   │   ├── DOCKER-ACCESS.md
│   │   ├── K8S-DASHBOARD.md
│   │   └── VM-ACCESS.md
│   ├── database/                         # 🗄️ 資料庫
│   │   ├── MYSQL_SETUP.md
│   │   └── TEST_DATA.md
│   ├── frontend/                         # 🎨 前端
│   │   └── FRONTEND-GUIDE.md
│   ├── troubleshooting/                  # 🔧 故障排除
│   │   ├── TROUBLESHOOTING.md
│   │   ├── FLANNEL-FIX.md
│   │   ├── KONG-ROUTE-FIX.md
│   │   ├── RESTART-ISSUES-ANALYSIS.md
│   │   └── RESTART-QUICK-FIX.md
│   └── archive/                          # 📦 歷史紀錄
│
├── tools/                                # 🛠️ 工具資料夾
│   ├── install-vagrant-env.bat           # 📦 環境安裝 (原 install.bat)
│   ├── install-prerequisites.ps1         # 安裝前置軟體
│   ├── check-requirements.bat            # 檢查需求
│   ├── diagnose.bat                      # 系統診斷
│   ├── docker-access.bat                 # 訪問容器
│   ├── start-docker.bat                  # 啟動服務
│   ├── rebuild-docker.bat                # 重建容器
│   ├── fix-and-restart.bat               # 修復重啟
│   ├── verify-deployment.bat             # 驗證部署
│   ├── wait-for-app.bat                  # 等待啟動
│   └── ...其他工具腳本
│
├── src/                                  # 💻 源代碼
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── DemoApplication.java      # 主程式
│   │   │   ├── config/                   # 配置類
│   │   │   ├── controller/               # 控制器
│   │   │   ├── model/                    # 數據模型
│   │   │   ├── repository/               # 數據訪問
│   │   │   ├── service/                  # 業務邏輯
│   │   │   └── dto/                      # 數據傳輸對象
│   │   └── resources/
│   │       ├── application.properties    # 應用配置
│   │       └── static/                   # 靜態資源
│   └── test/                             # 測試代碼
│
├── frontend/                             # 🎨 前端代碼
│   ├── src/
│   │   ├── pages/                        # 頁面組件
│   │   ├── components/                   # 通用組件
│   │   ├── services/                     # API 服務
│   │   └── stores/                       # 狀態管理
│   ├── index.html                        # 入口 HTML
│   ├── package.json                      # 依賴配置
│   └── vite.config.js                    # Vite 配置
│
├── scripts/                              # 📜 安裝腳本
│   ├── install-docker.sh                 # Docker 安裝
│   ├── install-k8s.sh                   # K8s 安裝
│   └── setup-k8s-cluster.sh             # K8s 集群配置
│
├── data/                                 # 💾 數據文件
│   └── init.sql                          # 數據庫初始化
│
├── Dockerfile                            # 🐳 Docker 構建
├── docker-compose.yml                    # 🐳 服務編排
├── docker-compose.dev.yml                # 🐳 開發配置
├── Vagrantfile                           # 📦 VM 配置
├── pom.xml                               # Maven 配置
├── README.md                             # 本文件
├── README.zh-TW.md                       # 中文 README
```

## ⚡ 常用命令

### Docker 管理
```cmd
.\tools\start-docker.bat        # 啟動所有服務
.\tools\rebuild-docker.bat      # 重新構建容器
.\tools\fix-and-restart.bat     # 修復並重啟
.\tools\docker-access.bat       # 進入容器
.\tools\diagnose.bat            # 系統診斷
```

### Vagrant 管理
```cmd
vagrant up                      # 啟動 VM
vagrant halt                    # 停止 VM
vagrant reload                  # 重啟 VM
vagrant ssh                     # SSH 連接
vagrant status                  # 查看狀態
```

### 訪問服務
- **主應用**: http://localhost:8080
- **API 文檔**: http://localhost:8080/swagger-ui.html
- **健康檢查**: http://localhost:8080/actuator/health
- **MySQL**: localhost:3307 (用戶: springboot/springboot123)
- **Redis**: localhost:6379

## 🔍 快速診斷

遇到問題時：
```cmd
# 1. 運行診斷工具
.\tools\diagnose.bat

# 2. 查看容器狀態
vagrant ssh -c "docker compose ps"

# 3. 查看應用日誌
vagrant ssh -c "docker compose logs -f app"
```

更多問題請參考 [故障排查指南](docs/troubleshooting/TROUBLESHOOTING.md)

## 📊 功能特性

### 用戶管理
- 用戶註冊/登入
- JWT Token 認證
- 個人資料管理

### 商品管理
- 商品 CRUD 操作
- 商品分類與標籤
- 商品評論系統

### 購物系統
- 購物車管理
- 訂單創建與追蹤
- 願望清單

### 即時聊天
- WebSocket 支持
- 實時消息推送

詳細 API 說明請查看：
- [購物 API 文檔](docs/api/SHOPPING_API.md)
- [聊天 API 文檔](docs/api/CHAT_API.md)

## 📄 許可證

MIT License

## 🤝 貢獻

歡迎提交 Issue 和 Pull Request！

---

**Version**: 1.0.0  
**Last Updated**: 2025-12-19  
**包含完整的 Docker 化部署方案**

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- 用戶名: `sa`
- 密碼: (空白)

## 測試

運行所有測試：

```bash
mvn test
```

## 開發指引

### 添加新功能

1. 在 `model` 套件中定義實體類別
2. 在 `repository` 套件中創建資料存取介面
3. 在 `service` 套件中實作業務邏輯
4. 在 `controller` 套件中創建 REST API

### 資料驗證

使用 Jakarta Validation 註解進行資料驗證：
- `@NotBlank`: 不能為空
- `@Email`: Email 格式驗證
- `@Size`: 長度限制

### 錯誤處理

服務層會拋出 `RuntimeException`，控制器會捕獲並返回適當的 HTTP 狀態碼和錯誤訊息。

## 設定檔說明

主要設定位於 `application.properties`：

- 伺服器埠號：`server.port=8080`
- 資料庫連接：H2 記憶體資料庫
- JPA 設定：自動建立/刪除資料表
- 日誌級別：開發模式下顯示 SQL

## 內建資料

應用程式啟動時會自動載入 `data.sql` 中的示例資料，包含 5 個測試用戶。

## 後續開發建議

- [ ] 添加全域異常處理器（@ControllerAdvice）
- [ ] 實作分頁和排序功能
- [ ] 添加安全性（Spring Security）
- [ ] 整合 Swagger/OpenAPI 文件
- [ ] 使用 PostgreSQL 或 MySQL 等生產級資料庫
- [ ] 添加更多單元測試和整合測試
- [ ] 實作 DTO（Data Transfer Object）層
- [ ] 添加日誌追蹤和監控

## 授權

MIT License

## 聯絡方式

如有問題或建議，請聯繫專案維護者。
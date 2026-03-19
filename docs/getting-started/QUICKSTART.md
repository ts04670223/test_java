# 快速開始指南

## 🚀 三步驟啟動應用程式

### 步驟 1：安裝 Vagrant 和 VirtualBox

```powershell
# 使用 Chocolatey（推薦）
choco install virtualbox vagrant -y
```

或手動下載安裝：
- VirtualBox: https://www.virtualbox.org/wiki/Downloads
- Vagrant: https://www.vagrantup.com/downloads

### 步驟 2：啟動環境

```powershell
# 在專案根目錄執行
vagrant up
```

⏱️ 首次執行需要 5-10 分鐘（下載 Ubuntu 映像檔並安裝軟體）

### 步驟 3：運行應用程式

```powershell
# 登入虛擬機
vagrant ssh

# 進入專案並運行
cd spring-boot-demo
mvn spring-boot:run
```

✅ 完成！在瀏覽器訪問：http://localhost:8080

## 🎯 測試 API

### 在 Windows 的 PowerShell 中測試

```powershell
# 查看歡迎頁面
curl http://localhost:8080

# 取得所有用戶
curl http://localhost:8080/api/users

# 創建新用戶
curl -Method POST -Uri http://localhost:8080/api/users `
  -ContentType "application/json" `
  -Body '{"name":"張三","email":"zhang@example.com","phone":"0912-345-678"}'
```

### 在虛擬機內測試

```bash
# 取得所有用戶
curl http://localhost:8080/api/users | jq

# 創建新用戶
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"李四","email":"li@example.com","phone":"0923-456-789"}' | jq
```

## 📊 查看 H2 資料庫

在瀏覽器開啟：http://localhost:8080/h2-console

登入資訊：
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **用戶名**: `sa`
- **密碼**: (留空)

## 🛠️ 開發模式

### 在 Windows 編輯，虛擬機運行

1. 在 VS Code（Windows）中開啟專案資料夾
2. 編輯 Java 檔案
3. 檔案會自動同步到虛擬機
4. 在虛擬機內重新編譯並運行

```bash
# 在虛擬機內執行
cd spring-boot-demo
mvn clean spring-boot:run
```

### 停止應用程式

在虛擬機終端按 `Ctrl+C`

## 🔧 常用 Vagrant 命令

```powershell
# 啟動虛擬機
vagrant up

# 登入虛擬機
vagrant ssh

# 停止虛擬機
vagrant halt

# 重新啟動
vagrant reload

# 刪除虛擬機
vagrant destroy

# 查看狀態
vagrant status
```

## 📚 完整文檔

- [INSTALL.md](INSTALL.md) - 詳細安裝指南和故障排除
- [README.md](README.md) - 專案結構和 API 文檔

## 💡 提示

- 虛擬機會佔用 2GB 記憶體，確保主機有足夠資源
- 專案檔案會自動同步，不需要手動複製
- 應用程式的 8080 埠已對應到主機
- 修改 Java 程式碼後需要重新啟動應用程式才會生效
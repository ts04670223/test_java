# 安裝指南 - 使用 Vagrant

本專案使用 Vagrant 提供一致的開發環境，讓您無需在本機安裝 Java 和 Maven。

## 前置需求

### 1. 安裝 Vagrant 和 VirtualBox

#### Windows 安裝

1. **安裝 VirtualBox**
   - 下載：https://www.virtualbox.org/wiki/Downloads
   - 下載 "Windows hosts" 版本
   - 執行安裝程式並完成安裝

2. **安裝 Vagrant**
   - 下載：https://www.vagrantup.com/downloads
   - 下載 Windows 版本
   - 執行安裝程式並完成安裝
   - **重新啟動電腦**

3. **驗證安裝**
   ```powershell
   vagrant --version
   # 應該顯示：Vagrant 2.x.x
   
   vboxmanage --version
   # 應該顯示：7.x.x
   ```

#### 使用 Chocolatey（推薦）

如果已安裝 Chocolatey，可以快速安裝：

```powershell
# 以管理員身份執行 PowerShell
choco install virtualbox vagrant -y
```

## 啟動 Vagrant 環境

### 1. 啟動虛擬機

在專案根目錄執行：

```powershell
# 啟動並自動配置虛擬機（首次執行需要較長時間）
vagrant up
```

這個命令會：
- 下載 Ubuntu 22.04 映像檔（首次執行）
- 創建虛擬機
- 自動安裝 Java 17 和 Maven
- 將專案資料夾同步到虛擬機

### 2. 登入虛擬機

```powershell
vagrant ssh
```

### 3. 進入專案目錄

```bash
cd spring-boot-demo
```

## 在 Vagrant 中編譯和運行專案

登入虛擬機後，執行以下命令：

### 1. 編譯專案

```bash
mvn clean install
```

### 2. 運行應用程式

```bash
mvn spring-boot:run
```

### 3. 訪問應用程式

在**主機（Windows）**的瀏覽器中訪問：
- 主頁: http://localhost:8080
- H2 資料庫控制台: http://localhost:8080/h2-console
- 用戶 API: http://localhost:8080/api/users

應用程式的 8080 埠已自動對應到主機，所以您可以在 Windows 上直接訪問。

## Vagrant 常用命令

在 Windows PowerShell 中執行：

```powershell
# 啟動虛擬機
vagrant up

# 登入虛擬機
vagrant ssh

# 重新啟動虛擬機
vagrant reload

# 暫停虛擬機
vagrant suspend

# 恢復虛擬機
vagrant resume

# 停止虛擬機
vagrant halt

# 刪除虛擬機（保留專案檔案）
vagrant destroy

# 查看虛擬機狀態
vagrant status

# 重新執行配置腳本
vagrant provision
```

## 開發工作流程

### 方式一：在虛擬機內開發（推薦）

1. 啟動虛擬機：`vagrant up`
2. 登入虛擬機：`vagrant ssh`
3. 進入專案：`cd spring-boot-demo`
4. 編輯代碼：使用 `vim` 或 `nano`
5. 運行應用：`mvn spring-boot:run`

### 方式二：在 Windows 上編輯，虛擬機內運行

1. 啟動虛擬機：`vagrant up`
2. 在 Windows 的 VS Code 中編輯專案檔案
3. 開啟另一個終端，登入虛擬機：`vagrant ssh`
4. 在虛擬機內運行：`cd spring-boot-demo && mvn spring-boot:run`
5. 檔案會自動同步到虛擬機

**優點**：可以使用 Windows 的 VS Code 編輯器，同時在 Linux 環境運行

## 測試 API

### 使用 curl（在虛擬機內）

```bash
# 取得所有用戶
curl http://localhost:8080/api/users

# 創建新用戶
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"測試用戶","email":"test@example.com","phone":"0900-000-000"}'

# 查詢特定用戶
curl http://localhost:8080/api/users/1
```

### 使用瀏覽器或 Postman（在 Windows 上）

直接訪問 `http://localhost:8080` 即可，埠號已對應到主機。

## 常見問題

### Vagrant 無法啟動

**問題**：執行 `vagrant up` 時出現錯誤

**解決方案**：
1. 確認 VirtualBox 已正確安裝
2. 檢查 BIOS 是否啟用虛擬化技術（VT-x/AMD-V）
3. 確認沒有其他虛擬化軟體衝突（如 Hyper-V）

### 無法訪問 http://localhost:8080

**問題**：在 Windows 瀏覽器無法訪問應用程式

**解決方案**：
1. 確認虛擬機內的應用程式正在運行
2. 檢查防火牆設定
3. 嘗試使用 `http://192.168.10.10:8080`（虛擬機的私有 IP）

### 檔案同步問題

**問題**：在 Windows 修改的檔案未同步到虛擬機

**解決方案**：
```powershell
# 重新載入虛擬機
vagrant reload
```

### Maven 無法下載依賴項

如果在虛擬機內遇到網路問題，可以配置 Maven 鏡像：

```bash
# 在虛擬機內執行
mkdir -p ~/.m2
cat > ~/.m2/settings.xml << EOF
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF
```

### 虛擬機效能問題

如果虛擬機運行緩慢，可以調整 `Vagrantfile` 中的資源配置：

```ruby
config.vm.provider "virtualbox" do |vb|
  vb.memory = "4096"  # 增加記憶體到 4GB
  vb.cpus = 4         # 增加 CPU 核心數
end
```

修改後執行：
```powershell
vagrant reload
```

## VS Code 擴充功能

專案已自動安裝以下擴充功能（在 Windows 的 VS Code 中）：
- Extension Pack for Java
- Spring Boot Dashboard
- Spring Boot Tools

如果需要在虛擬機內使用 VS Code，可以使用 Remote-SSH 擴充功能連接到虛擬機。

## 下一步

請參閱 [README.md](README.md) 了解專案結構和 API 使用方法。
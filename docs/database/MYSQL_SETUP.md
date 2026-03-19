# MySQL 開發環境設定指南

## 概述
此專案現在配置為使用 Vagrant 虛擬機中的 MySQL 資料庫，取代原本的 SQLite。

## 環境配置

### MySQL 資料庫資訊
- **主機**: localhost:3306 (從 Windows 主機訪問)
- **資料庫名稱**: spring_boot_demo
- **應用程式用戶**: springboot / springboot123
- **Root 用戶**: root / rootpassword

### Vagrant 虛擬機資訊
- **IP 地址**: 192.168.10.10
- **SSH**: vagrant ssh
- **埠對應**: 
  - 8080 (Spring Boot) -> localhost:8080
  - 3306 (MySQL) -> localhost:3306

## 快速開始

### 1. 啟動開發環境
```bash
# Windows
start-dev.bat

# Linux/Mac
./start-dev.sh
```

### 2. 手動啟動步驟
```bash
# 1. 啟動 Vagrant
vagrant up

# 2. 測試 MySQL 連線
test-mysql.bat  # Windows

# 3. SSH 到虛擬機
vagrant ssh

# 4. 在虛擬機內啟動應用程式
cd /home/vagrant/spring-boot-demo
mvn clean spring-boot:run -Dspring-boot.run.profiles=mysql
```

### 3. 驗證部署
- 應用程式: http://localhost:8080
- API 文件: http://localhost:8080/swagger-ui.html
- 健康檢查: http://localhost:8080/actuator/health

## 資料庫管理

### 連接 MySQL
```bash
# 從虛擬機內部
mysql -u springboot -pspringboot123 spring_boot_demo

# 從 Windows 主機 (需要 MySQL 客戶端)
mysql -h localhost -P 3306 -u springboot -pspringboot123 spring_boot_demo
```

### 重置資料庫
```sql
-- 連接為 root 用戶
DROP DATABASE IF EXISTS spring_boot_demo;
CREATE DATABASE spring_boot_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON spring_boot_demo.* TO 'springboot'@'%';
FLUSH PRIVILEGES;
```

## 配置檔案

### application.properties
- 主要配置檔案，現在指向 MySQL
- HikariCP 連線池配置已優化

### application-mysql.properties
- MySQL 專用配置檔案
- 使用 `-Dspring-boot.run.profiles=mysql` 啟用

## 疑難排解

### MySQL 連線問題
1. 檢查 Vagrant 狀態: `vagrant status`
2. 測試 MySQL 服務: `vagrant ssh -c "sudo systemctl status mysql"`
3. 測試連線: `vagrant ssh -c "mysql -u springboot -pspringboot123 -e 'SELECT 1;'"`

### 應用程式啟動問題
1. 檢查 Java 版本: `vagrant ssh -c "java -version"`
2. 清理並重新編譯: `vagrant ssh -c "cd /home/vagrant/spring-boot-demo && mvn clean compile"`
3. 檢查資料庫連線: 查看 Spring Boot 啟動日誌

### 重新佈建 Vagrant
```bash
vagrant destroy -f
vagrant up
```

## 開發注意事項

1. **資料持久性**: 資料庫資料存儲在虛擬機中，`vagrant destroy` 會清除所有資料
2. **連接池**: 已配置 HikariCP 連接池，適合開發環境使用
3. **字符編碼**: 使用 UTF-8 編碼，支援中文
4. **時區設定**: 設定為 Asia/Taipei

## 生產環境遷移

要部署到生產環境，請：
1. 修改 `application-prod.properties` 中的資料庫連線資訊
2. 使用環境變數設定敏感資訊（密碼等）
3. 調整 HikariCP 連接池參數
4. 啟用 SSL 連線
# Docker 容器訪問指南

## 🚀 快速訪問

### 方法 1: 使用訪問工具 (推薦)
```cmd
# Windows 主機執行
.\docker-access.bat
```

### 方法 2: 直接命令

#### 1️⃣ 進入 Spring Boot 容器
```cmd
# 使用 bash shell
vagrant ssh -c "docker exec -it spring-boot-app bash"

# 使用 sh shell (如果 bash 不可用)
vagrant ssh -c "docker exec -it spring-boot-app sh"

# 以 root 用戶進入
vagrant ssh -c "docker exec -u root -it spring-boot-app bash"
```

**容器內可執行操作:**
```bash
# 查看應用日誌
cat /app/logs/spring-boot-demo.log

# 查看運行的 Java 進程
ps aux | grep java

# 查看應用 JAR 文件
ls -lh /app/*.jar

# 查看 JVM 記憶體使用
java -XX:+PrintFlagsFinal -version | grep -i heapsize
```

---

#### 2️⃣ 進入 MySQL 容器
```cmd
# 連接到 MySQL CLI
vagrant ssh -c "docker exec -it spring-boot-mysql mysql -u springboot -p spring_boot_demo"
# 密碼: springboot123

# 以 root 用戶連接
vagrant ssh -c "docker exec -it spring-boot-mysql mysql -u root -p"
# 密碼: rootpassword
```

**MySQL 查詢範例:**
```sql
-- 顯示所有表
SHOW TABLES;

-- 查看用戶表
SELECT * FROM users LIMIT 10;

-- 查看表結構
DESCRIBE users;

-- 查看資料庫大小
SELECT 
    table_schema AS '數據庫',
    SUM(data_length + index_length) / 1024 / 1024 AS '大小(MB)' 
FROM information_schema.tables 
WHERE table_schema = 'spring_boot_demo'
GROUP BY table_schema;
```

---

#### 3️⃣ 進入 Redis 容器
```cmd
# 連接到 Redis CLI
vagrant ssh -c "docker exec -it spring-boot-redis redis-cli"
```

**Redis 命令範例:**
```bash
# 查看所有 key
KEYS *

# 查看 key 數量
DBSIZE

# 查看 Redis 資訊
INFO

# 查看記憶體使用
INFO memory

# 測試連接
PING

# 獲取某個 key 的值
GET your_key_name

# 清空當前資料庫
FLUSHDB
```

---

## 📊 監控與日誌

### 查看容器日誌
```cmd
# 查看 Spring Boot 應用日誌 (實時)
vagrant ssh -c "cd /vagrant && docker compose logs -f app"

# 查看最近 100 行
vagrant ssh -c "cd /vagrant && docker compose logs --tail 100 app"

# 查看所有容器日誌
vagrant ssh -c "cd /vagrant && docker compose logs -f"

# 查看 MySQL 日誌
vagrant ssh -c "cd /vagrant && docker compose logs -f mysql"

# 查看 Redis 日誌
vagrant ssh -c "cd /vagrant && docker compose logs -f redis"
```

### 查看容器狀態
```cmd
# 查看所有容器狀態
vagrant ssh -c "cd /vagrant && docker compose ps"

# 查看容器資源使用
vagrant ssh -c "docker stats --no-stream"

# 查看容器詳細資訊
vagrant ssh -c "docker inspect spring-boot-app"
```

---

## 🌐 從主機訪問服務

### HTTP API 訪問
```cmd
# Windows PowerShell
# 健康檢查
Invoke-WebRequest http://localhost:8080/actuator/health

# 查看 API 文檔
start http://localhost:8080/swagger-ui.html

# 測試 API
Invoke-RestMethod -Uri http://localhost:8080/api/test -Method Get
```

### 資料庫訪問
```cmd
# 使用 MySQL Workbench 或其他工具連接
主機: localhost
端口: 3307
用戶: springboot
密碼: springboot123
資料庫: spring_boot_demo
```

### Redis 訪問
```cmd
# 使用 Redis Desktop Manager 或其他工具連接
主機: localhost
端口: 6379
```

---

## 🔧 常用操作

### 重啟容器
```cmd
# 重啟 Spring Boot 應用
vagrant ssh -c "cd /vagrant && docker compose restart app"

# 重啟所有容器
vagrant ssh -c "cd /vagrant && docker compose restart"
```

### 停止/啟動容器
```cmd
# 停止所有容器
vagrant ssh -c "cd /vagrant && docker compose stop"

# 啟動所有容器
vagrant ssh -c "cd /vagrant && docker compose start"

# 停止並移除容器
vagrant ssh -c "cd /vagrant && docker compose down"
```

### 執行單次命令
```cmd
# 在容器內執行命令 (不進入 shell)
vagrant ssh -c "docker exec spring-boot-app ls -la /app"

# 查看 Java 版本
vagrant ssh -c "docker exec spring-boot-app java -version"

# 查看應用端口監聽
vagrant ssh -c "docker exec spring-boot-app netstat -tlnp"
```

---

## 📁 文件傳輸

### 從容器複製文件到主機
```cmd
# 複製日誌文件
vagrant ssh -c "docker cp spring-boot-app:/app/logs/spring-boot-demo.log /vagrant/logs/"

# 複製配置文件
vagrant ssh -c "docker cp spring-boot-app:/app/application.properties /vagrant/"
```

### 從主機複製文件到容器
```cmd
# 複製配置文件
vagrant ssh -c "docker cp /vagrant/config.yml spring-boot-app:/app/"
```

---

## 🐛 故障排查

### 應用無法啟動
```cmd
# 查看完整日誌
vagrant ssh -c "cd /vagrant && docker compose logs app"

# 檢查容器是否在運行
vagrant ssh -c "docker ps -a | grep spring-boot"

# 查看容器退出原因
vagrant ssh -c "docker inspect spring-boot-app --format='{{.State.Status}}'"
```

### 網路問題
```cmd
# 測試容器間網路
vagrant ssh -c "docker exec spring-boot-app ping -c 3 mysql"
vagrant ssh -c "docker exec spring-boot-app ping -c 3 redis"

# 查看網路配置
vagrant ssh -c "docker network inspect vagrant_spring-boot-network"
```

### 資料庫連接問題
```cmd
# 檢查 MySQL 是否就緒
vagrant ssh -c "docker exec spring-boot-mysql mysqladmin ping -h localhost -u root -prootpassword"

# 檢查 Redis 是否就緒
vagrant ssh -c "docker exec spring-boot-redis redis-cli ping"
```

---

## 💡 實用技巧

### 1. 在容器內安裝工具
```bash
# 進入容器後執行 (需要 root 權限)
docker exec -u root -it spring-boot-app bash

# 更新 apt 並安裝工具
apt-get update
apt-get install -y vim curl net-tools
```

### 2. 查看應用啟動時間
```cmd
vagrant ssh -c "docker exec spring-boot-app ps -eo pid,etime,cmd | grep java"
```

### 3. 監控記憶體使用
```cmd
vagrant ssh -c "docker exec spring-boot-app free -h"
```

### 4. 查看磁碟使用
```cmd
vagrant ssh -c "docker exec spring-boot-app df -h"
```

---

## 📚 相關文件

- [Docker 部署指南](DOCKER-GUIDE.md)
- [故障排查](TROUBLESHOOTING.md)
- [快速開始](QUICKSTART.md)

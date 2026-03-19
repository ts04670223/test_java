# Docker 容器化部署指南

## 📦 已創建的文件

- **Dockerfile** - 多階段構建（生產環境）
- **Dockerfile.simple** - 簡化版本（快速測試）
- **docker-compose.yml** - 完整環境（App + MySQL + Redis）
- **docker-compose.dev.yml** - 開發環境（僅資料庫）
- **.dockerignore** - 排除不需要的文件

## 🚀 快速開始

### 方法一：使用 Docker Compose（推薦）

一鍵啟動整個應用（包含 MySQL + Redis + Spring Boot）：

```bash
# 構建並啟動所有服務
docker-compose up -d

# 查看日誌
docker-compose logs -f app

# 停止所有服務
docker-compose down
```

訪問應用：http://localhost:8080

### 方法二：僅啟動資料庫（本地開發）

```bash
# 只啟動 MySQL 和 Redis
docker-compose -f docker-compose.dev.yml up -d

# 本地運行 Spring Boot
mvn spring-boot:run
```

### 方法三：手動構建和運行

```bash
# 1. 構建 Docker 映像
docker build -t spring-boot-demo:latest .

# 2. 創建網路
docker network create spring-network

# 3. 啟動 MySQL
docker run -d \
  --name mysql \
  --network spring-network \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=spring_boot_demo \
  -e MYSQL_USER=springboot \
  -e MYSQL_PASSWORD=springboot123 \
  -p 3306:3306 \
  mysql:8.0

# 4. 啟動 Redis
docker run -d \
  --name redis \
  --network spring-network \
  -p 6379:6379 \
  redis:7-alpine

# 5. 啟動應用
docker run -d \
  --name spring-boot-app \
  --network spring-network \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/spring_boot_demo?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Taipei \
  -e SPRING_DATASOURCE_USERNAME=springboot \
  -e SPRING_DATASOURCE_PASSWORD=springboot123 \
  -e SPRING_REDIS_HOST=redis \
  -p 8080:8080 \
  spring-boot-demo:latest
```

## 🔧 常用命令

### Docker Compose 操作

```bash
# 啟動服務（背景執行）
docker-compose up -d

# 啟動服務（前景執行，查看日誌）
docker-compose up

# 重新構建並啟動
docker-compose up -d --build

# 查看運行狀態
docker-compose ps

# 查看日誌
docker-compose logs -f
docker-compose logs -f app    # 只查看應用日誌
docker-compose logs -f mysql  # 只查看 MySQL 日誌

# 停止服務
docker-compose stop

# 停止並刪除容器
docker-compose down

# 停止並刪除容器+資料卷
docker-compose down -v

# 重啟特定服務
docker-compose restart app
```

### Docker 映像管理

```bash
# 構建映像
docker build -t spring-boot-demo:latest .
docker build -t spring-boot-demo:v1.0 .

# 使用簡化版 Dockerfile
docker build -f Dockerfile.simple -t spring-boot-demo:simple .

# 查看映像
docker images | grep spring-boot

# 刪除映像
docker rmi spring-boot-demo:latest

# 清理未使用的映像
docker image prune -a
```

### 容器操作

```bash
# 進入容器
docker exec -it spring-boot-app bash

# 查看容器日誌
docker logs -f spring-boot-app

# 查看容器資源使用
docker stats spring-boot-app

# 停止容器
docker stop spring-boot-app

# 刪除容器
docker rm spring-boot-app
```

## 📊 服務端口映射

| 服務 | 容器端口 | 主機端口 | 訪問地址 |
|------|---------|---------|---------|
| Spring Boot | 8080 | 8080 | http://localhost:8080 |
| MySQL | 3306 | 3306 | localhost:3306 |
| Redis | 6379 | 6379 | localhost:6379 |

## 🔍 健康檢查

```bash
# 檢查應用健康狀態
curl http://localhost:8080/actuator/health

# 檢查 MySQL 連接
docker exec -it spring-boot-mysql mysql -uspringboot -pspringboot123 -e "SELECT 1"

# 檢查 Redis 連接
docker exec -it spring-boot-redis redis-cli ping
```

## 🐛 除錯和疑難排解

### 查看應用日誌

```bash
# Docker Compose
docker-compose logs -f app

# 直接查看容器
docker logs -f spring-boot-app

# 查看最近 100 行
docker logs --tail 100 spring-boot-app
```

### 進入容器除錯

```bash
# 進入應用容器
docker exec -it spring-boot-app bash

# 進入 MySQL 容器
docker exec -it spring-boot-mysql bash

# 連接 MySQL 資料庫
docker exec -it spring-boot-mysql mysql -uspringboot -pspringboot123 spring_boot_demo
```

### 常見問題

#### 問題 1: 容器啟動失敗

```bash
# 查看詳細錯誤
docker-compose logs app

# 檢查容器狀態
docker-compose ps
```

#### 問題 2: 無法連接資料庫

```bash
# 確認 MySQL 已啟動
docker-compose ps mysql

# 測試 MySQL 連接
docker exec -it spring-boot-mysql mysqladmin ping -h localhost -u root -prootpassword
```

#### 問題 3: 端口衝突

```bash
# 修改 docker-compose.yml 中的端口映射
# 例如：將 "8080:8080" 改為 "8081:8080"
```

#### 問題 4: 資料庫初始化問題

```bash
# 清除並重新創建資料庫
docker-compose down -v
docker-compose up -d
```

## 🔄 更新應用

```bash
# 1. 重新構建映像
docker-compose build app

# 2. 重啟應用容器
docker-compose up -d app

# 或一步完成
docker-compose up -d --build app
```

## 💾 資料持久化

Docker Compose 會自動創建以下資料卷：

- **mysql-data** - MySQL 資料庫文件
- **redis-data** - Redis 持久化數據

```bash
# 查看資料卷
docker volume ls

# 備份 MySQL 資料
docker exec spring-boot-mysql mysqldump -uspringboot -pspringboot123 spring_boot_demo > backup.sql

# 恢復 MySQL 資料
docker exec -i spring-boot-mysql mysql -uspringboot -pspringboot123 spring_boot_demo < backup.sql
```

## 🌐 環境變數配置

可以創建 `.env` 文件來自定義配置：

```env
# .env 文件
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=spring_boot_demo
MYSQL_USER=springboot
MYSQL_PASSWORD=your_password

APP_PORT=8080
MYSQL_PORT=3306
REDIS_PORT=6379
```

然後在 docker-compose.yml 中引用：

```yaml
environment:
  MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
  MYSQL_DATABASE: ${MYSQL_DATABASE}
```

## 🚢 部署到生產環境

### 1. 使用特定版本標籤

```bash
# 構建標記版本
docker build -t spring-boot-demo:1.0.0 .

# 推送到 Docker Hub
docker tag spring-boot-demo:1.0.0 yourusername/spring-boot-demo:1.0.0
docker push yourusername/spring-boot-demo:1.0.0
```

### 2. 生產環境配置

創建 `docker-compose.prod.yml`：

```yaml
version: '3.8'

services:
  app:
    image: spring-boot-demo:1.0.0
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: production
      # 使用環境變數管理敏感資訊
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

### 3. 使用外部資料庫

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:mysql://external-db-host:3306/dbname
```

## 📝 最佳實踐

1. **使用多階段構建** - 減小最終映像大小
2. **使用 .dockerignore** - 排除不必要的文件
3. **健康檢查** - 確保容器正常運行
4. **資料持久化** - 使用 volumes 保存資料
5. **環境變數** - 管理不同環境的配置
6. **資源限制** - 設置 CPU 和記憶體限制
7. **日誌管理** - 配置日誌驅動和輪替
8. **網路隔離** - 使用自定義網路

## 🎯 下一步

- [ ] 添加 Nginx 反向代理
- [ ] 配置 SSL/TLS
- [ ] 設置 CI/CD 管道
- [ ] 添加監控（Prometheus + Grafana）
- [ ] 配置日誌聚合（ELK Stack）

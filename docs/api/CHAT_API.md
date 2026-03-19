# 聊天功能 API 文件

## 概述
這是一個基於 Spring Boot 和 SQLite 的聊天系統，提供完整的訊息發送、接收和管理功能。

## 資料庫
- 使用 SQLite 資料庫儲存聊天訊息
- 資料庫檔案：`chat.db` (自動建立於專案根目錄)
- 資料表會在應用程式啟動時自動建立

## API 端點

### 1. 發送訊息
```
POST /api/chat/send
Content-Type: application/json

{
  "senderId": 1,
  "receiverId": 2,
  "message": "你好！"
}
```

### 2. 取得兩個用戶之間的聊天記錄
```
GET /api/chat/history?user1=1&user2=2
```

### 3. 取得用戶的未讀訊息
```
GET /api/chat/unread/{userId}
```
範例：`GET /api/chat/unread/1`

### 4. 取得用戶的未讀訊息數量
```
GET /api/chat/unread-count/{userId}
```
範例：`GET /api/chat/unread-count/1`

### 5. 標記單一訊息為已讀
```
PUT /api/chat/read/{messageId}
```
範例：`PUT /api/chat/read/123`

### 6. 標記與特定用戶的對話為已讀
```
PUT /api/chat/read-chat?receiverId=1&senderId=2
```

### 7. 取得用戶發送的所有訊息
```
GET /api/chat/sent/{userId}
```
範例：`GET /api/chat/sent/1`

### 8. 取得用戶接收的所有訊息
```
GET /api/chat/received/{userId}
```
範例：`GET /api/chat/received/1`

### 9. 取得用戶所有相關訊息（發送+接收）
```
GET /api/chat/all/{userId}
```
範例：`GET /api/chat/all/1`

### 10. 刪除訊息
```
DELETE /api/chat/{messageId}
```
範例：`DELETE /api/chat/123`

### 11. 取得訊息詳情
```
GET /api/chat/message/{messageId}
```
範例：`GET /api/chat/message/123`

## 訊息資料結構

```json
{
  "id": 1,
  "senderId": 1,
  "receiverId": 2,
  "message": "你好！",
  "timestamp": "2025-10-30T10:30:00",
  "isRead": false
}
```

## 使用範例

### 使用 curl 發送訊息
```bash
curl -X POST http://localhost:8080/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": 1,
    "receiverId": 2,
    "message": "測試訊息"
  }'
```

### 使用 curl 查詢聊天記錄
```bash
curl "http://localhost:8080/api/chat/history?user1=1&user2=2"
```

### 使用 curl 查詢未讀訊息
```bash
curl http://localhost:8080/api/chat/unread/1
```

## 啟動應用程式

### 使用 Maven
```bash
mvn spring-boot:run
```

### 使用 VS Code Task
執行 "Spring Boot: Run" 任務

應用程式將在 `http://localhost:8080` 啟動。

## 注意事項

1. SQLite 資料庫檔案 `chat.db` 會自動建立在專案根目錄
2. 資料表結構會在第一次啟動時自動建立（`ddl-auto=update`）
3. 所有時間戳記使用 `LocalDateTime` 格式
4. 訊息內容最大長度為 2000 字元
5. 所有 API 都包含錯誤處理和驗證

## 功能特點

- ✅ 發送和接收訊息
- ✅ 查詢聊天記錄
- ✅ 未讀訊息管理
- ✅ 訊息已讀標記
- ✅ 訊息刪除功能
- ✅ 完整的錯誤處理
- ✅ 資料驗證
- ✅ RESTful API 設計

# 電商購物 API 文件

## 概述
此 API 提供完整的電商購物功能，包含商品管理、購物車和訂單處理。

## 基礎 URL
```
http://localhost:8080/api
```

## 功能模組

### 1. 商品管理 (Products)

#### 取得所有啟用的商品
```
GET /products
```

#### 根據ID取得商品
```
GET /products/{id}
```

#### 根據類別查詢商品
```
GET /products/category/{category}
```

#### 搜尋商品
```
GET /products/search?keyword={keyword}
```

#### 取得有庫存的商品
```
GET /products/available
```

#### 創建商品（管理員）
```
POST /products
Content-Type: application/json

{
  "name": "商品名稱",
  "description": "商品描述",
  "price": 999.99,
  "stock": 100,
  "imageUrl": "https://example.com/image.jpg",
  "category": "電子產品",
  "active": true
}
```

#### 更新商品（管理員）
```
PUT /products/{id}
Content-Type: application/json

{
  "name": "更新的商品名稱",
  "description": "更新的描述",
  "price": 1099.99,
  "stock": 150,
  "imageUrl": "https://example.com/image2.jpg",
  "category": "電子產品",
  "active": true
}
```

#### 刪除商品（管理員 - 軟刪除）
```
DELETE /products/{id}
```

### 2. 購物車 (Cart)

#### 取得用戶的購物車
```
GET /cart/{userId}
```

#### 添加商品到購物車
```
POST /cart/{userId}/items?productId={productId}&quantity={quantity}
```
參數:
- `productId`: 商品ID
- `quantity`: 數量（預設為 1）

#### 更新購物車項目數量
```
PUT /cart/{userId}/items/{cartItemId}?quantity={quantity}
```

#### 從購物車移除項目
```
DELETE /cart/{userId}/items/{cartItemId}
```

#### 清空購物車
```
DELETE /cart/{userId}
```

#### 取得購物車總金額
```
GET /cart/{userId}/total
```
回應:
```json
{
  "total": 2999.99,
  "itemCount": 5
}
```

### 3. 訂單 (Orders)

#### 從購物車建立訂單
```
POST /orders?userId={userId}&shippingAddress={address}&phone={phone}&note={note}
```
參數:
- `userId`: 用戶ID
- `shippingAddress`: 送貨地址
- `phone`: 聯絡電話
- `note`: 訂單備註（選填）

#### 取得用戶的所有訂單
```
GET /orders/user/{userId}
```

#### 根據ID取得訂單
```
GET /orders/{orderId}
```

#### 根據訂單編號取得訂單
```
GET /orders/number/{orderNumber}
```

#### 更新訂單狀態（管理員）
```
PUT /orders/{orderId}/status?status={status}
```
狀態選項:
- `PENDING`: 待處理
- `PROCESSING`: 處理中
- `SHIPPED`: 已出貨
- `DELIVERED`: 已送達
- `CANCELLED`: 已取消
- `REFUNDED`: 已退款

#### 取消訂單
```
POST /orders/{orderId}/cancel?userId={userId}
```

#### 取得所有訂單（管理員）
```
GET /orders
```

#### 根據狀態查詢訂單（管理員）
```
GET /orders/status/{status}
```

## 資料模型

### Product (商品)
```json
{
  "id": 1,
  "name": "商品名稱",
  "description": "商品描述",
  "price": 999.99,
  "stock": 100,
  "imageUrl": "https://example.com/image.jpg",
  "category": "電子產品",
  "active": true,
  "createdAt": "2025-10-31T10:00:00",
  "updatedAt": "2025-10-31T10:00:00"
}
```

### Cart (購物車)
```json
{
  "id": 1,
  "user": {
    "id": 1,
    "username": "user1",
    "name": "使用者"
  },
  "items": [
    {
      "id": 1,
      "product": { ... },
      "quantity": 2,
      "price": 999.99,
      "subtotal": 1999.98
    }
  ],
  "createdAt": "2025-10-31T10:00:00",
  "updatedAt": "2025-10-31T10:00:00"
}
```

### Order (訂單)
```json
{
  "id": 1,
  "orderNumber": "ORD1730329200000",
  "user": {
    "id": 1,
    "username": "user1",
    "name": "使用者"
  },
  "totalAmount": 2999.99,
  "status": "PENDING",
  "shippingAddress": "台北市信義區信義路五段7號",
  "phone": "0912345678",
  "note": "請小心包裝",
  "items": [
    {
      "id": 1,
      "product": { ... },
      "quantity": 2,
      "price": 999.99,
      "subtotal": 1999.98
    }
  ],
  "createdAt": "2025-10-31T10:00:00",
  "updatedAt": "2025-10-31T10:00:00"
}
```

## 錯誤回應
所有錯誤都會回傳 JSON 格式:
```json
{
  "error": "錯誤訊息描述"
}
```

## 狀態碼
- `200 OK`: 請求成功
- `201 Created`: 資源創建成功
- `400 Bad Request`: 請求參數錯誤
- `404 Not Found`: 資源不存在
- `500 Internal Server Error`: 伺服器內部錯誤

## 使用範例

### 1. 建立商品
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro",
    "description": "最新款 iPhone",
    "price": 35900,
    "stock": 50,
    "category": "手機",
    "active": true
  }'
```

### 2. 添加商品到購物車
```bash
curl -X POST "http://localhost:8080/api/cart/1/items?productId=1&quantity=2"
```

### 3. 建立訂單
```bash
curl -X POST "http://localhost:8080/api/orders?userId=1&shippingAddress=台北市信義區&phone=0912345678&note=盡快送達"
```

### 4. 查詢用戶訂單
```bash
curl http://localhost:8080/api/orders/user/1
```

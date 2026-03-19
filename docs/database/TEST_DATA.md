# 電商測試數據腳本

## 添加示例商品

使用以下 curl 命令添加測試商品:

```bash
# 商品 1 - iPhone 15 Pro
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro",
    "description": "最新款 iPhone,配備 A17 Pro 晶片,鈦金屬設計",
    "price": 36900,
    "stock": 50,
    "category": "手機",
    "imageUrl": "https://via.placeholder.com/300x300?text=iPhone+15+Pro",
    "active": true
  }'

# 商品 2 - MacBook Pro
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro 14吋",
    "description": "M3 Pro 晶片,16GB 記憶體,512GB SSD",
    "price": 62900,
    "stock": 30,
    "category": "筆記型電腦",
    "imageUrl": "https://via.placeholder.com/300x300?text=MacBook+Pro",
    "active": true
  }'

# 商品 3 - AirPods Pro
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "AirPods Pro (第2代)",
    "description": "主動降噪,空間音訊,USB-C 充電",
    "price": 7990,
    "stock": 100,
    "category": "耳機",
    "imageUrl": "https://via.placeholder.com/300x300?text=AirPods+Pro",
    "active": true
  }'

# 商品 4 - iPad Air
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPad Air 10.9吋",
    "description": "M1 晶片,64GB 儲存空間,支援 Apple Pencil",
    "price": 19900,
    "stock": 45,
    "category": "平板電腦",
    "imageUrl": "https://via.placeholder.com/300x300?text=iPad+Air",
    "active": true
  }'

# 商品 5 - Apple Watch
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Apple Watch Series 9",
    "description": "GPS + 行動網路,45mm,午夜色鋁金屬錶殼",
    "price": 16400,
    "stock": 60,
    "category": "穿戴裝置",
    "imageUrl": "https://via.placeholder.com/300x300?text=Apple+Watch",
    "active": true
  }'

# 商品 6 - Magic Keyboard
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Magic Keyboard",
    "description": "無線藍牙鍵盤,內建充電電池",
    "price": 3490,
    "stock": 80,
    "category": "配件",
    "imageUrl": "https://via.placeholder.com/300x300?text=Magic+Keyboard",
    "active": true
  }'

# 商品 7 - Magic Mouse
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Magic Mouse",
    "description": "無線藍牙滑鼠,Multi-Touch 表面",
    "price": 2590,
    "stock": 90,
    "category": "配件",
    "imageUrl": "https://via.placeholder.com/300x300?text=Magic+Mouse",
    "active": true
  }'

# 商品 8 - HomePod mini
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "HomePod mini",
    "description": "智慧音箱,支援 Siri,360度音效",
    "price": 3000,
    "stock": 70,
    "category": "音箱",
    "imageUrl": "https://via.placeholder.com/300x300?text=HomePod+mini",
    "active": true
  }'
```

## PowerShell 版本

```powershell
# 設定 API URL
$apiUrl = "http://localhost:8080/api/products"

# 商品數據
$products = @(
  @{name="iPhone 15 Pro";description="最新款 iPhone,配備 A17 Pro 晶片";price=36900;stock=50;category="手機";active=$true},
  @{name="MacBook Pro 14吋";description="M3 Pro 晶片,16GB RAM,512GB SSD";price=62900;stock=30;category="筆記型電腦";active=$true},
  @{name="AirPods Pro (第2代)";description="主動降噪,空間音訊";price=7990;stock=100;category="耳機";active=$true},
  @{name="iPad Air";description="M1 晶片,64GB 儲存空間";price=19900;stock=45;category="平板電腦";active=$true},
  @{name="Apple Watch Series 9";description="GPS + 行動網路,45mm";price=16400;stock=60;category="穿戴裝置";active=$true},
  @{name="Magic Keyboard";description="無線藍牙鍵盤";price=3490;stock=80;category="配件";active=$true},
  @{name="Magic Mouse";description="無線藍牙滑鼠";price=2590;stock=90;category="配件";active=$true},
  @{name="HomePod mini";description="智慧音箱,支援 Siri";price=3000;stock=70;category="音箱";active=$true}
)

foreach ($product in $products) {
  $json = $product | ConvertTo-Json
  Invoke-RestMethod -Uri $apiUrl -Method Post -Body $json -ContentType "application/json"
  Write-Host "已添加: $($product.name)"
}
```

## 測試電商功能

1. **啟動後端**: 確保 Spring Boot 應用在 port 8080 運行
2. **啟動前端**: `cd frontend && npm run dev`
3. **添加測試商品**: 執行上面的腳本
4. **訪問商城**: http://localhost:3001/shop
5. **測試流程**:
   - 瀏覽商品
   - 加入購物車
   - 調整數量
   - 結帳
   - 查看訂單

## API 端點測試

```bash
# 查看所有商品
curl http://localhost:8080/api/products

# 搜尋商品
curl http://localhost:8080/api/products/search?keyword=iPhone

# 按分類查詢
curl http://localhost:8080/api/products/category/手機

# 查看購物車
curl http://localhost:8080/api/cart/user/1

# 查看訂單
curl http://localhost:8080/api/orders/user/1
```

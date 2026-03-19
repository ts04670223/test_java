# 🔧 Kong 路由修復記錄

## 問題描述

**時間**: 2026-01-22  
**症狀**: 訪問 http://test6.test/api/products 出現錯誤
```json
{
  "message": "no Route matched with those values",
  "request_id": "8a918e4ba5b75214354a371ba35bf109"
}
```

## 根本原因

Kong Gateway 缺少 Spring Boot 應用的路由配置：
- ✅ Prometheus 路由存在
- ✅ Grafana 路由存在  
- ❌ **app-service 路由遺失**

## 解決方案

### 1. 診斷步驟

```bash
# 檢查 Kong 服務
vagrant ssh -c "curl -s http://localhost:30003/services"
# 結果: 只有 grafana 和 prometheus，沒有 spring-boot-app

# 檢查 Spring Boot Pod
vagrant ssh -c "kubectl get pods -l app=app"
# 結果: app Pod 正常運行 (2/2 READY)

# 檢查 Kubernetes 服務
vagrant ssh -c "kubectl get svc"
# 結果: app service 存在於 ClusterIP 10.103.117.29:8080
```

### 2. 修復操作

重新執行 Kong 路由配置腳本：

```bash
vagrant ssh -c "bash /vagrant/kong/setup-routes.sh"
```

**配置內容**:
- Service: `spring-boot-app` → `app.default.svc.cluster.local:8080`
- Route: `api-route` → paths=["/api"], strip_path=false

### 3. 驗證結果

```bash
# 測試 Kong 直接訪問
vagrant ssh -c "curl http://localhost:30000/api/products"
# ✅ HTTP 200, 返回商品 JSON

# 測試 test6.test 訪問
curl http://test6.test/api/products
# ✅ HTTP 200, 返回商品 JSON

# 完整測試
.\tools\test-test6.bat
# ✅ 所有服務 (前端/API/Prometheus/Grafana) 都返回 200
```

## Kong 路由配置

### 當前配置

**Services**:
```
1. grafana → grafana.monitoring.svc.cluster.local:3000
2. prometheus → prometheus.monitoring.svc.cluster.local:9090
3. spring-boot-app → app.default.svc.cluster.local:8080
```

**Routes**:
```
1. api-route → paths=["/api"], strip_path=false
2. grafana-route → paths=["/grafana"], strip_path=false
3. prometheus-route → paths=["/prometheus"], strip_path=true
```

### 配置腳本

**位置**: `kong/setup-routes.sh`

**用途**: 配置 Spring Boot API 路由

**執行**:
```bash
# Linux/Mac
vagrant ssh -c "bash /vagrant/kong/setup-routes.sh"

# Windows
vagrant ssh -c "bash /vagrant/kong/setup-routes.sh"
```

## 預防措施

### 1. 路由健康檢查

創建定期檢查腳本：

```bash
# 檢查所有 Kong 路由
vagrant ssh -c "curl -s http://localhost:30003/routes | jq '.data[] | {name, paths}'"

# 檢查所有 Kong 服務
vagrant ssh -c "curl -s http://localhost:30003/services | jq '.data[] | {name, host, port}'"
```

### 2. 自動恢復

如果發現路由遺失，執行：

```bash
# 重新配置 API 路由
bash kong/setup-routes.sh

# 重新配置監控路由
bash kong/setup-monitoring-routes.sh
```

### 3. 測試套件

使用 `tools/test-test6.bat` 定期驗證所有服務：

```cmd
cd c:\JOHNY\test
.\tools\test-test6.bat
```

預期輸出：
```
[1/4] 測試前端...        HTTP 200 ✅
[2/4] 測試後端 API...    HTTP 200 ✅
[3/4] 測試 Prometheus... HTTP 200 ✅
[4/4] 測試 Grafana...    HTTP 302 ✅
```

## 相關文件

- [Kong 路由配置腳本](../kong/setup-routes.sh)
- [監控路由配置腳本](../kong/setup-monitoring-routes.sh)
- [統一訪問指南](../TEST6-UNIFIED-ACCESS.md)
- [Kong 狀態文檔](../KONG-STATUS.md)

## 總結

✅ **問題已解決**: Kong 路由配置遺失  
✅ **修復方法**: 重新執行路由配置腳本  
✅ **驗證完成**: 所有服務正常訪問  
✅ **文檔更新**: 所有相關文檔已同步

---

**修復時間**: 2026-01-22 17:53  
**執行者**: GitHub Copilot  
**狀態**: ✅ 已解決

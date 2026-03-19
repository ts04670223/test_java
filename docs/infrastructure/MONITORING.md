# 監控系統訪問指南

所有服務統一整合至 **http://test6.test/** 入口，透過 Nginx + Kong Gateway 實現路由。

## 🌐 服務訪問地址

| 服務 | 統一入口（推薦） | 直接 NodePort（備用） |
|------|----------------|---------------------|
| **前端** | http://test6.test/ | — |
| **後端 API** | http://test6.test/api | http://localhost:30080 |
| **Prometheus** | http://test6.test/prometheus | http://localhost:30090 |
| **Grafana** | http://test6.test/grafana | http://localhost:30300 |

> **Grafana 帳號**: admin / NewAdminPassword123

## 🏗️ 架構流程

```
瀏覽器
    ↓
test6.test (Nginx:80)
    ↓
    ├─ / ────────────→ Vite (port 3000)
    ├─ /api ─────────→ Kong (port 30000) ─→ Spring Boot (K8s)
    ├─ /prometheus ──→ Kong (port 30000) ─→ Prometheus (monitoring ns)
    └─ /grafana ─────→ Kong (port 30000) ─→ Grafana (monitoring ns)
```

## 📋 前置條件

編輯 `C:\Windows\System32\drivers\etc\hosts`，加入：

```
192.168.10.10 test6.test
```

## 🔧 管理指令

### 確認服務狀態

```bash
vagrant ssh -c "kubectl get pods -n monitoring"
vagrant ssh -c "kubectl get pods | grep app"
vagrant ssh -c "kubectl get svc"
```

### 查看 Kong 路由

```bash
vagrant ssh -c "curl -s http://localhost:30003/routes | grep '\"name\"'"
```

### 重啟服務

```bash
# 重啟 Grafana
vagrant ssh -c "kubectl rollout restart deployment/grafana -n monitoring"

# 重啟 Prometheus
vagrant ssh -c "kubectl rollout restart deployment/prometheus -n monitoring"
```

## 📊 Prometheus 查詢範例

訪問 http://test6.test/prometheus/graph

```promql
# 所有運行的服務
up

# JVM 堆內存使用
jvm_memory_used_bytes{area="heap"}

# HTTP 請求率
rate(http_server_requests_seconds_count[5m])

# Pod CPU 使用
rate(container_cpu_usage_seconds_total{namespace="default"}[5m])

# HPA 副本數
kube_deployment_status_replicas{deployment="app"}
```

## 📊 Grafana 儀表板

訪問 http://test6.test/grafana，登入後 **Dashboards > Import** 輸入 ID：

| ID | 名稱 |
|----|------|
| 4701 | JVM Micrometer |
| 11378 | Spring Boot Statistics |
| 315 | Kubernetes Cluster |

## 🐛 故障排查

### 無法訪問 test6.test

確認 Windows hosts 文件已正確設定：
```cmd
notepad C:\Windows\System32\drivers\etc\hosts
```

### Prometheus/Grafana 404

檢查並重新配置 Kong 路由：
```bash
vagrant ssh -c "curl http://localhost:30003/routes | grep -A5 prometheus"
vagrant ssh -c "bash /vagrant/kong/setup-monitoring-routes.sh"
```

### Grafana 登入問題

改用直接 NodePort 訪問：http://localhost:30300

### Nginx 配置錯誤

```bash
vagrant ssh -c "sudo cp /etc/nginx/sites-available/test6.test.backup.* /etc/nginx/sites-available/test6.test && sudo systemctl reload nginx"
```

## 📁 相關配置文件

| 文件 | 說明 |
|------|------|
| `/etc/nginx/sites-available/test6.test` | Nginx 反向代理配置 |
| `kong/setup-monitoring-routes.sh` | Kong 路由設定腳本 |
| `scripts/update-nginx-monitoring.sh` | Nginx 更新腳本 |

## 🔐 安全注意事項

⚠️ **當前配置僅適用於開發環境**

生產環境建議：
1. 啟用 HTTPS (SSL/TLS)
2. 配置 Kong 認證插件 (JWT, OAuth2)
3. 修改 Grafana 預設密碼
4. 限制 Prometheus 訪問 (IP 白名單)
5. 啟用 Nginx 速率限制

## 📚 相關文檔

- [Kong 路由說明](../../kong/README-ROUTES.md)
- [Prometheus 入門](../../prometheus/00-START-HERE.md)
- [Grafana 使用指南](../../prometheus/GRAFANA-GUIDE.md)
- [故障排查](../troubleshooting/TROUBLESHOOTING.md)

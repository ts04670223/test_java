# Kubernetes 重啟問題快速解決指南

## 問題總覽

每次 `vagrant reload` 或 VM 重啟後，會遇到以下問題：

| 問題 | 原因 | 影響 |
|------|------|------|
| 🔴 Flannel 網絡未就緒 | subnet.env 文件延遲創建 | Pods 無法獲得 IP |
| 🔴 App 啟動失敗 | 健康檢查配置過於激進 | 連續重啟 |
| 🔴 Kong 無法啟動 | 資料庫未初始化 | 502 錯誤 |
| 🔴 API 路由失效 | Kong 路由配置丟失 | "no Route matched" |

---

## 🚀 快速修復（一鍵解決）

### 方法 1: 使用自動修復腳本

```bash
# 進入 VM
vagrant ssh

# 執行修復腳本
cd /vagrant/scripts
chmod +x fix-restart-issues.sh
sudo ./fix-restart-issues.sh
```

腳本會自動：
- ✅ 等待 Flannel 完全就緒
- ✅ 清理失敗的 pods
- ✅ 重新執行 Kong migrations
- ✅ 重啟 Kong 並配置路由
- ✅ 檢查並修復 App pods
- ✅ 驗證所有服務

**預計時間：3-5 分鐘**

---

### 方法 2: 手動修復步驟

如果自動腳本失敗，按照以下順序手動修復：

#### 步驟 1: 檢查 Flannel
```bash
# 檢查 Flannel 狀態
kubectl get pods -n kube-flannel

# 應該看到：
# NAME                    READY   STATUS    RESTARTS   AGE
# kube-flannel-ds-xxxxx   1/1     Running   0          Xm

# 驗證 subnet.env
ls -la /run/flannel/subnet.env
cat /run/flannel/subnet.env

# 如果不存在，等待或重啟 Flannel
kubectl delete pod -n kube-flannel -l app=flannel
```

#### 步驟 2: 修復 Kong 資料庫
```bash
# 刪除舊的 migrations job
kubectl delete job kong-migrations

# 重新運行 migrations
kubectl apply -f /vagrant/kong/kong-k8s.yaml

# 等待完成
kubectl wait --for=condition=complete job/kong-migrations --timeout=300s

# 檢查日誌
kubectl logs job/kong-migrations
```

#### 步驟 3: 重啟 Kong
```bash
# 刪除 Kong pod
kubectl delete pod -l io.kompose.service=kong

# 等待新 pod 啟動
kubectl get pods -l io.kompose.service=kong -w

# 檢查日誌
kubectl logs -l io.kompose.service=kong --tail=50
```

#### 步驟 4: 配置 Kong 路由
```bash
# 執行路由配置腳本
cd /vagrant/kong
bash setup-routes.sh

# 驗證路由
curl http://192.168.10.10:8003/routes
```

#### 步驟 5: 修復 App Pods
```bash
# 刪除失敗的 App pods
kubectl delete pod -l io.kompose.service=app --grace-period=0 --force

# 等待新 pods 啟動（需要 1-2 分鐘）
kubectl get pods -l io.kompose.service=app -w

# 檢查健康狀態
kubectl get pods -l io.kompose.service=app
```

#### 步驟 6: 驗證
```bash
# 測試 API
curl http://192.168.10.10:30000/api/products
curl http://test6.test/api/products
```

---

## 🔧 永久解決方案

### 選項 1: 自動執行修復腳本（推薦）

編輯 `Vagrantfile`，添加 `run: "always"`：

```ruby
# 在文件末尾添加
config.vm.provision "shell", run: "always", inline: <<-SHELL
  echo "執行啟動修復..."
  bash /vagrant/scripts/fix-restart-issues.sh
SHELL
```

這樣每次 `vagrant up` 或 `vagrant reload` 都會自動修復。

### 選項 2: 改進健康檢查配置

已修改 `app-deployment.yaml`：
```yaml
startupProbe:
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 30  # 最多等待 330 秒
```

### 選項 3: 使用資料持久化

創建 PersistentVolume 確保 Kong 資料不丟失（未實施）。

---

## 📊 診斷工具

### 檢查整體狀態
```bash
# 查看所有 pods
kubectl get pods -A

# 查看失敗的 pods
kubectl get pods --all-namespaces --field-selector=status.phase!=Running,status.phase!=Succeeded

# 查看最近的事件
kubectl get events --sort-by='.lastTimestamp' --all-namespaces | tail -30
```

### 檢查 Flannel
```bash
# Flannel pod 狀態
kubectl get pods -n kube-flannel -o wide

# Flannel 日誌
kubectl logs -n kube-flannel -l app=flannel --tail=50

# subnet.env 文件
cat /run/flannel/subnet.env
```

### 檢查 Kong
```bash
# Kong pod 狀態
kubectl get pods -l io.kompose.service=kong

# Kong 日誌
kubectl logs -l io.kompose.service=kong --tail=100

# Kong Admin API
curl http://192.168.10.10:8003/

# Kong 路由
curl http://192.168.10.10:8003/routes
```

### 檢查 App
```bash
# App pods 狀態
kubectl get pods -l io.kompose.service=app

# 特定 pod 詳情
kubectl describe pod <pod-name>

# App 日誌
kubectl logs -l io.kompose.service=app --tail=100

# 進入 pod 測試
kubectl exec -it <pod-name> -- curl http://localhost:8080/actuator/health
```

---

## ⚠️ 常見問題

### Q1: 修復腳本執行失敗
**原因：** 權限不足或文件不存在

**解決：**
```bash
cd /vagrant/scripts
chmod +x fix-restart-issues.sh
sudo ./fix-restart-issues.sh
```

### Q2: Flannel 一直不就緒
**原因：** CNI 配置衝突或網絡問題

**解決：**
```bash
# 清理並重新安裝 Flannel
sudo rm -rf /etc/cni/net.d/*
sudo rm -rf /run/flannel/
kubectl delete -f https://github.com/flannel-io/flannel/releases/latest/download/kube-flannel.yml
sleep 5
kubectl apply -f https://github.com/flannel-io/flannel/releases/latest/download/kube-flannel.yml
```

### Q3: App pods 一直 CrashLoopBackOff
**原因：** MySQL 或 Redis 未就緒，或啟動時間過長

**解決：**
```bash
# 檢查依賴服務
kubectl get pods -l io.kompose.service=mysql
kubectl get pods -l io.kompose.service=redis

# 檢查 App 日誌找原因
kubectl logs -l io.kompose.service=app --tail=100

# 如果是啟動超時，調整 startupProbe
kubectl edit deployment app
# 增加 failureThreshold 值
```

### Q4: Kong 一直報 "Database needs bootstrapping"
**原因：** Migrations 沒有正確執行

**解決：**
```bash
# 檢查 migrations job
kubectl get jobs
kubectl logs job/kong-migrations

# 重新執行
kubectl delete job kong-migrations
kubectl apply -f /vagrant/kong/kong-k8s.yaml

# 如果持續失敗，檢查 PostgreSQL
kubectl logs -l io.kompose.service=kong-database
```

### Q5: API 返回 "no Route matched"
**原因：** Kong 路由未配置

**解決：**
```bash
# 檢查路由
curl http://192.168.10.10:8003/routes

# 如果為空，重新配置
cd /vagrant/kong
bash setup-routes.sh

# 驗證
curl http://192.168.10.10:8003/routes
curl http://192.168.10.10:30000/api/products
```

---

## 📝 預防措施

### 1. VM 配置建議
```ruby
# Vagrantfile
config.vm.provider "virtualbox" do |vb|
  vb.memory = "8192"  # 至少 8GB
  vb.cpus = 4         # 至少 4 核心
end
```

### 2. 定期檢查
```bash
# 添加到 cron（可選）
*/10 * * * * /vagrant/scripts/fix-restart-issues.sh > /tmp/auto-fix.log 2>&1
```

### 3. 資源監控
```bash
# 檢查資源使用
kubectl top nodes
kubectl top pods
```

---

## 🎯 最佳實踐

1. **每次重啟後運行修復腳本**
   ```bash
   vagrant ssh -c "sudo /vagrant/scripts/fix-restart-issues.sh"
   ```

2. **等待足夠時間**
   - Flannel 啟動：~2 分鐘
   - Kong 啟動：~1 分鐘
   - App 啟動：~1.5 分鐘
   - 總計：~5 分鐘

3. **檢查日誌而非猜測**
   ```bash
   kubectl logs <pod-name> --tail=100
   kubectl describe pod <pod-name>
   ```

4. **一次解決一個問題**
   - 先修復 Flannel
   - 再修復 Kong
   - 最後修復 App

---

## 📚 相關文檔

- 詳細分析：[RESTART-ISSUES-ANALYSIS.md](./RESTART-ISSUES-ANALYSIS.md)
- Flannel 修復：[FLANNEL-FIX.md](./FLANNEL-FIX.md)
- Kong 配置：[../kong/README-ROUTES.md](../kong/README-ROUTES.md)

---

## 🆘 還是無法解決？

1. 收集診斷信息：
   ```bash
   kubectl get pods -A > pods-status.txt
   kubectl get events --sort-by='.lastTimestamp' -A > events.txt
   kubectl logs -l io.kompose.service=app --tail=200 > app-logs.txt
   kubectl logs -l io.kompose.service=kong --tail=200 > kong-logs.txt
   ```

2. 嘗試完全重置：
   ```bash
   vagrant destroy
   vagrant up
   # 等待 5-10 分鐘讓所有服務完全啟動
   vagrant ssh -c "sudo /vagrant/scripts/fix-restart-issues.sh"
   ```

3. 檢查系統資源：
   ```bash
   # 在 Windows 主機
   VBoxManage list runningvms
   VBoxManage showvminfo <vm-name> | grep Memory
   ```

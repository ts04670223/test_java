# Kubernetes 重啟問題分析與解決方案

## 問題總覽

當 Vagrant VM 或 Kubernetes 叢集重啟時，會出現以下問題：

1. **Flannel 網絡插件啟動延遲** → App/Kong pods 無法創建網絡沙箱
2. **App 啟動探針配置不當** → 健康檢查失敗導致 pod 重啟循環
3. **Kong 資料庫未初始化** → Kong 無法啟動
4. **Kong 路由配置丟失** → API 請求無法路由到後端

---

## 根本原因分析

### 1. Flannel 啟動時序問題

**問題現象：**
```
Failed to create pod sandbox: plugin type="flannel" failed (add): 
failed to load flannel 'subnet.env' file: 
open /run/flannel/subnet.env: no such file or directory
```

**根本原因：**

#### A. 啟動順序不當
```
VM 啟動 → kubelet 啟動 → 立即嘗試創建 app/kong pods
          ↓
      Flannel DaemonSet 尚未完全就緒
          ↓
      /run/flannel/subnet.env 尚未創建
          ↓
      Pod 創建失敗
```

**時序分析：**
```bash
# 從事件日誌可見：
# Flannel 啟動時間：~2-3 分鐘（下載鏡像 + 初始化）
# App/Kong 部署時間：VM provision 時立即執行
# 間隙：0 秒 ← 這是問題！
```

#### B. Vagrant provision 過於激進
`Vagrantfile` 在 VM 啟動時立即執行：
```ruby
config.vm.provision "shell", path: "scripts/setup-k8s-cluster.sh"
```

這個腳本會：
1. 初始化 Kubernetes (kubeadm init)
2. 安裝 Flannel
3. **等待 30 秒** ← 不夠！
4. 部署應用 (通過其他 provision 或手動)

#### C. Flannel 初始化流程
1. **Init Container 1**: install-cni-plugin (~10-15秒)
   - 複製 CNI 插件到 `/opt/cni/bin/`
2. **Init Container 2**: install-cni (~10-15秒)
   - 生成 CNI 配置文件到 `/etc/cni/net.d/`
3. **Main Container**: kube-flannel (~30-60秒)
   - 連接 K8s API
   - 獲取 Pod CIDR (10.244.0.0/16)
   - **創建 `/run/flannel/subnet.env`** ← 關鍵文件！
   - 設置網絡路由

**總啟動時間：50-90 秒**

但 `setup-k8s-cluster.sh` 只等待 30 秒：
```bash
# 等待所有系統 pods 啟動
echo "等待 Kubernetes 系統組件啟動..."
sleep 30  # ← 不夠！
```

---

### 2. App 啟動探針配置問題

**問題現象：**
```
Startup probe failed: Get "http://10.244.0.6:8080/actuator/health": 
dial tcp 10.244.0.6:8080: connect: connection refused
```

**根本原因：**

#### A. Spring Boot 啟動時間過長
從日誌分析：
```
啟動階段                           耗時
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
JPA Repository 掃描              ~12秒
Hibernate 初始化                 ~8秒
MySQL 連接池建立                 ~3秒
Spring Security 配置             ~10秒
Tomcat 啟動                      ~7秒
Controller/Service 初始化        ~15秒
應用完全就緒                     ~25秒
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
總計                             80-85秒
```

#### B. 原探針配置過於激進
```yaml
startupProbe:
  initialDelaySeconds: 10    # 等待 10 秒才開始檢查
  periodSeconds: 10          # 每 10 秒檢查一次
  failureThreshold: 12       # 最多失敗 12 次
  # 最大等待時間 = 10 + (10 × 12) = 130 秒
```

**問題：**
- 應用需要 80-85 秒啟動
- 探針在第 20 秒開始檢查（10 + 10）
- 此時應用還在初始化，返回 connection refused
- 連續失敗會導致 pod 被標記為 Unhealthy

#### C. VM 資源限制
```ruby
vb.memory = "8192"  # 8GB RAM
vb.cpus = 4         # 4 核心
```

但實際上：
- MySQL, Redis, Kong, Prometheus, Grafana 都在運行
- 多個 App pods (HPA 自動擴展到 5 個)
- 系統 pods (CoreDNS, Flannel, 等)

**資源競爭導致啟動更慢！**

---

### 3. Kong 資料庫未初始化問題

**問題現象：**
```
Database needs bootstrapping or is older than Kong 1.0.
To start a new installation from scratch, run 'kong migrations bootstrap'.
```

**根本原因：**

#### A. Kong Migrations Job 執行時機
```yaml
# kong-k8s.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: kong-migrations
spec:
  template:
    spec:
      containers:
        - args:
            - kong
            - migrations
            - bootstrap
```

**問題：**
1. Job 只在 `kubectl apply` 時執行一次
2. VM 重啟後，Job 狀態保持為 "Completed"
3. Kubernetes 不會重新執行已完成的 Job
4. PostgreSQL 資料若丟失（使用 emptyDir 或重置），資料庫為空
5. Kong pod 啟動時檢測到空資料庫，啟動失敗

#### B. 資料持久化問題
```yaml
volumes:
  - name: kong-data
    hostPath:
      path: /tmp/kong-data  # ← 問題！
      type: DirectoryOrCreate
```

`/tmp` 目錄在某些系統中會在重啟時被清空！

#### C. Init Container 檢查邏輯錯誤
原配置嘗試檢查 `schema_meta` 表：
```bash
psql -c "SELECT * FROM schema_meta LIMIT 1;"
```

但這個表名不正確（Kong 使用 `schema_migrations`），檢查永遠失敗。

---

### 4. Kong 路由配置丟失問題

**問題現象：**
```json
{
  "message": "no Route matched with those values",
  "request_id": "648c461b7828b2040e974d7b22962e12"
}
```

**根本原因：**

#### A. 路由存儲在 PostgreSQL
Kong 路由配置存在資料庫中，當資料庫重置時配置丟失。

#### B. Vagrantfile provision 不可靠
```ruby
config.vm.provision "shell", inline: <<-SHELL
  echo "等待 Kong 完全啟動..."
  sleep 30
  bash /vagrant/kong/check-and-configure.sh
SHELL
```

**問題：**
1. `vagrant up` 時執行，但 `vagrant reload` 不會重新執行 provision
2. 30 秒可能不足以等待 Kong 完全啟動
3. 沒有錯誤處理，腳本失敗會被忽略

#### C. Kong Setup Routes Job 只執行一次
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: kong-setup-routes-job
```

同樣的問題：Job 完成後不會重新執行。

---

## 完整解決方案

### 方案 1：改進啟動順序（推薦）

#### 1.1 修改 `setup-k8s-cluster.sh`

```bash
#!/bin/bash
set -e

echo "初始化 Kubernetes..."
# ... 現有的 kubeadm init 代碼 ...

echo "安裝 Flannel..."
kubectl apply -f https://github.com/flannel-io/flannel/releases/latest/download/kube-flannel.yml

# ✅ 改進：等待 Flannel 完全就緒
echo "等待 Flannel 網絡插件就緒..."
kubectl wait --for=condition=ready pod \
  -l app=flannel \
  -n kube-flannel \
  --timeout=300s

# ✅ 驗證 subnet.env 文件存在
echo "驗證 Flannel 配置..."
for i in {1..30}; do
  if [ -f /run/flannel/subnet.env ]; then
    echo "✓ Flannel subnet.env 已創建"
    cat /run/flannel/subnet.env
    break
  fi
  echo "等待 subnet.env 創建... ($i/30)"
  sleep 2
done

# ✅ 等待 CoreDNS 就緒
echo "等待 CoreDNS 就緒..."
kubectl wait --for=condition=ready pod \
  -l k8s-app=kube-dns \
  -n kube-system \
  --timeout=300s

echo "Kubernetes 系統組件已就緒！"
```

#### 1.2 修改 `Vagrantfile`

```ruby
# 改進 Kong 配置時機
config.vm.provision "shell", inline: <<-SHELL
  echo "等待 Kubernetes 完全就緒..."
  
  # ✅ 等待 Kong database 就緒
  kubectl wait --for=condition=ready pod \
    -l io.kompose.service=kong-database \
    --timeout=300s
  
  # ✅ 等待 Kong migrations 完成
  kubectl wait --for=condition=complete job/kong-migrations \
    --timeout=300s
  
  # ✅ 等待 Kong pod 就緒
  kubectl wait --for=condition=ready pod \
    -l io.kompose.service=kong \
    --timeout=300s
  
  echo "執行 Kong 路由配置..."
  bash /vagrant/kong/setup-routes.sh
  
  # ✅ 驗證配置
  sleep 5
  curl -f http://localhost:30000/api/products || echo "⚠ API 測試失敗"
SHELL
```

---

### 方案 2：使用 Kubernetes Job 自動化配置

#### 2.1 創建智能 Kong Migrations Job

```yaml
# kong/kong-migrations-smart.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: kong-migrations-{{ timestamp }}  # 使用時間戳確保每次都執行
  labels:
    app: kong-migrations
spec:
  ttlSecondsAfterFinished: 3600  # 1小時後自動清理
  template:
    spec:
      restartPolicy: OnFailure
      initContainers:
        - name: wait-for-postgres
          image: postgres:16
          env:
            - name: PGPASSWORD
              value: kongpass
          command:
            - sh
            - -c
            - |
              echo "等待 PostgreSQL 就緒..."
              until pg_isready -h kong-database -U kong -d kong; do
                sleep 3
              done
              
              # ✅ 檢查資料庫是否已初始化
              echo "檢查資料庫狀態..."
              TABLES=$(psql -h kong-database -U kong -d kong -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public';")
              
              if [ "$TABLES" -gt "0" ]; then
                echo "✓ 資料庫已初始化 ($TABLES 個表)"
                exit 0
              else
                echo "⚠ 資料庫為空，需要執行 migrations"
              fi
              
      containers:
        - name: kong-migrations
          image: kong:latest
          env:
            - name: KONG_DATABASE
              value: postgres
            - name: KONG_PG_HOST
              value: kong-database
            - name: KONG_PG_USER
              value: kong
            - name: KONG_PG_PASSWORD
              value: kongpass
            - name: KONG_PG_DATABASE
              value: kong
          command:
            - sh
            - -c
            - |
              # ✅ 智能執行 migrations
              echo "執行 Kong migrations..."
              kong migrations bootstrap || kong migrations up
              
              echo "✓ Migrations 完成"
```

#### 2.2 創建 Kong 路由配置 CronJob

```yaml
# kong/kong-routes-config-cronjob.yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: kong-routes-config
spec:
  schedule: "@reboot"  # VM 重啟時執行
  concurrencyPolicy: Replace
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: OnFailure
          containers:
            - name: configure-routes
              image: curlimages/curl:latest
              command:
                - sh
                - -c
                - |
                  echo "等待 Kong Admin API 就緒..."
                  for i in $(seq 1 60); do
                    if curl -f -s http://kong:8003/ > /dev/null; then
                      echo "✓ Kong Admin API 可用"
                      break
                    fi
                    sleep 5
                  done
                  
                  echo "配置 Kong 路由..."
                  
                  # 檢查 service 是否存在
                  if ! curl -f -s http://kong:8003/services/spring-boot-app; then
                    curl -X POST http://kong:8003/services \
                      --data name=spring-boot-app \
                      --data url=http://app:8080
                  fi
                  
                  # 檢查 route 是否存在
                  if ! curl -f -s http://kong:8003/routes/api-route; then
                    curl -X POST http://kong:8003/services/spring-boot-app/routes \
                      --data name=api-route \
                      --data 'paths[]=/api' \
                      --data strip_path=false
                  fi
                  
                  echo "✓ Kong 路由配置完成"
```

---

### 方案 3：改進資料持久化

#### 3.1 使用 PersistentVolume

```yaml
# kong/kong-pv.yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: kong-postgres-pv
spec:
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain  # ✅ 保留資料
  hostPath:
    path: /mnt/data/kong-postgres  # ✅ 使用穩定路徑
    type: DirectoryOrCreate

---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: kong-postgres-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
```

修改 Kong Database Deployment：
```yaml
spec:
  template:
    spec:
      containers:
        - name: kong-database
          volumeMounts:
            - mountPath: /var/lib/postgresql/data
              name: kong-data
      volumes:
        - name: kong-data
          persistentVolumeClaim:
            claimName: kong-postgres-pvc  # ✅ 使用 PVC
```

---

### 方案 4：健康檢查最佳實踐

#### 4.1 改進 App Deployment

```yaml
# app-deployment.yaml
spec:
  template:
    spec:
      containers:
        - name: spring-boot-app
          # ✅ 啟動探針：給足夠時間啟動
          startupProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 60     # ✅ 延遲 60 秒
            periodSeconds: 10
            failureThreshold: 30        # ✅ 最多等待 360 秒 (60 + 10×30)
            timeoutSeconds: 5
            
          # ✅ 就緒探針：應用完全就緒後才接收流量
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 3
            successThreshold: 1
            
          # ✅ 存活探針：檢測死鎖或假死
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 120    # ✅ 確保啟動後才檢查
            periodSeconds: 30
            failureThreshold: 3
            timeoutSeconds: 10
```

#### 4.2 分離健康檢查端點

在 Spring Boot 配置：
```yaml
# application.yml
management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    readiness:
      enabled: true
    liveness:
      enabled: true
```

---

## 快速修復腳本

創建一鍵修復腳本：

```bash
#!/bin/bash
# scripts/fix-restart-issues.sh

echo "🔧 修復 Kubernetes 重啟問題..."

# 1. 確保 Flannel 就緒
echo "檢查 Flannel 狀態..."
kubectl wait --for=condition=ready pod -l app=flannel -n kube-flannel --timeout=300s

# 2. 刪除失敗的 pods
echo "清理失敗的 pods..."
kubectl delete pods --field-selector=status.phase=Failed --all-namespaces
kubectl delete pods --field-selector=status.phase=Unknown --all-namespaces

# 3. 重新執行 Kong migrations
echo "重新執行 Kong migrations..."
kubectl delete job kong-migrations
kubectl apply -f /vagrant/kong/kong-k8s.yaml
kubectl wait --for=condition=complete job/kong-migrations --timeout=300s

# 4. 重啟 Kong
echo "重啟 Kong..."
kubectl rollout restart deployment/kong
kubectl wait --for=condition=ready pod -l io.kompose.service=kong --timeout=300s

# 5. 重新配置路由
echo "配置 Kong 路由..."
sleep 10
bash /vagrant/kong/setup-routes.sh

# 6. 重啟 App pods
echo "重啟 App pods..."
kubectl rollout restart deployment/app
kubectl wait --for=condition=ready pod -l io.kompose.service=app --timeout=600s

# 7. 驗證
echo "驗證服務..."
sleep 5
curl -f http://192.168.10.10:30000/api/products && echo "✅ API 正常" || echo "❌ API 失敗"

echo "✅ 修復完成！"
```

---

## 預防措施清單

### ✅ 啟動時自動執行

在 VM 啟動時自動修復：
```ruby
# Vagrantfile
config.vm.provision "shell", run: "always", inline: <<-SHELL
  # 每次 VM 啟動都執行
  bash /vagrant/scripts/fix-restart-issues.sh
SHELL
```

### ✅ 監控關鍵指標

```bash
# 添加到 crontab
*/5 * * * * kubectl get pods --all-namespaces | grep -vE 'Running|Completed' && bash /vagrant/scripts/fix-restart-issues.sh
```

### ✅ 資源預留

確保關鍵服務有足夠資源：
```yaml
spec:
  template:
    spec:
      containers:
        - name: spring-boot-app
          resources:
            requests:
              memory: "1Gi"    # ✅ 保證最少 1GB
              cpu: "500m"      # ✅ 保證最少 0.5 核心
            limits:
              memory: "2Gi"
              cpu: "2000m"
```

---

## 驗證測試

運行完整測試：
```bash
# 1. 重啟 VM
vagrant reload

# 2. 等待啟動
sleep 120

# 3. 檢查所有 pods
vagrant ssh -c "kubectl get pods -A"

# 4. 測試 API
curl http://test6.test/api/products

# 5. 檢查 Kong
curl http://192.168.10.10:8003/routes
```

---

## 總結

重啟問題的核心是 **時序和依賴管理**：

1. **Flannel 必須完全就緒** 才能創建其他 pods
2. **App 需要足夠時間啟動** 且健康檢查配置要合理
3. **Kong 資料庫需要持久化** 並在重啟後自動初始化
4. **Kong 路由需要在啟動後自動配置** 不能依賴手動執行

通過以上改進，系統可以在重啟後自動恢復到正常狀態。

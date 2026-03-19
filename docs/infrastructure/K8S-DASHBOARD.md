# Kubernetes Dashboard 管理頁面使用指南

完整的 Kubernetes Dashboard 安裝、配置和使用說明。

## 📋 目錄

1. [簡介](#簡介)
2. [快速開始](#快速開始)
3. [安裝說明](#安裝說明)
4. [訪問方式](#訪問方式)
5. [功能介紹](#功能介紹)
6. [故障排查](#故障排查)
7. [安全建議](#安全建議)

## 簡介

Kubernetes Dashboard 是 Kubernetes 官方提供的 Web 管理界面，提供：

- 📊 集群資源可視化監控
- 🚀 工作負載管理（Deployments, Pods, Services 等）
- 📝 配置管理（ConfigMaps, Secrets）
- 📈 實時日誌查看
- 🔍 資源使用情況分析

### 版本信息

- **Dashboard 版本**: v2.7.0
- **支持的 K8s 版本**: 1.21+
- **安裝包位置**: `k8s-dashboard/`

## 快速開始

### 🔑 登入 K8s 機器

**重要**: 如果需要在 Kubernetes 機器內執行命令，使用以下方式登入：

```cmd
# Windows PowerShell 或 CMD
vagrant ssh
```

登入後你會看到：
```
vagrant@k8s-master:~$
```

現在你可以執行任何 kubectl 或 docker 命令：
```bash
# 查看節點狀態
kubectl get nodes

# 查看所有 Pods
kubectl get pods -A

# 查看 Docker 容器
docker ps
```

**退出 VM**: 輸入 `exit` 或按 `Ctrl+D`

### 三步驟啟動 Dashboard

```cmd
# 1. 安裝 Dashboard
cd k8s-dashboard
install-dashboard.bat

# 2. 啟動服務
start-dashboard.bat

# 3. 瀏覽器會自動打開，使用令牌登入
```

訪問地址會自動在瀏覽器打開：
```
http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
```

### 🔑 取得 Token 登入

#### 方法一：查看 token 文件（推薦）

**Windows 命令**：
```cmd
# CMD
type k8s-dashboard\dashboard-token.txt

# PowerShell
Get-Content k8s-dashboard\dashboard-token.txt

# 記事本打開
notepad k8s-dashboard\dashboard-token.txt
```

**文件完整路徑**：`c:\JOHNY\test\k8s-dashboard\dashboard-token.txt`

#### 方法二：在 K8s 機器內查詢

```bash
# 登入 VM
vagrant ssh

# 查看已存檔的 token
cat /vagrant/k8s-dashboard/dashboard-token.txt

# 或重新獲取 token（生成新的）
kubectl -n kubernetes-dashboard create token admin-user
```

#### 如何使用 Token 登入

**步驟 1**: 打開 Dashboard 網址
```
http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
```

**步驟 2**: 選擇登入方式
- 點選 **"Token"** 選項（預設已選中）

**步驟 3**: 複製完整 Token
- 打開 `dashboard-token.txt`
- **Ctrl+A** 全選
- **Ctrl+C** 複製
- 確保複製完整，沒有空格或換行

**步驟 4**: 貼上 Token
- 在 "Enter token" 輸入框中 **Ctrl+V** 貼上
- 點擊 **"Sign in"**

**步驟 5**: 開始使用
- 登入成功後會看到 Dashboard 主頁面
- 可以查看和管理所有 Kubernetes 資源

#### Token 資訊

- **有效期**：10 年（87600 小時）
- **權限**：cluster-admin（完整集群管理權限）
- **位置**：`k8s-dashboard/dashboard-token.txt`

⚠️ **重要提醒**：
- Token 擁有完整管理權限，請妥善保管
- 不要分享 Token 給不信任的人
- 生產環境建議使用較短有效期的 Token

## 安裝說明

### 前置需求

✅ **必須完成**:
1. Vagrant VM 已啟動
2. Kubernetes 集群已安裝並運行
3. kubectl 可正常使用

驗證 Kubernetes 狀態：
```cmd
vagrant ssh -c "kubectl get nodes"
```

應該看到節點狀態為 `Ready`。

### 詳細安裝步驟

#### Windows 安裝

```cmd
# 方式 1: 使用安裝腳本（推薦）
cd k8s-dashboard
install-dashboard.bat

# 方式 2: 手動安裝
vagrant ssh
sudo bash /vagrant/k8s-dashboard/install-dashboard.sh
```

#### 安裝過程

安裝腳本會執行：

1. ✅ 檢查 kubectl 和集群狀態
2. ✅ 部署 Dashboard 到 `kubernetes-dashboard` 命名空間
3. ✅ 創建管理員用戶 `admin-user`
4. ✅ 生成訪問令牌（10 年有效期）
5. ✅ 保存令牌到 `dashboard-token.txt`

#### 驗證安裝

```cmd
# 查看 Pod 狀態
vagrant ssh -c "kubectl get pods -n kubernetes-dashboard"

# 應該看到類似輸出：
# NAME                                         READY   STATUS    RESTARTS   AGE
# dashboard-metrics-scraper-xxx                1/1     Running   0          2m
# kubernetes-dashboard-xxx                     1/1     Running   0          2m
```

## 訪問方式

### 方式 1: 自動啟動（推薦）

使用 `start-dashboard.bat`：

```cmd
cd k8s-dashboard
start-dashboard.bat
```

這會：
1. 啟動 kubectl proxy
2. 自動打開瀏覽器
3. 顯示令牌信息

### 方式 2: 手動啟動

#### 步驟 1: 啟動 kubectl proxy

```cmd
vagrant ssh -c "kubectl proxy --address='0.0.0.0' --accept-hosts='.*'"
```

> ⚠️ 保持此終端視窗開啟

#### 步驟 2: 打開瀏覽器

訪問地址：
```
http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
```

#### 步驟 3: 獲取令牌

```cmd
type k8s-dashboard\dashboard-token.txt
```

或在 VM 中重新生成：
```bash
kubectl -n kubernetes-dashboard create token admin-user
```

#### 步驟 4: 登入

1. 選擇 **Token** 登入方式
2. 複製並貼上令牌
3. 點擊 **Sign in**

### 方式 3: NodePort（生產環境不推薦）

修改服務類型為 NodePort：

```bash
kubectl patch svc kubernetes-dashboard -n kubernetes-dashboard \
  -p '{"spec":{"type":"NodePort"}}'
```

查看分配的端口：
```bash
kubectl get svc kubernetes-dashboard -n kubernetes-dashboard
```

訪問：
```
https://192.168.10.10:<NodePort>
```

> ⚠️ 使用 HTTPS，會有證書警告，選擇繼續即可

## 功能介紹

### 1. 概覽儀表板 (Overview)

**路徑**: 首頁

**功能**:
- 集群整體健康狀況
- 資源使用統計
- 命名空間概覽
- 最近事件

**操作**:
```
點擊 "Cluster" -> "Overview" 查看全局信息
```

### 2. 工作負載 (Workloads)

#### Deployments（部署）

**查看部署列表**:
```
左側菜單 -> Workloads -> Deployments
```

**操作**:
- ✏️ 編輯部署配置
- 🔄 調整副本數量
- 🗑️ 刪除部署
- 📊 查看 ReplicaSets
- 📝 查看 Pod 列表

**示例 - 擴展副本**:
1. 點擊部署名稱
2. 點擊右上角 "Scale"
3. 輸入新的副本數
4. 點擊 "Scale"

#### Pods（容器組）

**查看 Pod 列表**:
```
左側菜單 -> Workloads -> Pods
```

**操作**:
- 📊 查看 Pod 詳情
- 📝 查看日誌（Logs）
- 💻 進入容器終端（Exec）
- 🗑️ 刪除 Pod

**查看日誌**:
1. 點擊 Pod 名稱
2. 點擊右上角 "Logs" 圖標
3. 選擇容器（如果有多個）
4. 查看實時日誌

**進入容器**:
1. 點擊 Pod 名稱
2. 點擊右上角 "Exec" 圖標
3. 選擇容器
4. 在終端中執行命令

#### Jobs & CronJobs（任務）

**查看任務**:
```
左側菜單 -> Workloads -> Jobs
左側菜單 -> Workloads -> Cron Jobs
```

**操作**:
- 查看任務執行歷史
- 查看任務日誌
- 手動觸發 CronJob

### 3. 服務與網路 (Service)

#### Services（服務）

**查看服務列表**:
```
左側菜單 -> Service -> Services
```

**信息**:
- Service 名稱和類型（ClusterIP, NodePort, LoadBalancer）
- Cluster IP 和外部 IP
- 端口映射
- 選擇器（Selector）

**創建服務**:
1. 點擊右上角 "+" 或 "Create"
2. 選擇 "Create from form" 或 "Create from file"
3. 填寫配置
4. 點擊 "Deploy"

#### Ingress（入口）

**查看 Ingress 規則**:
```
左側菜單 -> Service -> Ingresses
```

**操作**:
- 查看路由規則
- 查看後端服務
- 編輯 Ingress 配置

### 4. 配置與存儲 (Config and Storage)

#### ConfigMaps（配置映射）

**查看 ConfigMaps**:
```
左側菜單 -> Config and Storage -> Config Maps
```

**操作**:
- 📝 創建新的 ConfigMap
- ✏️ 編輯配置內容
- 🗑️ 刪除 ConfigMap

**創建 ConfigMap**:
1. 點擊右上角 "Create"
2. 填寫名稱
3. 添加數據（Key-Value）
4. 點擊 "Deploy"

#### Secrets（密鑰）

**查看 Secrets**:
```
左側菜單 -> Config and Storage -> Secrets
```

**操作**:
- 創建密鑰
- 查看密鑰類型
- 編輯密鑰（base64 編碼）

> ⚠️ **安全提示**: Secrets 內容以 base64 編碼顯示，非加密

#### PersistentVolumeClaims（持久卷聲明）

**查看 PVC**:
```
左側菜單 -> Config and Storage -> Persistent Volume Claims
```

**信息**:
- 存儲大小
- 訪問模式（ReadWriteOnce, ReadOnlyMany 等）
- 存儲類別
- 掛載狀態

### 5. 集群管理 (Cluster)

#### Namespaces（命名空間）

**查看命名空間**:
```
左側菜單 -> Cluster -> Namespaces
```

**操作**:
- 創建新命名空間
- 查看命名空間配額
- 刪除命名空間

**切換命名空間**:
```
頂部下拉菜單 -> 選擇命名空間
```

#### Nodes（節點）

**查看節點信息**:
```
左側菜單 -> Cluster -> Nodes
```

**信息**:
- CPU 和記憶體使用率
- Pod 數量
- 節點標籤和污點
- 節點狀態

**查看節點詳情**:
1. 點擊節點名稱
2. 查看系統信息
3. 查看運行的 Pod
4. 查看資源分配

#### Events（事件）

**查看集群事件**:
```
左側菜單 -> Cluster -> Events
```

**用途**:
- 監控集群活動
- 排查錯誤
- 查看警告信息

## 故障排查

### 問題 1: 無法訪問 Dashboard

**症狀**: 瀏覽器無法打開 Dashboard

**檢查清單**:
```cmd
# 1. 檢查 Vagrant VM 狀態
vagrant status

# 2. 檢查 kubectl proxy 是否運行
vagrant ssh -c "ps aux | grep 'kubectl proxy'"

# 3. 檢查 Dashboard Pod 狀態
vagrant ssh -c "kubectl get pods -n kubernetes-dashboard"
```

**解決方案**:
```cmd
# 重新啟動 kubectl proxy
stop-dashboard.bat
start-dashboard.bat
```

### 問題 2: 令牌無效或過期

**症狀**: 登入時提示 "Invalid token"

**解決方案**:
```bash
# 在 VM 中重新生成令牌
vagrant ssh
kubectl -n kubernetes-dashboard create token admin-user --duration=87600h > /vagrant/k8s-dashboard/dashboard-token.txt
```

### 問題 3: Dashboard Pod 無法啟動

**症狀**: Pod 狀態為 Pending 或 CrashLoopBackOff

**檢查**:
```bash
# 查看 Pod 詳情
vagrant ssh -c "kubectl describe pod -n kubernetes-dashboard -l k8s-app=kubernetes-dashboard"

# 查看日誌
vagrant ssh -c "kubectl logs -n kubernetes-dashboard -l k8s-app=kubernetes-dashboard"
```

**常見原因**:
1. 資源不足（CPU/Memory）
2. 鏡像拉取失敗
3. 存儲不足

**解決方案**:
```bash
# 刪除 Pod 強制重啟
vagrant ssh -c "kubectl delete pod -n kubernetes-dashboard -l k8s-app=kubernetes-dashboard"
```

### 問題 4: 端口衝突

**症狀**: kubectl proxy 無法啟動，提示端口 8001 被佔用

**解決方案**:
```powershell
# Windows - 查找佔用進程
netstat -ano | findstr :8001

# 結束進程（替換 <PID>）
taskkill /PID <PID> /F
```

### 問題 5: 無法查看日誌或執行命令

**症狀**: 點擊 Logs 或 Exec 沒有反應

**原因**: Dashboard 需要 metrics-server 支持

**解決方案**:
```bash
# 安裝 metrics-server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# 驗證
kubectl get deployment metrics-server -n kube-system
```

## 安全建議

### 1. 令牌管理

✅ **建議**:
- 定期輪換令牌
- 不要將令牌提交到版本控制
- 限制令牌有效期

```bash
# 創建短期令牌（1 小時）
kubectl -n kubernetes-dashboard create token admin-user --duration=1h
```

### 2. 訪問控制

✅ **建議**:
- 僅在可信網路中使用
- 生產環境使用 RBAC 限制權限
- 考慮使用 Ingress + TLS

**創建只讀用戶**:
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: readonly-user
  namespace: kubernetes-dashboard
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: readonly-user
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: view  # 只讀權限
subjects:
- kind: ServiceAccount
  name: readonly-user
  namespace: kubernetes-dashboard
```

### 3. 網路安全

✅ **建議**:
- 不要在公網暴露 Dashboard
- 使用 VPN 或跳板機訪問
- 啟用網路策略限制訪問

### 4. 審計日誌

啟用 Kubernetes 審計日誌記錄 Dashboard 操作：

```yaml
# /etc/kubernetes/audit-policy.yaml
apiVersion: audit.k8s.io/v1
kind: Policy
rules:
- level: RequestResponse
  namespaces: ["kubernetes-dashboard"]
```

## 最佳實踐

### 1. 命名空間隔離

為不同環境創建獨立命名空間：
```
dev
staging
production
```

### 2. 資源配額

為命名空間設置資源配額：
```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: compute-quota
  namespace: dev
spec:
  hard:
    requests.cpu: "4"
    requests.memory: 8Gi
    limits.cpu: "8"
    limits.memory: 16Gi
```

### 3. 標籤規範

使用一致的標籤：
```yaml
labels:
  app: myapp
  env: production
  version: v1.0.0
```

### 4. 健康檢查

配置 Liveness 和 Readiness Probe：
```yaml
livenessProbe:
  httpGet:
    path: /health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
readinessProbe:
  httpGet:
    path: /ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
```

## 參考資源

- [Kubernetes Dashboard 官方文檔](https://kubernetes.io/docs/tasks/access-application-cluster/web-ui-dashboard/)
- [GitHub 倉庫](https://github.com/kubernetes/dashboard)
- [版本發布](https://github.com/kubernetes/dashboard/releases)
- [安全指南](https://github.com/kubernetes/dashboard/blob/master/docs/user/access-control/README.md)

---

**Last Updated**: 2025-12-19  
**Dashboard Version**: v2.7.0

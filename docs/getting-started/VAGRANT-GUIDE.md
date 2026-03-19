# Vagrant Kubernetes 實作指南

## 🚀 快速開始

### Step 1: 啟動環境

```bash
# Windows CMD 或 PowerShell
cd c:\JOHNY\test
vagrant up
```

等待 10-15 分鐘完成安裝。

### Step 2: 登入虛擬機（K8s 機器）

#### 🔑 登入方式

在**專案根目錄**執行：

```cmd
# Windows PowerShell 或 CMD
cd c:\JOHNY\test
vagrant ssh
```

#### 成功登入的標誌

看到以下提示符表示已進入 Kubernetes 主節點：
```
vagrant@k8s-master:~$
```

#### 登入後可以做什麼

```bash
# 1. 查看 Kubernetes 集群狀態
kubectl get nodes
kubectl get pods -A

# 2. 操作 Docker
docker ps
docker images

# 3. 訪問專案文件（Windows 的 c:\JOHNY\test 映射到 VM 的 /vagrant）
cd /vagrant
ls -la

# 4. 查看系統資源
free -h
df -h
```

#### 退出虛擬機

```bash
# 方式 1
exit

# 方式 2（快捷鍵）
Ctrl+D
```

退出後會返回到 Windows 的 PowerShell/CMD。

---

## ✅ 驗證 Kubernetes 環境

登入後立即執行以下命令：

```bash
# 1. 檢查節點狀態
kubectl get nodes
# 預期: k8s-master  Ready  control-plane

# 2. 查看所有系統 Pods
kubectl get pods -A
# 預期: 所有 Pods 都是 Running 狀態

# 3. 查看集群資訊
kubectl cluster-info

# 4. 檢查 Docker
docker ps
docker images
```

## 📦 部署 Spring Boot 應用

### 方法一：使用現有配置檔（推薦）

```bash
# 1. 切換到專案目錄
cd /vagrant

# 2. 先建構 Spring Boot 映像
mvn clean package
docker build -t spring-boot-demo:latest .

# 3. 驗證映像
docker images | grep spring-boot-demo

# 4. 創建命名空間
kubectl apply -f k8s-manifests/namespace.yaml

# 5. 修改部署配置中的映像名稱
# 編輯 k8s-manifests/spring-boot-deployment.yaml
# 將 image: your-spring-boot-image:latest 
# 改為 image: spring-boot-demo:latest

# 6. 部署應用
kubectl apply -f k8s-manifests/spring-boot-deployment.yaml

# 7. 查看部署狀態
kubectl get all -n spring-boot-app
```

### 方法二：手動快速部署

```bash
# 1. 直接創建部署
kubectl create deployment spring-boot-demo \
  --image=spring-boot-demo:latest \
  --replicas=2 \
  --port=8080

# 2. 設置映像拉取策略（使用本地映像）
kubectl set image deployment/spring-boot-demo \
  spring-boot-demo=spring-boot-demo:latest
kubectl patch deployment spring-boot-demo \
  -p '{"spec":{"template":{"spec":{"containers":[{"name":"spring-boot-demo","imagePullPolicy":"IfNotPresent"}]}}}}'

# 3. 暴露服務
kubectl expose deployment spring-boot-demo \
  --type=NodePort \
  --port=8080 \
  --target-port=8080 \
  --name=spring-boot-service

# 4. 獲取訪問端口
kubectl get svc spring-boot-service
```

### 方法三：使用 Maven 建構（完整流程）

```bash
# 1. 建構專案
cd /vagrant
mvn clean package -DskipTests

# 2. 創建 Dockerfile（如果還沒有）
cat > Dockerfile <<'EOF'
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# 3. 建構 Docker 映像
docker build -t spring-boot-demo:latest .

# 4. 部署到 K8s
kubectl create deployment spring-boot-demo \
  --image=spring-boot-demo:latest \
  --dry-run=client -o yaml > deployment.yaml

# 5. 編輯 deployment.yaml 添加 imagePullPolicy
cat >> deployment.yaml <<'EOF'
---
apiVersion: v1
kind: Service
metadata:
  name: spring-boot-service
spec:
  type: NodePort
  selector:
    app: spring-boot-demo
  ports:
  - port: 8080
    targetPort: 8080
    nodePort: 30080
EOF

# 6. 應用配置
kubectl apply -f deployment.yaml

# 7. 查看狀態
kubectl get pods
kubectl get svc
```

## 🔍 查看和測試應用

### 獲取訪問資訊

```bash
# 查看服務詳情
kubectl get svc

# 獲取 NodePort
NODE_PORT=$(kubectl get svc spring-boot-service -o jsonpath='{.spec.ports[0].nodePort}')
echo "應用訪問地址: http://192.168.10.10:$NODE_PORT"
```

### 測試應用

```bash
# 在 VM 內測試
curl http://192.168.10.10:30080
curl http://localhost:30080

# 測試健康檢查（如果有 actuator）
curl http://localhost:30080/actuator/health
```

### 從 Windows 主機訪問

打開瀏覽器訪問：
```
http://192.168.10.10:30080
```

或使用 curl：
```powershell
# 在 Windows PowerShell 中
curl http://192.168.10.10:30080
```

## 📊 監控和除錯

### 查看 Pod 日誌

```bash
# 列出所有 Pods
kubectl get pods

# 查看日誌
kubectl logs <pod-name>

# 即時追蹤日誌
kubectl logs -f <pod-name>

# 查看最近 100 行
kubectl logs --tail=100 <pod-name>
```

### 進入 Pod 除錯

```bash
# 進入 Pod 的 bash
kubectl exec -it <pod-name> -- bash

# 在 Pod 內執行命令
kubectl exec <pod-name> -- ps aux
kubectl exec <pod-name> -- env
```

### 查看 Pod 詳細資訊

```bash
# 查看 Pod 詳情（包含事件）
kubectl describe pod <pod-name>

# 查看所有事件
kubectl get events --sort-by='.lastTimestamp'

# 查看資源使用
kubectl top pods
kubectl top nodes
```

## 🎛️ Kubernetes Dashboard

### 啟動 Dashboard

```bash
# 方法一：前景執行（會占用終端）
kubectl proxy --address='0.0.0.0' --accept-hosts='.*'

# 方法二：背景執行
nohup kubectl proxy --address='0.0.0.0' --accept-hosts='.*' > /dev/null 2>&1 &
```

### 獲取訪問 Token

```bash
# 創建 Token
kubectl -n kubernetes-dashboard create token admin-user

# 複製輸出的 Token（很長的字串）
```

### 訪問 Dashboard

1. 在 Windows 瀏覽器開啟：
   ```
   http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
   ```

2. 選擇 "Token" 登入方式

3. 貼上剛才複製的 Token

## 🔧 常用操作

### 擴縮容

```bash
# 擴展到 3 個副本
kubectl scale deployment spring-boot-demo --replicas=3

# 縮減到 1 個副本
kubectl scale deployment spring-boot-demo --replicas=1

# 查看副本狀態
kubectl get deployments
kubectl get pods -w  # 監看變化
```

### 更新應用

```bash
# 重新建構映像
cd /vagrant
mvn clean package
docker build -t spring-boot-demo:v2 .

# 更新部署
kubectl set image deployment/spring-boot-demo \
  spring-boot-demo=spring-boot-demo:v2

# 查看滾動更新狀態
kubectl rollout status deployment/spring-boot-demo

# 查看更新歷史
kubectl rollout history deployment/spring-boot-demo
```

### 回滾

```bash
# 回滾到上一版本
kubectl rollout undo deployment/spring-boot-demo

# 查看回滾狀態
kubectl rollout status deployment/spring-boot-demo
```

### 重啟 Pod

```bash
# 重啟所有 Pods（會滾動更新）
kubectl rollout restart deployment/spring-boot-demo

# 刪除 Pod（會自動重建）
kubectl delete pod <pod-name>
```

## 🗄️ 配置管理

### 使用 ConfigMap

```bash
# 從檔案創建
kubectl create configmap app-config \
  --from-file=src/main/resources/application.properties

# 從環境變數創建
kubectl create configmap env-config \
  --from-literal=SPRING_PROFILES_ACTIVE=prod \
  --from-literal=SERVER_PORT=8080

# 查看 ConfigMap
kubectl get configmap
kubectl describe configmap app-config
```

### 使用 Secret

```bash
# 創建 Secret
kubectl create secret generic db-credentials \
  --from-literal=username=admin \
  --from-literal=password=secret123

# 查看 Secret（不顯示值）
kubectl get secrets

# 查看 Secret 內容（Base64 編碼）
kubectl get secret db-credentials -o yaml
```

### 在 Deployment 中使用

```yaml
# 編輯部署配置
kubectl edit deployment spring-boot-demo

# 添加環境變數
env:
  - name: DB_USERNAME
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: username
  - name: SPRING_PROFILES_ACTIVE
    valueFrom:
      configMapKeyRef:
        name: env-config
        key: SPRING_PROFILES_ACTIVE
```

## 🌐 網路和服務

### 查看服務

```bash
# 列出所有服務
kubectl get svc

# 查看服務詳情
kubectl describe svc spring-boot-service

# 查看端點
kubectl get endpoints
```

### 測試服務連接

```bash
# 創建測試 Pod
kubectl run test-pod --rm -it --image=busybox -- sh

# 在 test-pod 內測試
wget -O- http://spring-boot-service:8080
nslookup spring-boot-service
```

### 端口轉發（臨時測試用）

```bash
# 將 Pod 端口轉發到本地
kubectl port-forward pod/<pod-name> 8080:8080

# 將服務端口轉發到本地
kubectl port-forward service/spring-boot-service 8080:8080
```

## 🐛 常見問題排解

### Pod 一直 Pending

```bash
# 查看原因
kubectl describe pod <pod-name>

# 常見原因：
# 1. 資源不足
kubectl top nodes
kubectl describe node k8s-master

# 2. 映像拉取失敗
docker images  # 確認映像存在

# 3. 調度問題
kubectl get events
```

### Pod CrashLoopBackOff

```bash
# 查看日誌
kubectl logs <pod-name>
kubectl logs <pod-name> --previous

# 查看 Pod 詳情
kubectl describe pod <pod-name>

# 常見原因：
# 1. 應用程式錯誤
# 2. 配置錯誤
# 3. 端口衝突
```

### ImagePullBackOff

```bash
# 使用本地映像
kubectl patch deployment spring-boot-demo \
  -p '{"spec":{"template":{"spec":{"containers":[{"name":"spring-boot-demo","imagePullPolicy":"Never"}]}}}}'

# 或編輯部署
kubectl edit deployment spring-boot-demo
# 將 imagePullPolicy 改為 IfNotPresent 或 Never
```

### 服務無法訪問

```bash
# 檢查 Pod 是否運行
kubectl get pods

# 檢查服務
kubectl get svc

# 檢查端點
kubectl get endpoints spring-boot-service

# 測試 Pod 直接訪問
POD_IP=$(kubectl get pod <pod-name> -o jsonpath='{.status.podIP}')
curl http://$POD_IP:8080
```

## 📋 完整部署檢查清單

```bash
# ✅ 步驟 1: 確認環境
kubectl get nodes              # 節點 Ready
kubectl get pods -A           # 系統 Pods Running
docker ps                     # Docker 運行中

# ✅ 步驟 2: 建構應用
cd /vagrant
mvn clean package            # 編譯成功
docker build -t spring-boot-demo:latest .  # 建構成功
docker images | grep spring-boot-demo      # 映像存在

# ✅ 步驟 3: 部署應用
kubectl create deployment spring-boot-demo --image=spring-boot-demo:latest
kubectl expose deployment spring-boot-demo --type=NodePort --port=8080

# ✅ 步驟 4: 驗證部署
kubectl get deployments       # READY 1/1
kubectl get pods             # STATUS Running
kubectl get svc              # NodePort 顯示

# ✅ 步驟 5: 測試訪問
NODE_PORT=$(kubectl get svc spring-boot-service -o jsonpath='{.spec.ports[0].nodePort}')
curl http://localhost:$NODE_PORT  # 返回正常響應
```

## 🔚 退出和清理

### 清理資源

```bash
# 刪除特定部署
kubectl delete deployment spring-boot-demo
kubectl delete service spring-boot-service

# 刪除命名空間（會刪除其中所有資源）
kubectl delete namespace spring-boot-app

# 刪除所有自建資源
kubectl delete all --all
```

### 退出 Vagrant

```bash
# 退出 SSH
exit

# 回到 Windows 後：
# 暫停 VM（保留狀態，快速恢復）
vagrant suspend

# 停止 VM（關機）
vagrant halt

# 重新啟動
vagrant up

# 重新登入
vagrant ssh
```

### 完全清理

```bash
# 刪除 VM（釋放磁碟空間）
vagrant destroy

# 重新建立環境
vagrant up
```

## 📚 常用命令速查

```bash
# 查看資源
kubectl get all                # 所有資源
kubectl get pods              # Pods
kubectl get svc               # 服務
kubectl get deployments       # 部署

# 詳細資訊
kubectl describe <resource> <name>
kubectl logs <pod-name>
kubectl exec -it <pod-name> -- bash

# 操作資源
kubectl apply -f <file>       # 應用配置
kubectl delete -f <file>      # 刪除配置
kubectl scale --replicas=N deployment/<name>  # 擴縮容
kubectl rollout restart deployment/<name>     # 重啟

# 除錯
kubectl get events           # 查看事件
kubectl top pods            # 資源使用
kubectl port-forward        # 端口轉發
```

## 🎯 實戰範例

### 完整部署流程

```bash
# 1. 登入 Vagrant
vagrant ssh

# 2. 進入專案
cd /vagrant

# 3. 建構專案
mvn clean package

# 4. 建構映像
cat > Dockerfile <<EOF
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
EOF

docker build -t myapp:1.0 .

# 5. 部署
kubectl create deployment myapp --image=myapp:1.0
kubectl set image deployment/myapp myapp=myapp:1.0
kubectl patch deployment myapp -p '{"spec":{"template":{"spec":{"containers":[{"name":"myapp","imagePullPolicy":"Never"}]}}}}'

# 6. 暴露服務
kubectl expose deployment myapp --port=8080 --type=NodePort

# 7. 獲取訪問地址
kubectl get svc myapp
# 訪問: http://192.168.10.10:<NodePort>
```

---

**提示**: 
- 所有命令都在 Vagrant VM 內執行（vagrant ssh 之後）
- `/vagrant` 目錄對應 Windows 的 `c:\JOHNY\test`
- 可以在 Windows 編輯檔案，在 VM 內執行命令
- 遇到問題先查看 `kubectl describe` 和 `kubectl logs`

**需要更多幫助？** 查看 [README-K8S.md](README-K8S.md)

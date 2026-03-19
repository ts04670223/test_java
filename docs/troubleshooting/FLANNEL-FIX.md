# Flannel 網絡插件錯誤修復指南

## 問題描述
```
Failed to create pod sandbox: rpc error: code = Unknown desc = failed to setup network for sandbox 
"99a41de6d2b1f9adb08b374230cccc6a1620072d8661aa7db2c7c4d3738d61bd": 
plugin type="flannel" failed (add): failed to load flannel 'subnet.env' file: 
open /run/flannel/subnet.env: no such file or directory
```

這個錯誤表示 Flannel 網絡插件沒有正確運行，導致 Kubernetes 無法為 Pod 創建網絡。

## 原因分析
1. Flannel DaemonSet 可能沒有正確啟動
2. `/run/flannel/subnet.env` 文件沒有被創建
3. CNI 配置可能損壞
4. containerd 和 Flannel 之間的通信問題

## 快速修復步驟

### 方法 1: 使用自動修復腳本（推薦）

1. SSH 進入 Vagrant VM：
```bash
vagrant ssh
```

2. 執行修復腳本：
```bash
cd /vagrant/scripts
chmod +x fix-flannel.sh
sudo ./fix-flannel.sh
```

3. 等待修復完成，檢查結果

### 方法 2: 手動修復步驟

如果自動腳本無法解決，按照以下步驟手動修復：

#### 步驟 1: 檢查當前狀態
```bash
# 檢查 Flannel pods
kubectl get pods -n kube-flannel

# 檢查 Flannel 日誌
kubectl logs -n kube-flannel -l app=flannel --tail=50
```

#### 步驟 2: 刪除並重新安裝 Flannel
```bash
# 刪除現有 Flannel
kubectl delete -f https://github.com/flannel-io/flannel/releases/latest/download/kube-flannel.yml

# 清理 CNI 配置
sudo rm -rf /etc/cni/net.d/*
sudo rm -rf /var/lib/cni/
sudo rm -rf /run/flannel/

# 重啟 containerd
sudo systemctl restart containerd
sleep 5

# 重新安裝 Flannel
kubectl apply -f https://github.com/flannel-io/flannel/releases/latest/download/kube-flannel.yml
```

#### 步驟 3: 確保系統參數正確
```bash
# 加載內核模組
sudo modprobe br_netfilter

# 設置系統參數
sudo sysctl -w net.bridge.bridge-nf-call-iptables=1
sudo sysctl -w net.ipv4.ip_forward=1
```

#### 步驟 4: 等待 Flannel 啟動
```bash
# 監控 Flannel pods 狀態
kubectl get pods -n kube-flannel -w

# 等待所有 Flannel pods 變成 Running 狀態（約 1-2 分鐘）
```

#### 步驟 5: 驗證 subnet.env 文件
```bash
# 檢查文件是否存在
ls -la /run/flannel/subnet.env

# 查看內容
cat /run/flannel/subnet.env
```

應該看到類似：
```
FLANNEL_NETWORK=10.244.0.0/16
FLANNEL_SUBNET=10.244.0.1/24
FLANNEL_MTU=1450
FLANNEL_IPMASQ=true
```

#### 步驟 6: 重新創建失敗的 App Pods
```bash
# 刪除失敗的 app pods（會自動重建）
kubectl delete pods -l io.kompose.service=app --grace-period=0 --force

# 監控新 pod 的啟動
kubectl get pods -l io.kompose.service=app -w
```

## 診斷工具

使用診斷腳本檢查網絡狀態：
```bash
cd /vagrant/scripts
chmod +x check-k8s-network.sh
sudo ./check-k8s-network.sh
```

## 常見問題

### Q1: Flannel pods 卡在 Init 狀態
**解決方案：**
```bash
# 檢查 Flannel pods 詳情
kubectl describe pods -n kube-flannel

# 檢查 CoreDNS 是否正常
kubectl get pods -n kube-system -l k8s-app=kube-dns

# 重啟 kubelet
sudo systemctl restart kubelet
```

### Q2: App pod 啟動探針失敗
這是第二個錯誤：`Startup probe failed: Get "http://10.244.0.240:8080/actuator/health"`

修復網絡問題後，如果 App 仍然失敗，檢查：

1. **檢查 App 是否正常啟動：**
```bash
kubectl logs -l io.kompose.service=app --tail=100
```

2. **檢查啟動探針配置：**
```bash
kubectl describe deployment app | grep -A 10 "Startup"
```

3. **暫時移除啟動探針進行測試：**
編輯 `app-deployment.yaml`，註釋或刪除 `startupProbe` 部分：
```yaml
# startupProbe:
#   httpGet:
#     path: /actuator/health
#     port: 8080
#   initialDelaySeconds: 30
#   periodSeconds: 10
```

然後重新部署：
```bash
kubectl apply -f /vagrant/app-deployment.yaml
```

### Q3: 網絡完全無法工作
**完全重置方案：**
```bash
# 重置整個 Kubernetes 叢集
sudo kubeadm reset -f
sudo rm -rf /etc/kubernetes/
sudo rm -rf /etc/cni/net.d/
sudo rm -rf /var/lib/cni/
sudo rm -rf /run/flannel/
sudo rm -rf /home/vagrant/.kube/

# 重新初始化
cd /vagrant/scripts
sudo ./setup-k8s-cluster.sh
```

## 驗證修復成功

1. **Flannel 正常運行：**
```bash
kubectl get pods -n kube-flannel
# 所有 pods 應該是 Running 狀態
```

2. **subnet.env 文件存在：**
```bash
cat /run/flannel/subnet.env
# 應該顯示網絡配置
```

3. **App pods 正常運行：**
```bash
kubectl get pods -l io.kompose.service=app
# 狀態應該是 Running，READY 應該是 1/1
```

4. **App 健康檢查通過：**
```bash
# 獲取 pod 名稱
POD_NAME=$(kubectl get pods -l io.kompose.service=app -o jsonpath='{.items[0].metadata.name}')

# 進入 pod 測試健康檢查
kubectl exec -it $POD_NAME -- curl http://localhost:8080/actuator/health
```

應該返回：
```json
{"status":"UP"}
```

## 預防措施

為避免將來再次出現此問題：

1. **確保 VM 有足夠資源：**
   - 至少 2 CPU 核心
   - 至少 2GB RAM

2. **檢查 Vagrantfile 配置：**
```ruby
config.vm.provider "virtualbox" do |vb|
  vb.memory = "2048"
  vb.cpus = 2
end
```

3. **在 VM 重啟後等待所有服務就緒：**
```bash
# 等待 Flannel 完全啟動
kubectl wait --for=condition=ready pod -l app=flannel -n kube-flannel --timeout=300s

# 然後再部署應用
kubectl apply -f /vagrant/app-deployment.yaml
```

## 相關文件

- 修復腳本：[scripts/fix-flannel.sh](../scripts/fix-flannel.sh)
- 診斷腳本：[scripts/check-k8s-network.sh](../scripts/check-k8s-network.sh)
- App 部署配置：[app-deployment.yaml](../app-deployment.yaml)
- K8s 設置腳本：[scripts/setup-k8s-cluster.sh](../scripts/setup-k8s-cluster.sh)

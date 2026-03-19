# Kubernetes 安裝問題排查指南

## 🔍 問題：kubectl 命令找不到

當您在 Vagrant VM 中看到 `Command 'kubectl' not found` 時，表示 Kubernetes 安裝腳本沒有正確執行。

## ✅ 快速修復方法

### 方法一：使用自動修復腳本（推薦）

在 Vagrant VM 內執行：

```bash
# 1. 確認您在 VM 內（提示符應該是 vagrant@k8s-master）
# 2. 執行修復腳本
sudo bash /vagrant/fix-k8s-install.sh
```

### 方法二：檢查並手動執行安裝腳本

```bash
# 1. 檢查腳本是否存在
ls -la /vagrant/scripts/

# 2. 手動執行 Docker 安裝（如果需要）
sudo bash /vagrant/scripts/install-docker.sh

# 3. 手動執行 Kubernetes 安裝
sudo bash /vagrant/scripts/install-k8s.sh

# 4. 手動執行集群設置
sudo bash /vagrant/scripts/setup-k8s-cluster.sh

# 5. 設置 kubectl 配置
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

# 6. 驗證安裝
kubectl get nodes
```

### 方法三：從頭重新安裝

如果以上方法都不行，可以重新安裝整個環境：

```bash
# 1. 退出 VM
exit

# 2. 在 Windows 中銷毀並重建 VM
vagrant destroy -f
vagrant up

# 3. 重新登入
vagrant ssh
```

## 🔧 詳細排查步驟

### 步驟 1: 檢查 provisioning 是否執行

在 Vagrant VM 內：

```bash
# 檢查是否有執行記錄
ls -la /var/log/

# 檢查 Vagrant provision 狀態
cat /var/log/syslog | grep provision
```

### 步驟 2: 檢查 Docker 安裝

```bash
# 檢查 Docker
docker --version
sudo systemctl status docker

# 如果 Docker 未安裝
sudo bash /vagrant/scripts/install-docker.sh
```

### 步驟 3: 檢查 Kubernetes 組件

```bash
# 檢查 kubectl
which kubectl
kubectl version --client

# 檢查 kubeadm
which kubeadm
kubeadm version

# 檢查 kubelet
systemctl status kubelet
```

### 步驟 4: 檢查集群狀態

```bash
# 檢查集群配置文件
sudo ls -la /etc/kubernetes/

# 如果沒有 admin.conf，表示集群未初始化
sudo ls -la /etc/kubernetes/admin.conf
```

### 步驟 5: 手動初始化集群（如果需要）

```bash
# 初始化 Kubernetes 集群
sudo kubeadm init \
  --apiserver-advertise-address=192.168.10.10 \
  --pod-network-cidr=10.244.0.0/16 \
  --node-name k8s-master

# 設置 kubectl 配置
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

# 允許在 master 節點調度 pods
kubectl taint nodes --all node-role.kubernetes.io/control-plane- || true

# 安裝網路插件
kubectl apply -f https://github.com/flannel-io/flannel/releases/latest/download/kube-flannel.yml

# 等待節點就緒
kubectl get nodes -w
```

## 🚨 常見錯誤和解決方案

### 錯誤 1: swap 未關閉

```bash
# 關閉 swap
sudo swapoff -a
sudo sed -i '/ swap / s/^/#/' /etc/fstab
```

### 錯誤 2: 防火牆阻擋

```bash
# 檢查並配置防火牆
sudo ufw status
sudo ufw allow 6443/tcp
sudo ufw allow 10250/tcp
```

### 錯誤 3: kubelet 未啟動

```bash
# 檢查 kubelet 日誌
sudo journalctl -u kubelet -f

# 重啟 kubelet
sudo systemctl restart kubelet
```

### 錯誤 4: 環境變數未設置

```bash
# 臨時設置
export KUBECONFIG=$HOME/.kube/config

# 永久設置
echo 'export KUBECONFIG=$HOME/.kube/config' >> ~/.bashrc
source ~/.bashrc
```

## 📋 完整驗證檢查清單

```bash
# ✅ 檢查 Docker
docker --version
sudo docker ps

# ✅ 檢查 kubectl
kubectl version --client

# ✅ 檢查節點
kubectl get nodes

# ✅ 檢查系統 Pods
kubectl get pods -A

# ✅ 檢查網路
kubectl get pods -n kube-flannel

# ✅ 檢查服務
kubectl get svc -A
```

## 🔄 如果需要完全重置

```bash
# 在 VM 內重置 Kubernetes
sudo kubeadm reset -f
sudo rm -rf /etc/kubernetes/
sudo rm -rf $HOME/.kube/

# 清理 iptables
sudo iptables -F && sudo iptables -t nat -F && sudo iptables -t mangle -F && sudo iptables -X

# 重新初始化
sudo bash /vagrant/scripts/setup-k8s-cluster.sh
```

## 💡 預防措施

為了避免將來出現此問題：

1. **檢查 Vagrantfile**
   ```ruby
   # 確保有這些行
   config.vm.provision "shell", path: "scripts/install-docker.sh"
   config.vm.provision "shell", path: "scripts/install-k8s.sh"
   config.vm.provision "shell", path: "scripts/setup-k8s-cluster.sh"
   ```

2. **首次啟動時查看日誌**
   ```bash
   vagrant up --provision
   # 仔細觀察輸出，確保沒有錯誤
   ```

3. **保存日誌**
   ```bash
   vagrant up --provision > vagrant-install.log 2>&1
   ```

## 📞 需要幫助？

如果問題仍然存在，請提供：

1. `vagrant up` 的完整輸出
2. VM 內的 `sudo journalctl -u kubelet` 輸出
3. `/var/log/syslog` 中的相關錯誤訊息

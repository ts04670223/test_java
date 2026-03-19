# 🔑 K8s 機器登入指南

完整的 Vagrant VM（Kubernetes 機器）登入和操作說明。

## 📋 目錄

1. [登入方式](#登入方式)
2. [常用操作](#常用操作)
3. [文件訪問](#文件訪問)
4. [網路配置](#網路配置)
5. [故障排查](#故障排查)

---

## 登入方式

### 🪟 Windows 登入

#### 方式一：PowerShell（推薦）

```powershell
# 1. 切換到專案目錄
cd c:\JOHNY\test

# 2. 登入 VM
vagrant ssh
```

#### 方式二：CMD

```cmd
cd c:\JOHNY\test
vagrant ssh
```

### 🐧 Linux / macOS 登入

```bash
cd /path/to/project
vagrant ssh
```

---

## 成功登入的標誌

登入成功後會看到以下提示符：

```
vagrant@k8s-master:~$
```

這表示：
- ✅ 已進入 Vagrant VM
- ✅ 當前用戶是 `vagrant`
- ✅ 主機名是 `k8s-master`（Kubernetes 主節點）
- ✅ 當前目錄是 `/home/vagrant`

---

## 常用操作

### Kubernetes 操作

```bash
# 查看節點狀態
kubectl get nodes

# 查看所有 Pods（所有命名空間）
kubectl get pods -A

# 查看所有 Pods（特定命名空間）
kubectl get pods -n kubernetes-dashboard

# 查看服務
kubectl get svc -A

# 查看部署
kubectl get deployment -A

# 查看集群信息
kubectl cluster-info

# 查看詳細信息
kubectl describe pod <pod-name> -n <namespace>

# 查看 Pod 日誌
kubectl logs <pod-name> -n <namespace>

# 進入 Pod 容器
kubectl exec -it <pod-name> -n <namespace> -- /bin/bash
```

### Docker 操作

```bash
# 查看運行中的容器
docker ps

# 查看所有容器（包括停止的）
docker ps -a

# 查看映像
docker images

# 查看容器日誌
docker logs <container-id>

# 進入容器
docker exec -it <container-id> /bin/bash

# 清理未使用的資源
docker system prune -a
```

### 系統資源查看

```bash
# 查看記憶體使用
free -h

# 查看磁碟使用
df -h

# 查看 CPU 和記憶體
top
htop  # 如果已安裝

# 查看系統資訊
uname -a
lsb_release -a

# 查看網路配置
ip addr
ifconfig  # 如果已安裝
```

---

## 文件訪問

### Windows 專案目錄映射

Windows 的專案目錄自動映射到 VM 內：

| Windows 路徑 | VM 內路徑 |
|-------------|----------|
| `c:\JOHNY\test\` | `/vagrant/` |

### 訪問專案文件

```bash
# 切換到專案目錄
cd /vagrant

# 查看文件
ls -la

# 查看 Maven 構建結果
ls -lh target/*.jar

# 編輯文件（使用 vim）
vim pom.xml

# 編輯文件（使用 nano，更簡單）
nano README.md
```

### 文件同步說明

- ✅ **雙向同步**: 在 Windows 或 VM 內修改文件都會同步
- ✅ **即時更新**: 修改會立即反映到另一側
- ⚠️ **權限差異**: VM 內的權限設置可能與 Windows 不同

---

## 網路配置

### 端口轉發

Vagrantfile 已配置以下端口轉發：

| 服務 | VM 端口 | Windows 端口 | 說明 |
|------|---------|-------------|------|
| Spring Boot | 8080 | 8080 | 應用主端口 |
| MySQL | 3306 | 3307 | 資料庫 |
| Redis | 6379 | 6379 | 快取 |
| kubectl proxy | 8001 | 8001 | K8s Dashboard |

### 測試網路連接

```bash
# 在 VM 內測試
curl http://localhost:8080/actuator/health

# 測試 MySQL 連接
mysql -h localhost -P 3306 -u root -p

# 測試 Redis 連接
redis-cli ping
```

---

## 進階操作

### 執行一次性命令（不登入 VM）

```cmd
# 在 Windows 執行，不進入 VM
vagrant ssh -c "kubectl get nodes"
vagrant ssh -c "docker ps"
vagrant ssh -c "free -h"
```

### 以 root 權限執行

```bash
# 登入 VM 後，切換到 root
sudo su

# 或單個命令使用 sudo
sudo kubectl get nodes
sudo docker ps
```

### 後台執行命令

```bash
# 使用 nohup 後台執行
nohup kubectl proxy --address='0.0.0.0' --port=8001 --accept-hosts='.*' > /tmp/proxy.log 2>&1 &

# 查看後台進程
ps aux | grep kubectl

# 終止後台進程
pkill kubectl
```

---

## 退出 VM

### 退出方式

```bash
# 方式 1: 輸入 exit 命令
exit

# 方式 2: 使用快捷鍵
Ctrl+D
```

退出後會返回到 Windows 的 PowerShell/CMD。

### ⚠️ 注意事項

- ❌ **不要使用** `vagrant halt` 在 VM 內（這會關閉 VM）
- ✅ **使用** `exit` 或 `Ctrl+D` 退出 SSH 連接
- ✅ VM 會繼續運行，隨時可以再次登入

---

## 故障排查

### 無法登入 VM

#### 問題：`vagrant ssh` 無回應

```cmd
# 1. 檢查 VM 狀態
vagrant status

# 2. 如果 VM 未運行，啟動它
vagrant up

# 3. 如果 VM 運行中但無法 SSH
vagrant reload
```

#### 問題：SSH 連接超時

```cmd
# 重啟 VM
vagrant reload

# 強制重啟
vagrant halt
vagrant up
```

### VM 內無法執行 kubectl

#### 問題：`kubectl: command not found`

```bash
# 檢查 kubectl 是否安裝
which kubectl

# 如果未安裝，重新運行安裝腳本
sudo bash /vagrant/scripts/install-k8s.sh
```

#### 問題：`The connection to the server ... was refused`

```bash
# 檢查 Kubernetes 服務狀態
sudo systemctl status kubelet

# 重啟 Kubernetes
sudo systemctl restart kubelet

# 查看日誌
sudo journalctl -xeu kubelet
```

### Docker 無法使用

#### 問題：`Cannot connect to the Docker daemon`

```bash
# 啟動 Docker
sudo systemctl start docker

# 檢查狀態
sudo systemctl status docker

# 添加當前用戶到 docker 組
sudo usermod -aG docker vagrant
exit  # 重新登入以生效
```

---

## 🔒 安全建議

### SSH 密鑰

- Vagrant 使用不安全的預設密鑰（開發環境可接受）
- 生產環境應使用自定義 SSH 密鑰

### 用戶權限

- 預設用戶 `vagrant` 有 sudo 權限
- 執行危險操作時要小心（如 `rm -rf`）

### 網路安全

- VM 的網路配置允許外部訪問（端口轉發）
- 不要在生產環境使用相同配置

---

## 📚 相關文檔

- [Vagrant 使用指南](VAGRANT-GUIDE.md) - 完整 Vagrant 操作說明
- [K8s Dashboard 指南](K8S-DASHBOARD.md) - Kubernetes 管理界面
- [Docker 使用指南](DOCKER-GUIDE.md) - Docker 容器操作
- [故障排查](TROUBLESHOOTING.md) - 常見問題解決

---

## 💡 實用技巧

### 快速命令別名

在 VM 內編輯 `~/.bashrc` 添加別名：

```bash
# 編輯配置文件
nano ~/.bashrc

# 添加以下內容
alias k='kubectl'
alias kgp='kubectl get pods -A'
alias kgn='kubectl get nodes'
alias kgs='kubectl get svc -A'
alias dp='docker ps'
alias di='docker images'

# 重新載入配置
source ~/.bashrc
```

### 保持 kubectl proxy 運行

使用 systemd 服務（持久化）：

```bash
# 創建服務文件
sudo nano /etc/systemd/system/kubectl-proxy.service

# 內容：
[Unit]
Description=kubectl proxy
After=network.target

[Service]
User=vagrant
ExecStart=/usr/bin/kubectl proxy --address=0.0.0.0 --port=8001 --accept-hosts=.*
Restart=always

[Install]
WantedBy=multi-user.target

# 啟用服務
sudo systemctl daemon-reload
sudo systemctl enable kubectl-proxy
sudo systemctl start kubectl-proxy
```

---

**記住這個簡單命令即可登入**：

```cmd
vagrant ssh
```

**就是這麼簡單！** 🎉

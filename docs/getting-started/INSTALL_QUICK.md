# Spring Boot 電商平台 - 快速安裝

這是一個簡化的主安裝腳本，調用 tools 資料夾中的工具。

## 🚀 安裝步驟

### 步驟 1: 安裝前置軟體（VirtualBox + Vagrant）
以**管理員權限**執行 PowerShell：
```powershell
.\tools\install-prerequisites.ps1
```

### 步驟 2: 驗證需求
```cmd
.\tools\check-requirements.bat
```

### 步驟 3: 啟動服務
```cmd
.\install.bat
```

### 步驟 4: 等待啟動
```cmd
.\tools\wait-for-app.bat
```

### 步驟 5: 驗證部署
```cmd
.\tools\verify-deployment.bat
```

## 📝 詳細文檔

- [安裝包總覽](PACKAGES_OVERVIEW.md)
- [完整安裝指南](docs/INSTALL.md)
- [快速開始](docs/QUICKSTART.md)
- [故障排查](docs/TROUBLESHOOTING.md)

## 🛠️ 常用工具

### Docker 管理
```cmd
.\tools\start-docker.bat        # 啟動服務
.\tools\rebuild-docker.bat      # 重建容器
.\tools\fix-and-restart.bat     # 修復並重啟
.\tools\docker-access.bat       # 訪問容器
```

### 診斷工具
```cmd
.\tools\diagnose.bat            # 系統診斷
```

## 📞 幫助

如有問題，請執行診斷工具或查看 [故障排查文檔](docs/TROUBLESHOOTING.md)。

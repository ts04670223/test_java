# rebuild_and_run.ps1
# 在 Windows 上重新編譯 Java 程式並執行
# 使用方式：在 c:\JOHNY\test 目錄下執行 .\rebuild_and_run.ps1

$ErrorActionPreference = "Stop"

$APP_DIR    = "c:\JOHNY\test"
$BUILD_DIR  = "C:\Temp\spring-boot-build"   # 本地目錄，避免 vboxsf 檔案鎖問題
$JAR_NAME   = "spring-boot-demo-0.0.1-SNAPSHOT.jar"
$JAR_PATH   = "$BUILD_DIR\target\$JAR_NAME"
$LOG_FILE   = "$APP_DIR\logs\app.log"
$PID_FILE   = "$APP_DIR\app.pid"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  Spring Boot 重新編譯並啟動腳本 (Windows)" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

# --- 1. 停止舊版程式 ---
Write-Host ""
Write-Host "[1/4] 停止舊版 Spring Boot 應用程式..." -ForegroundColor Yellow

if (Test-Path $PID_FILE) {
    $oldPid = Get-Content $PID_FILE -ErrorAction SilentlyContinue
    if ($oldPid) {
        $proc = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "  -> 終止 PID: $oldPid"
            Stop-Process -Id $oldPid -Force
        }
    }
    Remove-Item $PID_FILE -Force
}

# 備援：找並終止所有持有 JAR 的 java 程序
Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    $_.MainWindowTitle -like "*spring*" -or
    (($_.CommandLine -ne $null) -and ($_.CommandLine -like "*$JAR_NAME*"))
} | ForEach-Object {
    Write-Host "  -> 終止殘留 java 程序 PID: $($_.Id)"
    Stop-Process -Id $_.Id -Force
}

# 等待 JAR 檔案釋放（最多 10 秒）
Write-Host "  -> 等待 JAR 檔案釋放..."
$waited = 0
while ($waited -lt 10) {
    try {
        if (Test-Path $JAR_PATH) {
            $stream = [System.IO.File]::Open($JAR_PATH, 'Open', 'Read', 'None')
            $stream.Close()
        }
        break
    } catch {
        Start-Sleep -Seconds 1
        $waited++
    }
}
Write-Host "  -> 完成"

# --- 2. 確認 Java 版本 ---
Write-Host ""
Write-Host "[2/4] 確認 Java 版本..." -ForegroundColor Yellow

$javaOutput = cmd /c "java -version 2>&1"
$javaVer = ($javaOutput | Select-String "version" | ForEach-Object {
    if ($_ -match '"(\d+)') { $Matches[1] }
}) | Select-Object -First 1
Write-Host "  -> 使用 Java 主版本: $javaVer"
if ([int]$javaVer -lt 17) {
    Write-Host "  [錯誤] 需要 Java 17 以上，目前為 $javaVer" -ForegroundColor Red
    exit 1
}

# --- 3. 同步原始碼並編譯 ---
Write-Host ""
Write-Host "[3/4] 重新編譯 (在本地目錄，避免 vboxsf 鎖定問題)..." -ForegroundColor Yellow

# 清除並重建 build 目錄
if (Test-Path $BUILD_DIR) {
    Remove-Item "$BUILD_DIR\target" -Recurse -Force -ErrorAction SilentlyContinue
}
Write-Host "  -> 同步原始碼到: $BUILD_DIR"
if (-not (Test-Path $BUILD_DIR)) { New-Item -ItemType Directory -Path $BUILD_DIR | Out-Null }

# 複製原始碼（排除 target 和 .git）
robocopy $APP_DIR $BUILD_DIR /E /XD "target" ".git" ".vagrant" "node_modules" /XF "*.log" /NFL /NDL /NJH /NJS | Out-Null

Set-Location $BUILD_DIR
Write-Host "  -> 開始 Maven 編譯..."
& mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [錯誤] Maven 編譯失敗" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $JAR_PATH)) {
    Write-Host "  [錯誤] 找不到 JAR: $JAR_PATH" -ForegroundColor Red
    exit 1
}
Write-Host "  -> 編譯完成: $JAR_PATH"

# --- 4. 複製 JAR 並重啟 K8s Pod ---
Write-Host ""
Write-Host "[4/4] 部署新版 JAR 並重啟 K8s Pod..." -ForegroundColor Yellow

# 回到 APP_DIR，確保 vagrant 能找到 Vagrantfile
Set-Location $APP_DIR

# 確保目標目錄存在（/vagrant/target 在 VM 內即此路徑）
$targetDir = "$APP_DIR\target"
if (-not (Test-Path $targetDir)) { New-Item -ItemType Directory -Path $targetDir | Out-Null }

# 複製 JAR 到 K8s Pod 讀取的 hostPath 位置
Copy-Item -Path $JAR_PATH -Destination "$targetDir\$JAR_NAME" -Force
Write-Host "  -> JAR 已複製到：$targetDir\$JAR_NAME"

# 透過 vagrant ssh 強制重啟 Pod
Write-Host "  -> 透過 vagrant ssh 重啟 K8s Pod..."
vagrant ssh -c "kubectl delete pod -l io.kompose.service=app --grace-period=0 --force 2>/dev/null || true"

if ($LASTEXITCODE -ne 0) {
    Write-Host "  [警告] vagrant ssh 指令失敗，請確認 Vagrant VM 是否正在執行" -ForegroundColor Yellow
    Write-Host "  -> 請手動執行：vagrant ssh -c `"kubectl delete pod -l io.kompose.service=app --grace-period=0 --force`""
    exit 1
}

Write-Host "  -> Pod 已重啟，等待就緒..."

# 等待新 Pod 就緒（最多 3 分鐘）
for ($i = 1; $i -le 36; $i++) {
    Start-Sleep -Seconds 5
    $ready = vagrant ssh -c "kubectl get pods -l io.kompose.service=app -o jsonpath='{.items[*].status.containerStatuses[*].ready}' 2>/dev/null" 2>$null
    if ($ready -match "true") {
        Write-Host ""
        Write-Host "======================================" -ForegroundColor Green
        Write-Host "  部署成功！($($i*5) 秒)" -ForegroundColor Green
        Write-Host "  API: http://test6.test/api" -ForegroundColor Green
        Write-Host "======================================" -ForegroundColor Green
        exit 0
    }
    $status = vagrant ssh -c "kubectl get pods -l io.kompose.service=app --no-headers 2>/dev/null | awk '{print `"READY=`"\$2, `"STATUS=`"\$3}' | head -1" 2>$null
    Write-Host "  -> 等待中... ($i/36) $status"
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  Pod 仍在啟動中，請稍後確認：" -ForegroundColor Cyan
Write-Host "  vagrant ssh -c `"kubectl get pods -l io.kompose.service=app`"" -ForegroundColor Cyan
Write-Host "  vagrant ssh -c `"kubectl logs -l io.kompose.service=app --tail=50`"" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

# 將 mkcert rootCA 匯入 Windows 受信任的根憑證授權單位
# 需要以系統管理員身分執行
# 用法：.\scripts\nginx\import-root-ca.ps1

$CertPath = "$PSScriptRoot\ssl\rootCA.pem"

if (-not (Test-Path $CertPath)) {
    Write-Host "✗ 找不到 rootCA.pem：$CertPath" -ForegroundColor Red
    Write-Host "  請先在 VM 中執行：bash /vagrant/scripts/nginx/setup-ssl.sh" -ForegroundColor Yellow
    exit 1
}

# 確認以系統管理員身分執行
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "✗ 請用系統管理員身分執行此腳本" -ForegroundColor Red
    Write-Host "  右鍵點選 PowerShell → 以系統管理員身分執行" -ForegroundColor Yellow
    exit 1
}

Write-Host "→ 匯入 rootCA.pem 到 Windows 受信任的根憑證授權單位..." -ForegroundColor Cyan

Import-Certificate -FilePath $CertPath -CertStoreLocation Cert:\LocalMachine\Root

Write-Host ""
Write-Host "✓ 根憑證匯入成功！" -ForegroundColor Green
Write-Host ""
Write-Host "  現在請重新開啟瀏覽器，用 https://test6.test 存取網站" -ForegroundColor White
Write-Host "  Passkeys / WebAuthn 功能將可正常使用" -ForegroundColor White

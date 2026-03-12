#!/bin/bash
# rebuild_and_run.sh
# 重新編譯 Java 程式並以新版本執行

set -e  # 任何步驟失敗就中止

APP_DIR="/vagrant"
BUILD_DIR="/tmp/spring-boot-build"   # VM 本地目錄，不受 vboxsf 鎖定影響
JAR_NAME="spring-boot-demo-0.0.1-SNAPSHOT.jar"
JAR_PATH="$BUILD_DIR/target/$JAR_NAME"
LOG_FILE="$APP_DIR/app.log"
PID_FILE="$APP_DIR/app.pid"

echo "======================================"
echo "  Spring Boot 重新編譯並啟動腳本"
echo "======================================"

# --- 1. 停止舊版程式 ---
echo ""
echo "[1/4] 停止舊版 Spring Boot 應用程式..."

# 從 PID 檔終止
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if kill -0 "$OLD_PID" 2>/dev/null; then
        echo "  -> 發送 SIGTERM 給 PID: $OLD_PID"
        kill "$OLD_PID" 2>/dev/null || true
    fi
    rm -f "$PID_FILE"
fi

# 備援：找並終止所有殘留的 java/mvn 程序
for SPID in $(pgrep -f "$JAR_NAME" 2>/dev/null) $(pgrep -f "spring-boot:run" 2>/dev/null); do
    echo "  -> 終止殘留程序 PID: $SPID"
    kill "$SPID" 2>/dev/null || true
done

# 等待程序真正結束（最多 15 秒），否則強制 SIGKILL
echo "  -> 等待程序結束..."
for i in $(seq 1 15); do
    REMAINING=$(pgrep -f "$JAR_NAME" 2>/dev/null || pgrep -f "spring-boot:run" 2>/dev/null || true)
    if [ -z "$REMAINING" ]; then
        echo "  -> 程序已結束"
        break
    fi
    if [ "$i" -eq 10 ]; then
        echo "  -> 程序未響應，強制終止 (SIGKILL)..."
        kill -9 $REMAINING 2>/dev/null || true
    fi
    sleep 1
done

# 額外確保 JAR 檔已釋放（等候 OS 釋放檔案鎖）
sleep 2
echo "  -> 完成"

# --- 2. 確認 Java 版本 ---
echo ""
echo "[2/4] 確認 Java 版本..."
java -version
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
echo "  -> 使用 Java 主版本: $JAVA_VER"

if [ "$JAVA_VER" -lt 17 ]; then
    echo "  [錯誤] 需要 Java 17 以上，目前為 $JAVA_VER，請先安裝正確版本"
    exit 1
fi

# --- 3. Maven 清除並重新編譯打包 ---
echo ""
echo "[3/4] 執行 Maven clean package (跳過測試)..."

# 在 VM 本地目錄 build，完全避開 vboxsf 的 Windows 檔案鎖問題
# 優化：不再每次刪除 target，保留增量編譯能力，僅清理最終 jar 確保新舊替換
echo "  -> 準備本地 build 目錄: $BUILD_DIR"
rm -f "$JAR_PATH"  

echo "  -> 同步原始碼 (僅同步有變更的文件)..."
mkdir -p "$BUILD_DIR"
# rsync 優化：排除不必要的目錄，僅更新變更檔案
rsync -a --size-only --no-perms --no-owner --no-group \
    --exclude='target/' --exclude='.git/' --exclude='.vagrant/' \
    --exclude='node_modules/' --exclude='*.log' \
    "$APP_DIR/" "$BUILD_DIR/"

cd "$BUILD_DIR"
echo "  -> 開始編譯 (增量模式)..."
# 改用 install 而非 clean package，且跳過 javadoc/source 加速
mvn install -DskipTests -Dmaven.javadoc.skip=true -Dmaven.source.skip=true -Dmaven.test.skip=true

if [ ! -f "$JAR_PATH" ]; then
    echo "  [錯誤] 找不到 JAR 檔：$JAR_PATH"
    exit 1
fi

echo "  -> 編譯完成：$JAR_PATH"

# --- 4. 複製 JAR 並重啟 K8s Pod ---
echo ""
echo "[4/4] 部署新版 JAR 並重啟 K8s Pod..."

# 確保目標目錄存在
mkdir -p "$APP_DIR/target"

# 複製 JAR 到 K8s Pod 讀取的位置（hostPath: /vagrant/target）
cp "$JAR_PATH" "$APP_DIR/target/$JAR_NAME"
echo "  -> JAR 已複製到：$APP_DIR/target/$JAR_NAME"

# 強制刪除 Pod 使其立即重建（Deployment 會自動拉起新 Pod）
kubectl delete pod -l io.kompose.service=app --grace-period=0 --force 2>/dev/null || true
echo "  -> Pod 已重啟，等待就緒..."

# 等待新 Pod 就緒
for i in $(seq 1 36); do
    sleep 5
    READY=$(kubectl get pods -l io.kompose.service=app \
        -o jsonpath='{.items[*].status.containerStatuses[*].ready}' 2>/dev/null | tr ' ' '\n' | grep -c "^true$" || echo 0)
    if [ "${READY:-0}" -ge 1 ]; then
        echo ""
        echo "======================================"
        echo "  部署成功！(${i}*5 秒)"
        echo "  API: http://test6.test/api"
        echo "======================================"
        exit 0
    fi
    STATUS=$(kubectl get pods -l io.kompose.service=app --no-headers 2>/dev/null \
        | awk '{print "READY="$2, "STATUS="$3}' | head -1)
    echo "  -> 等待中... (${i}/36) $STATUS"
done

echo ""
echo "======================================"
echo "  Pod 仍在啟動中，請稍後確認："
echo "  kubectl get pods -l io.kompose.service=app"
echo "  kubectl logs -l io.kompose.service=app --tail=50"
echo "======================================"

#!/bin/bash
echo "[Fix] 等待 Flannel subnet.env 就緒..."
for i in $(seq 1 30); do
    if [ -f /run/flannel/subnet.env ]; then
        SUBNET=$(grep FLANNEL_SUBNET /run/flannel/subnet.env 2>/dev/null)
        if [ -n "$SUBNET" ]; then
            echo "[Fix] Flannel 就緒: $SUBNET"
            break
        fi
    fi
    echo "[Fix] 第 ${i}/30 次等待..."
    sleep 3
done

echo "[Fix] 刪除 Pending/Error 狀態的 App Pods，讓 k8s 重建..."
kubectl delete pods -l io.kompose.service=app \
    --field-selector=status.phase!=Running \
    --grace-period=0 --force 2>/dev/null || true

# 同時修復所有 namespace 的 sandbox 失敗 pods
kubectl get pods -A --field-selector=status.phase=Pending -o json | \
    jq -r '.items[] | select(.status.conditions[]?.reason == "NetworkPluginNotReady" or
           (.status.containerStatuses[]?.state.waiting.reason == "ContainerCreating")) |
           "\(.metadata.namespace) \(.metadata.name)"' 2>/dev/null | \
while read ns pod; do
    echo "[Fix] 刪除卡住的 pod: $ns/$pod"
    kubectl delete pod -n $ns $pod --grace-period=0 --force 2>/dev/null || true
done

echo "[Fix] 完成！監控 app pod 狀態："
kubectl get pods -l io.kompose.service=app

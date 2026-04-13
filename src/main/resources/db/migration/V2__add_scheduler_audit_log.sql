-- V2__add_scheduler_audit_log.sql
-- 排程任務執行記錄表
-- 用於記錄每次排程任務的執行結果（CartCleanupScheduler、OrderAutoCloseScheduler 等）

CREATE TABLE IF NOT EXISTS scheduler_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL COMMENT '排程任務名稱',
    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '執行時間',
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS / FAILED',
    message TEXT COMMENT '執行結果訊息',
    affected_rows INT DEFAULT 0 COMMENT '影響筆數',
    duration_ms BIGINT DEFAULT 0 COMMENT '執行耗時（毫秒）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排程任務執行記錄';

CREATE INDEX idx_scheduler_audit_task ON scheduler_audit_log(task_name);
CREATE INDEX idx_scheduler_audit_executed_at ON scheduler_audit_log(executed_at);

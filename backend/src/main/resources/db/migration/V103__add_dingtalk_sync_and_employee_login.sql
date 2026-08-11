ALTER TABLE ding_department
    ADD COLUMN corp_id VARCHAR(100) DEFAULT NULL COMMENT '钉钉企业 corpId；用于区分员工目录所属企业' AFTER id,
    ADD COLUMN last_synced_at DATETIME(3) DEFAULT NULL COMMENT '最近一次从钉钉通讯录成功同步到本记录的时间' AFTER sort_no,
    ADD KEY idx_ding_department_corp_status (corp_id, status, id);

ALTER TABLE ding_employee
    ADD COLUMN corp_id VARCHAR(100) DEFAULT NULL COMMENT '钉钉企业 corpId；用于校验免登身份所属企业' AFTER id,
    ADD COLUMN union_id VARCHAR(100) DEFAULT NULL COMMENT '钉钉 unionId；同一用户跨应用的统一身份标识' AFTER ding_user_id,
    ADD COLUMN last_synced_at DATETIME(3) DEFAULT NULL COMMENT '最近一次从钉钉通讯录成功同步到本记录的时间' AFTER status,
    ADD KEY idx_ding_employee_union (union_id),
    ADD KEY idx_ding_employee_corp_status (corp_id, status, id);

CREATE TABLE ding_employee_department (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '员工与部门关系主键 ID',
    employee_id BIGINT UNSIGNED NOT NULL COMMENT '钉钉员工目录主键 ID',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '钉钉部门目录主键 ID',
    is_primary TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否为员工主部门：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '关系首次同步时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '关系最近同步时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ding_employee_department (employee_id, department_id),
    KEY idx_ding_employee_department_department (department_id, employee_id),
    CONSTRAINT fk_ding_employee_department_employee FOREIGN KEY (employee_id) REFERENCES ding_employee (id),
    CONSTRAINT fk_ding_employee_department_department FOREIGN KEY (department_id) REFERENCES ding_department (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钉钉员工多部门归属关系表；权限计算匹配员工任一有效部门';

CREATE TABLE ding_directory_sync_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '钉钉通讯录同步日志主键 ID',
    trigger_type VARCHAR(20) NOT NULL COMMENT '触发方式：SCHEDULED-每小时定时同步，MANUAL-接口手动触发',
    status VARCHAR(20) NOT NULL COMMENT '同步状态：RUNNING-进行中，SUCCESS-成功，FAILED-失败，SKIPPED-已有任务执行而跳过',
    department_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本次从钉钉获取并处理的部门数',
    employee_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本次从钉钉获取并处理的员工数',
    operator_name VARCHAR(100) NOT NULL COMMENT '触发人账号；定时任务固定为 system',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '失败原因；成功时为空且不得包含应用密钥等敏感信息',
    started_at DATETIME(3) NOT NULL COMMENT '同步开始时间',
    finished_at DATETIME(3) DEFAULT NULL COMMENT '同步结束时间；进行中时为空',
    PRIMARY KEY (id),
    KEY idx_ding_directory_sync_log_time (started_at, id),
    KEY idx_ding_directory_sync_log_status (status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钉钉通讯录同步执行日志表';

INSERT INTO ding_employee_department (employee_id, department_id, is_primary)
SELECT id, department_id, 1 FROM ding_employee;

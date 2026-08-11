-- 钉钉多企业目录：corp_code 是稳定的业务编码，不能使用可能变化的企业名称作为唯一键。
ALTER TABLE ding_department
    ADD COLUMN corp_code VARCHAR(50) NOT NULL DEFAULT 'default'
        COMMENT '企业业务编码：default-历史单企业，sebo-赛宝，walden-瓦尔登；与钉钉部门 ID 共同唯一' AFTER id,
    ADD COLUMN corp_name VARCHAR(200) NOT NULL DEFAULT '默认钉钉企业'
        COMMENT '钉钉企业名称，仅用于财务端展示，不参与身份匹配' AFTER corp_code,
    DROP INDEX uk_ding_department_id,
    ADD UNIQUE KEY uk_ding_department_corp_id (corp_code, ding_department_id),
    ADD KEY idx_ding_department_corp_name (corp_code, department_name, status, id);

ALTER TABLE ding_employee
    ADD COLUMN corp_code VARCHAR(50) NOT NULL DEFAULT 'default'
        COMMENT '企业业务编码：default-历史单企业，sebo-赛宝，walden-瓦尔登；与钉钉 userId 共同唯一' AFTER id,
    ADD COLUMN corp_name VARCHAR(200) NOT NULL DEFAULT '默认钉钉企业'
        COMMENT '钉钉企业名称，仅用于财务端展示和检索' AFTER corp_code,
    DROP INDEX uk_ding_employee_user,
    DROP INDEX uk_ding_employee_no,
    ADD UNIQUE KEY uk_ding_employee_corp_user (corp_code, ding_user_id),
    ADD UNIQUE KEY uk_ding_employee_corp_no (corp_code, employee_no),
    ADD KEY idx_ding_employee_corp_search (corp_code, employee_name, employee_no, department_name, mobile);

-- 授权对象也必须携带企业编码，避免两家企业相同 userId/departmentId 发生串权。
ALTER TABLE subject_permission
    ADD COLUMN target_corp_code VARCHAR(50) NOT NULL DEFAULT 'default'
        COMMENT '授权对象所属企业业务编码：default-历史单企业，sebo-赛宝，walden-瓦尔登' AFTER target_type,
    DROP INDEX uk_subject_permission_target_deleted,
    DROP INDEX idx_subject_permission_target,
    DROP INDEX idx_subject_permission_effect,
    ADD UNIQUE KEY uk_subject_permission_target_deleted
        (subject_id, target_type, target_corp_code, target_id, deleted),
    ADD KEY idx_subject_permission_target
        (target_type, target_corp_code, target_id, status),
    ADD KEY idx_subject_permission_effect
        (subject_id, target_type, target_corp_code, target_id, permission_effect, status);

-- 新二维码绑定本地员工主键；旧 ding_user_id 字段只用于迁移历史未过期令牌。
ALTER TABLE invoice_qr_token
    ADD COLUMN employee_id BIGINT UNSIGNED DEFAULT NULL
        COMMENT '生成二维码的本地钉钉员工目录主键；通过员工主键隔离企业身份' AFTER version_id,
    MODIFY COLUMN ding_user_id VARCHAR(100) DEFAULT NULL
        COMMENT '历史兼容字段：旧版生成令牌时记录的裸钉钉 userId，新版不再写入';

UPDATE invoice_qr_token token_record
INNER JOIN ding_employee employee
        ON employee.corp_code = 'default'
       AND employee.ding_user_id = token_record.ding_user_id
SET token_record.employee_id = employee.id
WHERE token_record.employee_id IS NULL;

ALTER TABLE invoice_qr_token
    ADD KEY idx_invoice_qr_token_employee (employee_id, expires_at),
    ADD CONSTRAINT fk_invoice_qr_token_employee
        FOREIGN KEY (employee_id) REFERENCES ding_employee (id);

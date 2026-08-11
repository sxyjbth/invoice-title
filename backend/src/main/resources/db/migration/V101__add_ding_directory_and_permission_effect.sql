CREATE TABLE ding_department (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '钉钉部门目录主键 ID',
    ding_department_id VARCHAR(100) NOT NULL COMMENT '钉钉部门 ID，企业内唯一',
    department_name VARCHAR(200) NOT NULL COMMENT '部门名称',
    parent_department_id VARCHAR(100) DEFAULT NULL COMMENT '上级钉钉部门 ID；根部门为空',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '部门状态：ENABLED-有效，DISABLED-已停用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '同级部门排序号，数值越小越靠前',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '目录记录创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最近一次钉钉同步时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ding_department_id (ding_department_id),
    KEY idx_ding_department_name_status (department_name, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钉钉通讯录部门目录表';

CREATE TABLE ding_employee (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '钉钉员工目录主键 ID',
    ding_user_id VARCHAR(100) NOT NULL COMMENT '钉钉 userId，企业内唯一',
    employee_no VARCHAR(50) NOT NULL COMMENT '员工工号',
    employee_name VARCHAR(100) NOT NULL COMMENT '员工姓名',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '员工当前所属部门目录主键 ID',
    department_name VARCHAR(200) NOT NULL COMMENT '员工当前所属部门名称冗余值，便于检索与展示',
    mobile VARCHAR(30) DEFAULT NULL COMMENT '员工手机号',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '员工状态：ACTIVE-在职，INACTIVE-离职或停用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '目录记录创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最近一次钉钉同步时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ding_employee_user (ding_user_id),
    UNIQUE KEY uk_ding_employee_no (employee_no),
    KEY idx_ding_employee_search (employee_name, employee_no, department_name, mobile),
    KEY idx_ding_employee_department_status (department_id, status, id),
    CONSTRAINT fk_ding_employee_department FOREIGN KEY (department_id) REFERENCES ding_department (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钉钉通讯录员工目录表';

ALTER TABLE subject_permission
    ADD COLUMN permission_effect VARCHAR(20) NOT NULL DEFAULT 'ALLOW'
        COMMENT '权限效果：ALLOW-允许查看，DENY-禁止查看；员工级规则优先于部门级规则'
        AFTER target_name,
    ADD KEY idx_subject_permission_effect (subject_id, target_type, target_id, permission_effect, status);

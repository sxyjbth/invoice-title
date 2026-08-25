-- 部门授权边例外：只屏蔽员工从某一已选部门继承的授权，不恢复全局 USER/DENY 优先级。
CREATE TABLE subject_department_employee_exclusion (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '部门员工授权例外主键 ID',
    subject_id BIGINT UNSIGNED NOT NULL COMMENT '展示主体 ID',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '被屏蔽授权边对应的钉钉部门目录主键 ID',
    employee_id BIGINT UNSIGNED NOT NULL COMMENT '被屏蔽授权边对应的钉钉员工目录主键 ID',
    created_by VARCHAR(100) NOT NULL COMMENT '创建人账号',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL COMMENT '最后更新人账号',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_subject_department_employee_exclusion (subject_id, department_id, employee_id),
    KEY idx_subject_department_employee_exclusion_lookup (subject_id, employee_id, department_id),
    CONSTRAINT fk_subject_department_employee_exclusion_subject
        FOREIGN KEY (subject_id) REFERENCES invoice_subject (id),
    CONSTRAINT fk_subject_department_employee_exclusion_department
        FOREIGN KEY (department_id) REFERENCES ding_department (id),
    CONSTRAINT fk_subject_department_employee_exclusion_employee
        FOREIGN KEY (employee_id) REFERENCES ding_employee (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='主体部门授权的员工级边例外；仅取消指定部门继承路径，不作为全局员工拒绝规则';

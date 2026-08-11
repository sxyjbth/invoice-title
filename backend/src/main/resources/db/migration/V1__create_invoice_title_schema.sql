CREATE TABLE finance_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '网页财务端账号主键 ID',
    username VARCHAR(50) NOT NULL COMMENT '财务端登录账号，忽略大小写且系统内唯一',
    display_name VARCHAR(100) NOT NULL COMMENT '账号使用人的显示姓名',
    password_hash VARCHAR(100) NOT NULL COMMENT '使用 BCrypt 算法生成的密码摘要，禁止保存明文密码',
    role_type VARCHAR(20) NOT NULL COMMENT '账号角色：SUPER_ADMIN-唯一超级管理员，FINANCE-普通财务人员',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '账号状态：ENABLED-允许登录，DISABLED-已停用',
    password_changed_at DATETIME(3) DEFAULT NULL COMMENT '密码最后修改或被管理员重置的时间',
    last_login_at DATETIME(3) DEFAULT NULL COMMENT '最近一次账号密码验证成功的时间',
    created_by BIGINT UNSIGNED NOT NULL COMMENT '创建该账号的超级管理员账号 ID；0 表示系统初始化',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '账号创建时间',
    updated_by BIGINT UNSIGNED NOT NULL COMMENT '最后更新该账号的账号 ID；0 表示系统初始化',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '账号最后更新时间',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_finance_user_username_deleted (username, deleted),
    KEY idx_finance_user_role_status (role_type, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='独立网页财务端账号与角色表';

CREATE TABLE invoice_subject (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主体主键 ID',
    subject_code VARCHAR(50) NOT NULL COMMENT '主体编码，业务内唯一，例如 HZ',
    subject_name VARCHAR(100) NOT NULL COMMENT '主体名称，例如杭州主体',
    ding_corp_id VARCHAR(100) DEFAULT NULL COMMENT '所属钉钉企业 CorpId；为空表示当前默认企业',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '主体状态：ENABLED-启用，DISABLED-停用',
    all_employee_visible TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '全员可见开关：0-关闭，仅按部门或员工授权；1-开启，全部在职员工可见',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '展示顺序，数值越小越靠前',
    created_by VARCHAR(100) NOT NULL COMMENT '创建人的钉钉用户 ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL COMMENT '最后更新人的钉钉用户 ID',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_subject_code_deleted (subject_code, deleted),
    KEY idx_invoice_subject_status_sort (status, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发票抬头展示主体表';

CREATE TABLE invoice_title (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '发票抬头主键 ID',
    company_name VARCHAR(200) NOT NULL COMMENT '公司名称',
    taxpayer_id VARCHAR(32) NOT NULL COMMENT '纳税人识别号',
    registered_address VARCHAR(500) DEFAULT NULL COMMENT '公司注册地址',
    phone VARCHAR(50) DEFAULT NULL COMMENT '开票联系电话',
    bank_name VARCHAR(200) DEFAULT NULL COMMENT '开户银行全称',
    bank_account VARCHAR(64) DEFAULT NULL COMMENT '银行账号，按字符串保存以保留前导零',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '抬头状态：DRAFT-草稿，PUBLISHED-已发布，DISABLED-已停用',
    current_published_version_id BIGINT UNSIGNED DEFAULT NULL COMMENT '当前已发布版本 ID；草稿或从未发布时可为空',
    subject_names VARCHAR(500) DEFAULT NULL COMMENT '展示主体名称冗余快照，仅用于列表快速展示，权限以关联表为准',
    created_by VARCHAR(100) NOT NULL COMMENT '创建人的钉钉用户 ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL COMMENT '最后更新人的钉钉用户 ID 或显示名',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_title_taxpayer_deleted (taxpayer_id, deleted),
    KEY idx_invoice_title_status_updated (status, updated_at DESC, id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发票抬头当前态表';

CREATE TABLE invoice_title_subject (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '抬头与主体关联主键 ID',
    title_id BIGINT UNSIGNED NOT NULL COMMENT '发票抬头 ID',
    subject_id BIGINT UNSIGNED NOT NULL COMMENT '展示主体 ID',
    created_by VARCHAR(100) NOT NULL COMMENT '关联创建人的钉钉用户 ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '关联创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_title_subject (title_id, subject_id),
    KEY idx_title_subject_subject (subject_id, title_id),
    CONSTRAINT fk_title_subject_title FOREIGN KEY (title_id) REFERENCES invoice_title (id),
    CONSTRAINT fk_title_subject_subject FOREIGN KEY (subject_id) REFERENCES invoice_subject (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发票抬头与展示主体多对多关联表';

CREATE TABLE subject_permission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主体查看权限主键 ID',
    subject_id BIGINT UNSIGNED NOT NULL COMMENT '被授权的主体 ID',
    target_type VARCHAR(20) NOT NULL COMMENT '授权对象类型：USER-钉钉员工，DEPARTMENT-钉钉部门',
    target_id VARCHAR(100) NOT NULL COMMENT '授权对象 ID：钉钉 userId 或 departmentId',
    target_name VARCHAR(200) NOT NULL COMMENT '授权对象显示名称，便于财务识别',
    include_child_departments TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否包含子部门：0-不包含，1-包含；仅部门授权时有效',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '权限状态：ENABLED-有效，DISABLED-停用',
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '权限来源：MANUAL-财务手动维护，DING_SYNC-钉钉同步',
    created_by VARCHAR(100) NOT NULL COMMENT '创建人的钉钉用户 ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL COMMENT '最后更新人的钉钉用户 ID',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_subject_permission_target_deleted (subject_id, target_type, target_id, deleted),
    KEY idx_subject_permission_target (target_type, target_id, status),
    CONSTRAINT fk_subject_permission_subject FOREIGN KEY (subject_id) REFERENCES invoice_subject (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工或部门的主体查看权限表';

CREATE TABLE invoice_title_version (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '抬头版本主键 ID',
    title_id BIGINT UNSIGNED NOT NULL COMMENT '所属发票抬头 ID',
    version_no INT UNSIGNED NOT NULL COMMENT '版本号，单个抬头内从 1 递增',
    status VARCHAR(20) NOT NULL COMMENT '版本状态：DRAFT-草稿快照，PUBLISHED-发布快照，DISABLED-停用快照',
    change_type VARCHAR(30) NOT NULL DEFAULT 'EDIT' COMMENT '版本产生方式：CREATE-新建，EDIT-编辑，IMPORT-导入，RESTORE-历史恢复，PUBLISH-发布',
    change_summary VARCHAR(500) DEFAULT NULL COMMENT '本次版本变更摘要',
    company_name VARCHAR(200) NOT NULL COMMENT '公司名称快照',
    taxpayer_id VARCHAR(32) NOT NULL COMMENT '纳税人识别号快照',
    registered_address VARCHAR(500) DEFAULT NULL COMMENT '公司注册地址快照',
    phone VARCHAR(50) DEFAULT NULL COMMENT '联系电话快照',
    bank_name VARCHAR(200) DEFAULT NULL COMMENT '开户银行快照',
    bank_account VARCHAR(64) DEFAULT NULL COMMENT '银行账号快照',
    subject_ids_json JSON DEFAULT NULL COMMENT '展示主体 ID 数组快照，例如[1,2]',
    created_by VARCHAR(100) NOT NULL COMMENT '版本创建人的钉钉用户 ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '版本创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_title_version (title_id, version_no),
    KEY idx_invoice_title_version_status (title_id, status, version_no DESC),
    CONSTRAINT fk_invoice_title_version_title FOREIGN KEY (title_id) REFERENCES invoice_title (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发票抬头不可变历史版本快照表';

CREATE TABLE invoice_import_task (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '批量导入任务主键 ID',
    task_no VARCHAR(40) NOT NULL COMMENT '导入任务编号，业务内唯一',
    original_file_name VARCHAR(255) NOT NULL COMMENT '用户上传的原始 Excel 文件名',
    storage_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL' COMMENT '文件存储类型：LOCAL-项目本地，OSS-阿里云 OSS，MINIO-MinIO',
    storage_key VARCHAR(500) NOT NULL COMMENT '导入源文件的存储对象 key 或项目内相对路径',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING-待处理，VALIDATING-校验中，COMPLETED-已完成，PARTIAL_FAILED-部分失败，FAILED-全部失败',
    total_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Excel 数据总条数',
    success_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '成功生成草稿的条数',
    failure_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '校验或落库失败的条数',
    error_file_key VARCHAR(500) DEFAULT NULL COMMENT '错误结果文件的存储对象 key 或项目内相对路径',
    started_at DATETIME(3) DEFAULT NULL COMMENT '任务开始处理时间',
    finished_at DATETIME(3) DEFAULT NULL COMMENT '任务处理完成时间',
    created_by VARCHAR(100) NOT NULL COMMENT '发起导入人的钉钉用户 ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '任务创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_import_task_no (task_no),
    KEY idx_invoice_import_task_created (created_at DESC, id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发票抬头批量导入任务表';

CREATE TABLE invoice_import_row_error (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '导入行错误主键 ID',
    task_id BIGINT UNSIGNED NOT NULL COMMENT '所属批量导入任务 ID',
    row_no INT UNSIGNED NOT NULL COMMENT 'Excel 行号，从 2 开始（第 1 行为表头）',
    taxpayer_id VARCHAR(32) DEFAULT NULL COMMENT '该行纳税人识别号，无法解析时可为空',
    error_code VARCHAR(50) NOT NULL COMMENT '错误码，例如 REQUIRED_MISSING、DUPLICATE_TAXPAYER_ID、SUBJECT_NOT_FOUND',
    error_message VARCHAR(500) NOT NULL COMMENT '可供财务修正的中文错误说明',
    raw_data_json JSON DEFAULT NULL COMMENT '该行原始数据 JSON，用于排查问题',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '错误记录创建时间',
    PRIMARY KEY (id),
    KEY idx_invoice_import_row_error_task (task_id, row_no),
    CONSTRAINT fk_import_row_error_task FOREIGN KEY (task_id) REFERENCES invoice_import_task (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发票抬头导入失败明细表';

CREATE TABLE invoice_qr_token (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '临时二维码令牌主键 ID',
    token VARCHAR(64) NOT NULL COMMENT '随机不可枚举令牌，二维码中仅暴露该值',
    title_id BIGINT UNSIGNED NOT NULL COMMENT '对应发票抬头 ID',
    version_id BIGINT UNSIGNED NOT NULL COMMENT '令牌创建时固定的已发布版本 ID',
    ding_user_id VARCHAR(100) NOT NULL COMMENT '令牌签发给的钉钉用户 ID',
    expires_at DATETIME(3) NOT NULL COMMENT '令牌绝对过期时间，创建后 10 分钟',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '令牌创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_qr_token (token),
    KEY idx_invoice_qr_token_expire (expires_at),
    CONSTRAINT fk_invoice_qr_token_title FOREIGN KEY (title_id) REFERENCES invoice_title (id),
    CONSTRAINT fk_invoice_qr_token_version FOREIGN KEY (version_id) REFERENCES invoice_title_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='十分钟临时发票抬头二维码令牌表';

CREATE TABLE invoice_operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '操作日志主键 ID',
    module_type VARCHAR(30) NOT NULL COMMENT '业务模块：TITLE-抬头，SUBJECT-主体，PERMISSION-主体权限，IMPORT-批量导入，QR-二维码，ACCOUNT-财务账号',
    operation_type VARCHAR(30) NOT NULL COMMENT '操作类型：CREATE/UPDATE/PUBLISH/DISABLE/RESTORE/IMPORT/AUTHORIZE/REVOKE，以及 CREATE_ACCOUNT/CHANGE_PASSWORD/RESET_PASSWORD/ENABLE_ACCOUNT/DISABLE_ACCOUNT',
    business_id VARCHAR(64) NOT NULL COMMENT '业务对象 ID；批量导入时可为任务编号',
    business_name VARCHAR(200) DEFAULT NULL COMMENT '业务对象名称，便于直接检索',
    detail_json JSON DEFAULT NULL COMMENT '变更前后或附加信息 JSON；不得写入密码等敏感信息',
    result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '操作结果：SUCCESS-成功，FAILED-失败',
    operator_user_id VARCHAR(100) NOT NULL COMMENT '操作人 ID：财务网页端为账号 ID，员工端为钉钉用户 ID',
    operator_name VARCHAR(100) NOT NULL COMMENT '操作人姓名',
    client_ip VARCHAR(64) DEFAULT NULL COMMENT '客户端 IP 地址，兼容 IPv6',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作发生时间',
    PRIMARY KEY (id),
    KEY idx_invoice_operation_log_created (created_at DESC, id DESC),
    KEY idx_invoice_operation_log_business (module_type, business_id),
    KEY idx_invoice_operation_log_operator (operator_user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发票抬头系统操作审计日志表';

-- 将历史多对多关系收口为零或一对零或一。被淘汰的关系先归档，便于上线后审计与人工核对。
CREATE TABLE IF NOT EXISTS invoice_title_subject_binding_archive (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '归档主键 ID',
    source_relation_id BIGINT UNSIGNED NOT NULL COMMENT '原 invoice_title_subject 关系 ID',
    title_id BIGINT UNSIGNED NOT NULL COMMENT '原发票抬头 ID',
    subject_id BIGINT UNSIGNED NOT NULL COMMENT '原展示主体 ID',
    created_by VARCHAR(100) NOT NULL COMMENT '原关系创建人',
    created_at DATETIME(3) NOT NULL COMMENT '原关系创建时间',
    archive_reason VARCHAR(50) NOT NULL COMMENT '归档原因：ONE_TO_ONE_MIGRATION-一对一迁移清理',
    archived_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '归档时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_title_subject_archive_source (source_relation_id),
    KEY idx_invoice_title_subject_archive_title (title_id),
    KEY idx_invoice_title_subject_archive_subject (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='抬头主体历史关系归档表';

-- 利用临时表两侧唯一键进行确定性保留：已发布、启用主体和最近更新的关系优先。
CREATE TEMPORARY TABLE tmp_invoice_title_subject_keep (
    relation_id BIGINT UNSIGNED NOT NULL,
    title_id BIGINT UNSIGNED NOT NULL,
    subject_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (relation_id),
    UNIQUE KEY uk_tmp_invoice_title_subject_title (title_id),
    UNIQUE KEY uk_tmp_invoice_title_subject_subject (subject_id)
) ENGINE=InnoDB;

INSERT IGNORE INTO tmp_invoice_title_subject_keep (relation_id, title_id, subject_id)
SELECT relation_record.id, relation_record.title_id, relation_record.subject_id
FROM invoice_title_subject relation_record
INNER JOIN invoice_title title_record
        ON title_record.id = relation_record.title_id
       AND title_record.deleted = 0
INNER JOIN invoice_subject subject_record
        ON subject_record.id = relation_record.subject_id
       AND subject_record.deleted = 0
ORDER BY
    CASE title_record.status
        WHEN 'PUBLISHED' THEN 0
        WHEN 'DRAFT' THEN 1
        ELSE 2
    END,
    CASE subject_record.status WHEN 'ENABLED' THEN 0 ELSE 1 END,
    title_record.updated_at DESC,
    relation_record.created_at DESC,
    relation_record.id DESC;

INSERT INTO invoice_title_subject_binding_archive
    (source_relation_id, title_id, subject_id, created_by, created_at, archive_reason)
SELECT duplicate_relation.id,
       duplicate_relation.title_id,
       duplicate_relation.subject_id,
       duplicate_relation.created_by,
       duplicate_relation.created_at,
       'ONE_TO_ONE_MIGRATION'
FROM invoice_title_subject duplicate_relation
LEFT JOIN tmp_invoice_title_subject_keep kept_relation
       ON kept_relation.relation_id = duplicate_relation.id
WHERE kept_relation.relation_id IS NULL
ON DUPLICATE KEY UPDATE source_relation_id = VALUES(source_relation_id);

DELETE duplicate_relation
FROM invoice_title_subject duplicate_relation
LEFT JOIN tmp_invoice_title_subject_keep kept_relation
       ON kept_relation.relation_id = duplicate_relation.id
INNER JOIN invoice_title_subject_binding_archive archived_relation
        ON archived_relation.source_relation_id = duplicate_relation.id
       AND archived_relation.title_id = duplicate_relation.title_id
       AND archived_relation.subject_id = duplicate_relation.subject_id
       AND archived_relation.archive_reason = 'ONE_TO_ONE_MIGRATION'
WHERE kept_relation.relation_id IS NULL;

-- 同步修正列表冗余字段，并显式保留业务更新时间，避免迁移被误认为财务修改。
UPDATE invoice_title title_record
LEFT JOIN invoice_title_subject relation_record
       ON relation_record.title_id = title_record.id
LEFT JOIN invoice_subject subject_record
       ON subject_record.id = relation_record.subject_id
      AND subject_record.deleted = 0
SET title_record.subject_names = COALESCE(subject_record.subject_name, ''),
    title_record.updated_at = title_record.updated_at
WHERE title_record.deleted = 0;

-- 临时表在加唯一索引前已完成使命，让原子 ALTER 成为迁移最后一步。
DROP TEMPORARY TABLE tmp_invoice_title_subject_keep;

ALTER TABLE invoice_title_subject
    ADD UNIQUE KEY uk_invoice_title_subject_title (title_id),
    ADD UNIQUE KEY uk_invoice_title_subject_subject (subject_id),
    COMMENT = '一个抬头最多绑定一个主体，一个主体最多绑定一个抬头';

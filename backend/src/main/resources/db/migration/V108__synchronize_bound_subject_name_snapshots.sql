-- 修复主体改名后历史抬头仍保留旧展示名称的问题。
-- 仅校正展示快照，不改变抬头更新时间、操作人或发布版本。
UPDATE invoice_title title_record
INNER JOIN invoice_title_subject relation
        ON relation.title_id = title_record.id
INNER JOIN invoice_subject subject_record
        ON subject_record.id = relation.subject_id
       AND subject_record.deleted = 0
SET title_record.subject_names = subject_record.subject_name
WHERE title_record.deleted = 0;

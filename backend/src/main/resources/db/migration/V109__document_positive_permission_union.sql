-- 当前权限只采用正向并集：全员可见、部门允许、员工允许任一命中即可查看。
-- 迁移不主动改写历史 DENY 数据；应用查询不再使用，主体权限下次整体保存时会随旧快照一并清理。
ALTER TABLE subject_permission
    MODIFY COLUMN permission_effect VARCHAR(20) NOT NULL DEFAULT 'ALLOW'
    COMMENT '权限效果：ALLOW-正向允许查看；历史DENY保留兼容但不再参与权限判定';

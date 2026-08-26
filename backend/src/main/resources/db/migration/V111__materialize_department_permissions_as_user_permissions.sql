-- 将旧版“部门允许 + 员工边例外”权限物化为最终员工允许权限。
-- 迁移后的有效授权对象统一为 USER；员工身份以 corp_code + ding_user_id 唯一确定，
-- 同一员工同时属于多个已授权部门时只生成一条主体员工权限。
-- permission_effect 类型含义：ALLOW-允许查看，DENY-禁止查看；本迁移只生成 ALLOW。
-- status 类型含义：ENABLED-有效，DISABLED-停用；仅迁移当前有效的部门允许规则和在职员工。
-- subject_department_employee_exclusion 表继续保留以兼容旧版本测试和回滚排查，
-- 但已迁移主体的旧例外记录会在部门规则删除前清理。

CREATE TEMPORARY TABLE tmp_v111_migrated_subject (
    subject_id BIGINT UNSIGNED NOT NULL COMMENT '待迁移的展示主体主键 ID；来源为有效的 DEPARTMENT/ALLOW 规则',
    PRIMARY KEY (subject_id)
) ENGINE=InnoDB COMMENT='V111 权限物化迁移涉及的展示主体临时集合';

CREATE TEMPORARY TABLE tmp_v111_department_scope (
    subject_id BIGINT UNSIGNED NOT NULL COMMENT '展示主体主键 ID',
    target_corp_code VARCHAR(50) NOT NULL COMMENT '钉钉企业业务编码；与 ding_user_id 共同确定员工身份',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '本地钉钉部门目录主键 ID；用于匹配员工部门关系及旧例外记录',
    PRIMARY KEY (subject_id, department_id),
    KEY idx_tmp_v111_department_scope_corp (target_corp_code, department_id)
) ENGINE=InnoDB COMMENT='V111 有效部门允许规则解析后的部门范围临时表';

CREATE TEMPORARY TABLE tmp_v111_unresolved_department_rule (
    permission_id BIGINT UNSIGNED NOT NULL COMMENT '无法解析为当前有效目录部门的旧部门权限主键 ID',
    PRIMARY KEY (permission_id)
) ENGINE=InnoDB COMMENT='V111 迁移前无法安全物化的旧部门权限集合';

CREATE TEMPORARY TABLE tmp_v111_resolution_guard (
    guard_key TINYINT UNSIGNED NOT NULL COMMENT '迁移保护键；重复写入表示存在无法解析的旧权限并主动终止迁移',
    PRIMARY KEY (guard_key)
) ENGINE=InnoDB COMMENT='V111 防止旧部门权限静默丢失的迁移保护表';

-- 先固定所有存在有效部门允许规则的主体。即使目录中的部门已失效或不存在，
-- 该主体的旧部门规则也会被收口清理，避免迁移后继续保留非规范权限数据。
INSERT INTO tmp_v111_migrated_subject (subject_id)
SELECT DISTINCT permission_record.subject_id
FROM subject_permission permission_record
WHERE permission_record.target_type = 'DEPARTMENT'
  AND permission_record.permission_effect = 'ALLOW'
  AND permission_record.status = 'ENABLED'
  AND permission_record.deleted = 0;

-- 只有仍处于 ENABLED 状态的目录部门能够产生最终员工权限。
INSERT INTO tmp_v111_department_scope (subject_id, target_corp_code, department_id)
SELECT DISTINCT
       permission_record.subject_id,
       permission_record.target_corp_code,
       department.id
FROM subject_permission permission_record
INNER JOIN ding_department department
        ON department.corp_code = permission_record.target_corp_code
       AND department.ding_department_id = permission_record.target_id
       AND department.status = 'ENABLED'
WHERE permission_record.target_type = 'DEPARTMENT'
  AND permission_record.permission_effect = 'ALLOW'
  AND permission_record.status = 'ENABLED'
  AND permission_record.deleted = 0;

-- 任何有效旧部门权限如果不能精确匹配“同企业 + 同钉钉部门 ID + ENABLED 部门”，
-- 都不能继续删除旧规则。先记录异常，再通过临时表唯一键冲突让 Flyway 在写业务表前失败，
-- 待通讯录同步或历史 corp_code 修正后重新执行，避免上线迁移静默丢权。
INSERT INTO tmp_v111_unresolved_department_rule (permission_id)
SELECT permission_record.id
FROM subject_permission permission_record
WHERE permission_record.target_type = 'DEPARTMENT'
  AND permission_record.permission_effect = 'ALLOW'
  AND permission_record.status = 'ENABLED'
  AND permission_record.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM ding_department department
      WHERE department.corp_code = permission_record.target_corp_code
        AND department.ding_department_id = permission_record.target_id
        AND department.status = 'ENABLED'
  );

INSERT INTO tmp_v111_resolution_guard (guard_key) VALUES (1);
INSERT INTO tmp_v111_resolution_guard (guard_key)
SELECT 1
FROM tmp_v111_unresolved_department_rule
LIMIT 1;

-- 物化当前最终可见员工：
-- 1. 只包含 ACTIVE 在职员工；
-- 2. 排除员工在指定部门继承边上的例外；
-- 3. SELECT DISTINCT 按主体、企业编码和钉钉 userId 消除多部门重复授权；
-- 4. 若已经存在同一 USER 权限，则统一更新为有效 ALLOW，而不是新增重复记录。
INSERT INTO subject_permission (
    subject_id,
    target_type,
    target_corp_code,
    target_id,
    target_name,
    permission_effect,
    include_child_departments,
    status,
    source,
    created_by,
    created_at,
    updated_by,
    updated_at,
    deleted
)
SELECT DISTINCT
       department_scope.subject_id,
       'USER', employee.corp_code, employee.ding_user_id, employee.employee_name,
       'ALLOW',
       0,
       'ENABLED',
       'MANUAL',
       'flyway-v111',
       CURRENT_TIMESTAMP(3),
       'flyway-v111',
       CURRENT_TIMESTAMP(3),
       0
FROM tmp_v111_department_scope department_scope
INNER JOIN ding_employee_department membership
        ON membership.department_id = department_scope.department_id
INNER JOIN ding_employee employee
        ON employee.id = membership.employee_id
       AND employee.corp_code = department_scope.target_corp_code
WHERE employee.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM subject_department_employee_exclusion exclusion_record
      WHERE exclusion_record.subject_id = department_scope.subject_id
        AND exclusion_record.department_id = department_scope.department_id
        AND exclusion_record.employee_id = employee.id
  )
ON DUPLICATE KEY UPDATE
    target_name = VALUES(target_name),
    permission_effect = VALUES(permission_effect),
    include_child_departments = VALUES(include_child_departments),
    status = VALUES(status),
    source = VALUES(source),
    updated_by = VALUES(updated_by),
    updated_at = VALUES(updated_at);

-- 先清理已物化主体的旧部门员工例外，再删除这些主体的全部旧部门规则。
-- 例外表结构本身必须保留，供旧版本契约测试及历史兼容使用。
DELETE exclusion_record
FROM subject_department_employee_exclusion exclusion_record
INNER JOIN tmp_v111_migrated_subject migrated_subject
        ON migrated_subject.subject_id = exclusion_record.subject_id;

DELETE permission_record
FROM subject_permission permission_record
INNER JOIN tmp_v111_migrated_subject migrated_subject
        ON migrated_subject.subject_id = permission_record.subject_id
WHERE permission_record.target_type = 'DEPARTMENT';

DROP TEMPORARY TABLE IF EXISTS tmp_v111_department_scope;
DROP TEMPORARY TABLE IF EXISTS tmp_v111_unresolved_department_rule;
DROP TEMPORARY TABLE IF EXISTS tmp_v111_resolution_guard;
DROP TEMPORARY TABLE IF EXISTS tmp_v111_migrated_subject;

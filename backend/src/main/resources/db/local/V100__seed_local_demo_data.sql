INSERT INTO finance_user
(id, username, display_name, password_hash, role_type, status, password_changed_at, created_by, updated_by)
VALUES
(1, 'admin', '超级管理员', '$2a$10$exra6j.LrYS.2Is316TB5Oxuk2rFrrhcNeB4TUsMDpeu0wVJGqZlK', 'SUPER_ADMIN', 'ENABLED', CURRENT_TIMESTAMP(3), 0, 0)
ON DUPLICATE KEY UPDATE
username = 'admin',
display_name = '超级管理员',
password_hash = '$2a$10$exra6j.LrYS.2Is316TB5Oxuk2rFrrhcNeB4TUsMDpeu0wVJGqZlK',
role_type = 'SUPER_ADMIN',
status = 'ENABLED',
password_changed_at = CURRENT_TIMESTAMP(3),
updated_by = 0,
deleted = 0;

INSERT INTO invoice_subject
(id, subject_code, subject_name, status, all_employee_visible, sort_no, created_by, updated_by)
VALUES
(1, 'HZ', '杭州主体', 'ENABLED', 0, 10, 'ding-user-finance', 'ding-user-finance'),
(2, 'BJ', '北京主体', 'ENABLED', 0, 20, 'ding-user-finance', 'ding-user-finance'),
(3, 'SH', '上海主体', 'ENABLED', 0, 30, 'ding-user-finance', 'ding-user-finance'),
(4, 'EAST', '华东主体', 'ENABLED', 1, 40, 'ding-user-finance', 'ding-user-finance');

INSERT INTO invoice_title
(id, company_name, taxpayer_id, registered_address, phone, bank_name, bank_account, status, current_published_version_id, subject_names, created_by, updated_by)
VALUES
(1, '杭州赛宝卓越技术有限公司', '91110400MADFF1HE1T', '浙江省杭州市钱塘区临江街道纬五路3688号临江科创园6号楼12楼', '4008696096', '宁波银行股份有限公司北京丰台支行', '86041110000957180', 'PUBLISHED', NULL, '杭州主体,华东主体', 'ding-user-finance', '王财务'),
(2, '北京示例技术服务有限公司', '91110108MA01EXAMPLE', '北京市海淀区示例路1号', '010-88888888', '招商银行北京分行', '6225888800003028', 'DRAFT', NULL, '北京主体', 'ding-user-finance', '李会计'),
(3, '上海赛宝技术服务有限公司', '91310115MA1KEXAMPLE', '上海市浦东新区示例大道88号', '021-66668888', '浦发银行上海分行', '6225888800006631', 'DISABLED', NULL, '上海主体', 'ding-user-finance', '王财务');

INSERT INTO invoice_title_subject (id, title_id, subject_id, created_by)
VALUES
(1, 1, 1, 'ding-user-finance'),
(2, 1, 4, 'ding-user-finance'),
(3, 2, 2, 'ding-user-finance'),
(4, 3, 3, 'ding-user-finance');

INSERT INTO subject_permission
(id, subject_id, target_type, target_id, target_name, status, source, created_by, updated_by)
VALUES
(1, 1, 'USER', 'ding-employee-001', '示例员工', 'ENABLED', 'MANUAL', 'ding-user-finance', 'ding-user-finance'),
(2, 4, 'DEPARTMENT', 'ding-dept-east', '华东交付中心', 'ENABLED', 'MANUAL', 'ding-user-finance', 'ding-user-finance'),
(3, 1, 'DEPARTMENT', 'ding-dept-tech', '技术中心', 'ENABLED', 'DING_SYNC', 'ding-user-finance', 'ding-user-finance'),
(4, 1, 'DEPARTMENT', 'ding-dept-finance', '财务部', 'ENABLED', 'DING_SYNC', 'ding-user-finance', 'ding-user-finance'),
(5, 1, 'DEPARTMENT', 'ding-dept-purchase', '采购部', 'ENABLED', 'DING_SYNC', 'ding-user-finance', 'ding-user-finance'),
(6, 1, 'USER', 'ding-employee-002', '陈员工', 'ENABLED', 'MANUAL', 'ding-user-finance', 'ding-user-finance'),
(7, 1, 'USER', 'ding-employee-003', '李员工', 'ENABLED', 'MANUAL', 'ding-user-finance', 'ding-user-finance');

INSERT INTO invoice_title_version
(id, title_id, version_no, status, change_type, change_summary, company_name, taxpayer_id, registered_address, phone, bank_name, bank_account, subject_ids_json, created_by)
VALUES
(1, 1, 1, 'PUBLISHED', 'CREATE', '初始版本', '杭州赛宝卓越技术有限公司', '91110400MADFF1HE1T', '浙江省杭州市钱塘区临江街道纬五路3688号临江科创园6号楼12楼', '4008696096', '宁波银行股份有限公司北京丰台支行', '86041110000957180', JSON_ARRAY(1,4), 'ding-user-finance'),
(2, 1, 2, 'PUBLISHED', 'EDIT', '银行信息调整', '杭州赛宝卓越技术有限公司', '91110400MADFF1HE1T', '浙江省杭州市钱塘区临江街道纬五路3688号临江科创园6号楼12楼', '4008696096', '宁波银行股份有限公司北京丰台支行', '86041110000957180', JSON_ARRAY(1,4), 'ding-user-finance'),
(3, 1, 3, 'PUBLISHED', 'PUBLISH', '财务复核并发布', '杭州赛宝卓越技术有限公司', '91110400MADFF1HE1T', '浙江省杭州市钱塘区临江街道纬五路3688号临江科创园6号楼12楼', '4008696096', '宁波银行股份有限公司北京丰台支行', '86041110000957180', JSON_ARRAY(1,4), 'ding-user-finance');

UPDATE invoice_title SET current_published_version_id = 3 WHERE id = 1;

INSERT INTO invoice_operation_log
(module_type, operation_type, business_id, business_name, detail_json, result, operator_user_id, operator_name, client_ip)
VALUES
('TITLE', 'PUBLISH', '1', '杭州赛宝卓越技术有限公司', JSON_OBJECT('versionNo', 3), 'SUCCESS', 'ding-user-finance', '王财务', '127.0.0.1'),
('ACCOUNT', 'CREATE_ACCOUNT', '1', 'admin', JSON_OBJECT('roleType', 'SUPER_ADMIN'), 'SUCCESS', '0', '系统初始化', '127.0.0.1');

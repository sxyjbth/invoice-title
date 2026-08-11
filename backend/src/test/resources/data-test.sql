INSERT INTO invoice_title
(id, company_name, taxpayer_id, registered_address, phone, bank_name, bank_account, status, subject_names, created_by, created_at, updated_at, updated_by, deleted)
VALUES
(1, '杭州赛宝卓越技术有限公司', '91110400MADFF1HE1T', '浙江省杭州市钱塘区临江街道纬五路3688号临江科创园6号楼12楼', '4008696096', '宁波银行股份有限公司北京丰台支行', '86041110000957180', 'PUBLISHED', '杭州主体,华东主体', 'ding-user-finance', '2026-08-07 15:46:00', '2026-08-07 15:46:00', '王财务', 0),
(2, '北京示例技术服务有限公司', '91110108MA01EXAMPLE', '北京市海淀区示例路1号', '010-88888888', '招商银行北京分行', '6225888800003028', 'DRAFT', '北京主体', 'ding-user-finance', '2026-08-06 10:22:00', '2026-08-06 10:22:00', '李会计', 0);

INSERT INTO invoice_title_version
(id, title_id, version_no, status, company_name, taxpayer_id, registered_address, phone, bank_name, bank_account, created_by, created_at)
VALUES
(1, 1, 1, 'PUBLISHED', '杭州赛宝卓越技术有限公司', '91110400MADFF1HE1T', '浙江省杭州市钱塘区临江街道纬五路3688号临江科创园6号楼12楼', '4008696096', '宁波银行股份有限公司北京丰台支行', '86041110000957180', 'ding-user-finance', '2026-06-18 09:12:00'),
(2, 1, 2, 'PUBLISHED', '杭州赛宝卓越技术有限公司', '91110400MADFF1HE1T', '浙江省杭州市钱塘区临江街道纬五路3688号临江科创园6号楼12楼', '4008696096', '宁波银行股份有限公司北京丰台支行', '86041110000957180', 'ding-user-finance', '2026-07-22 11:30:00'),
(3, 1, 3, 'PUBLISHED', '杭州赛宝卓越技术有限公司', '91110400MADFF1HE1T', '浙江省杭州市钱塘区临江街道纬五路3688号临江科创园6号楼12楼', '4008696096', '宁波银行股份有限公司北京丰台支行', '86041110000957180', 'ding-user-finance', '2026-08-07 15:46:00');

INSERT INTO invoice_subject (id, subject_name, subject_code, status, sort_no, created_by, updated_by, updated_at, deleted)
VALUES (1, '杭州主体', 'HZ', 'ENABLED', 10, 'ding-user-finance', '王财务', '2026-08-07 15:46:00', 0),
       (2, '北京主体', 'BJ', 'ENABLED', 20, 'ding-user-finance', '李会计', '2026-08-06 10:22:00', 0);

INSERT INTO ding_department (id, ding_department_id, department_name, status, sort_no)
VALUES (1, 'ding-dept-tech', '技术中心', 'ENABLED', 10),
       (2, 'ding-dept-finance', '财务部', 'ENABLED', 20),
       (3, 'ding-dept-purchase', '采购部', 'ENABLED', 30);

INSERT INTO ding_employee
(id, ding_user_id, employee_no, employee_name, department_id, department_name, mobile, status)
VALUES (1, 'ding-employee-001', 'SB0001', '示例员工', 1, '技术中心', '13800000001', 'ACTIVE'),
       (2, 'ding-employee-002', 'SB0002', '财务员工', 2, '财务部', '13800000002', 'ACTIVE'),
       (3, 'ding-employee-003', 'SB0003', '采购员工', 3, '采购部', '13800000003', 'ACTIVE'),
       (4, 'ding-employee-004', 'SB0004', '研发员工', 1, '技术中心', '13800000004', 'ACTIVE');

INSERT INTO ding_employee_department (employee_id, department_id, is_primary)
VALUES (1, 1, 1), (2, 2, 1), (3, 3, 1), (4, 1, 1);

INSERT INTO invoice_title_subject (id, title_id, subject_id)
VALUES (1, 1, 1), (2, 2, 2);

INSERT INTO subject_permission (id, subject_id, target_type, target_id, target_name, status, source, created_by, updated_by, updated_at, deleted)
VALUES (1, 1, 'USER', 'ding-employee-001', '示例员工', 'ENABLED', 'MANUAL', 'ding-user-finance', 'ding-user-finance', '2026-08-07 15:40:00', 0);

INSERT INTO invoice_operation_log
(id, module_type, operation_type, business_id, business_name, detail_json, result, operator_user_id, operator_name, client_ip, created_at)
VALUES (1, 'TITLE', 'PUBLISH', '1', '杭州赛宝卓越技术有限公司', '{"versionNo":3}', 'SUCCESS', 'ding-user-finance', '王财务', '127.0.0.1', '2026-08-07 15:46:12');

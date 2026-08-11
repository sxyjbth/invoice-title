INSERT INTO ding_department
(id, ding_department_id, department_name, parent_department_id, status, sort_no)
VALUES
(1, 'ding-dept-tech', '技术中心', NULL, 'ENABLED', 10),
(2, 'ding-dept-finance', '财务部', NULL, 'ENABLED', 20),
(3, 'ding-dept-purchase', '采购部', NULL, 'ENABLED', 30),
(4, 'ding-dept-east', '华东交付中心', NULL, 'ENABLED', 40),
(5, 'ding-dept-beijing', '北京研发中心', NULL, 'ENABLED', 50),
(6, 'ding-dept-shanghai', '上海交付中心', NULL, 'ENABLED', 60);

INSERT INTO ding_employee
(id, ding_user_id, employee_no, employee_name, department_id, department_name, mobile, status)
VALUES
(1, 'ding-employee-001', 'SB0001', '陈晓明', 1, '技术中心', '13800000001', 'ACTIVE'),
(2, 'ding-employee-002', 'SB0002', '李雨桐', 1, '技术中心', '13800000002', 'ACTIVE'),
(3, 'ding-employee-003', 'SB0003', '王子涵', 1, '技术中心', '13800000003', 'ACTIVE'),
(4, 'ding-employee-004', 'SB0004', '赵一鸣', 2, '财务部', '13800000004', 'ACTIVE'),
(5, 'ding-employee-005', 'SB0005', '周静怡', 2, '财务部', '13800000005', 'ACTIVE'),
(6, 'ding-employee-006', 'SB0006', '吴晨曦', 3, '采购部', '13800000006', 'ACTIVE'),
(7, 'ding-employee-007', 'SB0007', '郑思远', 3, '采购部', '13800000007', 'ACTIVE'),
(8, 'ding-employee-008', 'SB0008', '孙嘉禾', 4, '华东交付中心', '13800000008', 'ACTIVE'),
(9, 'ding-employee-009', 'SB0009', '钱若溪', 4, '华东交付中心', '13800000009', 'ACTIVE'),
(10, 'ding-employee-010', 'SB0010', '冯宇航', 5, '北京研发中心', '13800000010', 'ACTIVE'),
(11, 'ding-employee-011', 'SB0011', '褚文博', 5, '北京研发中心', '13800000011', 'ACTIVE'),
(12, 'ding-employee-012', 'SB0012', '卫佳宁', 6, '上海交付中心', '13800000012', 'ACTIVE');

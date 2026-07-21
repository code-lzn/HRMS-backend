-- ===================================================================
-- 5角色完整测试数据 v2 — 基于实体类精确列数
-- 密码: 12345678  MD5(limou12345678) = a1cedf10576ecbfef1ff522cdeba7c6e
-- ===================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ===================================================================
-- 1. user (8列) — 清空后插入
-- ===================================================================
TRUNCATE TABLE user;
INSERT INTO user (id, user_account, user_password, user_name, user_role) VALUES
(1, 'test_admin',     'a1cedf10576ecbfef1ff522cdeba7c6e', '张管理', 'admin'),
(2, 'test_hr',        'a1cedf10576ecbfef1ff522cdeba7c6e', '李人事', 'hr'),
(3, 'test_finance',   'a1cedf10576ecbfef1ff522cdeba7c6e', '王财务', 'finance'),
(4, 'test_dept_head', 'a1cedf10576ecbfef1ff522cdeba7c6e', '赵主管', 'dept_head'),
(5, 'test_user',      'a1cedf10576ecbfef1ff522cdeba7c6e', '钱员工', 'user');

-- ===================================================================
-- 2. department (10列)
-- ===================================================================
TRUNCATE TABLE department;
INSERT INTO department (id, name, code, parent_id, manager_id, sort_order, description) VALUES
(101, '集团总部',   '01',   NULL, NULL, 1, '公司总部'),
(102, '技术中心',   '0101', 101,  NULL, 1, '技术研发与平台建设'),
(103, '人力资源部', '0102', 101,  NULL, 2, '人力资源管理'),
(104, '财务部',     '0103', 101,  NULL, 3, '财务管理与薪资发放');

-- ===================================================================
-- 3. position (11列)
-- ===================================================================
TRUNCATE TABLE position;
INSERT INTO position (id, name, sequence, department_id, level_min, level_max, default_probation_months, description) VALUES
(201, '系统管理员',  2, 101, 7, 10, 0, '系统最高权限'),
(202, 'HR专员',      2, 103, 4,  6, 3, '负责员工档案、薪资、招聘'),
(203, '财务专员',    2, 104, 4,  6, 3, '负责薪酬核算、税务申报'),
(204, '技术主管',    2, 102, 6,  8, 3, '技术团队管理与架构设计'),
(205, '前端工程师',  2, 102, 4,  6, 3, '前端页面开发');

-- ===================================================================
-- 4. employee (9列)
-- ===================================================================
TRUNCATE TABLE employee;
INSERT INTO employee (id, user_id, employee_no, status, hire_date, hire_type) VALUES
(1001, 1, '20260101001', 2, '2024-01-15', 1),
(1002, 2, '20260103001', 2, '2024-03-01', 1),
(1003, 3, '20260104001', 2, '2024-06-01', 1),
(1004, 4, '20260102001', 2, '2024-02-01', 1),
(1005, 5, '20260102003', 1, '2026-07-01', 1);

-- ===================================================================
-- 5. employee_personal_info (14列)
-- ===================================================================
TRUNCATE TABLE employee_personal_info;
INSERT INTO employee_personal_info (id, employee_id, name, gender, phone, email, id_card, birthday, registered_address, current_address, emergency_contact_name, emergency_contact_phone) VALUES
(1, 1001, '张管理', 1, '13800001001', 'admin@hrms.com',   '110101199001011001', '1990-01-01', '北京市朝阳区',     '北京市朝阳区望京SOHO',    '张妻', '13900001001'),
(2, 1002, '李人事', 2, '13800001002', 'hr@hrms.com',      '110101199203152002', '1992-03-15', '上海市浦东新区',   '上海市浦东新区张江高科', '李夫', '13900001002'),
(3, 1003, '王财务', 1, '13800001003', 'finance@hrms.com', '110101198807203003', '1988-07-20', '广州市天河区',     '广州市天河区珠江新城',    '王妻', '13900001003'),
(4, 1004, '赵主管', 1, '13800001004', 'dept@hrms.com',    '110101198505104004', '1985-05-10', '深圳市南山区',     '深圳市南山区科技园',      '赵妻', '13900001004'),
(5, 1005, '钱员工', 2, '13800001005', 'user@hrms.com',    '110101199810015005', '1998-10-01', '杭州市西湖区',     '杭州市西湖区文三路',      '钱母', '13900001005');

-- ===================================================================
-- 6. employee_work_info (10列)
-- ===================================================================
TRUNCATE TABLE employee_work_info;
INSERT INTO employee_work_info (id, employee_id, department_id, position_id, job_level, direct_report_id, work_location) VALUES
(1, 1001, 101, 201, 'P8', NULL, '北京'),
(2, 1002, 103, 202, 'P5', 1001, '上海'),
(3, 1003, 104, 203, 'P5', 1001, '广州'),
(4, 1004, 102, 204, 'P7', 1001, '深圳'),
(5, 1005, 102, 205, 'P4', 1004, '杭州');

-- ===================================================================
-- 7. salary_account (7列)
-- ===================================================================
TRUNCATE TABLE salary_account;
INSERT INTO salary_account (id, name, scope_type, scope_ids, effective_date) VALUES
(1, '标准职员工资', 1, '101,102,103,104', '2024-01-01');

-- ===================================================================
-- 8. salary_item (9列)
-- ===================================================================
TRUNCATE TABLE salary_item;
INSERT INTO salary_item (id, account_id, name, item_type, formula, sort_order, is_taxable) VALUES
(1001, 1, '基本工资',   1, 'baseSalary',              1, 1),
(1002, 1, '岗位津贴',   1, 'allowanceBase',           2, 1),
(1003, 1, '绩效奖金',   2, 'performanceBase',         3, 1),
(1004, 1, '养老保险',   4, 'socialSecurityBase*0.08', 4, 0),
(1005, 1, '医疗保险',   4, 'socialSecurityBase*0.02', 5, 0),
(1006, 1, '失业保险',   4, 'socialSecurityBase*0.005',6, 0),
(1007, 1, '住房公积金', 5, 'housingFundBase*0.12',    7, 0),
(1008, 1, '个人所得税', 6, 'cumulative',              8, 0);

-- ===================================================================
-- 9. employee_salary (12列)
-- ===================================================================
TRUNCATE TABLE employee_salary;
INSERT INTO employee_salary (id, employee_id, account_id, base_salary, allowance_base, social_security_base, housing_fund_base, performance_base, effective_date) VALUES
(1, 1001, 1, 30000.00, 5000.00, 20000.00, 20000.00,  8000.00, '2024-01-15'),
(2, 1002, 1, 12000.00, 2000.00, 10000.00, 10000.00,  3000.00, '2024-03-01'),
(3, 1003, 1, 15000.00, 2000.00, 12000.00, 12000.00,  4000.00, '2024-06-01'),
(4, 1004, 1, 25000.00, 4000.00, 18000.00, 18000.00,  6000.00, '2024-02-01'),
(5, 1005, 1,  9000.00, 1000.00,  7000.00,  7000.00,  1500.00, '2026-07-01');

-- ===================================================================
-- 10. employee_salary_info (11列)
-- ===================================================================
TRUNCATE TABLE employee_salary_info;
INSERT INTO employee_salary_info (id, employee_id, employee_salary_id, contract_type, contract_expire_date, probation_ratio, salary_account_id, bank_account, bank_name) VALUES
(1, 1001, 1, 2, NULL,          1.0000, 1, '622200001001001', '工商银行'),
(2, 1002, 2, 1, '2027-03-01',  0.8000, 1, '622200001001002', '招商银行'),
(3, 1003, 3, 1, '2027-06-01',  0.8000, 1, '622200001001003', '建设银行'),
(4, 1004, 4, 1, '2027-02-01',  0.8000, 1, '622200001001004', '中国银行'),
(5, 1005, 5, 1, '2026-10-01',  0.8000, 1, '622200001001005', '农业银行');

-- ===================================================================
-- 11. salary_batch (3个批次)
-- ===================================================================
TRUNCATE TABLE salary_batch;
INSERT INTO salary_batch (id, batch_no, salary_month, status, create_by) VALUES
(101, 'SAL202605', '2026-05', 5, 1002),
(102, 'SAL202606', '2026-06', 5, 1002),
(103, 'SAL202607', '2026-07', 3, 1002);

-- ===================================================================
-- 12. salary_detail (14列 — 每批每人一条)
-- ===================================================================
TRUNCATE TABLE salary_detail;
-- 2026-05: 员工1001~1004 (1005未入职)
INSERT INTO salary_detail (id, batch_id, employee_id, salary_items, gross_pay, social_security, housing_fund, income_tax, total_deductions, net_pay) VALUES
(10001, 101, 1001, '[{"name":"基本工资","amount":30000},{"name":"岗位津贴","amount":5000},{"name":"绩效奖金","amount":8000},{"name":"养老保险","amount":1600},{"name":"医疗保险","amount":400},{"name":"失业保险","amount":100},{"name":"住房公积金","amount":2400},{"name":"个人所得税","amount":2500}]', 43000, 2100, 2400, 2500, 7000, 36000),
(10002, 101, 1002, '[{"name":"基本工资","amount":12000},{"name":"岗位津贴","amount":2000},{"name":"绩效奖金","amount":3000},{"name":"养老保险","amount":800},{"name":"医疗保险","amount":200},{"name":"失业保险","amount":50},{"name":"住房公积金","amount":1200},{"name":"个人所得税","amount":380}]',   17000, 1050, 1200,  380, 2630, 14370),
(10003, 101, 1003, '[{"name":"基本工资","amount":15000},{"name":"岗位津贴","amount":2000},{"name":"绩效奖金","amount":4000},{"name":"养老保险","amount":960},{"name":"医疗保险","amount":240},{"name":"失业保险","amount":60},{"name":"住房公积金","amount":1440},{"name":"个人所得税","amount":730}]',   21000, 1260, 1440,  730, 3430, 17570),
(10004, 101, 1004, '[{"name":"基本工资","amount":25000},{"name":"岗位津贴","amount":4000},{"name":"绩效奖金","amount":6000},{"name":"养老保险","amount":1440},{"name":"医疗保险","amount":360},{"name":"失业保险","amount":90},{"name":"住房公积金","amount":2160},{"name":"个人所得税","amount":1800}]',  35000, 1890, 2160, 1800, 5850, 29150);

-- 2026-06: 员工1001~1004
INSERT INTO salary_detail (id, batch_id, employee_id, salary_items, gross_pay, social_security, housing_fund, income_tax, total_deductions, net_pay) VALUES
(10005, 102, 1001, '[{"name":"基本工资","amount":30000},{"name":"岗位津贴","amount":5000},{"name":"绩效奖金","amount":8000},{"name":"养老保险","amount":1600},{"name":"医疗保险","amount":400},{"name":"失业保险","amount":100},{"name":"住房公积金","amount":2400},{"name":"个人所得税","amount":2500}]', 43000, 2100, 2400, 2500, 7000, 36000),
(10006, 102, 1002, '[{"name":"基本工资","amount":12000},{"name":"岗位津贴","amount":2000},{"name":"绩效奖金","amount":3000},{"name":"养老保险","amount":800},{"name":"医疗保险","amount":200},{"name":"失业保险","amount":50},{"name":"住房公积金","amount":1200},{"name":"个人所得税","amount":380}]',   17000, 1050, 1200,  380, 2630, 14370),
(10007, 102, 1003, '[{"name":"基本工资","amount":15000},{"name":"岗位津贴","amount":2000},{"name":"绩效奖金","amount":4000},{"name":"养老保险","amount":960},{"name":"医疗保险","amount":240},{"name":"失业保险","amount":60},{"name":"住房公积金","amount":1440},{"name":"个人所得税","amount":730}]',   21000, 1260, 1440,  730, 3430, 17570),
(10008, 102, 1004, '[{"name":"基本工资","amount":25000},{"name":"岗位津贴","amount":4000},{"name":"绩效奖金","amount":6000},{"name":"养老保险","amount":1440},{"name":"医疗保险","amount":360},{"name":"失业保险","amount":90},{"name":"住房公积金","amount":2160},{"name":"个人所得税","amount":1800}]',  35000, 1890, 2160, 1800, 5850, 29150);

-- 2026-07: 5人全参与
INSERT INTO salary_detail (id, batch_id, employee_id, salary_items, gross_pay, social_security, housing_fund, income_tax, total_deductions, net_pay, is_abnormal, abnormal_reason) VALUES
(10009, 103, 1001, '[{"name":"基本工资","amount":30000},{"name":"岗位津贴","amount":5000},{"name":"绩效奖金","amount":8000},{"name":"养老保险","amount":1600},{"name":"医疗保险","amount":400},{"name":"失业保险","amount":100},{"name":"住房公积金","amount":2400},{"name":"个人所得税","amount":2500}]', 43000, 2100, 2400, 2500, 7000, 36000, 0, NULL),
(10010, 103, 1002, '[{"name":"基本工资","amount":12000},{"name":"岗位津贴","amount":2000},{"name":"绩效奖金","amount":3000},{"name":"养老保险","amount":800},{"name":"医疗保险","amount":200},{"name":"失业保险","amount":50},{"name":"住房公积金","amount":1200},{"name":"个人所得税","amount":380}]',   17000, 1050, 1200,  380, 2630, 14370, 0, NULL),
(10011, 103, 1003, '[{"name":"基本工资","amount":15000},{"name":"岗位津贴","amount":2000},{"name":"绩效奖金","amount":4000},{"name":"养老保险","amount":960},{"name":"医疗保险","amount":240},{"name":"失业保险","amount":60},{"name":"住房公积金","amount":1440},{"name":"个人所得税","amount":730}]',   21000, 1260, 1440,  730, 3430, 17570, 0, NULL),
(10012, 103, 1004, '[{"name":"基本工资","amount":25000},{"name":"岗位津贴","amount":4000},{"name":"绩效奖金","amount":6000},{"name":"养老保险","amount":1440},{"name":"医疗保险","amount":360},{"name":"失业保险","amount":90},{"name":"住房公积金","amount":2160},{"name":"个人所得税","amount":1800}]',  35000, 1890, 2160, 1800, 5850, 29150, 0, NULL),
(10013, 103, 1005, '[{"name":"基本工资","amount":7200},{"name":"岗位津贴","amount":800},{"name":"绩效奖金","amount":1200},{"name":"养老保险","amount":560},{"name":"医疗保险","amount":140},{"name":"失业保险","amount":35},{"name":"住房公积金","amount":840},{"name":"个人所得税","amount":0}]',          9200,   735,  840,    0, 1575,  7625, 1, '试用期薪资，个税未达起征点');

-- 更新批次汇总
UPDATE salary_batch SET
  total_employees = (SELECT COUNT(*) FROM salary_detail WHERE batch_id = salary_batch.id),
  total_gross_pay = (SELECT COALESCE(SUM(gross_pay), 0) FROM salary_detail WHERE batch_id = salary_batch.id),
  total_net_pay   = (SELECT COALESCE(SUM(net_pay), 0) FROM salary_detail WHERE batch_id = salary_batch.id),
  total_tax       = (SELECT COALESCE(SUM(income_tax), 0) FROM salary_detail WHERE batch_id = salary_batch.id);

-- ===================================================================
-- 13. salary_change_history
-- ===================================================================
TRUNCATE TABLE salary_change_history;
INSERT INTO salary_change_history (id, employee_id, change_type, old_value, new_value, effective_date, operator_id, remark) VALUES
(1, 1002, 1, '{"base_salary":10000}', '{"base_salary":12000}', '2025-07-01', 1001, '年度调薪'),
(2, 1005, 4, NULL, '{"base_salary":9000}', '2026-07-01', 1002, '入职定薪(试用期)');

-- ===================================================================
-- 14. income_tax_cumulative
-- ===================================================================
TRUNCATE TABLE income_tax_cumulative;
INSERT INTO income_tax_cumulative (id, employee_id, tax_year, tax_month, cumulative_gross_pay, cumulative_threshold, cumulative_social_security, cumulative_housing_fund, cumulative_special_deduction, cumulative_taxable_income, tax_rate, quick_deduction, cumulative_tax_payable, cumulative_tax_paid, current_month_tax) VALUES
(1, 1001, 2026, 7, 301000, 35000, 14700, 16800, 0, 234500, 0.2000, 16920, 29980, 27480, 2500);

-- ===================================================================
-- 15. 考勤相关
-- ===================================================================
TRUNCATE TABLE attendance_group;
INSERT INTO attendance_group (id, name, shift_type, start_time, end_time, rest_start_time, rest_end_time, late_threshold, early_leave_threshold) VALUES
(1, '标准工时组', 1, '09:00:00', '18:00:00', '12:00:00', '13:00:00', 15, 15);

TRUNCATE TABLE attendance_group_rule;
INSERT INTO attendance_group_rule (attendance_group_id, rule_type, target_id) VALUES
(1, 1, 101), (1, 1, 102), (1, 1, 103), (1, 1, 104);

TRUNCATE TABLE work_calendar;
INSERT INTO work_calendar (calendar_date, day_type) VALUES
('2026-07-01', 1), ('2026-07-02', 1), ('2026-07-03', 1),
('2026-07-06', 1), ('2026-07-07', 1), ('2026-07-08', 1),
('2026-07-09', 1), ('2026-07-10', 1), ('2026-07-13', 1),
('2026-07-14', 1), ('2026-07-15', 1), ('2026-07-16', 1),
('2026-07-17', 1);

TRUNCATE TABLE attendance_record;
INSERT INTO attendance_record (employee_id, attendance_date, attendance_group_id, scheduled_start_time, scheduled_end_time, actual_start_time, actual_end_time, start_status, end_status) VALUES
(1001, '2026-07-17', 1, '09:00:00', '18:00:00', '2026-07-17 08:55:00', '2026-07-17 18:05:00', 1, 1),
(1002, '2026-07-17', 1, '09:00:00', '18:00:00', '2026-07-17 09:10:00', '2026-07-17 17:55:00', 2, 1),
(1003, '2026-07-17', 1, '09:00:00', '18:00:00', '2026-07-17 08:50:00', '2026-07-17 18:00:00', 1, 1),
(1004, '2026-07-17', 1, '09:00:00', '18:00:00', '2026-07-17 08:45:00', '2026-07-17 18:30:00', 1, 1),
(1005, '2026-07-17', 1, '09:00:00', '18:00:00', NULL, NULL, 4, 4);

-- ===================================================================
-- 16. 假期/加班
-- ===================================================================
TRUNCATE TABLE leave_request;
INSERT INTO leave_request (employee_id, leave_type, start_time, end_time, leave_days, reason, status) VALUES
(1002, 1, '2026-07-20 09:00:00', '2026-07-20 18:00:00', 1.0, '年假休息', 3);

TRUNCATE TABLE overtime_record;
INSERT INTO overtime_record (employee_id, overtime_date, start_time, end_time, hours, expire_date) VALUES
(1004, '2026-07-16', '2026-07-16 18:00:00', '2026-07-16 21:00:00', 3.0, '2026-08-31');

TRUNCATE TABLE employee_leave_balance;
INSERT INTO employee_leave_balance (employee_id, year, leave_type, total_days, used_days, remaining_days) VALUES
(1001, 2026, 1, 15, 0, 15),
(1002, 2026, 1, 10, 1,  9),
(1003, 2026, 1, 10, 0, 10),
(1004, 2026, 1, 12, 0, 12),
(1005, 2026, 1,  5, 0,  5);

TRUNCATE TABLE supplement_card_request;
INSERT INTO supplement_card_request (employee_id, attendance_date, card_type, reason, status) VALUES
(1005, '2026-07-17', 1, '忘记打卡', 1);

TRUNCATE TABLE attendance_statistics;
INSERT INTO attendance_statistics (employee_id, stat_year, stat_month, scheduled_days, actual_days, late_count, early_leave_count, absent_days, leave_days, overtime_hours) VALUES
(1001, 2026, 6, 22, 22, 0, 0, 0, 0,  0),
(1002, 2026, 6, 22, 21, 1, 0, 0, 1,  2),
(1003, 2026, 6, 22, 22, 0, 0, 0, 0,  0),
(1004, 2026, 6, 22, 22, 0, 0, 0, 0,  5);

-- ===================================================================
-- 17. 入职/审批
-- ===================================================================
TRUNCATE TABLE onboarding_application;
INSERT INTO onboarding_application (name, gender, phone, email, id_card, expected_hire_date, department_id, position_id, hire_type, default_probation_months, probation_ratio, status, applicant_id) VALUES
('孙新人', 1, '13800001006', 'new@hrms.com', '110101200001016006', '2026-08-01', 102, 205, 1, 3, 0.8000, 1, 1002);

TRUNCATE TABLE approval_instance;
INSERT INTO approval_instance (biz_type, biz_id, title, status, applicant_id, current_node_order) VALUES
('SALARY_BATCH', 103, '2026年07月薪资核算审批', 2, 1002, 2),
('LEAVE', 1, '李人事-年假申请', 2, 1002, 1);

TRUNCATE TABLE approval_node;
INSERT INTO approval_node (instance_id, node_name, node_order, approver_id, status, comment, operate_time) VALUES
(1, 'HR负责人审批', 1, 1002, 2, '数据无误', '2026-07-16 10:00:00'),
(1, '财务审核',     2, 1003, 2, '审核通过', '2026-07-16 11:00:00'),
(2, '部门负责人审批', 1, 1004, 2, '同意',     '2026-07-15 11:00:00');

-- ===================================================================
-- 空表 TRUNCATE（清除旧数据）
-- ===================================================================
TRUNCATE TABLE approval_delegate;
TRUNCATE TABLE attendance_correction_log;
TRUNCATE TABLE attendance_schedule;
TRUNCATE TABLE employee_change_log;
TRUNCATE TABLE employee_no_sequence;
TRUNCATE TABLE probation_application;
TRUNCATE TABLE resignation_application;
TRUNCATE TABLE transfer_application;
TRUNCATE TABLE transfer_history;

SET FOREIGN_KEY_CHECKS = 1;

-- ===================================================================
-- 验证
-- ===================================================================
SELECT '=== 登录账号 ===' AS '';
SELECT user_account AS 账号, user_name AS 姓名, user_role AS 角色, '12345678' AS 密码 FROM user WHERE is_deleted = 0;

SELECT '=== 员工关联 ===' AS '';
SELECT e.id, e.employee_no, u.user_account, u.user_role, d.name AS dept
FROM employee e
JOIN user u ON e.user_id = u.id
JOIN employee_work_info w ON e.id = w.employee_id
JOIN department d ON w.department_id = d.id
WHERE u.is_deleted = 0;

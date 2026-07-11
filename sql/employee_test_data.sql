-- 员工测试数据（用于测试部门合并）
use szml;

-- 先确认有哪些部门
-- SELECT id, name, code, parent_id FROM department WHERE is_deleted = 0;

-- 总公司（id=1）下的员工
INSERT INTO `employee` (`id`, `name`, `department_id`, `position_id`, `status`) VALUES
(101, '王总',     1, 1, 2),
(102, '行政小李', 1, 5, 1);

-- 假设已创建了技术部（id 可能是 2 或 3，请根据实际情况调整）
-- 如果技术部 id=2，则：
INSERT INTO `employee` (`id`, `name`, `department_id`, `position_id`, `status`) VALUES
(201, '技术张经理', 7, 2, 2),
(202, 'Java工程师A', 7, 4, 1),
(203, 'Java工程师B', 7, 4, 2),
(204, '高级工程师C', 7, 3, 2);

-- 如果产品部 id=3，则：
INSERT INTO `employee` (`id`, `name`, `department_id`, `position_id`, `status`) VALUES
(301, '产品李经理', 8, 2, 2),
(302, '产品专员小王', 8, 4, 1);

-- 如果有子部门（如技术部下设前端组 id=4），则：
INSERT INTO `employee` (`id`, `name`, `department_id`, `position_id`, `status`) VALUES
(401, '前端工程师D', 4, 4, 2),
(402, '前端工程师E', 4, 4, 1);

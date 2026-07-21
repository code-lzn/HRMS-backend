-- =====================================================
-- AI 智能助理模块 - 数据库初始化脚本
-- =====================================================

-- 1. 知识库文档表
use szml1;
DROP TABLE IF EXISTS ai_knowledge_doc;
CREATE TABLE ai_knowledge_doc (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    content TEXT NOT NULL COMMENT '文档正文',
    category VARCHAR(50) COMMENT '分类：leave/salary/attendance/onboarding/welfare/transfer/resignation',
    route_path VARCHAR(200) COMMENT '关联前端路由',
    route_name VARCHAR(100) COMMENT '路由显示名称',
    status TINYINT DEFAULT 1 COMMENT '状态：1=启用 0=停用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库文档';

-- 2. 对话历史表
DROP TABLE IF EXISTS ai_chat_history;
CREATE TABLE ai_chat_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话UUID',
    role VARCHAR(20) NOT NULL COMMENT '角色：user/assistant',
    content TEXT NOT NULL COMMENT '消息内容',
    intent VARCHAR(50) COMMENT '识别到的意图',
    route_path VARCHAR(200) COMMENT '推送的路由路径',
    route_label VARCHAR(100) COMMENT '推送的路由名称',
    feedback TINYINT COMMENT '用户反馈：1=点赞 0=点踩 NULL=未反馈',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_session (user_id, session_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话历史';

-- 3. 意图-路由映射表
DROP TABLE IF EXISTS ai_intent_route;
CREATE TABLE ai_intent_route (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    intent_name VARCHAR(50) NOT NULL COMMENT '意图名称：leave_query/leave_action/salary_query等',
    keywords VARCHAR(500) NOT NULL COMMENT '触发关键词，逗号分隔',
    route_path VARCHAR(200) NOT NULL COMMENT '前端路由路径',
    route_label VARCHAR(100) COMMENT '路由显示名称',
    response_type VARCHAR(20) DEFAULT 'both' COMMENT '响应类型：route_only=仅推送路由 answer_route=答案+路由 both=两者都做',
    priority INT DEFAULT 0 COMMENT '优先级，数值越大越优先',
    status TINYINT DEFAULT 1 COMMENT '状态：1=启用 0=停用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status_priority (status, priority DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='意图路由映射表';

-- =====================================================
-- 种子数据：意图路由映射
-- =====================================================
INSERT INTO ai_intent_route (intent_name, keywords, route_path, route_label, response_type, priority) VALUES
('leave_query',    '年假,请假,休假,假期,调休,事假,病假,婚假,产假,丧假,请假流程,请假制度,怎么请假,如何请假,请假规则',          '/attendance/leave',     '员工请假',   'answer_route', 100),
('leave_action',   '我想请假,我要请假,申请请假,帮我请假,想请假,要请假,提交请假',                                          '/attendance/leave',     '申请请假',   'route_only',   90),
('salary_query',   '工资,薪资,薪酬,待遇,福利,五险一金,社保,公积金,个税,工资条,发工资,薪水,奖金,绩效,调薪制度',                 '/salary/mine',          '工资表查询', 'answer_route', 100),
('salary_action',  '我要调薪,申请调薪,想调薪,调薪申请,涨薪,加薪',                                                          '/salary/adjust',        '员工调薪',   'both',         90),
('attendance_query','考勤,打卡,签到,迟到,早退,旷工,缺卡,上班时间,下班时间,打卡规则',                                        '/attendance/records',   '考勤记录',   'answer_route', 100),
('welfare_query',  '公司有哪些福利,福利制度,福利待遇,补贴,餐补,交通补贴,房补,节日福利,体检,团建',                              '/welfare',              '薪资福利总览','answer_route', 80),
('transfer_action','调岗,转岗,岗位调动,申请调岗,想换岗位,换部门',                                                           '/transfer/apply',       '岗位调动',   'both',         90),
('resignation_action','离职,辞职,申请离职,我要离职,想辞职,办离职',                                                           '/resignation/apply',    '申请离职',   'both',         90),
('onboarding_query','入职,入职流程,入职手续,新人,报到,入职材料,入职培训',                                                    '/onboarding',           '入职管理',   'answer_route', 80),
('regularization_action','转正,申请转正,实习转正,试用期',                                                                  '/regularization/apply', '申请转正',   'both',         90);

-- =====================================================
-- 种子数据：知识库文档
-- =====================================================
INSERT INTO ai_knowledge_doc (title, content, category, route_path, route_name) VALUES
('年假制度', '公司员工年假制度如下：\n1. 员工入职满一年后享有5天年假\n2. 工龄满5年享有10天年假\n3. 工龄满10年享有15天年假\n4. 年假可分段休，每次至少半天\n5. 年假当年有效，不可跨年累积\n6. 申请年假需提前至少1个工作日提交', 'leave', '/attendance/leave', '员工请假'),
('请假流程', '请假申请流程：\n1. 登录HRMS系统，进入「考勤管理」→「员工请假」\n2. 选择请假类型（事假/病假/年假/婚假/产假/丧假/调休）\n3. 填写请假起止日期和请假原因\n4. 半天请假需选择上午或下午\n5. 提交后系统自动匹配审批流\n6. 审批流程：直属上级→部门负责人→HR（视请假类型和天数而定）\n7. 审批通过后考勤自动更新为请假状态', 'leave', '/attendance/leave', '员工请假'),
('工资查询', '工资查询方法：\n1. 登录HRMS系统，进入「薪资管理」→「工资表查询」\n2. 选择对应月份即可查看工资详情\n3. 工资条包含：基本工资、绩效工资、加班费、补贴、扣款项、社保公积金、个税等\n4. 每月10号发放上月工资\n5. 如需查看工资条，需输入验证密码', 'salary', '/salary/mine', '工资表查询'),
('公司福利', '公司福利制度：\n1. 五险一金：按国家规定缴纳养老、医疗、失业、工伤、生育保险及住房公积金\n2. 带薪年假：入职满一年享有5-15天带薪年假\n3. 餐补：工作日提供餐补20元/天\n4. 交通补贴：每月200元交通补贴\n5. 节日福利：春节、中秋、端午发放节日礼品\n6. 年度体检：每年一次免费健康体检\n7. 团建活动：每季度一次团队建设活动\n8. 培训发展：提供内外部培训机会', 'welfare', '/salary/mine', '薪资福利总览'),
('考勤制度', '公司考勤制度：\n1. 上班时间：9:00-18:00（中午12:00-13:00午餐休息）\n2. 打卡要求：每日上下班各打卡一次，共两次\n3. 迟到处理：9:00后打卡计为迟到，月累计超过3次扣绩效\n4. 早退处理：18:00前离岗计为早退\n5. 缺卡处理：忘记打卡需在次日提交补卡申请\n6. 加班制度：加班需提前申请并审批，加班可申请调休或加班费\n7. 外勤：外出办公需提前报备并在系统登记', 'attendance', '/attendance/records', '考勤记录'),
('调薪流程', '员工调薪流程：\n1. 员工可向直属上级提出调薪申请\n2. 在HRMS系统中进入「薪资管理」→「员工调薪」\n3. 填写调薪申请，说明调薪原因\n4. 审批流程：直属上级→部门负责人→HR→财务→总经理\n5. 调薪审批通过后，次月生效\n6. 调薪参考因素：绩效表现、市场薪资水平、岗位价值', 'salary', '/salary/adjust', '员工调薪'),
('调岗流程', '员工调岗流程：\n1. 在HRMS系统中进入「入转调离」→「岗位调动」\n2. 填写调动申请：目标部门、目标岗位、调动原因\n3. 审批流程：调出部门负责人→调入部门负责人→HR\n4. 调岗审批通过后，系统自动更新组织架构和员工档案\n5. 调岗可能涉及薪资调整，需同步走调薪流程', 'transfer', '/transfer/apply', '岗位调动'),
('入职流程', '新员工入职流程：\n1. HR在系统中录入员工基本信息\n2. 填写入职信息：个人信息、教育经历、工作经历等\n3. 上传入职材料：身份证、学历证书、离职证明等\n4. 分配工号、部门、岗位\n5. 开通系统账号\n6. 安排入职培训\n7. 领取办公设备和工卡', 'onboarding', '/onboarding', '入职管理'),
('离职流程', '员工离职流程：\n1. 员工在HRMS系统中提交离职申请\n2. 进入「入转调离」→「离职办理」\n3. 填写离职原因和最后工作日\n4. 审批流程：直属上级→部门负责人→HR\n5. 办理离职交接：工作交接、资产归还、财务结算\n6. HR确认离职日期，系统停用账号', 'resignation', '/resignation/apply', '申请离职'),
('转正流程', '员工转正流程：\n1. 试用期满前一周，系统自动提醒\n2. 员工在HRMS系统中提交转正申请\n3. 进入「入转调离」→「转正管理」\n4. 填写试用期工作总结\n5. 审批流程：直属上级→部门负责人→HR\n6. 转正审批通过后自动更新员工状态', 'regularization', '/regularization/apply', '申请转正');

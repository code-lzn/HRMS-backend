/*
SQLyog Ultimate v12.08 (64 bit)
MySQL - 5.7.40-log : Database - szml1
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`szml1` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;

USE `szml1`;

/*Table structure for table `approval_delegation` */

DROP TABLE IF EXISTS `approval_delegation`;

CREATE TABLE `approval_delegation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `delegatorId` bigint(20) NOT NULL COMMENT '委托人ID（employeeId）',
  `delegatorName` varchar(64) NOT NULL COMMENT '委托人姓名（冗余）',
  `delegateId` bigint(20) NOT NULL COMMENT '被委托人ID（employeeId）',
  `delegateName` varchar(64) NOT NULL COMMENT '被委托人姓名（冗余）',
  `businessTypes` varchar(256) DEFAULT NULL COMMENT '委托业务类型（逗号分隔，NULL=全部）',
  `startDate` date NOT NULL COMMENT '委托开始日期',
  `endDate` date NOT NULL COMMENT '委托结束日期',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态: 1=有效, 0=已取消',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_delegator_id` (`delegatorId`),
  KEY `idx_delegate_id` (`delegateId`),
  KEY `idx_status_dates` (`status`,`startDate`,`endDate`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='审批委托表';

/*Data for the table `approval_delegation` */

insert  into `approval_delegation`(`id`,`delegatorId`,`delegatorName`,`delegateId`,`delegateName`,`businessTypes`,`startDate`,`endDate`,`status`,`createTime`,`updateTime`) values (1,2,'李四',15,'张三','LEAVE,PATCH_CLOCK','2026-07-01','2026-07-31',1,'2026-07-12 20:30:38','2026-07-12 20:30:38');

/*Table structure for table `approval_detail` */

DROP TABLE IF EXISTS `approval_detail`;

CREATE TABLE `approval_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `recordId` bigint(20) NOT NULL COMMENT '审批实例ID',
  `nodeId` bigint(20) NOT NULL COMMENT '审批节点定义ID',
  `nodeName` varchar(64) NOT NULL COMMENT '节点名称（快照）',
  `stepOrder` int(11) NOT NULL COMMENT '步骤序号',
  `approverId` bigint(20) DEFAULT NULL COMMENT '审批人ID',
  `approverName` varchar(64) DEFAULT NULL COMMENT '审批人姓名（冗余）',
  `action` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '审批动作: PENDING=待审批, APPROVE=通过, REJECT=拒绝, TRANSFER=转交',
  `comment` text COMMENT '审批意见',
  `isDelegated` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否代审批: 0=否, 1=是',
  `delegatedBy` bigint(20) DEFAULT NULL COMMENT '委托人ID（代审批时记录）',
  `operateTime` datetime DEFAULT NULL COMMENT '操作时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_record_id` (`recordId`),
  KEY `idx_approver_id` (`approverId`),
  KEY `idx_action` (`action`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COMMENT='审批明细表';

/*Data for the table `approval_detail` */

insert  into `approval_detail`(`id`,`recordId`,`nodeId`,`nodeName`,`stepOrder`,`approverId`,`approverName`,`action`,`comment`,`isDelegated`,`delegatedBy`,`operateTime`,`createTime`) values (1,1,10,'直接上级',1,2,'李四','PENDING',NULL,0,NULL,NULL,'2026-07-12 20:30:35'),(2,2,11,'直接上级',1,2,'李四','PENDING',NULL,0,NULL,NULL,'2026-07-12 20:30:35'),(3,3,3,'部门负责人',1,2,'李四','APPROVE','表现优秀，同意转正',0,NULL,'2026-07-05 10:00:00','2026-07-12 20:30:36'),(4,3,4,'HR负责人',2,3,'赵六','APPROVE','同意',0,NULL,'2026-07-05 16:00:00','2026-07-12 20:30:36'),(5,4,8,'部门负责人',1,2,'李四','APPROVE','确认',0,NULL,'2026-07-03 09:00:00','2026-07-12 20:30:37'),(6,4,9,'HR负责人',2,3,'赵六','REJECT','暂不批准离职',0,NULL,'2026-07-03 14:00:00','2026-07-12 20:30:37'),(7,5,12,'财务专员',1,4,'孙八','APPROVE','薪资核算无误',0,NULL,'2026-07-10 09:00:00','2026-07-12 20:30:37'),(8,5,13,'老板',2,1,'周董','PENDING',NULL,0,NULL,NULL,'2026-07-12 20:30:37'),(9,6,10,'直接上级',1,15,'limou','PENDING',NULL,0,NULL,NULL,'2026-07-14 15:06:47'),(10,7,10,'直接上级',1,15,'limou','APPROVE','',0,NULL,'2026-07-14 16:57:24','2026-07-14 15:16:53'),(11,8,10,'直接上级',1,15,'limou','APPROVE','',0,NULL,'2026-07-14 16:56:30','2026-07-14 16:15:10'),(12,9,16,'部门负责人',1,1,'张伟','APPROVE','通过',0,NULL,'2026-07-15 22:29:36','2026-07-15 12:11:03'),(13,9,17,'HR负责人',2,19,'HHRR','APPROVE','同意',0,NULL,'2026-07-15 22:44:00','2026-07-15 12:11:03'),(14,10,16,'部门负责人',1,NULL,NULL,'PENDING',NULL,0,NULL,NULL,'2026-07-15 21:19:56'),(15,10,17,'HR负责人',2,19,'HHRR','APPROVE','同意',0,NULL,'2026-07-15 22:13:33','2026-07-15 21:19:56');

/*Table structure for table `approval_flow` */

DROP TABLE IF EXISTS `approval_flow`;

CREATE TABLE `approval_flow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `businessType` varchar(32) NOT NULL COMMENT '业务类型: ONBOARDING=入职, REGULARIZATION=转正, TRANSFER=调岗, RESIGNATION=离职, LEAVE=请假, PATCH_CLOCK=补卡, SALARY_BATCH=薪资批次',
  `flowName` varchar(64) NOT NULL COMMENT '审批流名称',
  `description` varchar(256) DEFAULT NULL COMMENT '说明',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态: 1=启用, 0=禁用',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_type` (`businessType`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COMMENT='审批流定义表';

/*Data for the table `approval_flow` */

insert  into `approval_flow`(`id`,`businessType`,`flowName`,`description`,`status`,`createTime`,`updateTime`) values (1,'ONBOARDING','入职审批','HR发起 → 部门负责人 → HR负责人',1,'2026-07-12 20:30:31','2026-07-12 20:30:31'),(2,'REGULARIZATION','转正审批','HR发起 → 部门负责人 → HR负责人',1,'2026-07-12 20:30:31','2026-07-12 20:30:31'),(3,'TRANSFER','调岗审批','HR发起 → 原部门负责人 → 新部门负责人 → HR负责人',1,'2026-07-12 20:30:31','2026-07-12 20:30:31'),(4,'RESIGNATION','离职审批','HR发起 → 部门负责人 → HR负责人',1,'2026-07-12 20:30:31','2026-07-12 20:30:31'),(5,'LEAVE','请假审批','员工发起 → 直接上级',1,'2026-07-12 20:30:31','2026-07-12 20:30:31'),(6,'PATCH_CLOCK','补卡审批','员工发起 → 直接上级',1,'2026-07-12 20:30:31','2026-07-12 20:30:31'),(7,'SALARY_BATCH','薪资批次审批','HR发起 → 财务专员 → 老板',1,'2026-07-12 20:30:31','2026-07-12 20:30:31');

/*Table structure for table `approval_flow_node` */

DROP TABLE IF EXISTS `approval_flow_node`;

CREATE TABLE `approval_flow_node` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `flowId` bigint(20) NOT NULL COMMENT '审批流ID',
  `nodeName` varchar(64) NOT NULL COMMENT '节点名称, 如"部门负责人审批"',
  `nodeOrder` int(11) NOT NULL COMMENT '节点顺序, 从1开始',
  `approverType` varchar(16) NOT NULL COMMENT '审批人类型: DEPT_MANAGER=部门负责人, HR_MANAGER=HR负责人, DIRECT_SUPERIOR=直接上级, FINANCE=财务专员, BOSS=老板, SPECIFIED=指定人',
  `approverId` bigint(20) DEFAULT NULL COMMENT '指定审批人ID（approverType=SPECIFIED时使用）',
  `isOptional` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否可选: 0=必选, 1=可选',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_flow_id` (`flowId`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COMMENT='审批节点定义表';

/*Data for the table `approval_flow_node` */

insert  into `approval_flow_node`(`id`,`flowId`,`nodeName`,`nodeOrder`,`approverType`,`approverId`,`isOptional`,`createTime`) values (3,2,'部门负责人',1,'DEPT_MANAGER',NULL,0,'2026-07-12 20:30:32'),(4,2,'HR负责人',2,'HR_MANAGER',NULL,0,'2026-07-12 20:30:32'),(5,3,'原部门负责人',1,'DEPT_MANAGER',NULL,0,'2026-07-12 20:30:32'),(6,3,'新部门负责人',2,'DEPT_MANAGER',NULL,0,'2026-07-12 20:30:32'),(7,3,'HR负责人',3,'HR_MANAGER',NULL,0,'2026-07-12 20:30:32'),(8,4,'部门负责人',1,'DEPT_MANAGER',NULL,0,'2026-07-12 20:30:33'),(9,4,'HR负责人',2,'HR_MANAGER',NULL,0,'2026-07-12 20:30:33'),(10,5,'直接上级',1,'DIRECT_SUPERIOR',NULL,0,'2026-07-12 20:30:33'),(11,6,'直接上级',1,'DIRECT_SUPERIOR',NULL,0,'2026-07-12 20:30:33'),(12,7,'财务专员',1,'FINANCE',NULL,0,'2026-07-12 20:30:34'),(13,7,'老板',2,'BOSS',NULL,1,'2026-07-12 20:30:34'),(16,1,'部门负责人',1,'DEPT_MANAGER',NULL,0,'2026-07-15 11:53:36'),(17,1,'HR负责人',2,'SPECIFIED',1,1,'2026-07-15 11:53:36');

/*Table structure for table `approval_record` */

DROP TABLE IF EXISTS `approval_record`;

CREATE TABLE `approval_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `flowId` bigint(20) NOT NULL COMMENT '审批流定义ID',
  `businessType` varchar(32) NOT NULL COMMENT '业务类型',
  `businessId` bigint(20) NOT NULL COMMENT '关联业务表记录ID',
  `applicantId` bigint(20) NOT NULL COMMENT '申请人ID（employeeId）',
  `applicantName` varchar(64) DEFAULT NULL COMMENT '申请人姓名（冗余，便于列表展示）',
  `currentStep` int(11) NOT NULL DEFAULT '1' COMMENT '当前审批步骤',
  `totalSteps` int(11) NOT NULL COMMENT '总步骤数',
  `status` varchar(16) NOT NULL DEFAULT 'APPROVING' COMMENT '审批状态: APPROVING=审批中, APPROVED=已通过, REJECTED=已拒绝, WITHDRAWN=已撤回',
  `finishedAt` datetime DEFAULT NULL COMMENT '审批完成时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business` (`businessType`,`businessId`),
  KEY `idx_applicant_id` (`applicantId`),
  KEY `idx_status` (`status`),
  KEY `idx_current_step` (`currentStep`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COMMENT='审批实例表';

/*Data for the table `approval_record` */

insert  into `approval_record`(`id`,`flowId`,`businessType`,`businessId`,`applicantId`,`applicantName`,`currentStep`,`totalSteps`,`status`,`finishedAt`,`createTime`,`updateTime`) values (1,5,'LEAVE',100,15,'张三',1,1,'APPROVING',NULL,'2026-07-12 20:30:34','2026-07-12 20:30:34'),(2,6,'PATCH_CLOCK',101,15,'张三',1,1,'APPROVING',NULL,'2026-07-12 20:30:35','2026-07-12 20:30:35'),(3,2,'REGULARIZATION',200,8,'王五',2,2,'APPROVED','2026-07-05 16:00:00','2026-07-12 20:30:36','2026-07-12 20:30:36'),(4,4,'RESIGNATION',300,10,'钱七',2,2,'REJECTED','2026-07-03 14:00:00','2026-07-12 20:30:36','2026-07-12 20:30:36'),(5,7,'SALARY_BATCH',400,3,'赵六',2,2,'APPROVING',NULL,'2026-07-12 20:30:37','2026-07-12 20:30:37'),(6,5,'LEAVE',2076926360054890498,15,'limou',1,1,'WITHDRAWN','2026-07-14 15:17:11','2026-07-14 15:06:47','2026-07-14 15:06:47'),(7,5,'LEAVE',2076928900096032769,15,'limou',1,1,'APPROVED','2026-07-14 16:57:24','2026-07-14 15:16:53','2026-07-14 15:16:53'),(8,5,'LEAVE',2076943565853204481,15,'limou',1,1,'APPROVED','2026-07-14 16:56:31','2026-07-14 16:15:09','2026-07-14 16:15:09'),(9,1,'ONBOARDING',1,2076964719435886594,'张三',2,2,'APPROVED','2026-07-15 22:44:00','2026-07-15 12:11:03','2026-07-15 12:11:03'),(10,1,'ONBOARDING',2,2076964719435886594,'林城',2,2,'APPROVING',NULL,'2026-07-15 21:19:56','2026-07-15 21:19:56');

/*Table structure for table `attendance` */

DROP TABLE IF EXISTS `attendance`;

CREATE TABLE `attendance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employeeId` bigint(20) NOT NULL COMMENT '员工ID',
  `userId` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `attendanceDate` date NOT NULL COMMENT '考勤日期',
  `punchInTime` datetime DEFAULT NULL COMMENT '上班打卡时间',
  `punchOutTime` datetime DEFAULT NULL COMMENT '下班打卡时间',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态：0=正常 1=迟到 2=早退 3=缺卡 4=请假 5=旷工',
  `punchInType` tinyint(4) DEFAULT NULL COMMENT '上班打卡方式：0=网页 1=APP',
  `punchOutType` tinyint(4) DEFAULT NULL COMMENT '下班打卡方式：0=网页 1=APP',
  `punchInLocation` varchar(256) DEFAULT NULL COMMENT '上班打卡位置',
  `punchOutLocation` varchar(256) DEFAULT NULL COMMENT '下班打卡位置',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDeleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=否 1=是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_date` (`employeeId`,`attendanceDate`),
  KEY `idx_user_id` (`userId`),
  KEY `idx_attendance_date` (`attendanceDate`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2077292019532095490 DEFAULT CHARSET=utf8mb4 COMMENT='考勤打卡记录表';

/*Data for the table `attendance` */

insert  into `attendance`(`id`,`employeeId`,`userId`,`attendanceDate`,`punchInTime`,`punchOutTime`,`status`,`punchInType`,`punchOutType`,`punchInLocation`,`punchOutLocation`,`remark`,`createTime`,`updateTime`,`isDeleted`) values (2076926030575534082,15,2075829151662010370,'2026-07-14','2026-07-14 15:05:28','2026-07-14 17:20:30',0,0,0,'','',NULL,'2026-07-14 15:05:28','2026-07-14 15:05:28',0,1),(2076943569447723009,15,NULL,'2026-07-11',NULL,NULL,4,NULL,NULL,NULL,NULL,NULL,'2026-07-14 16:15:10','2026-07-14 16:15:10',0,1),(2076943570227863554,15,NULL,'2026-07-12',NULL,NULL,4,NULL,NULL,NULL,NULL,NULL,'2026-07-14 16:15:10','2026-07-14 16:15:10',0,1),(2077292019532095489,15,2075829151662010370,'2026-07-15','2026-07-15 15:19:46','2026-07-15 15:19:55',1,0,0,'','',NULL,'2026-07-15 15:19:46','2026-07-15 15:19:46',0);

/*Table structure for table `department` */

DROP TABLE IF EXISTS `department`;

CREATE TABLE `department` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `deptName` varchar(64) NOT NULL COMMENT '部门名称',
  `deptCode` varchar(16) NOT NULL COMMENT '部门编码（2位，用于工号生成）',
  `parentId` bigint(20) DEFAULT NULL COMMENT '上级部门ID，NULL表示根部门',
  `managerId` bigint(20) DEFAULT NULL COMMENT '部门负责人ID（关联员工表）',
  `sortOrder` int(11) NOT NULL DEFAULT '0' COMMENT '排序序号（越小越靠前）',
  `description` varchar(256) DEFAULT NULL COMMENT '部门描述',
  `createdTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatedTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDeleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=否 1=是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`deptCode`),
  KEY `idx_parent_id` (`parentId`),
  KEY `idx_manager_id` (`managerId`)
) ENGINE=InnoDB AUTO_INCREMENT=2077235806949306370 DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

/*Data for the table `department` */

insert  into `department`(`id`,`deptName`,`deptCode`,`parentId`,`managerId`,`sortOrder`,`description`,`createdTime`,`updatedTime`,`isDeleted`) values (1,'总公司','00',NULL,1,0,'公司根部门','2026-07-11 11:43:13','2026-07-13 15:31:18',0,1),(2,'技术研发中心','01',1,1,1,'负责公司所有技术研发工作','2026-07-11 11:50:14','2026-07-13 15:31:17',0,1),(3,'市场营销中心','02',1,1,2,'负责市场推广与品牌建设','2026-07-11 11:50:14','2026-07-13 15:31:18',0,1),(4,'人力资源中心','03',1,1,3,'负责招聘、培训、薪酬绩效','2026-07-11 11:50:14','2026-07-13 15:31:18',0,1),(5,'财务管理中心','04',1,1,4,'负责公司财务核算与资金管理','2026-07-11 11:50:14','2026-07-13 15:31:18',0,1),(6,'运营管理中心','05',1,1,5,'负责公司日常运营与流程管理','2026-07-11 11:50:14','2026-07-13 15:31:18',0,1),(7,'后端开发部','11',2,1,1,'负责后端服务开发与架构设计','2026-07-11 11:50:14','2026-07-13 15:31:18',0,1),(8,'前端开发部','12',2,1,2,'负责Web/移动端前端开发','2026-07-11 11:50:14','2026-07-13 15:31:18',0,1),(9,'测试质量部','13',2,1,3,'负责质量保障与自动化测试','2026-07-11 11:50:14','2026-07-13 15:31:18',0,1),(10,'运维安全部','14',2,1,4,'负责系统运维与信息安全','2026-07-11 11:50:14','2026-07-13 15:31:17',0,1),(11,'市场推广部','21',3,15,1,'负责线上线下市场推广活动','2026-07-11 11:50:14','2026-07-13 15:31:18',0,1),(12,'品牌公关部','22',3,1,2,'负责品牌建设与公关关系','2026-07-11 11:50:14','2026-07-13 15:31:17',0,1),(13,'销售管理部','23',3,1,3,'负责销售团队管理与业绩达成','2026-07-11 11:50:14','2026-07-13 15:31:17',0,1),(14,'招聘培训部','31',4,1,1,'负责人才招聘与培训发展','2026-07-11 11:50:14','2026-07-13 15:31:17',0,1),(15,'薪酬绩效部','32',4,1,2,'负责薪酬福利与绩效考核','2026-07-11 11:50:14','2026-07-13 15:31:17',0,1),(16,'会计核算部','41',5,1,1,'负责日常账务核算','2026-07-11 11:50:14','2026-07-13 15:31:18',0,1),(17,'资金管理部','42',5,1,2,'负责资金调度与风险控制','2026-07-11 11:50:14','2026-07-13 15:31:18',0,1),(2076573550385262594,'12b','20',NULL,1,0,'1','2026-07-13 15:44:49','2026-07-15 10:56:24',1),(2076573607402631169,'12bu','30',2,15,10,'负责12','2026-07-13 15:45:03','2026-07-15 11:53:37',0,1),(2076950674464034818,'12b','99',2,1,10,'负责12','2026-07-14 16:43:23','2026-07-15 11:53:37',1),(2077215610507337730,'12','97',1,1,0,'123','2026-07-15 10:16:09','2026-07-15 10:29:37',1),(2077225077760303105,'12','98',1,4,0,NULL,'2026-07-15 10:53:46','2026-07-15 10:53:56',1),(2077235806949306369,'12','96',2076573607402631169,15,0,NULL,'2026-07-15 11:36:24','2026-07-15 11:36:24',0);

/*Table structure for table `department_merge_log` */

DROP TABLE IF EXISTS `department_merge_log`;

CREATE TABLE `department_merge_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sourceDeptId` bigint(20) unsigned NOT NULL COMMENT '源部门ID（被合并）',
  `sourceDeptName` varchar(64) NOT NULL COMMENT '源部门名称（快照）',
  `targetDeptId` bigint(20) unsigned NOT NULL COMMENT '目标部门ID（保留）',
  `targetDeptName` varchar(64) NOT NULL COMMENT '目标部门名称（快照）',
  `transferredEmployees` int(11) NOT NULL DEFAULT '0' COMMENT '转移员工数',
  `operatorId` bigint(20) unsigned NOT NULL COMMENT '操作人ID',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_source_dept_id` (`targetDeptId`),
  KEY `idx_target_dept_id` (`targetDeptName`),
  KEY `idx_create_time` (`createTime`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='部门合并日志表';

/*Data for the table `department_merge_log` */

insert  into `department_merge_log`(`id`,`sourceDeptId`,`sourceDeptName`,`targetDeptId`,`targetDeptName`,`transferredEmployees`,`operatorId`,`createTime`) values (1,18,'127部',19,'128部',2,1,'2026-07-11 14:14:58'),(2,2076554934705135618,'123',2076557187461619714,'123',0,1,'2026-07-13 14:40:14'),(3,2076950674464034818,'12b',2076573607402631169,'12bu',1,1,'2026-07-14 16:46:57'),(4,2077225077760303105,'12',2076573550385262594,'12b',0,1,'2026-07-15 10:53:56');

/*Table structure for table `emp_mutation_log` */

DROP TABLE IF EXISTS `emp_mutation_log`;

CREATE TABLE `emp_mutation_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `businessType` varchar(32) NOT NULL COMMENT '异动类型枚举:ONBOARDING入职/PROBATION转正/TRANSFER调岗/RESIGN离职',
  `businessId` bigint(20) NOT NULL COMMENT '对应异动单据主键ID（4张异动表id）',
  `businessNo` varchar(32) NOT NULL COMMENT '异动单据编号',
  `employeeId` bigint(20) DEFAULT NULL COMMENT '关联员工ID（入职单据审批通过后回填）',
  `employeeName` varchar(128) DEFAULT NULL COMMENT '员工姓名快照',
  `deptId` bigint(20) NOT NULL COMMENT '所属部门ID',
  `deptName` varchar(64) NOT NULL COMMENT '部门名称快照',
  `effectDate` date NOT NULL COMMENT '异动生效日期',
  `approvalStatus` varchar(16) NOT NULL COMMENT '审批状态，复用approval_record.status',
  `operatorId` bigint(20) NOT NULL COMMENT '操作HR员工ID',
  `operatorName` varchar(64) NOT NULL COMMENT '操作人姓名快照',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '单据创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_employee` (`employeeId`),
  KEY `idx_dept` (`deptId`),
  KEY `idx_business_type` (`businessType`),
  KEY `idx_effect_date` (`effectDate`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='人事异动统一汇总日志（员工我的人事异动页面专用）';

/*Data for the table `emp_mutation_log` */

insert  into `emp_mutation_log`(`id`,`businessType`,`businessId`,`businessNo`,`employeeId`,`employeeName`,`deptId`,`deptName`,`effectDate`,`approvalStatus`,`operatorId`,`operatorName`,`createTime`) values (1,'ONBOARDING',1,'ON202607150001',20,'张三',10,'运维安全部','2026-07-15','APPROVED',2076964719435886594,'','2026-07-15 23:32:28');

/*Table structure for table `emp_onboarding` */

DROP TABLE IF EXISTS `emp_onboarding`;

CREATE TABLE `emp_onboarding` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `businessNo` varchar(32) DEFAULT NULL,
  `flowId` bigint(20) DEFAULT NULL,
  `recordId` bigint(20) DEFAULT NULL,
  `deptId` bigint(20) NOT NULL,
  `positionId` bigint(20) NOT NULL,
  `hireDate` date NOT NULL,
  `probationMonth` int(11) NOT NULL DEFAULT '3',
  `employmentType` varchar(16) NOT NULL,
  `contractType` tinyint(4) DEFAULT NULL,
  `contractExpireDate` date DEFAULT NULL,
  `baseSalary` decimal(12,2) DEFAULT NULL,
  `socialInsuranceBase` decimal(12,2) DEFAULT NULL,
  `housingFundBase` decimal(12,2) DEFAULT NULL,
  `bankAccount` varchar(64) DEFAULT NULL,
  `bankName` varchar(128) DEFAULT NULL,
  `candidateName` varchar(64) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `idCard` varchar(256) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `emergencyContactName` varchar(64) DEFAULT NULL,
  `emergencyContactPhone` varchar(20) DEFAULT NULL,
  `employeeId` bigint(20) DEFAULT NULL,
  `operatorId` bigint(20) NOT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDeleted` tinyint(4) NOT NULL DEFAULT '0',
  `approverId` bigint(20) DEFAULT NULL COMMENT '审批人ID（部门负责人，关联employee.id）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;

/*Data for the table `emp_onboarding` */

insert  into `emp_onboarding`(`id`,`businessNo`,`flowId`,`recordId`,`deptId`,`positionId`,`hireDate`,`probationMonth`,`employmentType`,`contractType`,`contractExpireDate`,`baseSalary`,`socialInsuranceBase`,`housingFundBase`,`bankAccount`,`bankName`,`candidateName`,`phone`,`idCard`,`email`,`emergencyContactName`,`emergencyContactPhone`,`employeeId`,`operatorId`,`remark`,`createTime`,`updateTime`,`isDeleted`,`approverId`) values (1,'ON202607150001',NULL,9,10,4,'2026-07-15',3,'FULL_TIME',1,NULL,'8990.00',NULL,NULL,NULL,NULL,'张三','13525968115','09876','2215895433@qq.com',NULL,NULL,20,2076964719435886594,NULL,'2026-07-15 12:11:03','2026-07-15 21:50:19',0,1),(2,'ON202607150002',NULL,10,8,3,'2026-07-15',3,'FULL_TIME',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'林城','13525968115','455677200099887766','2215895433@qq.com',NULL,NULL,NULL,2076964719435886594,NULL,'2026-07-15 21:19:56','2026-07-15 21:19:56',0,1);

/*Table structure for table `emp_probation` */

DROP TABLE IF EXISTS `emp_probation`;

CREATE TABLE `emp_probation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `businessNo` varchar(32) NOT NULL COMMENT '转正单号，ZB+年月日+流水号',
  `flowId` bigint(20) NOT NULL COMMENT '审批流ID(approval_flow.id)',
  `recordId` bigint(20) DEFAULT NULL COMMENT '审批实例ID(approval_record.id)',
  `employeeId` bigint(20) NOT NULL COMMENT '待转正员工ID(employee.id)',
  `originHireDate` date NOT NULL COMMENT '原始入职日期',
  `probationEndDate` date NOT NULL COMMENT '试用期到期日期',
  `confirmDate` date NOT NULL COMMENT '转正生效日期',
  `probationScore` decimal(4,1) DEFAULT NULL COMMENT '试用期考核分数',
  `probationComment` text COMMENT '试用期工作评价',
  `confirmBaseSalary` decimal(12,2) NOT NULL COMMENT '转正后基本工资',
  `probationSalaryRatio` decimal(5,4) DEFAULT '1.0000' COMMENT '试用期薪资比例',
  `operatorId` bigint(20) NOT NULL COMMENT '操作HR员工ID',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDeleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除:0=否,1=是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_businessNo` (`businessNo`),
  KEY `idx_emp` (`employeeId`),
  KEY `idx_record` (`recordId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工转正申请表';

/*Data for the table `emp_probation` */

/*Table structure for table `emp_resign` */

DROP TABLE IF EXISTS `emp_resign`;

CREATE TABLE `emp_resign` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `businessNo` varchar(32) NOT NULL COMMENT '离职单号，LZ+年月日+流水号',
  `flowId` bigint(20) NOT NULL COMMENT '审批流ID(approval_flow.id)',
  `recordId` bigint(20) DEFAULT NULL COMMENT '审批实例ID(approval_record.id)',
  `employeeId` bigint(20) NOT NULL COMMENT '离职员工ID(employee.id)',
  `applyDate` date NOT NULL COMMENT '离职申请提交日期',
  `lastWorkDate` date NOT NULL COMMENT '最后工作日',
  `resignType` tinyint(4) NOT NULL COMMENT '离职类型:1主动离职/2公司辞退/3合同到期/4自离',
  `resignReason` varchar(512) NOT NULL COMMENT '离职详细原因',
  `handoverPersonId` bigint(20) DEFAULT NULL COMMENT '工作交接人员工ID',
  `handoverStatus` tinyint(4) DEFAULT '0' COMMENT '交接状态:0未交接/1已完成交接',
  `settleSalary` decimal(14,2) DEFAULT '0.00' COMMENT '离职结算应发薪资',
  `settleDate` date DEFAULT NULL COMMENT '薪资结算日期',
  `operatorId` bigint(20) NOT NULL COMMENT '操作HR员工ID',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDeleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除:0=否,1=是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_businessNo` (`businessNo`),
  KEY `idx_emp` (`employeeId`),
  KEY `idx_handover` (`handoverPersonId`),
  KEY `idx_record` (`recordId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工离职申请表';

/*Data for the table `emp_resign` */

/*Table structure for table `emp_salary_profile` */

DROP TABLE IF EXISTS `emp_salary_profile`;

CREATE TABLE `emp_salary_profile` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employeeId` bigint(20) NOT NULL COMMENT '员工ID',
  `accountSetId` bigint(20) DEFAULT NULL COMMENT '适用薪资账套ID',
  `baseSalary` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '基本工资',
  `allowanceBase` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '岗位津贴基数',
  `performanceBase` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '绩效奖金基数',
  `socialInsuranceBase` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '社保缴纳基数',
  `housingFundBase` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '公积金缴纳基数',
  `probationSalaryRatio` decimal(4,2) NOT NULL DEFAULT '1.00' COMMENT '试用期薪资比例 (0.80~1.00)',
  `bankAccount` varchar(256) DEFAULT NULL COMMENT '银行账号（加密存储）',
  `bankName` varchar(128) DEFAULT NULL COMMENT '开户行名称',
  `effectiveDate` date NOT NULL COMMENT '生效日期',
  `createdTIme` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatedTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDeleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=否 1=是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_id` (`employeeId`),
  KEY `idx_account_set_id` (`accountSetId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工薪资档案表';

/*Data for the table `emp_salary_profile` */

/*Table structure for table `emp_transfer` */

DROP TABLE IF EXISTS `emp_transfer`;

CREATE TABLE `emp_transfer` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `businessNo` varchar(32) NOT NULL COMMENT '调岗单号，DG+年月日+流水号',
  `flowId` bigint(20) NOT NULL COMMENT '审批流ID(approval_flow.id)',
  `recordId` bigint(20) DEFAULT NULL COMMENT '审批实例ID(approval_record.id)',
  `employeeId` bigint(20) NOT NULL COMMENT '调岗员工ID(employee.id)',
  `sourceDeptId` bigint(20) NOT NULL COMMENT '原部门ID',
  `sourcePositionId` bigint(20) NOT NULL COMMENT '原职位ID',
  `targetDeptId` bigint(20) NOT NULL COMMENT '目标部门ID',
  `targetPositionId` bigint(20) NOT NULL COMMENT '目标职位ID',
  `transferDate` date NOT NULL COMMENT '调岗生效日期',
  `transferReason` varchar(512) NOT NULL COMMENT '调岗详细原因',
  `newBaseSalary` decimal(12,2) DEFAULT NULL COMMENT '调岗后新基本工资（薪资变动时填写）',
  `operatorId` bigint(20) NOT NULL COMMENT '操作HR员工ID',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDeleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除:0=否,1=是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_businessNo` (`businessNo`),
  KEY `idx_emp` (`employeeId`),
  KEY `idx_source_dept` (`sourceDeptId`),
  KEY `idx_target_dept` (`targetDeptId`),
  KEY `idx_record` (`recordId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工调岗申请表';

/*Data for the table `emp_transfer` */

/*Table structure for table `employee` */

DROP TABLE IF EXISTS `employee`;

CREATE TABLE `employee` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `employeeName` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '员工名称（真实姓名）',
  `userId` bigint(20) DEFAULT NULL COMMENT '关联用户ID',
  `employeeNo` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工号, 格式: 年份(4)+部门编码(2)+序号(3)',
  `account` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '系统账号（=手机号）',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '在职状态：1=试用期 2=正式 3=待离职 4=已离职',
  `gender` tinyint(4) NOT NULL DEFAULT '0' COMMENT '性别: 0=女, 1=男',
  `hireDate` datetime DEFAULT NULL COMMENT '入职日期',
  `phone` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人电话',
  `departmentId` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `positionId` bigint(20) DEFAULT NULL COMMENT '职位ID',
  `employmentType` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '录用类型: FULL_TIME=全职, PART_TIME=兼职, INTERN=实习',
  `email` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDeleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `salaryProfileId` bigint(20) DEFAULT NULL COMMENT '薪资ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工';

/*Data for the table `employee` */

insert  into `employee`(`id`,`employeeName`,`userId`,`employeeNo`,`account`,`status`,`gender`,`hireDate`,`phone`,`departmentId`,`positionId`,`employmentType`,`email`,`createTime`,`updateTime`,`isDeleted`,`salaryProfileId`) values (1,'张伟',2077317279501983746,'2026000001',NULL,1,1,'2026-01-15 09:00:00','13900139000',1,1,'FULL_TIME','zhangwei@company.com','2026-07-11 11:46:15','2026-07-15 21:50:17',0,NULL),(2,'李娜',1002,'2026000002',NULL,1,0,'2026-02-01 09:00:00',NULL,2,2,'FULL_TIME','lina@company.com','2026-07-11 11:46:15','2026-07-13 14:18:22',0,NULL),(3,'王磊',1003,'2026000003',NULL,1,1,'2026-03-10 09:00:00',NULL,1,3,'FULL_TIME','wanglei@company.com','2026-07-11 11:46:15','2026-07-11 11:46:15',0,NULL),(4,'刘洋',1004,'2026000004',NULL,1,1,'2026-01-20 09:00:00',NULL,1,3,'FULL_TIME','liuyang@company.com','2026-07-11 11:46:15','2026-07-11 11:46:15',0,NULL),(5,'陈静',1005,'2026000005',NULL,1,0,'2026-03-01 09:00:00',NULL,4,4,'FULL_TIME','chenjing@company.com','2026-07-11 11:46:15','2026-07-13 14:18:24',0,NULL),(6,'赵雪',1006,'2026000006',NULL,1,0,'2026-02-15 09:00:00',NULL,1,4,'FULL_TIME','zhaoxue@company.com','2026-07-11 11:46:15','2026-07-11 11:46:15',0,NULL),(7,'孙鹏',1007,'2026000007',NULL,1,1,'2026-01-10 09:00:00',NULL,1,4,'FULL_TIME','sunpeng@company.com','2026-07-11 11:46:15','2026-07-11 11:46:15',0,NULL),(8,'周婷',1008,'2026000008',NULL,1,0,'2026-05-01 09:00:00',NULL,1,5,'FULL_TIME','zhouting@company.com','2026-07-11 11:46:15','2026-07-11 11:46:15',0,NULL),(9,'吴浩',1009,'2026000009',NULL,1,1,'2026-04-15 09:00:00',NULL,1,5,'FULL_TIME','wuhao@company.com','2026-07-11 11:46:15','2026-07-11 11:46:15',0,NULL),(10,'林峰',1010,'2026000010',NULL,2,1,'2026-05-20 09:00:00',NULL,17,4,'FULL_TIME','linfeng@company.com','2026-07-11 11:46:15','2026-07-13 14:18:14',0,NULL),(11,'黄丽',1011,'2026000011',NULL,2,0,'2026-06-01 09:00:00',NULL,16,5,'FULL_TIME','huangli@company.com','2026-07-11 11:46:15','2026-07-13 14:18:16',0,NULL),(12,'郑欣',1012,'2025000012',NULL,0,0,'2025-06-01 09:00:00',NULL,15,5,'FULL_TIME','zhengxin@company.com','2026-07-11 11:46:15','2026-07-13 14:18:17',0,NULL),(13,'冯雪',1013,'2026000013',NULL,1,0,'2026-04-10 09:00:00',NULL,14,4,'PART_TIME','fengxue@company.com','2026-07-11 11:46:15','2026-07-13 14:18:19',0,NULL),(14,'褚涛',1014,'2026000014',NULL,2,1,'2026-06-15 09:00:00',NULL,13,3,'INTERN','chutao@company.com','2026-07-11 11:46:15','2026-07-13 14:18:21',0,NULL),(15,'limou',2075829151662010370,'2026000015',NULL,1,1,'2026-07-14 10:59:26','13800138000',2076573607402631169,32,'INTERN','chutao@company.com','2026-07-11 14:34:21','2026-07-14 10:59:30',0,NULL),(16,'huitiayang',NULL,'2026201001',NULL,1,0,'2026-07-13 08:00:00','18888888888',2076573607402631169,2076574980433211394,'INTERN','','2026-07-13 16:23:55','2026-07-14 16:46:57',0,NULL),(18,'huitaiyang HUITAIYANG',2077275185780559873,'202600001','18444444444',1,1,NULL,'18444444444',1,6,'PART_TIME','1839429592@qq.com','2026-07-15 14:12:53','2026-07-15 14:12:53',0,NULL),(19,'HHRR',2076964719435886594,'2026000019','HHRR',1,1,NULL,'13800138000',1,2,'FULL_TIME',NULL,'2026-07-15 22:43:15','2026-07-15 22:43:15',0,NULL),(20,'张三',2077416009366466561,'202614001','13525968115',1,0,'2026-07-15 00:00:00','13525968115',10,4,'FULL_TIME','2215895433@qq.com','2026-07-15 23:32:29','2026-07-15 23:32:29',0,NULL);

/*Table structure for table `employee_change_log` */

DROP TABLE IF EXISTS `employee_change_log`;

CREATE TABLE `employee_change_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employeeId` bigint(20) unsigned NOT NULL COMMENT '员工ID',
  `fieldName` varchar(64) NOT NULL COMMENT '变更字段名',
  `oldValue` varchar(512) DEFAULT NULL COMMENT '变更前值',
  `newValue` varchar(512) DEFAULT NULL COMMENT '变更后值',
  `changeType` varchar(32) NOT NULL COMMENT 'DIRECT_EDIT/FLOW_CHANGE/SYSTEM',
  `operatorId` bigint(20) unsigned DEFAULT NULL COMMENT '操作人ID',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employeeId`),
  KEY `idx_create_time` (`createTime`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='员工档案变更日志表';

/*Data for the table `employee_change_log` */

insert  into `employee_change_log`(`id`,`employeeId`,`fieldName`,`oldValue`,`newValue`,`changeType`,`operatorId`,`remark`,`createTime`) values (1,15,'currentAddress','青岛市市南区香港中路','郑州市金水区','个人档案自主修改',2075829151662010370,'员工自行更新现居住地址','2026-07-14 11:51:34'),(2,15,'emergencyContactName','青岛市市南区香港中路','李依依','个人档案自主修改',2075829151662010370,'员工自行更新紧急联系人姓名','2026-07-14 11:51:34'),(3,15,'emergencyContactPhone','13800001115','13526789988','个人档案自主修改',2075829151662010370,'员工自行更新紧急联系人电话','2026-07-14 11:51:34');

/*Table structure for table `employee_detail` */

DROP TABLE IF EXISTS `employee_detail`;

CREATE TABLE `employee_detail` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employeeId` bigint(20) unsigned NOT NULL COMMENT '员工ID，关联employee.id',
  `idCard` varchar(256) DEFAULT NULL COMMENT '身份证号（加密存储）',
  `currentAddress` varchar(512) DEFAULT NULL COMMENT '现居住地址',
  `emergencyContactName` varchar(128) DEFAULT NULL COMMENT '紧急联系人姓名',
  `emergencyContactPhone` varchar(32) DEFAULT NULL COMMENT '紧急联系人电话',
  `account` varchar(32) DEFAULT NULL COMMENT '系统账号（=手机号）',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `registeredAddress` varchar(512) DEFAULT NULL COMMENT '户籍地址',
  `jobLevel` varchar(8) DEFAULT NULL COMMENT '职级，如P5、M2',
  `directReportId` bigint(20) unsigned DEFAULT NULL COMMENT '直接汇报人ID，关联employee.id',
  `workLocation` varchar(128) DEFAULT NULL COMMENT '工作地点',
  `contractType` tinyint(4) DEFAULT NULL COMMENT '合同类型：1=固定期限 2=无固定期限 3=劳务合同',
  `contractExpireDate` date DEFAULT NULL COMMENT '合同到期日',
  `probationRatio` decimal(5,4) DEFAULT NULL COMMENT '试用期待遇比例 0.8000~1.0000',
  `baseSalary` decimal(12,2) DEFAULT NULL COMMENT '基本工资',
  `bankAccount` varchar(64) DEFAULT NULL COMMENT '银行账号（加密存储）',
  `bankName` varchar(128) DEFAULT NULL COMMENT '开户行',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_id` (`employeeId`),
  KEY `idx_direct_report_id` (`directReportId`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COMMENT='员工详情表（补充信息）';

/*Data for the table `employee_detail` */

insert  into `employee_detail`(`id`,`employeeId`,`idCard`,`currentAddress`,`emergencyContactName`,`emergencyContactPhone`,`account`,`birthday`,`registeredAddress`,`jobLevel`,`directReportId`,`workLocation`,`contractType`,`contractExpireDate`,`probationRatio`,`baseSalary`,`bankAccount`,`bankName`,`createTime`,`updateTime`) values (1,1,'110101199003071234','北京市海淀区中关村大街1号','李芳','13800001111',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(2,2,'110102199205151234','北京市朝阳区望京SOHO','王强','13800002222',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(3,3,'110103199408201234','上海市浦东新区张江高科技园区','赵敏','13800003333',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(4,4,'110104199605251234','上海市静安区南京西路1788号','孙丽','13800004444',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(5,5,'110105199810301234','广州市天河区珠江新城','周明','13800005555',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(6,6,'110106199912051234','深圳市南山区科技园','吴刚','13800006666',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(7,7,'110107200101151234','成都市高新区天府大道中段','郑华','13800007777',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(8,8,'110108200306201234','杭州市西湖区文三路','钱军','13800008888',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(9,9,'110109200408101234','武汉市洪山区光谷','冯艳','13800009999',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(10,10,'110110200010051234','西安市雁塔区高新路','卫红','13800001112',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(11,11,'110111199501181234','长沙市岳麓区梅溪湖','蒋文','13800001113',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(12,12,'110112199812251234','南京市建邺区奥体大街','褚明','13800001110',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(13,13,'110113199709151234','郑州市郑东新区CBD','韩冰','13800001114',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(14,14,'110114200208201234','青岛市市南区香港中路','韩冰','13800001115',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-13 15:08:52'),(15,15,'110113199709151230','郑州市金水区','李依依','13526789988',NULL,NULL,NULL,NULL,15,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 15:08:52','2026-07-14 11:51:33'),(16,16,'','','','','18888888888',NULL,'',NULL,1,'',0,NULL,'0.0000','0.00','','123','2026-07-13 16:23:55','2026-07-13 16:23:55'),(17,17,'','','','','18888888888',NULL,'',NULL,1,'',0,NULL,'0.0000','0.00','','123','2026-07-13 16:24:06','2026-07-13 16:24:06'),(18,18,'111111111111111111',NULL,NULL,NULL,NULL,NULL,'111111111',NULL,NULL,NULL,1,'2026-08-06','0.8000','1233333.00',NULL,NULL,'2026-07-15 14:12:53','2026-07-15 14:12:53'),(19,20,'09876',NULL,NULL,NULL,'13525968115',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'8990.00',NULL,NULL,'2026-07-15 23:32:29','2026-07-15 23:32:29');

/*Table structure for table `leave_request` */

DROP TABLE IF EXISTS `leave_request`;

CREATE TABLE `leave_request` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employeeId` bigint(20) NOT NULL COMMENT '员工ID',
  `userId` bigint(20) NOT NULL COMMENT '用户ID',
  `leaveType` tinyint(4) NOT NULL COMMENT '请假类型：0=事假 1=病假 2=年假 3=婚假 4=产假 5=丧假 6=调休',
  `startDate` date NOT NULL COMMENT '开始日期',
  `endDate` date NOT NULL COMMENT '结束日期',
  `totalDays` decimal(4,1) NOT NULL COMMENT '请假总天数',
  `reason` varchar(512) NOT NULL COMMENT '请假原因',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态：0=待审批 1=已通过 2=已拒绝 3=已撤销',
  `approverId` bigint(20) DEFAULT NULL COMMENT '审批人ID',
  `approveTime` datetime DEFAULT NULL COMMENT '审批时间',
  `approveComment` varchar(512) DEFAULT NULL COMMENT '审批意见',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDeleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=否 1=是',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employeeId`),
  KEY `idx_user_id` (`userId`),
  KEY `idx_status` (`status`),
  KEY `idx_start_date` (`startDate`)
) ENGINE=InnoDB AUTO_INCREMENT=2076943565853204482 DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';

/*Data for the table `leave_request` */

insert  into `leave_request`(`id`,`employeeId`,`userId`,`leaveType`,`startDate`,`endDate`,`totalDays`,`reason`,`status`,`approverId`,`approveTime`,`approveComment`,`createTime`,`updateTime`,`isDeleted`) values (1,1,1,0,'2026-07-01','2026-07-03','3.0','家里有急事需要处理',0,NULL,NULL,NULL,'2026-06-28 09:00:00','2026-06-28 09:00:00',0,1),(2,1,1,1,'2026-06-20','2026-06-21','2.0','感冒发烧，需要休息',1,10,'2026-06-22 10:00:00','同意，注意休息','2026-06-19 14:30:00','2026-06-22 10:00:00',0,1),(3,2,2,2,'2026-07-10','2026-07-12','3.0','年假出去旅游',0,NULL,NULL,NULL,'2026-07-01 08:15:00','2026-07-01 08:15:00',0,1),(4,2,2,3,'2026-05-01','2026-05-03','3.0','本人婚礼',2,10,'2026-05-02 09:00:00','婚假额度已用完，建议调休','2026-04-25 16:20:00','2026-05-02 09:00:00',0,1),(5,3,3,4,'2026-08-01','2026-08-30','30.0','产假申请',1,10,'2026-07-15 11:00:00','批准，祝母子平安','2026-07-10 10:00:00','2026-07-15 11:00:00',0,1),(6,3,3,0,'2026-07-05','2026-07-05','1.0','搬家需要处理',3,NULL,NULL,NULL,'2026-07-03 17:00:00','2026-07-04 08:30:00',0,1),(7,4,4,6,'2026-07-15','2026-07-16','2.0','周末加班调休',0,NULL,NULL,NULL,'2026-07-12 13:45:00','2026-07-12 13:45:00',0,1),(8,1,1,5,'2026-06-10','2026-06-12','3.0','亲属过世，回家奔丧',1,10,'2026-06-11 08:00:00','节哀顺变，准假','2026-06-09 20:00:00','2026-06-11 08:00:00',0,1),(9,5,5,2,'2026-07-20','2026-07-22','3.0','带家人出游',1,10,'2026-07-18 14:00:00','同意，旅途愉快','2026-07-17 09:30:00','2026-07-18 14:00:00',0,1),(10,5,5,0,'2026-07-28','2026-07-29','2.0','孩子生病需要照顾',0,NULL,NULL,NULL,'2026-07-27 11:10:00','2026-07-27 11:10:00',0,1),(2076584278517833730,15,2075829151662010370,4,'2026-07-01','2026-07-29','29.0','www',3,NULL,NULL,NULL,'2026-07-13 16:27:28','2026-07-13 16:27:28',0,1),(2076588532695506946,15,2075829151662010370,2,'2026-07-31','2026-08-01','2.0','www',3,NULL,NULL,NULL,'2026-07-13 16:44:22','2026-07-13 16:44:22',0,1),(2076594713770008578,15,2075829151662010370,1,'2026-07-13','2026-07-14','2.0','ww',3,15,NULL,NULL,'2026-07-13 17:08:56','2026-07-13 17:08:56',0,1),(2076926360054890498,15,2075829151662010370,1,'2026-07-15','2026-07-15','1.0','ww',3,15,NULL,NULL,'2026-07-14 15:06:47','2026-07-14 15:06:47',0,1),(2076928900096032769,15,2075829151662010370,0,'2026-07-14','2026-07-14','1.0','ww',0,15,NULL,NULL,'2026-07-14 15:16:52','2026-07-14 15:16:52',0,1),(2076943565853204481,15,2075829151662010370,1,'2026-07-11','2026-07-12','2.0','ww',0,15,NULL,NULL,'2026-07-14 16:15:09','2026-07-14 16:15:09',0);

/*Table structure for table `login_log` */

DROP TABLE IF EXISTS `login_log`;

CREATE TABLE `login_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `userId` bigint(20) NOT NULL COMMENT '用户ID',
  `loginTime` datetime NOT NULL COMMENT '登录时间',
  `ip` varchar(45) NOT NULL COMMENT '登录IP',
  `device` varchar(256) DEFAULT NULL COMMENT '设备信息（User-Agent）',
  `loginType` tinyint(4) NOT NULL COMMENT '登录方式：1=密码登录 2=短信验证码登录',
  `isSuccess` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否成功：0=失败 1=成功',
  `failReason` varchar(128) DEFAULT NULL COMMENT '失败原因',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`userId`),
  KEY `idx_login_time` (`loginTime`)
) ENGINE=InnoDB AUTO_INCREMENT=227 DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

/*Data for the table `login_log` */

insert  into `login_log`(`id`,`userId`,`loginTime`,`ip`,`device`,`loginType`,`isSuccess`,`failReason`) values (1,2075829151662010370,'2026-04-01 08:55:00','192.168.1.100','Mozilla/5.0 Chrome/120 Windows',1,1,NULL),(2,2075829151662010370,'2026-04-03 09:02:00','192.168.1.100','Mozilla/5.0 Chrome/120 Windows',1,1,NULL),(3,2075829151662010370,'2026-04-07 08:48:00','192.168.1.100','Mozilla/5.0 Chrome/120 Windows',1,1,NULL),(4,2075829151662010370,'2026-04-10 09:15:00','10.0.0.55','Mozilla/5.0 Safari/17 iPhone',1,1,NULL),(5,2075829151662010370,'2026-04-12 14:20:00','192.168.1.100','Mozilla/5.0 Chrome/120 Windows',1,0,'密码错误'),(6,2075829151662010370,'2026-04-12 14:21:00','192.168.1.100','Mozilla/5.0 Chrome/120 Windows',1,1,NULL),(7,2075829151662010370,'2026-04-15 08:50:00','192.168.1.100','Mozilla/5.0 Chrome/120 Windows',1,1,NULL),(8,2075829151662010370,'2026-04-20 09:05:00','113.87.23.45','Mozilla/5.0 Edge/120 Mac',1,1,NULL),(9,2075829151662010370,'2026-04-25 08:58:00','192.168.1.100','Mozilla/5.0 Chrome/121 Windows',1,1,NULL),(10,2075829151662010370,'2026-04-28 08:45:00','192.168.1.100','Mozilla/5.0 Chrome/121 Windows',1,1,NULL),(11,2075829151662010370,'2026-05-04 09:10:00','192.168.1.100','Mozilla/5.0 Chrome/121 Windows',1,1,NULL),(12,2075829151662010370,'2026-05-08 08:52:00','192.168.1.100','Mozilla/5.0 Chrome/121 Windows',1,1,NULL),(13,2075829151662010370,'2026-05-12 09:00:00','192.168.1.100','Mozilla/5.0 Chrome/121 Windows',1,1,NULL),(14,2075829151662010370,'2026-05-15 08:47:00','10.0.0.55','Mozilla/5.0 Safari/17 iPhone',2,0,'验证码已过期'),(15,2075829151662010370,'2026-05-15 08:48:00','10.0.0.55','Mozilla/5.0 Safari/17 iPhone',2,1,NULL),(16,2075829151662010370,'2026-05-20 08:55:00','192.168.1.100','Mozilla/5.0 Chrome/121 Windows',1,1,NULL),(17,2075829151662010370,'2026-05-25 14:30:00','114.25.66.78','Mozilla/5.0 Chrome/121 Android',1,0,'密码错误'),(18,2075829151662010370,'2026-05-25 14:32:00','114.25.66.78','Mozilla/5.0 Chrome/121 Android',1,1,NULL),(19,2075829151662010370,'2026-05-28 08:50:00','192.168.1.100','Mozilla/5.0 Chrome/121 Windows',1,1,NULL),(20,2075829151662010370,'2026-06-01 09:03:00','192.168.1.100','Mozilla/5.0 Chrome/122 Windows',1,1,NULL),(21,2075829151662010370,'2026-06-05 08:56:00','192.168.1.100','Mozilla/5.0 Chrome/122 Windows',1,1,NULL),(22,2075829151662010370,'2026-06-10 09:08:00','192.168.1.100','Mozilla/5.0 Chrome/122 Windows',1,1,NULL),(23,2075829151662010370,'2026-06-15 08:50:00','10.0.0.55','Mozilla/5.0 Safari/17 iPhone',1,1,NULL),(24,2075829151662010370,'2026-06-20 08:53:00','192.168.1.100','Mozilla/5.0 Chrome/122 Windows',1,0,'密码错误'),(25,2075829151662010370,'2026-06-20 08:54:00','192.168.1.100','Mozilla/5.0 Chrome/122 Windows',1,1,NULL),(26,2075829151662010370,'2026-06-25 08:58:00','192.168.1.100','Mozilla/5.0 Chrome/122 Windows',1,1,NULL),(27,2075829151662010370,'2026-07-01 09:01:00','192.168.1.100','Mozilla/5.0 Chrome/123 Windows',1,1,NULL),(28,2075829151662010370,'2026-07-05 08:49:00','192.168.1.100','Mozilla/5.0 Chrome/123 Windows',1,1,NULL),(29,2075829151662010370,'2026-07-08 08:55:00','10.0.0.55','Mozilla/5.0 Safari/17 iPhone',1,1,NULL),(30,2075829151662010370,'2026-07-10 09:02:00','192.168.1.100','Mozilla/5.0 Chrome/123 Windows',1,1,NULL),(31,2075829151662010370,'2026-07-12 21:19:04','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36',1,1,NULL),(32,2,'2026-07-13 11:08:28','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Code/1.128.0 Chrome/148.0.7778.271 Electron/42.5.0 Safari/537.36',1,1,NULL),(33,2,'2026-07-13 11:14:15','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(34,2,'2026-07-13 11:52:27','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(35,2,'2026-07-13 15:28:37','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(36,2075829151662010372,'2026-07-13 15:28:38','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(37,2,'2026-07-13 15:31:45','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(38,2075829151662010372,'2026-07-13 15:32:05','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(39,6,'2026-07-13 15:37:45','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(40,2,'2026-07-13 15:37:58','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(41,2,'2026-07-13 15:38:17','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(42,6,'2026-07-13 15:43:25','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(43,2,'2026-07-13 15:43:26','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(44,6,'2026-07-13 15:51:35','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(45,2,'2026-07-13 15:52:46','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(46,2,'2026-07-13 15:57:14','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(47,6,'2026-07-13 15:57:14','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(48,2,'2026-07-13 15:58:17','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(49,2,'2026-07-13 16:05:30','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(50,2,'2026-07-13 16:09:46','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(51,2,'2026-07-13 16:09:48','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(52,2,'2026-07-13 16:11:01','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(53,2,'2026-07-13 16:11:28','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(54,4,'2026-07-13 16:11:29','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(55,5,'2026-07-13 16:11:31','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(56,6,'2026-07-13 16:11:32','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(57,2,'2026-07-13 16:13:06','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(58,2075829151662010370,'2026-07-13 16:13:11','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36',1,1,NULL),(59,2076580773217914881,'2026-07-13 16:13:33','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(60,2,'2026-07-13 16:13:49','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(61,2076580773217914881,'2026-07-13 16:14:09','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(62,2,'2026-07-13 16:14:10','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(63,2076580773217914881,'2026-07-13 16:14:11','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(64,4,'2026-07-13 16:14:12','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(65,5,'2026-07-13 16:14:13','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(66,6,'2026-07-13 16:14:14','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(67,6,'2026-07-13 16:14:16','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(68,2,'2026-07-13 16:17:07','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(69,2,'2026-07-13 16:20:22','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(70,2,'2026-07-13 16:23:31','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(71,6,'2026-07-13 16:24:52','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(72,6,'2026-07-13 17:03:30','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(73,6,'2026-07-13 17:07:38','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(74,2,'2026-07-13 17:14:18','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(75,6,'2026-07-13 17:14:18','0:0:0:0:0:0:0:1','curl/8.18.0',1,1,NULL),(76,6,'2026-07-13 17:15:07','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(77,2,'2026-07-13 17:15:57','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(78,6,'2026-07-13 17:21:02','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(79,6,'2026-07-13 17:28:27','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(80,2,'2026-07-13 17:29:11','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(81,2,'2026-07-13 17:43:13','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(82,6,'2026-07-13 17:43:44','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(83,6,'2026-07-13 17:46:57','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(84,6,'2026-07-14 09:45:00','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(85,2075829151662010370,'2026-07-14 10:14:05','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Code/1.119.0 Chrome/142.0.7444.265 Electron/39.8.8 Safari/537.36',1,1,NULL),(86,2075829151662010370,'2026-07-14 10:18:47','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(87,2075829151662010370,'2026-07-14 10:19:00','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(88,2075829151662010370,'2026-07-14 10:19:05','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(89,2075829151662010370,'2026-07-14 10:19:10','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(90,2075829151662010370,'2026-07-14 10:22:05','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(91,2075829151662010370,'2026-07-14 10:22:30','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(92,2075829151662010370,'2026-07-14 10:22:43','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(93,2,'2026-07-14 10:38:37','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(94,2075829151662010370,'2026-07-14 10:45:55','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(95,2,'2026-07-14 10:54:40','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(96,2,'2026-07-14 11:01:05','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(97,2075829151662010370,'2026-07-14 11:11:18','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(98,2075829151662010370,'2026-07-14 11:24:15','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(99,2075829151662010370,'2026-07-14 11:24:19','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(100,2075829151662010370,'2026-07-14 11:24:23','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(101,2075829151662010370,'2026-07-14 11:24:38','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(102,2075829151662010370,'2026-07-14 11:26:34','0:0:0:0:0:0:0:1','Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36',1,1,NULL),(103,2075829151662010370,'2026-07-14 11:34:24','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(104,2075829151662010370,'2026-07-14 14:19:43','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(105,2076915011577393153,'2026-07-14 14:21:49','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(106,2,'2026-07-14 14:22:25','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(107,2,'2026-07-14 14:23:41','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Code/1.128.0 Chrome/148.0.7778.271 Electron/42.5.0 Safari/537.36',1,1,NULL),(108,2075829151662010370,'2026-07-14 14:26:04','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(109,2076915011577393153,'2026-07-14 14:43:25','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(110,2075829151662010370,'2026-07-14 14:43:42','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(111,2,'2026-07-14 15:06:01','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(112,2075829151662010371,'2026-07-14 15:07:03','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(113,2076580773217914882,'2026-07-14 15:07:38','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(114,2076580773217914882,'2026-07-14 15:10:13','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(115,2,'2026-07-14 15:14:41','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(116,2075829151662010370,'2026-07-14 15:22:15','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,0,'密码错误'),(117,2075829151662010370,'2026-07-14 15:22:23','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(118,2,'2026-07-14 16:10:09','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(119,2,'2026-07-14 16:42:52','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(120,2,'2026-07-14 16:54:53','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(121,2076915011577393153,'2026-07-14 17:22:13','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(122,2075829151662010370,'2026-07-14 17:22:56','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(123,2076915011577393153,'2026-07-14 17:23:16','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(124,2,'2026-07-14 17:37:44','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(125,2076964719435886594,'2026-07-14 17:39:47','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36',1,1,NULL),(126,2076964719435886594,'2026-07-14 17:40:35','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36',1,1,NULL),(127,2076964719435886594,'2026-07-14 17:45:09','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36',1,1,NULL),(128,2076964719435886594,'2026-07-15 09:34:42','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36',1,1,NULL),(129,2,'2026-07-15 09:37:16','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(130,2075829151662010370,'2026-07-15 10:08:44','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36',1,1,NULL),(131,2,'2026-07-15 10:13:07','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(132,2075829151662010370,'2026-07-15 10:25:47','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36',1,1,NULL),(133,2076580773217914882,'2026-07-15 10:33:05','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(134,2,'2026-07-15 10:35:38','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(135,2076580773217914882,'2026-07-15 10:39:03','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(136,2,'2026-07-15 10:40:23','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(137,2,'2026-07-15 10:47:48','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(138,2075829151662010371,'2026-07-15 10:54:35','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(139,2,'2026-07-15 10:59:48','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(140,2075829151662010370,'2026-07-15 11:00:52','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(141,2075829151662010371,'2026-07-15 11:01:45','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(142,2075829151662010370,'2026-07-15 11:02:57','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(143,2,'2026-07-15 11:03:53','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(144,2,'2026-07-15 11:22:52','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(145,2075829151662010370,'2026-07-15 11:23:39','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(146,2075829151662010371,'2026-07-15 11:24:24','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(147,2075829151662010371,'2026-07-15 11:36:06','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(148,2076964719435886594,'2026-07-15 11:37:03','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(149,2,'2026-07-15 11:39:41','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(150,2076580773217914882,'2026-07-15 11:41:39','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(151,2075829151662010371,'2026-07-15 11:43:21','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,0,'密码错误'),(152,2075829151662010371,'2026-07-15 11:43:24','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,0,'密码错误'),(153,2075829151662010371,'2026-07-15 11:43:30','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(154,2,'2026-07-15 11:44:02','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(155,5,'2026-07-15 11:45:00','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(156,2,'2026-07-15 11:45:17','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(157,2076580773217914882,'2026-07-15 11:45:28','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(158,2076580773217914881,'2026-07-15 11:46:41','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(159,2075829151662010371,'2026-07-15 11:46:55','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(160,2075829151662010371,'2026-07-15 11:49:21','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(161,4,'2026-07-15 11:49:38','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(162,4,'2026-07-15 11:55:45','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(163,2,'2026-07-15 11:56:04','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(164,2076580773217914882,'2026-07-15 11:56:38','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(165,2,'2026-07-15 11:58:13','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(166,2075829151662010371,'2026-07-15 11:58:42','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(167,2,'2026-07-15 14:12:16','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(168,2,'2026-07-15 15:12:14','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(169,2075829151662010370,'2026-07-15 15:23:30','0:0:0:0:0:0:0:1','Apifox/1.0.0 (https://apifox.com)',1,1,NULL),(170,2076964719435886594,'2026-07-15 15:32:09','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(171,2075829151662010370,'2026-07-15 15:36:28','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36',1,1,NULL),(172,2075829151662010371,'2026-07-15 15:44:22','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(173,2076580773217914882,'2026-07-15 15:47:25','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(174,2075829151662010371,'2026-07-15 16:19:55','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(175,2075829151662010370,'2026-07-15 16:25:04','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(176,2075829151662010370,'2026-07-15 16:34:31','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(177,2076915011577393153,'2026-07-15 16:39:27','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(178,2076964719435886594,'2026-07-15 16:42:54','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(179,2075829151662010370,'2026-07-15 16:44:08','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(180,2075829151662010370,'2026-07-15 16:44:36','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(181,2,'2026-07-15 16:45:11','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(182,2075829151662010370,'2026-07-15 16:46:02','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(183,2075829151662010370,'2026-07-15 16:49:06','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(184,2,'2026-07-15 16:49:20','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(185,2076964719435886594,'2026-07-15 16:50:15','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(186,2075829151662010370,'2026-07-15 16:52:43','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(187,2075829151662010370,'2026-07-15 16:53:22','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(188,2075829151662010370,'2026-07-15 16:54:42','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(189,2075829151662010370,'2026-07-15 16:56:23','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36',1,1,NULL),(190,2075829151662010370,'2026-07-15 16:57:13','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36',1,1,NULL),(191,2076964719435886594,'2026-07-15 16:57:28','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(192,2075829151662010371,'2026-07-15 16:58:07','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(193,2077317279501983745,'2026-07-15 17:00:14','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(194,2075829151662010370,'2026-07-15 17:06:27','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36',1,1,NULL),(195,2,'2026-07-15 17:06:34','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(196,2076964719435886594,'2026-07-15 17:08:48','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(197,2075829151662010370,'2026-07-15 17:14:49','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36',1,1,NULL),(198,2075829151662010370,'2026-07-15 17:15:05','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36',1,1,NULL),(199,2075829151662010370,'2026-07-15 17:15:16','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36',1,1,NULL),(200,2075829151662010370,'2026-07-15 17:15:42','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(201,2,'2026-07-15 17:16:34','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(202,2076964719435886594,'2026-07-15 17:17:31','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(203,2075829151662010370,'2026-07-15 17:17:43','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',1,1,NULL),(204,2076964719435886594,'2026-07-15 17:17:47','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(205,2076964719435886594,'2026-07-15 17:18:58','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(206,2076964719435886594,'2026-07-15 17:19:16','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(207,2076964719435886594,'2026-07-15 17:19:23','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(208,2076964719435886594,'2026-07-15 17:19:28','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(209,2075829151662010370,'2026-07-15 17:19:38','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36',1,1,NULL),(210,2076964719435886594,'2026-07-15 17:19:51','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(211,2076964719435886594,'2026-07-15 17:37:09','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(212,2,'2026-07-15 17:46:15','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Code/1.128.1 Chrome/148.0.7778.271 Electron/42.5.0 Safari/537.36',1,1,NULL),(213,2076964719435886594,'2026-07-15 17:50:19','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(214,2076964719435886594,'2026-07-15 18:01:39','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(215,2076964719435886594,'2026-07-15 21:08:36','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(216,2076964719435886594,'2026-07-15 22:00:57','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(217,2076964719435886594,'2026-07-15 22:07:08','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(218,2077317279501983746,'2026-07-15 22:12:36','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(219,2076964719435886594,'2026-07-15 22:14:37','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(220,2077317279501983746,'2026-07-15 22:28:58','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(221,2076964719435886594,'2026-07-15 22:29:56','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(222,2076964719435886594,'2026-07-15 22:58:39','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(223,2077317279501983746,'2026-07-15 23:04:49','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(224,2076964719435886594,'2026-07-15 23:05:29','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,1,NULL),(225,2077416009366466561,'2026-07-15 23:37:56','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,0,'密码错误'),(226,2077416009366466561,'2026-07-15 23:38:34','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.5161 SLBChan/112 SLBVPV/64-bit',1,0,'密码错误');

/*Table structure for table `makeup_punch` */

DROP TABLE IF EXISTS `makeup_punch`;

CREATE TABLE `makeup_punch` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employeeId` bigint(20) NOT NULL COMMENT '员工ID',
  `userId` bigint(20) NOT NULL COMMENT '用户ID',
  `punchDate` date NOT NULL COMMENT '补卡日期',
  `punchType` tinyint(4) NOT NULL COMMENT '补卡类型：0=上班补卡 1=下班补卡',
  `punchTime` datetime NOT NULL COMMENT '实际到岗/离岗时间',
  `reason` varchar(512) NOT NULL COMMENT '缺卡原因',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态：0=待审批 1=已通过 2=已拒绝',
  `approverId` bigint(20) DEFAULT NULL COMMENT '审批人ID',
  `approveTime` datetime DEFAULT NULL COMMENT '审批时间',
  `approveComment` varchar(512) DEFAULT NULL COMMENT '审批意见',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDeleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=否 1=是',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employeeId`),
  KEY `idx_user_id` (`userId`),
  KEY `idx_status` (`status`),
  KEY `idx_punch_date` (`punchDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡申请表';

/*Data for the table `makeup_punch` */

/*Table structure for table `oper_log` */

DROP TABLE IF EXISTS `oper_log`;

CREATE TABLE `oper_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operatorName` varchar(64) NOT NULL COMMENT '操作人姓名',
  `module` varchar(64) NOT NULL COMMENT '操作模块',
  `action` varchar(64) NOT NULL COMMENT '操作类型',
  `description` varchar(512) DEFAULT NULL COMMENT '操作描述',
  `operateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_module` (`module`),
  KEY `idx_operate_time` (`operateTime`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

/*Data for the table `oper_log` */

insert  into `oper_log`(`id`,`operatorName`,`module`,`action`,`description`,`operateTime`) values (1,'张三','用户管理','新增用户','新增了用户 王五','2026-07-14 10:30:00'),(2,'张三','角色权限','编辑角色','修改了角色 管理员 的权限','2026-07-14 09:45:00'),(3,'赵六','用户管理','删除用户','删除了用户 test001','2026-07-13 16:20:00'),(4,'张三','系统配置','修改配置','修改了系统参数 登录超时时间','2026-07-13 14:10:00'),(5,'李四','用户管理','编辑用户','修改了用户 王五 的信息','2026-07-13 11:00:00'),(6,'赵六','部门管理','新增部门','新增了部门 研发二部','2026-07-12 15:30:00'),(7,'张三','薪资管理','核算薪资','完成了2026-07月薪资核算','2026-07-12 09:00:00'),(8,'李四','员工管理','入职办理','办理了员工 孙九 的入职','2026-07-12 08:45:00'),(9,'赵六','角色权限','新增角色','新增了角色 财务专员','2026-07-11 17:20:00'),(10,'张三','审批中心','审批通过','通过了员工 钱七 的请假申请','2026-07-11 14:00:00'),(11,'李四','考勤管理','补卡审批','通过了员工 周八 的补卡申请','2026-07-11 10:30:00'),(12,'赵六','系统配置','修改配置','修改了系统参数 密码策略','2026-07-10 16:00:00'),(13,'张三','数据分析','导出报表','导出了2026-Q2员工分析报表','2026-07-10 11:00:00'),(14,'李四','员工管理','编辑员工','修改了员工 周八 的职级信息','2026-07-10 09:20:00');

/*Table structure for table `page_view_log` */

DROP TABLE IF EXISTS `page_view_log`;

CREATE TABLE `page_view_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `viewDate` date NOT NULL COMMENT '访问日期',
  `viewCount` bigint(20) NOT NULL DEFAULT '0' COMMENT '页面访问量',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_view_date` (`viewDate`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COMMENT='页面访问日志表';

/*Data for the table `page_view_log` */

insert  into `page_view_log`(`id`,`viewDate`,`viewCount`) values (1,'2026-07-08',520),(2,'2026-07-09',480),(3,'2026-07-10',630),(4,'2026-07-11',350),(5,'2026-07-12',280),(6,'2026-07-13',710),(7,'2026-07-14',590);

/*Table structure for table `password_history` */

DROP TABLE IF EXISTS `password_history`;

CREATE TABLE `password_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `userId` bigint(20) NOT NULL COMMENT '用户ID',
  `passwordHash` varchar(128) NOT NULL COMMENT '历史密码哈希',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id_time` (`userId`,`createTime`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='密码历史表';

/*Data for the table `password_history` */

insert  into `password_history`(`id`,`userId`,`passwordHash`,`createTime`) values (1,2075829151662010370,'e10adc3949ba59abbe56e057f20f883e','2025-12-15 09:00:00'),(2,2075829151662010370,'5d93ceb70e7bf5f408f0a8e5c4d5e5e3','2026-03-20 14:30:00'),(3,2075829151662010370,'25d55ad283aa400af464c76d713c07ad','2026-06-01 08:15:00');

/*Table structure for table `position` */

DROP TABLE IF EXISTS `position`;

CREATE TABLE `position` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) NOT NULL COMMENT '职位名称，如Java开发工程师',
  `sequence` tinyint(4) NOT NULL COMMENT '职位序列：1=M管理 2=P专业 3=S支持',
  `departmentId` bigint(20) unsigned DEFAULT NULL COMMENT '所属部门ID，空表示全公司通用',
  `levelMin` varchar(8) NOT NULL COMMENT '职级下限，如P1',
  `levelMax` varchar(8) NOT NULL COMMENT '职级上限，如P10',
  `defaultProbationMonths` int(11) NOT NULL DEFAULT '3' COMMENT '默认试用期月数',
  `description` varchar(256) DEFAULT NULL COMMENT '职位描述/岗位职责',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDeleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=否 1=是',
  PRIMARY KEY (`id`),
  KEY `idx_department_id` (`departmentId`),
  KEY `idx_sequence` (`sequence`)
) ENGINE=InnoDB AUTO_INCREMENT=2077217119303344131 DEFAULT CHARSET=utf8mb4 COMMENT='职位表';

/*Data for the table `position` */

insert  into `position`(`id`,`name`,`sequence`,`departmentId`,`levelMin`,`levelMax`,`defaultProbationMonths`,`description`,`createTime`,`updateTime`,`isDeleted`) values (1,'总经理',2,NULL,'P2','P6',3,'公司总经理，管理序列','2026-07-11 11:43:20','2026-07-11 11:43:20',0,1),(2,'部门经理111',1,NULL,'M1','M3',3,'部门经理，管理序列','2026-07-11 11:43:20','2026-07-11 11:43:20',0,1),(3,'高级开发工程师',2,NULL,'P6','P8',3,'高级开发，专业序列','2026-07-11 11:43:20','2026-07-11 11:43:20',0,1),(4,'开发工程师',2,NULL,'P3','P6',3,'初中级开发，专业序列','2026-07-11 11:43:20','2026-07-11 11:43:20',0,1),(5,'行政专员',3,NULL,'S1','S3',3,'行政支持，支持序列','2026-07-11 11:43:20','2026-07-11 11:43:20',0,1),(6,'高级经理',1,NULL,'M1','M2',3,'部门下设二级团队负责人','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(7,'项目经理',1,NULL,'M1','M2',3,'项目制团队负责人','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(8,'首席架构师',2,2,'P9','P10',3,'技术最高决策者，主导技术方向','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(9,'高级技术专家',2,NULL,'P7','P9',3,'技术领域专家，解决疑难问题','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(10,'技术专家',2,NULL,'P6','P8',3,'核心技术骨干，主导重要模块','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(11,'高级开发工程师',2,5,'P7','P7',2,'5年以上经验，能独立完成复杂模块','2026-07-11 11:50:35','2026-07-11 13:58:43',0,1),(12,'中级开发工程师',2,5,'P3','P5',3,'2-5年经验，能独立完成功能开发','2026-07-11 11:50:35','2026-07-11 13:58:40',0,1),(13,'初级开发工程师',2,5,'P1','P3',3,'0-2年经验，应届生或转行者','2026-07-11 11:50:35','2026-07-11 13:58:40',0,1),(14,'高级产品经理',2,5,'P6','P8',3,'资深产品负责人，主导产品规划','2026-07-11 11:50:35','2026-07-11 13:58:39',0,1),(15,'中级产品经理',2,5,'P4','P6',3,'独立负责产品线或模块','2026-07-11 11:50:35','2026-07-11 13:58:39',0,1),(16,'高级测试工程师',2,5,'P5','P7',3,'资深测试，主导测试体系建设','2026-07-11 11:50:35','2026-07-11 13:58:38',0,1),(17,'测试工程师',2,NULL,'P3','P5',3,'负责功能测试与自动化','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(18,'高级运维工程师',2,5,'P5','P7',3,'资深运维，负责系统稳定性','2026-07-11 11:50:35','2026-07-11 13:58:38',0,1),(19,'运维工程师',2,NULL,'P3','P5',3,'日常运维与监控','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(20,'安全工程师',2,NULL,'P4','P6',3,'负责信息安全与合规','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(21,'高级人力资源经理',3,4,'S5','S7',3,'HR领域的专家或管理者','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(22,'人力资源专员',3,NULL,'S2','S4',3,'负责招聘、培训、员工关系等','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(23,'高级财务经理',3,5,'S5','S7',3,'财务管理专家或部门负责人','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(24,'财务专员',3,NULL,'S2','S4',3,'负责账务核算、出纳等','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(25,'高级市场经理',3,3,'S5','S7',3,'市场战略制定与执行','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(26,'市场专员',3,NULL,'S2','S4',3,'市场推广活动执行','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(27,'高级行政经理',3,6,'S5','S7',3,'行政体系负责人','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(28,'行政专员',3,NULL,'S1','S3',3,'日常行政支持事务','2026-07-11 11:50:35','2026-07-11 11:50:35',0,1),(2076574980433211394,'1',2,1,'P1','P10',3,'wqewq','2026-07-13 15:50:30','2026-07-13 15:50:30',0,1),(2077217119303344130,'123214',2,2076573550385262594,'P3','P3',3,'123','2026-07-15 10:22:09','2026-07-15 10:22:09',0);

/*Table structure for table `role` */

DROP TABLE IF EXISTS `role`;

CREATE TABLE `role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `roleName` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `roleCode` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色描述',
  `dataScope` tinyint(4) NOT NULL DEFAULT '5' COMMENT '默认数据范围：1=全量 2=全部员工 3=本部门及下属 4=薪资相关 5=仅本人',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `permissions` json DEFAULT NULL COMMENT '权限列表（JSON数组，存储权限编码）',
  `fieldPermissions` json DEFAULT NULL COMMENT '字段权限：{"字段名":{"viewable":[角色ID],"editable":[角色ID],"mask":"脱敏规则"}}',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roleCode` (`roleCode`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色';

/*Data for the table `role` */

insert  into `role`(`id`,`roleName`,`roleCode`,`description`,`dataScope`,`status`,`permissions`,`fieldPermissions`,`createTime`,`updateTime`,`isDelete`) values (1,'系统管理员','ADMIN','全平台最高权限',1,1,'[\"*:*:*\"]',NULL,'2026-07-11 11:44:17','2026-07-11 11:44:17',0,1),(2,'HR专员','HR','员工管理、薪资核算',2,1,'[\"employee:list\", \"employee:add\", \"employee:edit\", \"employee:delete\", \"salary:list\", \"salary:view\", \"approval:process\"]','{\"idCard\": {\"viewable\": [1, 2]}, \"salaryInfo\": {\"viewable\": [1, 2, 4]}}','2026-07-11 11:44:17','2026-07-15 22:37:22',0,1),(3,'部门主管','MANAGER','本部门管理',3,1,'[\"employee:list\", \"employee:edit\", \"approval:process\"]',NULL,'2026-07-11 11:44:17','2026-07-11 11:44:17',0,1),(4,'财务专员','FINANCE','薪资相关',4,1,'[\"salary:list\", \"salary:view\", \"salary:audit\"]','{\"salaryInfo\": {\"viewable\": [1, 2, 4]}}','2026-07-11 11:44:17','2026-07-11 11:44:17',0,1),(5,'普通员工','EMPLOYEE','仅本人',5,1,'[\"employee:detail\", \"attendance:clock\"]','{\"salaryInfo\": {\"viewable\": [1, 2, 4, 5]}}','2026-07-11 11:44:17','2026-07-11 11:44:17',0);

/*Table structure for table `sal_account` */

DROP TABLE IF EXISTS `sal_account`;

CREATE TABLE `sal_account` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) NOT NULL COMMENT '账套名称，如"标准职员工资"',
  `scope_type` tinyint(4) NOT NULL COMMENT '适用范围类型：1=部门 2=职位 3=职级',
  `scope_ids` varchar(512) DEFAULT NULL COMMENT '适用范围ID集合(JSON数组)',
  `effective_date` date NOT NULL COMMENT '生效日期',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=否 1=是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2077332754700050435 DEFAULT CHARSET=utf8mb4 COMMENT='薪资账套表';

/*Data for the table `sal_account` */

insert  into `sal_account`(`id`,`name`,`scope_type`,`scope_ids`,`effective_date`,`is_deleted`,`create_time`,`update_time`) values (1001,'标准职员工资',2,'[1,2,3]','2026-01-01',0,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(1002,'实习生工资',3,'[\"P1\",\"P2\"]','2026-01-01',0,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(2076921584975605761,'劳子欣',85,'15','1991-04-09',1,'2026-07-14 14:47:48','2026-07-14 14:48:23'),(2076968822401622018,'外包职员工资',2,'2','2026-07-14',0,'2026-07-14 17:55:30','2026-07-14 17:55:30'),(2077332754700050434,'huitaiyang',2,NULL,'2026-07-15',0,'2026-07-15 18:01:38','2026-07-15 18:01:38');

/*Table structure for table `sal_batch` */

DROP TABLE IF EXISTS `sal_batch`;

CREATE TABLE `sal_batch` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batchNo` varchar(32) NOT NULL COMMENT '批次号',
  `salaryMonth` varchar(7) NOT NULL COMMENT '薪资月份: YYYY-MM',
  `status` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT=草稿, PENDING_CONFIRM=待确认, APPROVING=审批中, APPROVED=已通过, PAID=已发放',
  `totalEmployeeCount` int(11) NOT NULL DEFAULT '0' COMMENT '核算员工总数',
  `totalGross` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '应发工资总额',
  `totalDeduction` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '扣除总额',
  `totalNet` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '实发工资总额',
  `createdBy` bigint(20) NOT NULL COMMENT '创建人ID',
  `approvedBy` bigint(20) DEFAULT NULL COMMENT '审批人ID',
  `paidAt` datetime DEFAULT NULL COMMENT '实际发放时间',
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_no` (`batchNo`),
  UNIQUE KEY `uk_salary_month` (`salaryMonth`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='薪资核算批次表';

/*Data for the table `sal_batch` */

insert  into `sal_batch`(`id`,`batchNo`,`salaryMonth`,`status`,`totalEmployeeCount`,`totalGross`,`totalDeduction`,`totalNet`,`createdBy`,`approvedBy`,`paidAt`,`createdAt`,`updatedAt`) values (1,'B202601','2026-01','PAID',1,'15000.00','2870.00','12130.00',1,NULL,'2026-02-10 10:00:00','2026-07-12 17:15:39','2026-07-12 17:15:39'),(2,'B202602','2026-02','PAID',1,'15200.00','2890.00','12310.00',1,NULL,'2026-03-10 10:00:00','2026-07-12 17:15:39','2026-07-12 17:15:39'),(3,'B202603','2026-03','PAID',1,'15500.00','2930.00','12570.00',1,NULL,'2026-04-10 10:00:00','2026-07-12 17:15:39','2026-07-12 17:15:39'),(4,'B202604','2026-04','PAID',1,'15500.00','2930.00','12570.00',1,NULL,'2026-05-10 10:00:00','2026-07-12 17:15:39','2026-07-12 17:15:39'),(5,'B202605','2026-05','PAID',1,'15800.00','2975.00','12825.00',1,NULL,'2026-06-10 10:00:00','2026-07-12 17:15:39','2026-07-12 17:15:39'),(6,'B202606','2026-06','PAID',1,'16000.00','3000.00','13000.00',1,NULL,'2026-07-10 10:00:00','2026-07-12 17:15:39','2026-07-12 17:15:39');

/*Table structure for table `sal_batch_detail` */

DROP TABLE IF EXISTS `sal_batch_detail`;

CREATE TABLE `sal_batch_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batchId` bigint(20) NOT NULL COMMENT '批次ID',
  `employeeId` bigint(20) NOT NULL COMMENT '员工ID',
  `baseSalary` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '基本工资',
  `allowance` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '岗位津贴',
  `performanceBonus` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '绩效奖金',
  `overtimePay` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '加班费',
  `lateDeduction` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '迟到扣款',
  `leaveDeduction` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '请假扣款',
  `socialPension` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '养老保险',
  `socialMedical` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '医疗保险',
  `socialUnemployment` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '失业保险',
  `housingFund` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '住房公积金',
  `incomeTax` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '个人所得税',
  `grossSalary` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '应发工资',
  `totalDeduction` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '应扣合计',
  `netSalary` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '实发工资',
  `hasAnomaly` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否有异常: 0=正常, 1=预警, 2=阻断',
  `anomalyReason` varchar(256) DEFAULT NULL COMMENT '异常说明',
  `manualAdjust` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '手动调整金额',
  `adjustReason` varchar(256) DEFAULT NULL COMMENT '手动调整原因',
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_employee` (`batchId`,`employeeId`),
  KEY `idx_employee_id` (`employeeId`),
  KEY `idx_batch_id` (`batchId`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='薪资核算明细表（工资条）';

/*Data for the table `sal_batch_detail` */

insert  into `sal_batch_detail`(`id`,`batchId`,`employeeId`,`baseSalary`,`allowance`,`performanceBonus`,`overtimePay`,`lateDeduction`,`leaveDeduction`,`socialPension`,`socialMedical`,`socialUnemployment`,`housingFund`,`incomeTax`,`grossSalary`,`totalDeduction`,`netSalary`,`hasAnomaly`,`anomalyReason`,`manualAdjust`,`adjustReason`,`createdAt`,`updatedAt`) values (1,1,15,'10000.00','2500.00','2000.00','500.00','0.00','0.00','800.00','200.00','50.00','1800.00','20.00','15000.00','2870.00','12130.00',0,NULL,'0.00',NULL,'2026-07-12 17:15:39','2026-07-12 17:15:39'),(2,2,15,'10000.00','2500.00','2200.00','500.00','0.00','0.00','800.00','200.00','50.00','1800.00','40.00','15200.00','2890.00','12310.00',0,NULL,'0.00',NULL,'2026-07-12 17:15:39','2026-07-12 17:15:39'),(3,3,15,'10000.00','2500.00','2500.00','500.00','0.00','0.00','800.00','200.00','50.00','1800.00','80.00','15500.00','2930.00','12570.00',0,NULL,'0.00',NULL,'2026-07-12 17:15:39','2026-07-12 17:15:39'),(4,4,15,'10000.00','2500.00','2500.00','500.00','50.00','0.00','800.00','200.00','50.00','1800.00','80.00','15500.00','2980.00','12520.00',1,NULL,'0.00',NULL,'2026-07-12 17:15:39','2026-07-12 17:15:39'),(5,5,15,'10500.00','2500.00','2300.00','500.00','0.00','0.00','840.00','210.00','52.50','1800.00','72.50','15800.00','2975.00','12825.00',0,NULL,'0.00',NULL,'2026-07-12 17:15:39','2026-07-12 17:15:39'),(6,6,15,'10500.00','2500.00','2500.00','500.00','0.00','0.00','840.00','210.00','52.50','1800.00','97.50','16000.00','3000.00','13000.00',0,NULL,'0.00',NULL,'2026-07-12 17:15:39','2026-07-12 17:15:39');

/*Table structure for table `sal_change_log` */

DROP TABLE IF EXISTS `sal_change_log`;

CREATE TABLE `sal_change_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_id` bigint(20) unsigned NOT NULL COMMENT '员工ID',
  `change_type` tinyint(4) NOT NULL COMMENT '变更类型：1=调薪 2=账套变更 3=基数调整 4=转正调薪 5=调岗调薪',
  `old_value` json DEFAULT NULL COMMENT '变更前薪资档案快照',
  `new_value` json DEFAULT NULL COMMENT '变更后薪资档案快照',
  `effective_date` date NOT NULL COMMENT '生效日期',
  `operator_id` bigint(20) unsigned NOT NULL COMMENT '操作人ID',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_effective_date` (`effective_date`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='调薪历史表';

/*Data for the table `sal_change_log` */

insert  into `sal_change_log`(`id`,`employee_id`,`change_type`,`old_value`,`new_value`,`effective_date`,`operator_id`,`remark`,`create_time`) values (1,1,1,'{\"baseSalary\": 9000}','{\"baseSalary\": 10000}','2026-03-01',1,'年度调薪','2026-07-14 14:19:59'),(2,2,3,'{\"socialInsuranceBase\": 10000}','{\"socialInsuranceBase\": 12000}','2026-04-01',1,'社保基数年度调整','2026-07-14 14:19:59');

/*Table structure for table `sal_item` */

DROP TABLE IF EXISTS `sal_item`;

CREATE TABLE `sal_item` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_id` bigint(20) unsigned NOT NULL COMMENT '所属账套ID',
  `name` varchar(64) NOT NULL COMMENT '项目名称，如基本工资、绩效奖金',
  `item_type` tinyint(4) NOT NULL COMMENT '项目类型：1=固定收入 2=变动收入 3=考勤扣款 4=社保扣除 5=公积金扣除 6=个税',
  `formula` varchar(512) DEFAULT NULL COMMENT '计算公式/规则描述',
  `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '排序序号',
  `is_taxable` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否计入个税：0=否 1=是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_account_id` (`account_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2076921586443612162 DEFAULT CHARSET=utf8mb4 COMMENT='工资项目表';

/*Data for the table `sal_item` */

insert  into `sal_item`(`id`,`account_id`,`name`,`item_type`,`formula`,`sort_order`,`is_taxable`,`create_time`,`update_time`) values (1,1001,'基本工资',1,'直接取值',1,1,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(2,1001,'岗位津贴',1,'直接取值',2,1,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(3,1001,'绩效奖金',2,'基数×绩效系数',3,1,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(4,1001,'加班费',2,'小时工资×倍数×时长',4,1,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(5,1001,'迟到扣款',3,'50元×迟到次数',5,0,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(6,1001,'请假扣款',3,'日工资×请假天数',6,0,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(7,1001,'养老保险',4,'社保基数×8%',7,0,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(8,1001,'医疗保险',4,'社保基数×2%',8,0,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(9,1001,'失业保险',4,'社保基数×0.5%',9,0,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(10,1001,'住房公积金',5,'公积金基数×12%',10,0,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(11,1001,'个人所得税',6,'累计预扣法',11,0,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(12,1002,'实习工资',1,'直接取值',2,1,'2026-07-14 14:19:58','2026-07-15 09:41:51'),(13,1002,'加班费',2,'小时工资×1.5×时长',2,1,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(14,1002,'个人所得税',6,'累计预扣法',3,0,'2026-07-14 14:19:58','2026-07-14 14:19:58'),(2076921585717997569,2076921584975605761,'回雨涵',53,'dolor sint dolore',34,39,'2026-07-14 14:47:48','2026-07-14 14:47:48'),(2076921586049347585,2076921584975605761,'糜浩轩',28,'culpa',21,97,'2026-07-14 14:47:48','2026-07-14 14:47:48'),(2076921586443612161,2076921584975605761,'益雨桐',87,'minim ipsum qui esse',91,37,'2026-07-14 14:47:48','2026-07-14 14:47:48');

/*Table structure for table `sal_tax_cumulative` */

DROP TABLE IF EXISTS `sal_tax_cumulative`;

CREATE TABLE `sal_tax_cumulative` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_id` bigint(20) unsigned NOT NULL COMMENT '员工ID',
  `tax_year` smallint(6) NOT NULL COMMENT '纳税年度',
  `tax_month` tinyint(4) NOT NULL COMMENT '纳税月份',
  `cumulative_gross_pay` decimal(16,2) NOT NULL COMMENT '累计应发工资',
  `cumulative_threshold` decimal(12,2) NOT NULL COMMENT '累计起征点（5000×月数）',
  `cumulative_social_security` decimal(12,2) NOT NULL COMMENT '累计社保扣除',
  `cumulative_housing_fund` decimal(12,2) NOT NULL COMMENT '累计公积金扣除',
  `cumulative_special_deduction` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '累计专项附加扣除',
  `cumulative_taxable_income` decimal(16,2) NOT NULL COMMENT '累计应纳税所得额',
  `tax_rate` decimal(5,4) NOT NULL COMMENT '适用税率',
  `quick_deduction` decimal(12,2) NOT NULL COMMENT '速算扣除数',
  `cumulative_tax_payable` decimal(16,2) NOT NULL COMMENT '累计应缴个税',
  `cumulative_tax_paid` decimal(16,2) NOT NULL COMMENT '累计已缴个税',
  `current_month_tax` decimal(12,2) NOT NULL COMMENT '当月应缴个税',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_year_month` (`employee_id`,`tax_year`,`tax_month`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='个税累计表';

/*Data for the table `sal_tax_cumulative` */

insert  into `sal_tax_cumulative`(`id`,`employee_id`,`tax_year`,`tax_month`,`cumulative_gross_pay`,`cumulative_threshold`,`cumulative_social_security`,`cumulative_housing_fund`,`cumulative_special_deduction`,`cumulative_taxable_income`,`tax_rate`,`quick_deduction`,`cumulative_tax_payable`,`cumulative_tax_paid`,`current_month_tax`,`create_time`) values (1,1,2026,1,'12000.00','5000.00','840.00','960.00','0.00','5200.00','0.0300','0.00','156.00','0.00','156.00','2026-07-14 14:19:59'),(2,1,2026,2,'24000.00','10000.00','1680.00','1920.00','0.00','10400.00','0.0300','0.00','312.00','156.00','156.00','2026-07-14 14:19:59'),(3,1,2026,3,'36000.00','15000.00','2520.00','2880.00','0.00','15600.00','0.0300','0.00','468.00','312.00','156.00','2026-07-14 14:19:59'),(4,1,2026,4,'48000.00','20000.00','3360.00','3840.00','0.00','20800.00','0.0300','0.00','624.00','468.00','156.00','2026-07-14 14:19:59'),(5,1,2026,5,'60000.00','25000.00','4200.00','4800.00','0.00','26000.00','0.0300','0.00','780.00','624.00','156.00','2026-07-14 14:19:59'),(6,1,2026,6,'72000.00','30000.00','5040.00','5760.00','0.00','31200.00','0.0300','0.00','936.00','780.00','156.00','2026-07-14 14:19:59');

/*Table structure for table `user` */

DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `userAccount` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号',
  `userPassword` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `userName` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `userAvatar` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像',
  `userProfile` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户简介',
  `roleId` bigint(20) DEFAULT NULL COMMENT '角色ID',
  `employeeId` bigint(20) DEFAULT NULL COMMENT '员工ID',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '账号状态: 1=启用, 0=禁用',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2077416009366466562 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';

/*Data for the table `user` */

insert  into `user`(`id`,`userAccount`,`userPassword`,`userName`,`userAvatar`,`userProfile`,`roleId`,`employeeId`,`createTime`,`updateTime`,`isDelete`,`status`) values (2,'admin','a1cedf10576ecbfef1ff522cdeba7c6e','系统管理员',NULL,NULL,1,NULL,'2026-07-11 11:49:34','2026-07-11 14:45:50',0,1),(3,'hr','a1cedf10576ecbfef1ff522cdeba7c6e','HR专员',NULL,NULL,2,NULL,'2026-07-11 11:49:34','2026-07-11 15:05:24',0,1),(4,'manager','a1cedf10576ecbfef1ff522cdeba7c6e','部门主管',NULL,NULL,3,NULL,'2026-07-11 11:49:34','2026-07-11 15:05:24',0,1),(5,'finance','a1cedf10576ecbfef1ff522cdeba7c6e','财务专员',NULL,NULL,4,NULL,'2026-07-11 11:49:34','2026-07-11 15:05:24',0,1),(6,'employee','a1cedf10576ecbfef1ff522cdeba7c6e','系统管理员',NULL,NULL,5,NULL,'2026-07-11 11:49:34','2026-07-15 16:19:24',0,1),(2075829151662010370,'limou','a1cedf10576ecbfef1ff522cdeba7c6e','系统管理员',NULL,NULL,1,NULL,'2026-07-11 14:26:51','2026-07-15 16:19:38',0,1),(2075829151662010371,'zhangsan','a1cedf10576ecbfef1ff522cdeba7c6e','普通员工',NULL,NULL,5,NULL,'2026-07-11 15:24:14','2026-07-15 16:57:54',0,1),(2075829151662010372,'testemp','a1cedf10576ecbfef1ff522cdeba7c6e',NULL,NULL,NULL,5,NULL,'2026-07-13 09:12:43','2026-07-13 09:12:43',0,1),(2076580773217914881,'hruser','a1cedf10576ecbfef1ff522cdeba7c6e',NULL,NULL,NULL,2,NULL,'2026-07-13 16:13:31','2026-07-13 16:13:31',0,1),(2076580773217914882,'hr111111','a1cedf10576ecbfef1ff522cdeba7c6e','HR专员',NULL,NULL,2,NULL,'2026-07-14 14:18:34','2026-07-14 15:07:13',0,1),(2076915011577393153,'HHHHRRR','e66fe67031a1bb358c41e4198b8eff85',NULL,NULL,NULL,2,NULL,'2026-07-14 14:21:41','2026-07-15 10:23:27',0,1),(2076964719435886594,'HHRR','7b29a09abb82968493450cd7e5774b3f',NULL,NULL,NULL,2,19,'2026-07-14 17:39:13','2026-07-15 22:26:03',0,1),(2077275185780559873,'18444444444','a4bf4db7910a8348f24368e5c60fe91e','huitaiyang HUITAIYANG',NULL,NULL,NULL,NULL,'2026-07-15 14:12:53','2026-07-15 14:12:53',0,1),(2077317279501983745,'Three','fa250c628fde7d085307b5e074354f37',NULL,NULL,NULL,NULL,NULL,'2026-07-15 17:00:10','2026-07-15 17:00:10',0,1),(2077317279501983746,'zhangwei','a1cedf10576ecbfef1ff522cdeba7c6e','张伟',NULL,NULL,3,1,'2026-07-15 21:50:16','2026-07-15 21:50:18',0,1),(2077416009366466561,'13525968115','eb079b0c32dcbcb7f26939c2485f32de','张三',NULL,NULL,5,NULL,'2026-07-15 23:32:29','2026-07-15 23:32:29',0,1);

-- ============================================================
-- user 表：账号状态字段（启用/禁用）
-- ============================================================
ALTER TABLE `user`
    ADD COLUMN `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '账号状态: 1=启用, 0=禁用' AFTER `isDelete`;

-- ============================================================
-- 员工异动模块（转正/调岗/离职）补充字段
-- ============================================================

-- emp_probation: 补充审批人、状态、薪资调整、审批结果等字段
ALTER TABLE emp_probation
    ADD COLUMN approverId bigint(20) DEFAULT NULL COMMENT '审批人ID(employee.id)' AFTER employeeId,
    ADD COLUMN salaryAdjustment decimal(12,2) DEFAULT NULL COMMENT '转正后薪资调整金额' AFTER confirmBaseSalary,
    ADD COLUMN adjustRemark varchar(256) DEFAULT NULL COMMENT '薪资调整说明' AFTER salaryAdjustment,
    ADD COLUMN result varchar(16) DEFAULT NULL COMMENT '审批结果: PASS=通过, EXTEND=延长, REJECT=不通过' AFTER adjustRemark,
    ADD COLUMN extendedMonths int DEFAULT NULL COMMENT '延长试用月数' AFTER result,
    ADD COLUMN status varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/APPROVING/APPROVED/REJECTED' AFTER extendedMonths;

-- emp_transfer: 补充审批人、职级、汇报人、状态字段
ALTER TABLE emp_transfer
    ADD COLUMN approverId bigint(20) DEFAULT NULL COMMENT '审批人ID(employee.id)' AFTER employeeId,
    ADD COLUMN toRankCode varchar(8) DEFAULT NULL COMMENT '新职级编码(可选)' AFTER newBaseSalary,
    ADD COLUMN toReporterId bigint(20) DEFAULT NULL COMMENT '新直接汇报人ID(可选)' AFTER toRankCode,
    ADD COLUMN status varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/APPROVING/APPROVED/REJECTED' AFTER toReporterId;

-- emp_resign: 补充审批人、原因大类、状态字段
ALTER TABLE emp_resign
    ADD COLUMN approverId bigint(20) DEFAULT NULL COMMENT '审批人ID(employee.id)' AFTER employeeId,
    ADD COLUMN resignReasonType varchar(16) DEFAULT NULL COMMENT '原因大类: VOLUNTARY=主动, INVOLUNTARY=被动, NEGOTIATED=协商' AFTER resignReason,
    ADD COLUMN status varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/APPROVING/PENDING_RESIGN/RESIGNED/REJECTED' AFTER remark;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

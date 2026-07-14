# 审批中心 — 委托审批 + 超时处理 + 红点数量

**Goal:** 实现委托审批、审批超时处理、待办红点数量三个功能。

## Global Constraints
- Java 8, Spring Boot 2.7.2, MyBatis-Plus 3.5.2
- 包路径: com.limou.hrms
- 编译: `./mvnw compile -q`

---

### Task A: 待办红点数量

**接口:** `GET /api/approvals/pending-count` → `{"count": N}`

1. ApprovalFlowService 加方法: `int getPendingCount(Long employeeId)`
   - SQL: `SELECT COUNT(*) FROM approval_node WHERE approver_id = ? AND status = 1`
   - 含委托路由：还要算上被委托给我的
2. ApprovalFlowServiceImpl 实现
3. ApprovalController 加端点
4. 暂不引入 Redis

---

### Task B: 委托审批

**接口:**
- `POST /api/approvals/delegates` — 设置委托 (delegateId, startTime, endTime)
- `DELETE /api/approvals/delegates/{delegateId}` — 取消委托
- `GET /api/approvals/delegates/my` — 查询我的委托关系

**校验:** 不能委托给自己、时间不可重叠、endTime > startTime

**路由逻辑:** 
- `ApprovalDelegateService.resolveApprover(originalApproverId)` — 查 approval_delegate WHERE delegator_id=? AND enabled=1 AND NOW() BETWEEN start_time AND end_time，有委托返回 delegate_id，无返回本人
- `getPendingList` 需查两个来源：① approver_id = currentUser ② approver_id = 被委托给 currentUser 的节点

**实现:**
1. ApprovalDelegateService 接口 + impl
2. DelegateSettingDTO
3. DelegateVO（响应用）
4. ApprovalController 加 3 个端点
5. 修改 ApprovalFlowServiceImpl.getPendingList/getPendingCount 加入委托路由
6. 修改 ApprovalFlowServiceImpl.approve/transfer 记录 original_approver_id

---

### Task C: 审批超时处理

**方案:** 定时任务（@Scheduled）每小时扫描超时节点，不依赖 MQ。

1. NodeStatus 加 TIMED_OUT(5, "已超时")
2. ErrorCode 加 APPROVAL_NODE_TIMEOUT(40011, "审批已超时")
3. ApprovalTimeoutJob: @Scheduled(cron = "0 0 * * * ?") 每小时执行
   - 扫描 approval_node WHERE status = 1(PENDING) AND create_time < NOW() - 48h
   - 将超时节点标记为 TIMED_OUT(5)
   - 记录日志（instanceId, nodeId, approverId）
4. MainApplication 加 @EnableScheduling
5. ApprovalFlowServiceImpl.approve/reject 中校验：节点已超时则抛 APPROVAL_NODE_TIMEOUT

---

### Task D: 编译验证

`./mvnw compile -q` + `./mvnw test -q`

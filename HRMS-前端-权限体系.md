# HRMS-前端-权限体系

# 1. 需求背景

> 基于 RBAC 模型实现系统权限体系的统一管理，涵盖角色 CRUD、用户角色分配、功能权限控制、数据范围控制、字段级权限控制。前端需根据登录用户角色动态控制菜单可见性、按钮可见性、数据查询范围、以及敏感字段的展示与编辑权限。

## 1.1 项目成员

| **角色** | **成员** | **备注** |
| --- | --- | --- |
| 业务方 | - | HR部门 |
| 产品经理（PD） | - | |
| 后端技术 | 陆博 | |
| UED（设计师） | - | |
| 前端 | - | |
| 质量 | - | |

## 1.2 项目文档

PRD 文档：[HRMS.md](./HRMS.md) 第2章

UED：选填

后端系分：[HRMS-后端-权限体系.md](./HRMS-后端-权限体系.md)

前端公共组件系分: 选填

迭代地址: -

开发环境地址：-

测试环境地址：-

# 2. 详细设计

## 2.1 前端迭代目标

1. 角色管理页：角色列表（分页） + 新增角色弹窗 + 编辑角色弹窗 + 权限码多选分配 + 字段权限配置
2. 用户角色分配：在用户列表页为每个用户分配/切换角色
3. 全局权限控制：登录后根据权限码动态控制左侧菜单、页面内按钮、Tab 页签的显隐
4. 数据范围适配：各业务页面根据用户 dataScope 控制列表数据加载范围（全量/本部门/仅自己）
5. 字段级权限适配：员工档案等详情页根据 fieldPermissions 控制敏感字段的可见性和可编辑性

## 2.2 迭代具体描述

### 2.2.1 角色管理页

##### UI&交互

- 顶部工具栏：搜索框（角色名称模糊）+ 「新增角色」按钮
- 表格展示角色列表，支持分页、排序
- 表格列：角色名称、角色编码、数据范围、状态（启用/禁用 Tag）、操作（编辑/删除）
- 新增/编辑统一使用弹窗（Modal）
- 权限分配使用「模块-权限」树形多选组件
- 角色编码：新增后可编辑，系统做唯一校验
- 数据范围使用 Radio 选择

##### 前端逻辑

###### 列表搜索字段

| 字段名称 | 说明 | 输入方式 | 是否必填 | 默认值 | 字段类型 | 提示文案 | 数据源 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 角色名称 | 模糊搜索 | Input | N | - | string | 请输入角色名称 | - |
| 角色编码 | 精确匹配 | Input | N | - | string | 请输入角色编码 | - |
| 数据范围 | 下拉单选 | Select | N | - | number | 请选择数据范围 | 枚举：全量/全部员工/本部门/薪资/仅本人 |
| 状态 | 下拉单选 | Select | N | - | number | 请选择状态 | 枚举：启用/禁用 |

###### 列表展示字段

| 字段名称 | 说明 | 默认值 | 宽度 |
| --- | --- | --- | --- |
| 角色名称 | - | N | 150 |
| 角色编码 | - | N | 120 |
| 描述 | 超长截断 + Tooltip | N | 200 |
| 数据范围 | Tag 展示 | N | 120 |
| 状态 | 启用(绿) / 禁用(红) Tag | N | 80 |
| 操作 | 编辑 / 删除 | N | 150 |

###### 列表工具栏

| 按钮 | 交互 | 显示控制 |
| --- | --- | --- |
| 新增角色 | 打开新增弹窗 | 拥有 `role:manage` 权限 |
| 刷新 | 重新加载列表 | 始终显示 |

###### 新增/编辑角色弹窗

| 字段名称 | 说明 | 输入方式 | 是否必填 | 默认值 | 最大输入长度 | 输入限制 | 字段类型 | 提示文案 | 数据源 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 角色名称 | 如"HR专员" | Input | Y | - | 32 | - | string | 请输入角色名称 | - |
| 角色编码 | 唯一标识 | Input | Y | - | 32 | 字母或数字 | string | 请输入角色编码（唯一） | - |
| 描述 | 角色职责说明 | TextArea | N | - | 128 | - | string | 请输入角色描述 | - |
| 数据范围 | 控制数据可见范围 | Radio.Group | Y | 5 | - | - | number | - | 枚举：1全量/2全部员工/3本部门/4薪资/5仅本人 |
| 状态 | - | Switch | Y | true | - | - | boolean | - | - |
| 功能权限 | 多选权限码 | TreeSelect multiple | N | - | - | - | string[] | 请勾选权限 | GET /api/permission/codes |
| 字段权限 | JSON 配置 | JsonEditor / TextArea | N | - | - | - | object | 配置字段可见/可编辑规则 | - |

###### 权限分配组件

权限码按模块分组展示，支持全选/取消全选模块：

```
☐ 员工管理
   ☑ employee:list    员工列表
   ☑ employee:add     新增员工
   ☑ employee:edit    编辑员工
   ☐ employee:delete  删除员工
   ☑ employee:detail  员工详情
☐ 薪资管理
   ☐ salary:list      薪资列表
   ☐ salary:view      薪资查看
   ☐ salary:audit     薪资审核
☐ 审批管理
   ☐ approval:process 审批处理
☐ 考勤管理
   ☐ attendance:clock  打卡
   ☐ attendance:list   考勤列表
   ☐ attendance:manage 考勤管理
☐ 组织架构
   ☐ org:manage        组织架构管理
☐ 系统管理
   ☐ system:config     系统配置
   ☐ system:backup     数据备份
   ☐ role:manage       角色管理
```

###### 操作按钮

| 按钮 | 交互 | 二次确认 | 显示/禁用控制 |
| --- | --- | --- | --- |
| 编辑 | 打开编辑弹窗（预填数据） | N | 拥有 `role:manage` 权限 |
| 删除 | 调用删除接口 | Y，"确定删除该角色？已分配该角色的用户将失去对应权限" | 拥有 `role:manage` 权限 |
| 保存 | 提交新增/编辑表单 | N | 表单校验通过后可用 |

###### 校验规则

| 规则 | 提示 |
| --- | --- |
| 角色名称非空 | "请输入角色名称" |
| 角色编码非空 + 唯一 | "角色编码已存在" |
| 数据范围必选 | "请选择数据范围" |

##### 所需API

| 接口 | 说明 |
| --- | --- |
| POST /api/role/list/page | 角色分页列表（含搜索、排序） |
| POST /api/role/add | 新增角色 |
| POST /api/role/update | 更新角色 |
| POST /api/role/delete | 删除角色（软删除） |
| GET /api/permission/codes | 获取全部权限码列表（渲染权限分配树） |

---

### 2.2.2 用户角色分配

##### UI&交互

- 在用户列表页，每行操作列增加「分配角色」按钮或下拉
- 弹出角色选择 Modal：展示已启用角色列表（Radio 或 Select），当前角色高亮
- 选择新角色 → 确认 → 调用分配接口 → 提示成功
- 也可在用户详情页展示当前角色并提供切换入口

##### 前端逻辑

###### 角色选择弹窗

| 字段名称 | 说明 | 输入方式 | 是否必填 | 字段类型 | 数据源 |
| --- | --- | --- | --- | --- | --- |
| 当前角色 | 只读展示 | Text | - | string | - |
| 选择角色 | 角色列表 | Radio.Group | Y | number | GET /api/role/list/enabled |

###### 操作按钮

| 按钮 | 交互 | 二次确认 | 显示控制 |
| --- | --- | --- | --- |
| 分配角色 | 打开角色选择弹窗 | Y，"将把用户 XXX 的角色切换为 YYY" | 拥有 `role:manage` 权限 |

##### 所需API

| 接口 | 说明 |
| --- | --- |
| POST /api/role/assign | 为用户分配角色 |
| GET /api/role/list/enabled | 获取已启用角色列表（选择框数据源） |

---

### 2.2.3 全局权限控制（菜单 & 按钮）

##### UI&交互

- 左侧导航菜单根据权限码动态渲染
- 页面内按钮根据权限码显示/隐藏
- 无权限访问的页面路由在路由守卫中拦截并跳转 403 页

##### 前端逻辑

###### 登录后初始化流程

```typescript
// 1. 用户登录成功，拿到 session
// 2. 立即调用 GET /api/permission/current 获取完整权限信息
// 3. 将 UserPermissionVO 存入全局状态（Redux / Pinia / Vuex）
// 4. 根据 permissionCodes 控制菜单和按钮渲染

interface UserPermission {
  userId: number
  userAccount: string
  userName: string
  roleId: number | null
  roleName: string | null
  roleCode: string | null
  dataScope: number           // 1~5
  dataScopeDesc: string       // "全量" / "全部员工" / ...
  permissions: string         // 原始 JSON 字符串
  permissionCodes: string[]   // 解析后的权限码数组
  fieldPermissions: string | null  // 字段权限 JSON 字符串
}
```

###### 菜单权限映射

| 菜单路径 | 所需权限码 | 谁可见 |
| --- | --- | --- |
| 员工管理 > 员工列表 | `employee:list` | 管理员、HR、部门主管 |
| 员工管理 > 新增员工 | `employee:add` | 管理员、HR |
| 员工管理 > 员工详情 | `employee:detail` | 所有登录用户 |
| 薪资管理 > 薪资列表 | `salary:list` | 管理员、HR、财务 |
| 薪资管理 > 薪资审核 | `salary:audit` | 管理员、财务 |
| 薪资管理 > 工资条 | `salary:view` | 管理员、HR、财务、普通员工（仅自己） |
| 审批管理 | `approval:process` | 管理员、HR、部门主管 |
| 考勤管理 > 打卡 | `attendance:clock` | 所有登录用户 |
| 考勤管理 > 考勤列表 | `attendance:list` | 管理员、HR、部门主管、普通员工（仅自己） |
| 考勤管理 > 考勤管理 | `attendance:manage` | 管理员、HR |
| 组织架构 > 部门管理 | `org:manage` | 管理员、HR |
| 组织架构 > 职位管理 | `org:manage` | 管理员、HR |
| 系统管理 > 角色管理 | `role:manage` | 管理员 |
| 系统管理 > 系统配置 | `system:config` | 管理员 |
| 系统管理 > 数据备份 | `system:backup` | 管理员 |

###### 权限判断工具函数

```typescript
// utils/permission.ts

// 检查是否拥有某个权限码
export function hasPermission(code: string): boolean {
  const perm = useUserPermissionStore()
  return perm.permissionCodes.includes('*:*:*')  // 超级管理员
      || perm.permissionCodes.includes(code)
}

// 检查是否拥有任意一个权限码
export function hasAnyPermission(...codes: string[]): boolean {
  return codes.some(code => hasPermission(code))
}

// 检查是否拥有所有权限码
export function hasAllPermission(...codes: string[]): boolean {
  return codes.every(code => hasPermission(code))
}

// 获取当前数据范围
export function getDataScope(): number {
  return useUserPermissionStore().dataScope
}
```

###### 指令/组件使用示例

```vue
<!-- 菜单渲染 -->
<el-menu-item v-if="hasPermission('employee:list')" index="/employee/list">
  员工列表
</el-menu-item>

<!-- 按钮控制 -->
<el-button v-if="hasPermission('employee:add')" @click="handleAdd">
  新增员工
</el-button>

<!-- 编辑按钮：HR 全字段可编辑，部门主管部分字段 -->
<el-button
  v-if="hasPermission('employee:edit')"
  @click="handleEdit"
>
  编辑
</el-button>
```

###### 路由守卫

```typescript
// router/permissionGuard.ts
router.beforeEach(async (to, from, next) => {
  // 白名单路由直接放行（登录、注册、403、404）
  if (whiteList.includes(to.path)) {
    return next()
  }

  const permStore = useUserPermissionStore()

  // 如果权限信息未加载，先获取
  if (!permStore.loaded) {
    try {
      const res = await api.getUserPermissions()
      permStore.setPermissions(res.data)
    } catch {
      return next('/login')
    }
  }

  // 检查路由元信息中定义的权限码
  const requiredPermission = to.meta?.permission as string | undefined
  if (requiredPermission && !hasPermission(requiredPermission)) {
    return next('/403')
  }

  next()
})
```

###### 路由定义示例

```typescript
const routes = [
  {
    path: '/employee/list',
    name: 'EmployeeList',
    component: () => import('@/views/employee/List.vue'),
    meta: {
      title: '员工列表',
      permission: 'employee:list'  // 进入此路由需要的权限码
    }
  },
  {
    path: '/salary/list',
    name: 'SalaryList',
    component: () => import('@/views/salary/List.vue'),
    meta: {
      title: '薪资列表',
      permission: 'salary:list'
    }
  }
]
```

##### 所需API

| 接口 | 说明 |
| --- | --- |
| GET /api/permission/current | 登录后立即调用，获取用户完整权限信息 |
| GET /api/permission/check?code=xxx | 可选：调用服务端实时校验权限 |

---

### 2.2.4 数据范围适配（各业务页面）

##### UI&交互

- 各列表页根据用户 `dataScope` 调整数据加载范围
- 前端不单独发送 dataScope 参数（由后端自动过滤），但需理解后端行为
- 搜索条件中的部门树选择器等可按 dataScope 限制可选范围

##### 前端逻辑

###### 数据范围展示映射

| dataScope | 名称 | 列表数据范围 | 部门选择器限制 |
| --- | --- | --- | --- |
| 1 | 全平台 | 显示所有数据 | 所有部门可选 |
| 2 | 全部员工 | 显示所有员工数据 | 所有部门可选 |
| 3 | 本部门及下属 | 仅显示本部门及子部门数据 | 仅可搜索本部门及下属 |
| 4 | 薪资相关 | 仅显示薪资模块数据 | 薪资模块专用 |
| 5 | 仅本人 | 仅显示当前登录用户数据 | 隐藏部门选择器 |

###### 业务页面适配示例

```typescript
// 员工列表页
const dataScope = getDataScope()  // 从全局状态获取

// 1=全平台 2=全部员工 → 不加过滤，后端返回全量
// 3=本部门及下属 → 后端自动过滤，前端无需传参
// 5=仅本人 → 后端自动过滤，前端列表仅显示1条数据

// 对于部门树选择器：
// dataScope <= 2 时才展示"全部部门"选项
// dataScope === 3 时默认选中用户所在部门且不可取消
// dataScope >= 4 时隐藏部门筛选器
```

##### 所需API

| 接口 | 说明 |
| --- | --- |
| GET /api/permission/data-scope | 获取当前用户数据范围等级 |

---

### 2.2.5 字段级权限适配（员工档案详情页）

##### UI&交互

- 员工档案详情页和编辑页，根据 `fieldPermissions` 控制字段的可见性和可编辑性
- 不可见的字段直接隐藏（不占据布局空间）
- 可见但不可编辑的字段：Input disabled + Tooltip "如需修改请联系HR"

##### 前端逻辑

###### 字段权限数据结构

从 `/api/permission/current` 返回的 `fieldPermissions` 是 JSON 字符串：

```json
{
  "idCard":       { "viewable": [1, 2], "editable": [1] },
  "salaryInfo":   { "viewable": [1, 2, 4], "editable": [1, 2] },
  "bankAccount":  { "viewable": [1, 2], "editable": [1] },
  "emergencyContact": { "viewable": [1, 2, 5], "editable": [5] }
}
```

- `viewable`: 角色 ID 数组，代表哪些角色可以看到此字段
- `editable`: 角色 ID 数组，代表哪些角色可以编辑此字段
- 数组为空或字段未配置 → 对所有角色不可见

###### 字段权限判断函数

```typescript
// utils/fieldPermission.ts

interface FieldRule {
  viewable: number[]   // 可查看的角色 ID 列表
  editable: number[]   // 可编辑的角色 ID 列表
}

interface FieldPermissions {
  [fieldName: string]: FieldRule
}

// 检查字段是否可见
export function isFieldViewable(
  fieldName: string,
  fieldPermissions: FieldPermissions | null,
  roleId: number | null
): boolean {
  if (!fieldPermissions || !roleId) return true  // 无配置默认可见
  const rule = fieldPermissions[fieldName]
  if (!rule) return true  // 无规则默认可见
  return rule.viewable.includes(roleId)
}

// 检查字段是否可编辑
export function isFieldEditable(
  fieldName: string,
  fieldPermissions: FieldPermissions | null,
  roleId: number | null
): boolean {
  if (!fieldPermissions || !roleId) return false  // 无配置默认不可编辑
  const rule = fieldPermissions[fieldName]
  if (!rule || !rule.editable) return false
  return rule.editable.includes(roleId)
}

// 获取需要隐藏的字段列表
export function getHiddenFields(
  fieldPermissions: FieldPermissions | null,
  roleId: number | null
): string[] {
  if (!fieldPermissions || !roleId) return []
  return Object.keys(fieldPermissions).filter(
    field => !isFieldViewable(field, fieldPermissions, roleId)
  )
}
```

###### 字段权限对照表

| **字段** | 系统管理员(1) | HR 专员(2) | 部门主管(3) | 财务专员(4) | 普通员工(5) |
| --- | --- | --- | --- | --- | --- |
| 姓名 | 查看 | 查看 | 查看 | - | 查看（仅自己） |
| 手机号 | 查看 | 查看 | 查看 | - | 查看（仅自己） |
| 身份证号 | 查看 | 查看 | 不可见 | - | 查看（仅自己） |
| 薪资信息 | - | 查看 | 不可见 | 查看 | 查看（仅自己） |
| 紧急联系人 | 查看 | 查看 | 不可见 | - | 查看（仅自己） |
| 银行卡号 | 查看 | 查看 | 不可见 | - | 查看（仅自己） |

> **说明**：系统管理员拥有 `*:*:*` 权限，但按业务规则不查看薪资数据（由 dataScope 和业务逻辑协同控制）。

###### 组件应用示例

```vue
<template>
  <!-- 员工详情 -->
  <el-descriptions>
    <!-- 所有人可见 -->
    <el-descriptions-item label="姓名">{{ employee.name }}</el-descriptions-item>

    <!-- 仅 HR 和管理员可见 -->
    <el-descriptions-item
      v-if="isFieldViewable('idCard', fieldPermissions, roleId)"
      label="身份证号"
    >
      {{ maskIdCard(employee.idCard) }}
    </el-descriptions-item>

    <!-- 可见但可能不可编辑 -->
    <el-form-item label="邮箱">
      <el-input
        v-model="form.email"
        :disabled="!isFieldEditable('email', fieldPermissions, roleId)"
        :title="!isFieldEditable('email', fieldPermissions, roleId) ? '如需修改邮箱请联系HR' : ''"
      />
    </el-form-item>
  </el-descriptions>
</template>

<script setup>
import { isFieldViewable, isFieldEditable } from '@/utils/fieldPermission'

const permStore = useUserPermissionStore()
const roleId = permStore.roleId
const fieldPermissions = permStore.fieldPermissions
  ? JSON.parse(permStore.fieldPermissions)
  : null
</script>
```

##### 所需API

| 接口 | 说明 |
| --- | --- |
| GET /api/permission/current | 返回的 fieldPermissions 字段用于控制字段可见性 |

---

### 2.2.6 403 无权限页面

##### UI&交互

- 居中展示：403 状态图标 + "抱歉，您没有权限访问此页面"
- 提供「返回首页」按钮
- 如果已登录，展示当前角色名称

##### 所需API

无

---

## 2.3 菜单与权限变动

| 菜单路径 | 所需权限码 | 可见角色 |
| --- | --- | --- |
| 员工管理 > 员工列表 | `employee:list` | 系统管理员、HR、部门主管 |
| 员工管理 > 新增员工 | `employee:add` | 系统管理员、HR |
| 员工管理 > 员工详情 | `employee:detail` | 所有角色 |
| 员工管理 > 编辑员工 | `employee:edit` | 系统管理员、HR、部门主管 |
| 员工管理 > 删除员工 | `employee:delete` | 系统管理员、HR |
| 薪资管理 > 薪资列表 | `salary:list` | 系统管理员、HR、财务 |
| 薪资管理 > 薪资查看 | `salary:view` | 系统管理员、HR、财务、普通员工（仅自己） |
| 薪资管理 > 薪资审核 | `salary:audit` | 系统管理员、财务 |
| 审批管理 | `approval:process` | 系统管理员、HR、部门主管 |
| 考勤管理 > 打卡 | `attendance:clock` | 所有角色 |
| 考勤管理 > 考勤列表 | `attendance:list` | 系统管理员、HR、部门主管、普通员工（仅自己） |
| 考勤管理 > 考勤管理 | `attendance:manage` | 系统管理员、HR |
| 组织架构 > 部门管理 | `org:manage` | 系统管理员、HR |
| 组织架构 > 职位管理 | `org:manage` | 系统管理员、HR |
| 系统管理 > 角色管理 | `role:manage` | 系统管理员 |
| 系统管理 > 系统配置 | `system:config` | 系统管理员 |
| 系统管理 > 数据备份 | `system:backup` | 系统管理员 |

---

## 2.4 模块划分与工作量评估

| 模块 | 细节 | 开发 | 联调 | 自测 | 前端 | 后端 |
| --- | --- | --- | --- | --- | --- | --- |
| 角色管理页 | 列表 + 分页 + 新增/编辑弹窗 + 权限分配树 + 字段权限配置 | 2 | 0.5 | 0.5 | - | 陆博 |
| 用户角色分配 | 角色选择弹窗 + 分配确认 | 0.5 | 0.5 | 0.5 | - | - |
| 全局权限控制 | 菜单/按钮/路由守卫 + 工具函数 + 指令 | 1.5 | 0.5 | 0.5 | - | - |
| 数据范围适配 | 各业务页面 dataScope 适配 | 1 | 0.5 | 0.5 | - | - |
| 字段级权限适配 | 员工详情/编辑页字段控制 + 工具函数 | 1 | 0.5 | 0.5 | - | - |
| 403 页面 | 无权限提示页 | 0.5 | 0 | 0 | - | - |

\[20260713~~20260714\] 系分编写
\[20260714~~20260715\] 系分评审
\[20260716~~20260718\] 实现角色管理页
\[20260718~~20260719\] 实现全局权限控制（菜单/按钮/路由守卫）
\[20260719~~20260721\] 实现用户角色分配 + 数据范围适配 + 字段权限适配
\[20260721~~20260725\] 联调
\[20260725~~20260727\] 测试

# 3. 监控和埋点

- 角色新增/编辑/删除操作埋点
- 用户角色分配操作埋点
- 403 页面访问埋点（记录触发页面和用户角色）
- 权限拒绝埋点（记录被拦截的 API 路径和缺失权限码）

# 4. 发布计划

- 预发验证：5 个角色逐一切换验证菜单可见性、按钮可见性、数据范围、字段权限
- 灰度发布：先在系统管理员角色下验证角色管理功能
- 回滚方案：可独立回滚前端，后端向下兼容

# 5. 其他

## 5.1 风险评估

- 权限矩阵复杂度较高（5 个角色 × 16 个权限码 × 10+ 页面），需逐角色、逐页面验证
- 字段权限 JSON 配置若格式错误会导致前端解析失败，需在前端做容错处理（try-catch + 降级为全隐藏）
- 路由守卫中的权限加载是异步的，需处理加载态和失败态
- 权限变更后（如管理员修改了某角色的权限码），已登录用户不会立即生效，需考虑是否需要实时推送（WebSocket）或要求用户重新登录

## 5.2 前后端约定

| 约定项 | 说明 |
| --- | --- |
| 权限码格式 | `模块:操作`，如 `employee:list` |
| 通配符 | `*:*:*` 代表超级管理员，拥有一切权限 |
| 权限加载时机 | 登录成功后立即调用 `/api/permission/current` |
| 菜单控制 | 前端根据 `permissionCodes` 数组自主判断，不再每次请求服务端 |
| 后端兜底 | 即使前端展示了按钮，后端三层防线仍然会拦截无权限的 API 调用 |
| 字段权限配置 | 存储在 `role.fieldPermissions` JSON 字段中，key 为字段名，value 包含 `viewable` 和 `editable` 角色 ID 数组 |

## 5.3 变更记录

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-13 | 1.0 | 初稿 | 陆博 |

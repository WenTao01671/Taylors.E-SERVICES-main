# Taylors E-Services 后端仓库分析（中文）

## 1. 仓库定位与技术栈
- 这是一个 **Spring Boot 3** 的后端 API 项目，面向高校场景的 e-services（签证、体检、预约、个人资料、文件管理、认证与 2FA）。
- 构建工具是 **Maven**，核心依赖包含：
  - `spring-boot-starter-web`（REST API）
  - `spring-boot-starter-data-jpa`（数据库 ORM）
  - `spring-boot-starter-security` + JWT（鉴权）
  - `spring-boot-starter-mail`（邮件）
  - `googleauth` + `zxing`（Google Authenticator/TOTP 与二维码）
- 默认数据库为 MySQL（`eservices_db`），服务端口 `9090`。

## 2. 代码分层结构
项目结构清晰，按典型 Spring 分层组织：

- `controller/`：对外 API 入口（Auth、2FA、Profile、Visa、Medical、Appointment、StudentFile 等）
- `service/`：业务逻辑（签证流程推进、体检流程、文件上传、密码重置、2FA、邮件通知）
- `repository/`：JPA 数据访问层
- `entity/`：领域模型（`User`, `VisaApplication`, `MedicalExamination`, `Appointment`, `StudentFile` 等）
- `security/`：JWT 工具、过滤器、Spring Security 配置
- `config/`：CORS、文件存储与启动初始化数据
- `dto/`：Profile 相关 DTO（部分模块仍直接使用 `Map<String,Object>`）

## 3. 主要业务域解读

### 3.1 认证与会话（Auth + JWT + Refresh Token）
- 登录流程是“密码成功后强制 OTP”模式：
  1. `/api/auth/login` 校验账号密码
  2. 发送邮箱 OTP
  3. `/api/auth/login/verify-otp` 成功后签发 access token + refresh token
- 提供 `/api/auth/refresh` 续签 access token，refresh token 存在 `users` 表字段中。
- 提供注册、注册 OTP 验证、登出、忘记密码/重置密码等接口。

### 3.2 双因子认证（2FA）
- 支持两种方式：
  - Email OTP
  - Google Authenticator（TOTP）
- 能生成 GA secret 与 QR code（base64），并能启用/禁用 2FA。
- 用户实体维护 `twoFactorEnabled`, `twoFactorMethod`, `twoFactorSecret`, `emailOtp` 等字段。

### 3.3 签证流程（Visa）
- `VisaApplication` 对象覆盖签证全流程状态：申请创建、材料提交、EMGS 处理、VAL、移民环节等。
- 内置 `updateProgress()` 通过里程碑日期计算进度百分比。
- 与 `MedicalExamination` 建立关联，用于决定是否可提交 EMGS。
- Staff 端可以推进状态、填写处理信息并触发通知邮件。

### 3.4 体检流程（Medical）
- `MedicalExamination` 记录预约时间、检查项目、结果、是否提交 EMGS、有效期等。
- 支持更新 tests（X-ray/血检/尿检）、提交通过/不通过结果、提交 EMGS。
- 同样使用进度计算，状态机较清晰（PENDING/SCHEDULED/COMPLETED/PASSED/FAILED/...）。

### 3.5 预约系统（Appointment）
- `Appointment` 支持多类型预约（医疗/办公室咨询/材料提交/面签等）。
- 提供学生侧操作（book/confirm/cancel/reschedule/upcoming）和 staff 侧管理（全量查询、状态更新、创建时段、统计）。
- 实体含时间、地点、提醒、改期次数、备注等完整字段。

### 3.6 文件管理与个人资料
- `StudentFileController` 提供上传、列表、下载、查询、删除。
- Profile 模块支持查看/更新个人资料、改密码、头像上传、登录历史。

## 4. 安全机制观察

### 已有优点
- 使用 Spring Security + JWT 无状态会话。
- Access Token 与 Refresh Token 分离。
- 强制 OTP 登录（提高账号安全性）。
- 角色隔离（`/api/student/**` 与 `/api/staff/**`）基础策略已设置。

### 主要风险（建议优先修复）
1. **配置文件中存在明文敏感信息**（数据库密码、邮箱账号/授权码、JWT secret）。建议迁移到环境变量或 secret manager，并立即轮换。  
2. **`@CrossOrigin(origins = "*")` 与全局 CORS 需收敛**，生产环境应限制可信前端域名。  
3. 大量接口返回 `Map<String,Object>`，缺少统一响应模型与输入校验（部分 DTO 化未完成），容易导致 API 契约漂移。  
4. 业务状态普遍使用字符串，建议逐步替换为 enum + 状态转换约束，减少拼写与非法状态风险。  
5. 部分代码仍有 `System.out.println` 调试输出，建议统一使用结构化日志。  

## 5. 工程质量与可维护性评价

### 当前亮点
- 领域模型覆盖完整，业务流程具备真实场景深度。
- 分层清楚、命名可读性较高。
- 邮件通知、2FA、签证+体检联动等功能体现了系统闭环能力。

### 改进方向
- 引入更系统的 DTO/Mapper 体系，减少 controller/service 中的手工 map 转换。
- 增加单元测试与集成测试覆盖（当前测试样例较少）。
- 完善异常体系（全局异常处理 + 错误码）与 API 文档（OpenAPI/Swagger）。
- 若后续并发增长，建议把“流程推进+通知”拆分事件化处理（消息队列/异步任务）。

## 6. 快速上手建议
1. 先检查并替换 `application.properties` 中敏感项。  
2. 本地准备 MySQL，创建 `eservices_db` 与对应用户。  
3. 运行：`./mvnw spring-boot:run`。  
4. 先从 `/api/auth/register` -> `/api/auth/register/verify-otp` -> `/api/auth/login` -> `/api/auth/login/verify-otp` 跑通登录链路。  
5. 再联调业务主线：Visa 创建 -> Medical 预约/结果 -> EMGS 提交 -> Appointment/Profile/File 模块。  

## 7. 总结
这是一个功能覆盖面较大的校园国际学生服务后端，已经具备“身份认证 + 多业务流程 + staff/student 分角色”的完整雏形。短期最关键的不是“加新功能”，而是先完成 **安全加固（敏感配置、CORS）** 与 **接口契约治理（DTO/校验/异常）**，这样后续迭代会更稳。

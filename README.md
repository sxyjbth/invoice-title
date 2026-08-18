# 发票抬头维护与展示

项目包含钉钉员工端、财务管理端和单体 Spring Boot 后端。应用固定复用已有 MySQL；项目专用的 JDK、Node 和 Maven 安装在 `.runtime` 目录，不修改系统全局配置，也不会下载、启动或停止 MySQL。

## 项目结构

- `frontend/employee-h5`：员工端，Vue 3 + Vite + Element Plus
- `frontend/finance-admin`：财务端，Vue 3 + Vite + Element Plus
- `backend`：Java 21 + Spring Boot 4 + MyBatis + Springdoc OpenAPI
- `scripts`：依赖下载、启动、停止和状态检查脚本
- `.runtime`：项目专用运行时、数据和日志，不提交仓库

## 本地启动

```powershell
.\scripts\bootstrap.ps1
.\scripts\start-all.ps1
```

本地默认连接 `root/root@127.0.0.1:3306/invoice_title`。需要使用其他凭据时，通过环境变量覆盖；启动脚本只检查连接地址是否可达，不会管理 MySQL：

```powershell
$env:INVOICE_MYSQL_HOST = '127.0.0.1'
$env:INVOICE_MYSQL_PORT = '3306'
$env:INVOICE_MYSQL_DATABASE = 'invoice_title'
$env:INVOICE_MYSQL_USERNAME = '你的用户名'
$env:INVOICE_MYSQL_PASSWORD = '你的密码'
.\scripts\bootstrap.ps1
.\scripts\start-all.ps1
```

首次启动可通过环境变量创建唯一超级管理员；后续启动不会覆盖数据库中已修改的密码：

```powershell
$env:INVOICE_SUPER_ADMIN_USERNAME = 'superadmin'
$env:INVOICE_SUPER_ADMIN_PASSWORD = '请替换为强密码'
$env:INVOICE_SUPER_ADMIN_DISPLAY_NAME = '超级管理员'
```

## 双企业钉钉配置

生产密钥不写入 `application.yml`，只通过生产环境变量注入。项目按下列变量名绑定到 `sebo.dingtalk.organizations`：

```dotenv
SEBO_DINGTALK_ENABLED=true

# 企业 0：赛宝
SEBO_DINGTALK_ORGANIZATIONS_0_CORP_CODE=sebo
SEBO_DINGTALK_ORGANIZATIONS_0_CORP_NAME=赛宝绿创能源技术（上海）有限公司
SEBO_DINGTALK_ORGANIZATIONS_0_CORP_ID=<赛宝CorpId>
SEBO_DINGTALK_ORGANIZATIONS_0_APP_ID=<赛宝AppId>
SEBO_DINGTALK_ORGANIZATIONS_0_AGENT_ID=<赛宝AgentId>
SEBO_DINGTALK_ORGANIZATIONS_0_CLIENT_ID=<赛宝ClientId>
SEBO_DINGTALK_ORGANIZATIONS_0_CLIENT_SECRET=<赛宝ClientSecret>

# 企业 1：瓦尔登
SEBO_DINGTALK_ORGANIZATIONS_1_CORP_CODE=walden
SEBO_DINGTALK_ORGANIZATIONS_1_CORP_NAME=瓦尔登环境科学研究院（北京）有限公司
SEBO_DINGTALK_ORGANIZATIONS_1_CORP_ID=<瓦尔登CorpId>
SEBO_DINGTALK_ORGANIZATIONS_1_APP_ID=<瓦尔登AppId>
SEBO_DINGTALK_ORGANIZATIONS_1_AGENT_ID=<瓦尔登AgentId>
SEBO_DINGTALK_ORGANIZATIONS_1_CLIENT_ID=<瓦尔登ClientId>
SEBO_DINGTALK_ORGANIZATIONS_1_CLIENT_SECRET=<瓦尔登ClientSecret>
```

Spring Boot 默认不能把上述 `_0_`、`_1_` 环境变量可靠绑定为 List，项目已在启动绑定前将其转换为索引配置，并有自动化测试覆盖。企业列表非空时优先使用列表，不使用旧单企业字段。

两个钉钉工作台入口需要分别带上业务编码：

```text
https://<员工端域名>/?corpCode=sebo
https://<员工端域名>/?corpCode=walden
```

员工端先从后端读取对应企业的非敏感 `corpId`，再申请免登码；ClientSecret 不会返回前端。服务端使用 `corp_code + ding_user_id` 识别员工，同一个钉钉 `userid` 出现在两家企业时保存为两条独立身份。部门、员工授权和二维码也使用企业隔离后的身份，避免串权。

## 通讯录同步与重试

同步顺序固定为：

```text
赛宝 access_token → 赛宝部门 → 赛宝员工
瓦尔登 access_token → 瓦尔登部门 → 瓦尔登员工
两家全部成功 → 单事务写入数据库
```

- 单个正式通讯录请求前默认等待 120 毫秒。
- 单请求最多执行 3 次，临时限流后等待 1200 毫秒。
- 整个双企业同步最多执行 3 轮，临时限流后等待 2 秒并从赛宝重新开始。
- 重试只识别 `90002`、`90018`、“请求被暂时限制”和“qps流控”。
- 密钥错误、权限错误、普通数据库错误不会盲目重试；数据库写入失败会整体回滚。
- 单接口在内外两层都持续限流时，理论上最多请求 9 次；三轮仍失败后记录失败，等待下一次定时或人工同步。

后端默认每小时整点同步。财务或超级管理员也可调用 `POST /api/admin/directory/sync` 手动同步，结果写入 `ding_directory_sync_log`。首次开放员工端前应先完成一次同步。

## 固定端口

| 服务 | 端口 |
|---|---:|
| 员工端 | 24173 |
| 财务端 | 24175 |
| 后端 API / Swagger | 28082 |
| 本机 MySQL（外部服务） | 3306 |

启动脚本会先检查端口，遇到占用时直接停止，不关闭其他项目的进程。

## 验证

```powershell
# 后端测试
$env:JAVA_HOME = "$PWD\.runtime\jdk\jdk-21.0.11+10"
.\.runtime\maven\apache-maven-3.9.16\bin\mvn.cmd `
  "-Dmaven.repo.local=$PWD\.runtime\maven-repository" `
  -s .\scripts\maven-settings.xml -f .\backend\pom.xml test

# 前端测试与构建
.\.runtime\pnpm\node_modules\.bin\pnpm.cmd run test:frontend
.\.runtime\pnpm\node_modules\.bin\pnpm.cmd run build:frontend
```

Swagger：`http://127.0.0.1:28082/swagger-ui.html`

所有业务列表使用服务端分页。数据库迁移脚本为表、字段及枚举状态添加中文注释；Swagger DTO 对入参和返回字段提供说明。

## 生产发布

生产服务器使用 Git 提交作为唯一发布来源，源码工作区固定为
`/opt/invoice-title/source`，生产密钥只保存在权限为 `600` 的
`/opt/invoice-title/config/invoice-title.env`，禁止提交到仓库。

```bash
# 拉取并固定到待发布提交
cd /opt/invoice-title/source
git fetch --prune origin
git checkout --detach <commit-sha>

# 使用 /opt/invoice-title/runtime 中的项目专属 Node、pnpm、Maven 构建 release
sudo -u invoice_title ./deploy/server-build-release.sh

# 以 root 原子切换 release；健康检查失败将自动回滚
sudo ./deploy/server-activate-release.sh <release目录名称>
```

同一 IP 下的生产入口：

- 员工端：`http://<服务器IP>/invoice/employee`
- 财务端：`http://<服务器IP>/invoice/finance`
- API：`http://<服务器IP>/invoice/api/`

Nginx 只在现有站点中包含 `deploy/nginx/invoice-title.conf` 这个 location
片段，不新增默认站点、不替换原有 `/` 和 `/api/`，从而与 sebo-meal 隔离。

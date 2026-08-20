# AGENTS.md

## 本地快速启动

所有命令都从仓库根目录执行。项目运行时和日志统一放在 `.runtime/`，不要改用系统全局的 Java、Maven 或 Node。MySQL 是唯一例外：固定复用本机已有服务，项目不得下载、启动或停止 MySQL。

### 1. “本地启动”固定执行约定

当用户要求“本地启动”“启动一下本地项目”或同义操作时，按以下顺序直接执行，不要先做一系列状态、端口、日志和历史进程排查：

1. 保留已经运行的员工端和财务端 Vite 进程，让前端继续通过 HMR 热更新；不得为了重启后端而停止或重启前端。
2. 直接定位监听 `28082` 的本项目后端进程，确认其 Java 可执行文件位于本仓库 `.runtime/jdk/` 下后，只停止该后端进程。
3. 使用仓库 `.runtime/` 内置 JDK 和 Maven 重新打包当前后端代码，然后重新启动后端。
4. 后端重启后只做最小验证：员工端、财务端和 `/v3/api-docs` 返回 HTTP 200，后端日志出现 `Started InvoiceTitleApplication`。
5. 只有前端端口原本没有监听时，才补启动对应的 Vite 服务；已运行的前端绝不重复启动。
6. 整个过程不得启动、停止或修改本机 MySQL，不得使用 `start-all.ps1` 或 `stop-all.ps1` 重启整套环境。

本约定优先于下文的一般排障说明。只有上述直接重启失败时，才进入日志和端口排障。

### 2. 固定端口

固定端口：

- 员工端：`24173`
- 财务端：`24175`
- 后端 API：`28082`
- 本机 MySQL：`3306`（外部服务，项目不得启动、停止或替换）

本地访问地址：

- 财务端：<http://127.0.0.1:24175/>
- 员工端：<http://127.0.0.1:24173/>
- Swagger：<http://127.0.0.1:28082/swagger-ui.html>

### 3. 首次初始化完整环境

本项目固定复用本机 `127.0.0.1:3306`，数据库为 `invoice_title`，本地开发连接账号为 `root/root`。脚本没有项目内置 MySQL 分支。

仅在 `.runtime` 依赖尚未准备、且三个应用都从未启动过时，才允许执行完整初始化：

```powershell
$env:INVOICE_MYSQL_HOST = '127.0.0.1'
$env:INVOICE_MYSQL_PORT = '3306'
$env:INVOICE_MYSQL_DATABASE = 'invoice_title'
$env:INVOICE_MYSQL_USERNAME = 'root'
$env:INVOICE_MYSQL_PASSWORD = 'root'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\start-all.ps1"
```

如果 `.runtime` 依赖不完整，先执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\bootstrap.ps1"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\start-all.ps1"
```

初始化通常需要 1-3 分钟，因为脚本会离线打包 Spring Boot 后端。命令超时至少设置为 180 秒。初始化完成后确认两个前端和 `/v3/api-docs` 均返回 HTTP 200。

在受限执行环境中，`Get-NetTCPConnection` 和后台服务启动可能需要提升权限；应直接对上述仓库脚本申请权限，不要绕过端口检查，也不要停止不属于本项目的进程。

### 4. 异常恢复

直接重启失败后才能排障。禁止为了释放端口直接结束未知进程；只有监听端口和可执行文件路径都能确认属于本项目时，才可处理残留进程。前端单个服务缺失时只补启动缺失服务，不停止另一个仍在热更新的前端。禁止停止本机 `3306` MySQL。

### 5. 本机数据库连接检查

启动前可使用本机已有的 MySQL 客户端验证 `3306` 数据库：

```powershell
mysql --protocol=TCP --host=127.0.0.1 --port=3306 --user=root --password=root --batch --skip-column-names -e "SELECT VERSION(); SHOW DATABASES LIKE 'invoice_title'; SELECT COUNT(*) FROM invoice_title.finance_user;"
```

该检查只读。若失败，不要修改本机 MySQL 用户；先确认本机 MySQL 服务、`root` 凭据和 `invoice_title` 数据库状态。

### 6. 本地演示登录

- 财务端账号：`admin`
- 登录密码以本机 `3306` 的 `invoice_title.finance_user.password_hash` 为准；仓库不保存明文密码

`password_hash` 是 BCrypt 单向摘要，不能与明文密码直接比较或反推。不要把生产账号、密码或钉钉密钥写入仓库。

### 7. 排障日志

优先查看：

```powershell
Get-Content ".\.runtime\logs\backend.out.log" -Tail 100
Get-Content ".\.runtime\logs\backend.error.log" -Tail 100
Get-Content ".\.runtime\logs\admin.out.log" -Tail 50
Get-Content ".\.runtime\logs\admin.error.log" -Tail 50
```

启动成功的最终判断是：端口状态为 `RUNNING`，财务端和 Swagger HTTP 检查均为 200；不能只依据启动脚本退出码。

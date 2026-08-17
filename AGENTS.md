# AGENTS.md

## 本地快速启动

所有命令都从仓库根目录执行。项目运行时、数据和日志统一放在 `.runtime/`，不要改用系统全局的 Java、Maven、Node、MySQL、Redis 或 Nacos。

### 1. 先检查状态

固定端口：

- 员工端：`24173`
- 财务端：`24175`
- 后端 API：`28082`
- Nacos：`28848`
- 本机 MySQL：`3306`（外部服务，项目不得启动或停止第二个 MySQL）
- Redis：`26379`

检查状态前先指定本机 MySQL 端口，否则旧版状态脚本会按 `23306` 检查：

```powershell
$env:INVOICE_MYSQL_PORT = '3306'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\status.ps1"
```

如果六项均为 `RUNNING`，不要重复启动，直接访问：

- 财务端：<http://127.0.0.1:24175/>
- 员工端：<http://127.0.0.1:24173/>
- Swagger：<http://127.0.0.1:28082/swagger-ui.html>

### 2. 一键启动完整环境

本项目固定复用本机 `127.0.0.1:3306`，数据库为 `invoice_title`，本地开发连接账号为 `root/root`。禁止省略 `-UseLocalMySql`，否则脚本会额外启动项目内置的 `23306` MySQL。

如果所有项目服务均为 `STOPPED`：

```powershell
$env:INVOICE_MYSQL_HOST = '127.0.0.1'
$env:INVOICE_MYSQL_PORT = '3306'
$env:INVOICE_MYSQL_DATABASE = 'invoice_title'
$env:INVOICE_MYSQL_USERNAME = 'root'
$env:INVOICE_MYSQL_PASSWORD = 'root'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\start-all.ps1" -UseLocalMySql
```

首次准备运行时、或 `.runtime` 依赖不完整时，先执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\bootstrap.ps1" -UseLocalMySql
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\start-all.ps1" -UseLocalMySql
```

启动通常需要 1-3 分钟，因为脚本会离线打包 Spring Boot 后端。命令超时至少设置为 180 秒。启动完成后必须再次运行 `status.ps1`，并确认财务端和 Swagger 均返回 HTTP 200。

在受限执行环境中，`Get-NetTCPConnection` 和后台服务启动可能需要提升权限；应直接对上述仓库脚本申请权限，不要绕过端口检查，也不要停止不属于本项目的进程。

### 3. 部分启动时恢复

如果状态显示部分服务 `RUNNING`、部分服务 `STOPPED`，先停止本项目拥有的进程，再完整启动：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\stop-all.ps1"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\status.ps1"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\start-all.ps1" -UseLocalMySql
```

上述命令必须在同一个 PowerShell 会话中保留第 2 节的 `INVOICE_MYSQL_*` 环境变量。`stop-all.ps1` 只能停止本项目进程，禁止停止本机 `3306` MySQL。禁止为了释放端口直接结束未知进程；只有进程可执行文件位于本仓库 `.runtime/` 下，且 PID 与 `.runtime/pids/*.pid` 一致时，才可处理残留进程。

### 4. 本机数据库连接检查

启动前可用项目内 MySQL 客户端验证 `3306` 数据库：

```powershell
$mysql = ".\.runtime\mysql\mysql-8.4.10-winx64\bin\mysql.exe"
& $mysql '--protocol=TCP' '--host=127.0.0.1' '--port=3306' '--user=root' '--password=root' '--batch' '--skip-column-names' '-e' "SELECT VERSION(); SHOW DATABASES LIKE 'invoice_title'; SELECT COUNT(*) FROM invoice_title.finance_user;"
```

该检查只读。若失败，不要启动项目内置 MySQL，也不要修改本机 MySQL 用户；先确认本机 `root` 凭据和 `invoice_title` 数据库状态。

### 5. 本地演示登录

- 财务端账号：`admin`
- 登录密码以本机 `3306` 的 `invoice_title.finance_user.password_hash` 为准；仓库不保存明文密码

`password_hash` 是 BCrypt 单向摘要，不能与明文密码直接比较或反推。不要把生产账号、密码或钉钉密钥写入仓库。

### 6. 排障日志

优先查看：

```powershell
Get-Content ".\.runtime\logs\backend.out.log" -Tail 100
Get-Content ".\.runtime\logs\backend.error.log" -Tail 100
Get-Content ".\.runtime\logs\admin.out.log" -Tail 50
Get-Content ".\.runtime\logs\admin.error.log" -Tail 50
```

启动成功的最终判断是：端口状态为 `RUNNING`，财务端和 Swagger HTTP 检查均为 200；不能只依据启动脚本退出码。

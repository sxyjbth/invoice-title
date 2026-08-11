#!/usr/bin/env bash
set -Eeuo pipefail

# 在服务器的 Git 工作区构建不可变 release。脚本只写 invoice-title 专属目录，
# 不安装系统级 Node/Maven，也不触碰其他项目的进程或文件。
umask 027

APP_HOME="${INVOICE_APP_HOME:-/opt/invoice-title}"
SOURCE_DIR="${INVOICE_SOURCE_DIR:-${APP_HOME}/source}"
NODE_HOME="${INVOICE_NODE_HOME:-${APP_HOME}/runtime/node}"
PNPM_HOME="${INVOICE_PNPM_HOME:-${APP_HOME}/runtime/pnpm}"
MAVEN_HOME="${INVOICE_MAVEN_HOME:-${APP_HOME}/runtime/maven}"

export PATH="${NODE_HOME}/bin:${PNPM_HOME}/node_modules/.bin:${MAVEN_HOME}/bin:${PATH}"

for command_name in git node pnpm mvn; do
    command -v "${command_name}" >/dev/null 2>&1 || {
        echo "缺少构建命令: ${command_name}" >&2
        exit 1
    }
done

cd "${SOURCE_DIR}"
commit_id="$(git rev-parse HEAD)"
release_id="$(date -u +%Y%m%d%H%M%S)-${commit_id:0:12}"
release_dir="${APP_HOME}/releases/${release_id}"
staging_dir="${APP_HOME}/releases/.${release_id}.staging"

if [[ -e "${release_dir}" ]]; then
    echo "${release_dir}"
    exit 0
fi

rm -rf -- "${staging_dir}"
mkdir -p "${staging_dir}/backend" \
    "${staging_dir}/frontend/employee-h5" \
    "${staging_dir}/frontend/finance-admin"

# 锁定依赖版本，并在构建制品前执行全部前后端测试。
pnpm install --frozen-lockfile
pnpm run test:frontend

VITE_PUBLIC_BASE=/invoice/employee/ \
VITE_API_BASE_PREFIX=/invoice \
pnpm --filter @invoice-title/employee-h5 build

VITE_PUBLIC_BASE=/invoice/finance/ \
VITE_API_BASE_PREFIX=/invoice \
pnpm --filter @invoice-title/finance-admin build

mvn -f backend/pom.xml --batch-mode clean verify

install -m 0640 backend/target/invoice-title-service-0.1.0-SNAPSHOT.jar \
    "${staging_dir}/backend/invoice-title-service.jar"
cp -a frontend/employee-h5/dist/. "${staging_dir}/frontend/employee-h5/"
cp -a frontend/finance-admin/dist/. "${staging_dir}/frontend/finance-admin/"
printf '%s\n' "${commit_id}" > "${staging_dir}/GIT_COMMIT"

# 同一文件系统内原子改名，避免 current 指向复制到一半的目录。
mv -- "${staging_dir}" "${release_dir}"
echo "${release_dir}"

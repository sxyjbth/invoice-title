#!/usr/bin/env bash
set -Eeuo pipefail

# 在服务器的 Git 工作区构建不可变 release。脚本只写 invoice-title 专属目录，
# 不安装系统级 Node/Maven，也不触碰其他项目的进程或文件。
umask 027

APP_HOME="${INVOICE_APP_HOME:-/opt/invoice-title}"
APP_USER="${INVOICE_APP_USER:-invoice_title}"
APP_GROUP="${INVOICE_APP_GROUP:-invoice_title}"
SOURCE_DIR="${INVOICE_SOURCE_DIR:-${APP_HOME}/source}"
NODE_HOME="${INVOICE_NODE_HOME:-${APP_HOME}/runtime/node}"
PNPM_HOME="${INVOICE_PNPM_HOME:-${APP_HOME}/runtime/pnpm}"
MAVEN_HOME="${INVOICE_MAVEN_HOME:-${APP_HOME}/runtime/maven}"
PNPM_STORE="${APP_HOME}/runtime/pnpm-store"
MAVEN_REPOSITORY="${APP_HOME}/runtime/maven-repository"
JAVA_HOME="${INVOICE_JAVA_HOME:-${APP_HOME}/runtime/jdk-21}"

export PATH="${NODE_HOME}/bin:${PNPM_HOME}/node_modules/.bin:${MAVEN_HOME}/bin:${PATH}"
export JAVA_HOME
# 服务器同时承载既有项目，限制单次构建的堆内存，避免挤压线上服务。
export NODE_OPTIONS="${NODE_OPTIONS:---max-old-space-size=768}"
export MAVEN_OPTS="${MAVEN_OPTS:--Xms128m -Xmx768m}"

[[ -x "${JAVA_HOME}/bin/java" && -x "${JAVA_HOME}/bin/javac" ]] || {
    echo "Java 21 不可用: ${JAVA_HOME}" >&2
    exit 1
}

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

# 已打开的浏览器页面仍可能在路由切换时请求旧版的 Vite 分块文件。
# 新 release 保留最近两代的哈希资源，避免连续发版后这些请求瞬间变成 404。
current_release="$(readlink -f -- "${APP_HOME}/current" 2>/dev/null || true)"
previous_release=""
if [[ -d "${APP_HOME}/releases" ]]; then
    while IFS= read -r candidate_name; do
        candidate_release="$(readlink -f -- "${APP_HOME}/releases/${candidate_name}")"
        [[ "${candidate_release}" == "${current_release}" ]] && continue
        previous_release="${candidate_release}"
        break
    done < <(find "${APP_HOME}/releases" -mindepth 1 -maxdepth 1 -type d ! -name '.*' -printf '%f\n' | sort -r)
fi

copy_release_assets() {
    local release_root="$1"
    local app_name="$2"
    local release_frontend="${release_root}/frontend/${app_name}"
    local source_assets="${release_frontend}/assets"
    local target_assets="${staging_dir}/frontend/${app_name}/assets"
    local asset_manifest="${release_frontend}/.release-assets"

    [[ -n "${release_root}" && -d "${source_assets}" ]] || return 0
    mkdir -p "${target_assets}"

    if [[ -f "${asset_manifest}" ]]; then
        while IFS= read -r relative_path; do
            [[ -n "${relative_path}" && "${relative_path}" != /* && "${relative_path}" != *..* ]] || continue
            [[ -f "${source_assets}/${relative_path}" ]] || continue
            mkdir -p "${target_assets}/$(dirname -- "${relative_path}")"
            cp -a "${source_assets}/${relative_path}" "${target_assets}/${relative_path}"
        done < "${asset_manifest}"
    else
        # 兼容首次启用资源清单前创建的 release。
        cp -a "${source_assets}/." "${target_assets}/"
    fi
}

copy_current_assets() {
    local app_name="$1"
    copy_release_assets "${previous_release}" "${app_name}"
    copy_release_assets "${current_release}" "${app_name}"
}

rm -rf -- "${staging_dir}"
mkdir -p "${staging_dir}/backend" \
    "${staging_dir}/frontend/employee-h5" \
    "${staging_dir}/frontend/finance-admin" \
    "${PNPM_STORE}" "${MAVEN_REPOSITORY}"

# 锁定依赖版本，并在构建制品前执行全部前后端测试。
pnpm install --frozen-lockfile --store-dir "${APP_HOME}/runtime/pnpm-store"
pnpm run test:frontend

VITE_PUBLIC_BASE=/invoice/employee/ \
VITE_API_BASE_PREFIX=/invoice \
pnpm --filter @invoice-title/employee-h5 build

VITE_PUBLIC_BASE=/invoice/finance/ \
VITE_API_BASE_PREFIX=/invoice \
pnpm --filter @invoice-title/finance-admin build

mvn -f backend/pom.xml --batch-mode \
    "-Dmaven.repo.local=${APP_HOME}/runtime/maven-repository" \
    -s scripts/maven-settings.xml clean verify

install -m 0640 backend/target/invoice-title-service-0.1.0-SNAPSHOT.jar \
    "${staging_dir}/backend/invoice-title-service.jar"
copy_current_assets employee-h5
copy_current_assets finance-admin
cp -a frontend/employee-h5/dist/. "${staging_dir}/frontend/employee-h5/"
cp -a frontend/finance-admin/dist/. "${staging_dir}/frontend/finance-admin/"
find frontend/employee-h5/dist/assets -type f -printf '%P\n' | sort \
    > "${staging_dir}/frontend/employee-h5/.release-assets"
find frontend/finance-admin/dist/assets -type f -printf '%P\n' | sort \
    > "${staging_dir}/frontend/finance-admin/.release-assets"
printf '%s\n' "${commit_id}" > "${staging_dir}/GIT_COMMIT"

# Nginx 只需读取公开静态资源；后端 JAR 和 release 元数据继续保持私有权限。
chmod 0755 "${staging_dir}" "${staging_dir}/frontend"
find "${staging_dir}/frontend" -type d -exec chmod 0755 {} +
find "${staging_dir}/frontend" -type f -exec chmod 0644 {} +
# systemd 以专用账号运行后端；构建用户可能是 root，交付前统一 release 归属。
chown -R "${APP_USER}:${APP_GROUP}" "${staging_dir}"

# 同一文件系统内原子改名，避免 current 指向复制到一半的目录。
mv -- "${staging_dir}" "${release_dir}"
echo "${release_dir}"

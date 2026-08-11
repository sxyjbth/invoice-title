#!/usr/bin/env bash
set -Eeuo pipefail

# 原子切换 release 并进行后端健康检查。切换或启动失败时自动恢复上一版本。
APP_HOME="${INVOICE_APP_HOME:-/opt/invoice-title}"
release_ref="${1:-}"

if [[ -z "${release_ref}" ]]; then
    echo "用法: $0 <release目录或release名称>" >&2
    exit 2
fi

if [[ "${release_ref}" = /* ]]; then
    release_dir="${release_ref}"
else
    release_dir="${APP_HOME}/releases/${release_ref}"
fi

release_dir="$(readlink -f -- "${release_dir}")"
case "${release_dir}" in
    "${APP_HOME}/releases/"*) ;;
    *) echo "release 必须位于 ${APP_HOME}/releases" >&2; exit 2 ;;
esac

test -f "${release_dir}/backend/invoice-title-service.jar"
test -f "${release_dir}/frontend/employee-h5/index.html"
test -f "${release_dir}/frontend/finance-admin/index.html"

current_link="${APP_HOME}/current"
previous_release=""
if [[ -L "${current_link}" ]]; then
    previous_release="$(readlink -f -- "${current_link}")"
fi

rollback_required=false

rollback() {
    set +e
    echo "发布失败，正在恢复上一版本: ${previous_release:-无}" >&2
    if [[ -n "${previous_release}" && -d "${previous_release}" ]]; then
        ln -sfn "${previous_release}" "${current_link}.rollback"
        mv -Tf "${current_link}.rollback" "${current_link}"
        systemctl restart invoice-title.service
    else
        rm -f -- "${current_link}"
        systemctl stop invoice-title.service
    fi
}

on_error() {
    status=$?
    if [[ "${rollback_required}" == true ]]; then
        rollback
    fi
    exit "${status}"
}
trap on_error ERR

ln -sfn "${release_dir}" "${current_link}.next"
mv -Tf "${current_link}.next" "${current_link}"
rollback_required=true
systemctl restart invoice-title.service

healthy=false
for _ in $(seq 1 30); do
    if curl --fail --silent --show-error --max-time 3 \
        http://127.0.0.1:28082/v3/api-docs >/dev/null; then
        healthy=true
        break
    fi
    sleep 2
done

if [[ "${healthy}" != true ]]; then
    echo "后端健康检查超时" >&2
    false
fi

rollback_required=false
trap - ERR
echo "已启用 release: ${release_dir}"

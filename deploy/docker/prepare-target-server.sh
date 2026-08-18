#!/usr/bin/env bash
set -Eeuo pipefail

# 仅准备 invoice-title 专属目录、账号和数据库；不启动服务、不修改共享 Nginx。
APP_HOME="/opt/invoice-title"
DATA_HOME="/data/invoice-title"
APP_USER="invoice_title"
APP_GROUP="invoice_title"
DB_NAME="invoice_title"
DB_USER="invoice_title_app"
MYSQL_CONTAINER="sebo-meal-mysql"

if [[ "${EUID}" -ne 0 ]]; then
    echo "必须使用 root 执行" >&2
    exit 1
fi

: "${INVOICE_DB_PASSWORD:?必须通过环境变量提供 INVOICE_DB_PASSWORD}"
if [[ ! "${INVOICE_DB_PASSWORD}" =~ ^[A-Za-z0-9]{24,128}$ ]]; then
    echo "INVOICE_DB_PASSWORD 必须是24至128位字母或数字" >&2
    exit 1
fi

docker inspect "${MYSQL_CONTAINER}" >/dev/null

if ! getent group "${APP_GROUP}" >/dev/null; then
    groupadd --system "${APP_GROUP}"
fi
if ! id "${APP_USER}" >/dev/null 2>&1; then
    useradd --system --gid "${APP_GROUP}" --home-dir "${APP_HOME}" --shell /usr/sbin/nologin "${APP_USER}"
fi

install -d -o root -g "${APP_GROUP}" -m 0750 \
    "${APP_HOME}" "${APP_HOME}/app" "${APP_HOME}/config" "${APP_HOME}/source"
install -d -o "${APP_USER}" -g "${APP_GROUP}" -m 0750 \
    "${DATA_HOME}" "${DATA_HOME}/imports" "${DATA_HOME}/backups"

docker exec -i "${MYSQL_CONTAINER}" sh -c \
    'MYSQL_PWD="$(cat /run/secrets/mysql_root_password)" exec mysql --default-character-set=utf8mb4 -uroot' <<SQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME}
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${INVOICE_DB_PASSWORD}';
ALTER USER '${DB_USER}'@'%' IDENTIFIED BY '${INVOICE_DB_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
  ON ${DB_NAME}.* TO '${DB_USER}'@'%';
FLUSH PRIVILEGES;
SQL

echo "invoice-title 部署前目录和空数据库准备完成；未启动任何服务。"

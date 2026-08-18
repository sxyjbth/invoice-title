from pathlib import Path
import re
import unittest


PROJECT_ROOT = Path(__file__).resolve().parents[2]


class DeploymentContractTest(unittest.TestCase):
    def test_docker_compose_is_project_scoped_and_does_not_publish_ports(self):
        compose = (PROJECT_ROOT / "deploy/docker/compose.production.yml").read_text(encoding="utf-8")

        self.assertIn("name: invoice-title", compose)
        self.assertIn("container_name: invoice-title-backend", compose)
        self.assertIn("container_name: invoice-title-web", compose)
        self.assertIn("/opt/invoice-title/config/invoice-title.env", compose)
        self.assertIn("/data/invoice-title/imports", compose)
        self.assertIn("name: sebo-meal-network", compose)
        self.assertIn("external: true", compose)
        self.assertIsNone(re.search(r"(?m)^\s*ports:\s*$", compose))
        self.assertNotIn("/opt/sebo-meal", compose)
        self.assertNotIn("/opt/zhaocai", compose)

    def test_backend_container_is_java21_non_root_and_health_checked(self):
        dockerfile = (PROJECT_ROOT / "deploy/docker/backend.Dockerfile").read_text(encoding="utf-8")
        compose = (PROJECT_ROOT / "deploy/docker/compose.production.yml").read_text(encoding="utf-8")

        self.assertIn("eclipse-temurin-21", dockerfile)
        self.assertIn("USER 10001:10001", dockerfile)
        self.assertIn("invoice-title-service.jar", dockerfile)
        self.assertIn("127.0.0.1:28082/v3/api-docs", compose)
        self.assertIn("read_only: true", compose)
        self.assertIn("pids_limit:", compose)
        self.assertIn("max-size: \"20m\"", compose)

    def test_container_base_images_use_the_server_reachable_registry_mirror(self):
        backend = (PROJECT_ROOT / "deploy/docker/backend.Dockerfile").read_text(encoding="utf-8")
        web = (PROJECT_ROOT / "deploy/docker/web.Dockerfile").read_text(encoding="utf-8")

        mirror = "m.daocloud.io/docker.io/library/"
        self.assertIn(f"FROM {mirror}maven:3.9.11-eclipse-temurin-21", backend)
        self.assertIn(f"FROM {mirror}eclipse-temurin:21-jre-jammy", backend)
        self.assertIn(f"FROM {mirror}node:24.11.1-alpine3.22", web)
        self.assertIn(f"FROM {mirror}nginx:1.28.0-alpine", web)

    def test_web_container_owns_invoice_paths_and_proxies_api_internally(self):
        dockerfile = (PROJECT_ROOT / "deploy/docker/web.Dockerfile").read_text(encoding="utf-8")
        nginx = (PROJECT_ROOT / "deploy/docker/nginx.conf").read_text(encoding="utf-8")

        self.assertIn("VITE_PUBLIC_BASE=/invoice/employee/", dockerfile)
        self.assertIn("VITE_PUBLIC_BASE=/invoice/finance/", dockerfile)
        self.assertIn("VITE_API_BASE_PREFIX=/invoice", dockerfile)
        self.assertIn("location ^~ /invoice/api/", nginx)
        self.assertIn("proxy_pass http://backend:28082/api/;", nginx)
        self.assertIn("location ^~ /invoice/employee/", nginx)
        self.assertIn("location ^~ /invoice/finance/", nginx)
        self.assertIn("try_files $uri $uri/ /invoice/employee/index.html;", nginx)
        self.assertIn("try_files $uri $uri/ /invoice/finance/index.html;", nginx)

    def test_shared_ingress_candidate_only_adds_invoice_routing(self):
        upstream = (PROJECT_ROOT / "deploy/nginx/invoice-title-docker-upstream.conf").read_text(encoding="utf-8")
        location = (PROJECT_ROOT / "deploy/nginx/invoice-title-docker-location.conf").read_text(encoding="utf-8")

        self.assertIn("upstream invoice_title_web", upstream)
        self.assertIn("server invoice-title-web:8080 resolve;", upstream)
        self.assertIn("location ^~ /invoice/", location)
        self.assertIn("proxy_pass http://invoice_title_web;", location)
        self.assertNotIn("listen ", upstream + location)
        self.assertNotIn("location / ", upstream + location)
        self.assertIsNone(re.search(r"location\s+(?:\^~\s+)?/api/", location))

    def test_employee_entry_publishes_invoice_share_metadata(self):
        html = (PROJECT_ROOT / "frontend/employee-h5/index.html").read_text(encoding="utf-8")

        self.assertIn('<meta property="og:title" content="发票抬头"', html)
        self.assertIn('<meta property="og:description"', html)
        self.assertIn('%BASE_URL%invoice-title-share.png', html)
        self.assertTrue((PROJECT_ROOT / "frontend/employee-h5/public/invoice-title-share.png").is_file())

    def test_server_prepare_script_is_scoped_and_refuses_to_start_services(self):
        script = (PROJECT_ROOT / "deploy/docker/prepare-target-server.sh").read_text(encoding="utf-8")

        self.assertIn('APP_HOME="/opt/invoice-title"', script)
        self.assertIn('DATA_HOME="/data/invoice-title"', script)
        self.assertIn('invoice_title_app', script)
        self.assertIn('invoice_title', script)
        self.assertNotIn("docker compose up", script)
        self.assertNotIn("systemctl restart", script)
        self.assertNotIn("nginx -s reload", script)
        self.assertNotIn("/opt/sebo-meal/app", script)
        self.assertNotIn("/opt/zhaocai", script)
        self.assertNotIn("`", script)

    def test_nginx_uses_isolated_ip_paths_without_replacing_existing_routes(self):
        config = (PROJECT_ROOT / "deploy/nginx/invoice-title.conf").read_text(encoding="utf-8")

        self.assertIn("location ^~ /invoice/api/", config)
        self.assertIn(
            "location = /invoice/employee {\n    alias /opt/invoice-title/current/frontend/employee-h5/index.html;",
            config,
        )
        self.assertIn(
            "location = /invoice/finance {\n    alias /opt/invoice-title/current/frontend/finance-admin/index.html;",
            config,
        )
        self.assertIn("proxy_pass http://127.0.0.1:28082/api/;", config)
        self.assertNotIn("listen ", config)
        self.assertNotIn("18080", config)

    def test_entry_pages_are_html_and_publish_an_isolated_invoice_favicon(self):
        config = (PROJECT_ROOT / "deploy/nginx/invoice-title.conf").read_text(encoding="utf-8")
        employee_html = (PROJECT_ROOT / "frontend/employee-h5/index.html").read_text(encoding="utf-8")
        finance_html = (PROJECT_ROOT / "frontend/finance-admin/index.html").read_text(encoding="utf-8")

        self.assertEqual(3, config.count("default_type text/html;"))
        self.assertIn("location = /invoice/employee/favicon.svg", config)
        self.assertIn("location = /invoice/finance/invoice-title-finance-icon-v1.svg", config)
        self.assertIn('rel="icon" type="image/svg+xml" href="%BASE_URL%favicon.svg"', employee_html)
        self.assertIn(
            'rel="icon" type="image/svg+xml" href="%BASE_URL%invoice-title-finance-icon-v1.svg"',
            finance_html,
        )
        self.assertTrue((PROJECT_ROOT / "frontend/employee-h5/public/favicon.svg").is_file())
        self.assertTrue(
            (PROJECT_ROOT / "frontend/finance-admin/public/invoice-title-finance-icon-v1.svg").is_file()
        )

    def test_backend_service_uses_java21_local_bind_and_isolated_user(self):
        service = (PROJECT_ROOT / "deploy/systemd/invoice-title.service").read_text(encoding="utf-8")

        self.assertIn("User=invoice_title", service)
        self.assertIn("EnvironmentFile=/opt/invoice-title/config/invoice-title.env", service)
        self.assertIn("/usr/lib/jvm/java-21-openjdk-amd64/bin/java", service)
        self.assertIn("--server.address=127.0.0.1", service)
        self.assertIn("--server.port=28082", service)

    def test_backend_uses_an_invoice_specific_session_cookie(self):
        config = (PROJECT_ROOT / "backend/src/main/resources/application.yml").read_text(encoding="utf-8")

        self.assertIn(
            "name: ${INVOICE_SESSION_COOKIE_NAME:INVOICE_TITLE_SESSION}",
            config,
        )

    def test_backend_service_depends_only_on_network_and_mysql(self):
        service = (PROJECT_ROOT / "deploy/systemd/invoice-title.service").read_text(encoding="utf-8")

        self.assertIn("After=network-online.target mysql.service", service)
        self.assertNotIn("nacos", service.lower())
        self.assertNotIn("redis", service.lower())
        self.assertFalse((PROJECT_ROOT / "deploy/systemd/invoice-title-nacos.service").exists())

    def test_production_profile_uses_environment_configuration_without_local_seed_migrations(self):
        config = (PROJECT_ROOT / "backend/src/main/resources/application-prod.yml").read_text(encoding="utf-8")

        self.assertNotIn("nacos", config.lower())
        self.assertNotIn("cloud:", config.lower())
        self.assertIn("classpath:db/migration", config)
        self.assertNotIn("classpath:db/local", config)

    def test_backend_dependencies_do_not_include_nacos_or_redis(self):
        pom = (PROJECT_ROOT / "backend/pom.xml").read_text(encoding="utf-8")
        application = (PROJECT_ROOT / "backend/src/main/resources/application.yml").read_text(encoding="utf-8")

        self.assertNotIn("nacos", pom.lower())
        self.assertNotIn("redis", pom.lower())
        self.assertNotIn("spring-cloud", pom.lower())
        self.assertNotIn("redis", application.lower())

    def test_local_runtime_always_reuses_existing_mysql_on_port_3306(self):
        application = (PROJECT_ROOT / "backend/src/main/resources/application.yml").read_text(encoding="utf-8")
        bootstrap = (PROJECT_ROOT / "scripts/bootstrap.ps1").read_text(encoding="utf-8")
        start_all = (PROJECT_ROOT / "scripts/start-all.ps1").read_text(encoding="utf-8")
        infrastructure = (PROJECT_ROOT / "scripts/start-infrastructure.ps1").read_text(encoding="utf-8")
        status = (PROJECT_ROOT / "scripts/status.ps1").read_text(encoding="utf-8")
        stop = (PROJECT_ROOT / "scripts/stop-all.ps1").read_text(encoding="utf-8")

        self.assertIn("${INVOICE_MYSQL_PORT:3306}", application)
        self.assertIn("${INVOICE_MYSQL_USERNAME:root}", application)
        self.assertIn("${INVOICE_MYSQL_PASSWORD:root}", application)
        self.assertIn("else { 3306 }", status)
        self.assertIn("existing MySQL", infrastructure)
        self.assertNotIn("mysqld", infrastructure.lower())
        self.assertNotIn("mysqladmin", stop.lower())
        self.assertNotIn("mysql-8.4.10", bootstrap.lower())

        for content in (application, bootstrap, start_all, infrastructure, status, stop):
            self.assertNotIn("23306", content)
            self.assertNotIn("UseLocalMySql", content)

    def test_frontend_builds_support_a_public_path_prefix(self):
        employee = (PROJECT_ROOT / "frontend/employee-h5/vite.config.ts").read_text(encoding="utf-8")
        finance = (PROJECT_ROOT / "frontend/finance-admin/vite.config.ts").read_text(encoding="utf-8")

        self.assertIn("VITE_PUBLIC_BASE", employee)
        self.assertIn("VITE_PUBLIC_BASE", finance)

    def test_finance_admin_history_routes_fall_back_to_index_html(self):
        config = (PROJECT_ROOT / "deploy/nginx/invoice-title.conf").read_text(encoding="utf-8")

        self.assertIn("location ^~ /invoice/finance/ {", config)
        self.assertIn(
            "try_files $uri /invoice/finance/index.html;",
            config,
        )
        self.assertIn(
            "location = /invoice/finance/index.html {",
            config,
        )

    def test_frontend_tests_run_sequentially_for_release_stability(self):
        package = (PROJECT_ROOT / "package.json").read_text(encoding="utf-8")

        self.assertIn("--workspace-concurrency=1", package)

    def test_server_build_creates_a_release_from_the_checked_out_commit(self):
        script = (PROJECT_ROOT / "deploy/server-build-release.sh").read_text(encoding="utf-8")

        self.assertIn('git rev-parse HEAD', script)
        self.assertIn('pnpm install --frozen-lockfile', script)
        self.assertIn('VITE_PUBLIC_BASE=/invoice/employee/', script)
        self.assertIn('VITE_PUBLIC_BASE=/invoice/finance/', script)
        self.assertIn('mvn -f backend/pom.xml', script)
        self.assertIn('${APP_HOME}/releases/', script)
        self.assertIn('NODE_OPTIONS', script)
        self.assertIn('MAVEN_OPTS', script)
        self.assertIn('--store-dir "${APP_HOME}/runtime/pnpm-store"', script)
        self.assertIn('-Dmaven.repo.local=${APP_HOME}/runtime/maven-repository', script)
        self.assertIn('-s scripts/maven-settings.xml', script)
        self.assertIn('JAVA_HOME="${INVOICE_JAVA_HOME:-${APP_HOME}/runtime/jdk-21}"', script)
        self.assertIn('${JAVA_HOME}/bin/javac', script)
        self.assertIn('find "${staging_dir}/frontend" -type d -exec chmod 0755 {} +', script)
        self.assertIn('find "${staging_dir}/frontend" -type f -exec chmod 0644 {} +', script)

    def test_server_activation_rolls_back_when_health_check_fails(self):
        script = (PROJECT_ROOT / "deploy/server-activate-release.sh").read_text(encoding="utf-8")

        self.assertIn('ln -sfn', script)
        self.assertIn('systemctl restart invoice-title.service', script)
        self.assertIn('127.0.0.1:28082/v3/api-docs', script)
        self.assertIn('rollback', script)


if __name__ == "__main__":
    unittest.main()

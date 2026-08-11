from pathlib import Path
import unittest


PROJECT_ROOT = Path(__file__).resolve().parents[2]


class DeploymentContractTest(unittest.TestCase):
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

    def test_backend_service_uses_java21_local_bind_and_isolated_user(self):
        service = (PROJECT_ROOT / "deploy/systemd/invoice-title.service").read_text(encoding="utf-8")

        self.assertIn("User=invoice_title", service)
        self.assertIn("EnvironmentFile=/opt/invoice-title/config/invoice-title.env", service)
        self.assertIn("/usr/lib/jvm/java-21-openjdk-amd64/bin/java", service)
        self.assertIn("--server.address=127.0.0.1", service)
        self.assertIn("--server.port=28082", service)

    def test_nacos_service_is_private_and_has_memory_limits(self):
        service = (PROJECT_ROOT / "deploy/systemd/invoice-title-nacos.service").read_text(encoding="utf-8")

        self.assertIn("User=invoice_title", service)
        self.assertIn("NACOS_SERVER_PORT=28848", service)
        self.assertIn("NACOS_BIND_IP=127.0.0.1", service)
        self.assertIn("JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64", service)
        self.assertIn("MODE=standalone", service)
        self.assertIn("--server.address=${NACOS_BIND_IP}", service)
        self.assertIn("-Dnacos.deployment.type=server", service)
        self.assertIn("--nacos.server.main.port=${NACOS_SERVER_PORT}", service)
        self.assertNotIn("--server.port=${NACOS_SERVER_PORT}", service)
        self.assertIn("IPAddressDeny=any", service)
        self.assertIn("IPAddressAllow=localhost", service)

    def test_production_profile_uses_nacos_without_local_seed_migrations(self):
        config = (PROJECT_ROOT / "backend/src/main/resources/application-prod.yml").read_text(encoding="utf-8")

        self.assertIn("optional:nacos:invoice-title-service.yml", config)
        self.assertIn("127.0.0.1:28848", config)
        self.assertIn("classpath:db/migration", config)
        self.assertNotIn("classpath:db/local", config)

    def test_frontend_builds_support_a_public_path_prefix(self):
        employee = (PROJECT_ROOT / "frontend/employee-h5/vite.config.ts").read_text(encoding="utf-8")
        finance = (PROJECT_ROOT / "frontend/finance-admin/vite.config.ts").read_text(encoding="utf-8")

        self.assertIn("VITE_PUBLIC_BASE", employee)
        self.assertIn("VITE_PUBLIC_BASE", finance)

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

    def test_server_activation_rolls_back_when_health_check_fails(self):
        script = (PROJECT_ROOT / "deploy/server-activate-release.sh").read_text(encoding="utf-8")

        self.assertIn('ln -sfn', script)
        self.assertIn('systemctl restart invoice-title.service', script)
        self.assertIn('127.0.0.1:28082/v3/api-docs', script)
        self.assertIn('rollback', script)


if __name__ == "__main__":
    unittest.main()

import { defineConfig } from "vitest/config";
import vue from "@vitejs/plugin-vue";
import { loadEnv } from "vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  return {
    base: env.VITE_PUBLIC_BASE || "/",
    plugins: [vue()],
    server: {
      host: "127.0.0.1",
      port: 24173,
      strictPort: true,
      proxy: {
        "/api": "http://127.0.0.1:28082",
      },
    },
    test: {
      environment: "happy-dom",
      globals: true,
    },
  };
});

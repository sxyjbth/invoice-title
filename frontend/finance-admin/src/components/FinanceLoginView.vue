<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Lock, User } from "@element-plus/icons-vue";

export type FinanceSessionUser = {
  id: number;
  username: string;
  displayName: string;
  roleType: "SUPER_ADMIN" | "FINANCE";
  status: "ENABLED" | "DISABLED";
};

const emit = defineEmits<{ loggedIn: [user: FinanceSessionUser] }>();
const form = reactive({ username: "", password: "" });
const submitting = ref(false);

async function submitLogin() {
  if (submitting.value) return;
  if (!form.username.trim() || !form.password) {
    ElMessage.warning("请输入登录账号和密码");
    return;
  }
  submitting.value = true;
  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(form),
    });
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      throw new Error(payload?.message || "账号或密码错误");
    }
    emit("loggedIn", payload as FinanceSessionUser);
    ElMessage.success("登录成功");
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "登录失败，请稍后重试");
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="brand-panel" aria-label="产品介绍">
      <div class="brand-mark">票</div>
      <div>
        <p class="eyebrow">INVOICE TITLE CENTER</p>
        <h1>发票抬头管理</h1>
        <p>集中维护企业抬头、展示主体和员工查看权限，让每一次开票信息都准确、可追溯。</p>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <div class="mobile-brand"><span>票</span> 发票抬头管理</div>
        <p class="welcome">欢迎回来</p>
        <h2>登录财务管理端</h2>
        <p class="login-hint">使用超级管理员为你开通的财务账号登录</p>

        <el-form label-position="top" @submit.prevent="submitLogin">
          <el-form-item label="登录账号">
            <el-input v-model="form.username" size="large" placeholder="请输入登录账号" autocomplete="username" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" autocomplete="current-password" :prefix-icon="Lock" />
          </el-form-item>
          <el-button class="login-button" type="primary" size="large" :loading="submitting" native-type="submit">登录</el-button>
        </el-form>

        <p class="reset-tip">忘记密码请联系超级管理员重置</p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page { min-height: 100vh; display: grid; grid-template-columns: minmax(360px, 42%) 1fr; background: #f4f7fb; color: #15213a; }
.brand-panel { position: relative; padding: 72px 10%; display: flex; flex-direction: column; justify-content: center; gap: 28px; overflow: hidden; color: white; background: radial-gradient(circle at 20% 20%, #2d79ff 0, transparent 35%), linear-gradient(145deg, #071b3c 15%, #0d3973 65%, #1262c6); }
.brand-panel::after { content: ""; position: absolute; width: 420px; height: 420px; right: -180px; bottom: -150px; border: 80px solid rgb(255 255 255 / 7%); border-radius: 50%; }
.brand-mark { width: 64px; height: 64px; border-radius: 18px; display: grid; place-items: center; font-size: 30px; font-weight: 700; background: #1478ff; box-shadow: 0 18px 45px rgb(0 0 0 / 22%); }
.eyebrow { margin: 0 0 12px; font-size: 12px; letter-spacing: 3px; color: #8ebeff; }
h1 { margin: 0; font-size: clamp(36px, 4vw, 56px); letter-spacing: -2px; }
.brand-panel p:not(.eyebrow) { max-width: 520px; font-size: 17px; line-height: 1.9; color: #c7dbf8; }
.login-panel { display: grid; place-items: center; padding: 48px; }
.login-card { width: min(420px, 100%); padding: 42px; border: 1px solid #e4eaf3; border-radius: 22px; background: white; box-shadow: 0 24px 70px rgb(41 72 120 / 10%); }
.mobile-brand { display: none; }
.welcome { margin: 0 0 8px; color: #1677ff; font-weight: 600; }
h2 { margin: 0; font-size: 30px; }
.login-hint { margin: 12px 0 30px; color: #7a879d; }
.login-button { width: 100%; margin-top: 4px; font-weight: 600; }
.reset-tip { margin: 22px 0 0; text-align: center; font-size: 13px; color: #8b96a8; }
@media (max-width: 820px) {
  .login-page { grid-template-columns: 1fr; }
  .brand-panel { display: none; }
  .login-panel { padding: 24px; }
  .login-card { padding: 32px 24px; }
  .mobile-brand { display: flex; align-items: center; gap: 10px; margin-bottom: 32px; font-weight: 700; }
  .mobile-brand span { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 10px; color: white; background: #1677ff; }
}
</style>

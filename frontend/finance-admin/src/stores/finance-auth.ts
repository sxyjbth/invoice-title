import { defineStore } from "pinia";
import { computed, ref } from "vue";
import type { FinanceSessionUser } from "../components/FinanceLoginView.vue";

const testMode = import.meta.env.MODE === "test";

export const useFinanceAuthStore = defineStore("finance-auth", () => {
  const currentUser = ref<FinanceSessionUser | null>(testMode ? {
    id: 1,
    username: "superadmin",
    displayName: "超级管理员",
    roleType: "SUPER_ADMIN",
    status: "ENABLED",
  } : null);
  const sessionReady = ref(testMode);
  const checking = ref(!testMode);
  let pendingCheck: Promise<void> | null = null;

  const isAuthenticated = computed(() => currentUser.value !== null);

  function acceptUser(user: FinanceSessionUser) {
    currentUser.value = user;
    sessionReady.value = true;
    checking.value = false;
  }

  function clearSession() {
    currentUser.value = null;
    sessionReady.value = true;
    checking.value = false;
  }

  async function checkSession(force = false) {
    if ((!force && sessionReady.value) || testMode) return;
    if (pendingCheck) return await pendingCheck;

    checking.value = true;
    pendingCheck = (async () => {
      try {
        const response = await fetch("/api/auth/me", { credentials: "include" });
        if (response.ok) acceptUser(await response.json() as FinanceSessionUser);
        else clearSession();
      } catch {
        clearSession();
      } finally {
        checking.value = false;
        sessionReady.value = true;
        pendingCheck = null;
      }
    })();

    return await pendingCheck;
  }

  async function logout() {
    try {
      await fetch("/api/auth/logout", { method: "POST", credentials: "include" });
    } finally {
      clearSession();
    }
  }

  return {
    checking,
    currentUser,
    isAuthenticated,
    sessionReady,
    acceptUser,
    checkSession,
    clearSession,
    logout,
  };
});

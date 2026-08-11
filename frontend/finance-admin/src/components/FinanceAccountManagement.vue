<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { formatDateTime } from "../utils/date";
import { Plus, Refresh, Search } from "@element-plus/icons-vue";

export type FinanceAccount = {
  id: number;
  username: string;
  displayName: string;
  status: "ENABLED" | "DISABLED";
  lastLoginAt?: string | null;
  createdAt?: string;
};

withDefaults(defineProps<{ accounts: FinanceAccount[]; total: number; loading: boolean; pageNum?: number; pageSize?: number }>(), {
  pageNum: 1,
  pageSize: 20,
});
const emit = defineEmits<{
  refresh: [];
  search: [keyword: string, status: string];
  "page-change": [page: number, size: number];
}>();

const keyword = ref("");
const status = ref("");
const createVisible = ref(false);
const resetVisible = ref(false);
const submitting = ref(false);
const selectedAccount = ref<FinanceAccount | null>(null);
const createForm = reactive({ username: "", displayName: "", initialPassword: "" });
const resetForm = reactive({ newPassword: "" });

function openCreate() {
  Object.assign(createForm, { username: "", displayName: "", initialPassword: "" });
  createVisible.value = true;
}

function openReset(account: FinanceAccount) {
  selectedAccount.value = account;
  resetForm.newPassword = "";
  resetVisible.value = true;
}

async function request(url: string, options: RequestInit) {
  const response = await fetch(url, { ...options, credentials: "include", headers: { "Content-Type": "application/json", ...options.headers } });
  const payload = await response.json().catch(() => null);
  if (!response.ok) throw new Error(payload?.message || "操作失败");
  return payload;
}

async function createAccount() {
  if (!createForm.username.trim() || !createForm.displayName.trim() || !createForm.initialPassword) {
    ElMessage.warning("请填写完整账号信息");
    return;
  }
  submitting.value = true;
  try {
    await request("/api/admin/finance-users", { method: "POST", body: JSON.stringify(createForm) });
    ElMessage.success("财务账号创建成功");
    createVisible.value = false;
    emit("refresh");
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "账号创建失败");
  } finally { submitting.value = false; }
}

async function resetPassword() {
  if (!selectedAccount.value || !resetForm.newPassword) return;
  submitting.value = true;
  try {
    await request(`/api/admin/finance-users/${selectedAccount.value.id}/reset-password`, { method: "POST", body: JSON.stringify(resetForm) });
    ElMessage.success("密码已重置，请将新密码安全告知本人");
    resetVisible.value = false;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "密码重置失败");
  } finally { submitting.value = false; }
}

async function toggleStatus(account: FinanceAccount) {
  const nextStatus = account.status === "ENABLED" ? "DISABLED" : "ENABLED";
  const action = nextStatus === "DISABLED" ? "停用" : "启用";
  try {
    await ElMessageBox.confirm(`${action}后${nextStatus === "DISABLED" ? "该账号将无法登录" : "该账号可恢复登录"}，确认继续吗？`, `${action}财务账号`, { type: "warning" });
    await request(`/api/admin/finance-users/${account.id}/status`, { method: "PATCH", body: JSON.stringify({ status: nextStatus }) });
    ElMessage.success(`账号已${action}`);
    emit("refresh");
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message);
  }
}
</script>

<template>
  <section class="account-page">
    <div class="account-toolbar">
      <div class="filters">
        <el-input v-model="keyword" clearable placeholder="搜索登录账号或姓名" :prefix-icon="Search" @keyup.enter="emit('search', keyword, status)" />
        <el-select v-model="status" clearable placeholder="全部状态" @change="emit('search', keyword, status)">
          <el-option label="已启用" value="ENABLED" />
          <el-option label="已停用" value="DISABLED" />
        </el-select>
        <el-button :icon="Search" @click="emit('search', keyword, status)">查询</el-button>
        <el-button :icon="Refresh" @click="keyword = ''; status = ''; emit('search', '', '')">重置</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增财务账号</el-button>
    </div>

    <div class="account-card">
      <div class="card-heading"><div><h3>财务账号</h3><p>超级管理员负责开户、停用/启用和重置密码</p></div><span>共 {{ total }} 个账号</span></div>
      <el-table :data="accounts" v-loading="loading">
        <el-table-column label="财务人员" min-width="220">
          <template #default="{ row }"><div class="account-name"><span>{{ row.displayName.slice(0, 1) }}</span><div><strong>{{ row.displayName }}</strong><small>{{ row.username }}</small></div></div></template>
        </el-table-column>
        <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="light">{{ row.status === "ENABLED" ? "已启用" : "已停用" }}</el-tag></template></el-table-column>
        <el-table-column prop="lastLoginAt" label="最近登录" min-width="170"><template #default="{ row }">{{ row.lastLoginAt || "尚未登录" }}</template></el-table-column>
        <el-table-column label="创建时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="openReset(row)">重置密码</el-button><el-button link :type="row.status === 'ENABLED' ? 'danger' : 'primary'" @click="toggleStatus(row)">{{ row.status === "ENABLED" ? "停用" : "启用" }}</el-button></template>
        </el-table-column>
      </el-table>
      <div class="pagination-row" aria-label="财务账号列表分页"><el-pagination background layout="total, sizes, prev, pager, next" :total="total" :current-page="pageNum" :page-size="pageSize" :page-sizes="[10, 20, 50, 100]" @current-change="emit('page-change', $event, pageSize)" @size-change="emit('page-change', 1, $event)" /></div>
    </div>

    <el-dialog v-model="createVisible" title="新增财务账号" width="520px" :teleported="false">
      <el-alert title="财务人员首次登录后可自行修改密码；忘记密码由超级管理员重置。" type="info" :closable="false" />
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="登录账号"><el-input v-model="createForm.username" placeholder="建议使用姓名拼音或工号" /></el-form-item>
        <el-form-item label="财务人员姓名"><el-input v-model="createForm.displayName" placeholder="用于页面显示和操作记录" /></el-form-item>
        <el-form-item label="初始密码"><el-input v-model="createForm.initialPassword" type="password" show-password placeholder="8–72 位，包含字母和数字" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="createAccount">确认创建</el-button></template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="460px" :teleported="false">
      <p class="reset-description">正在为 <strong>{{ selectedAccount?.displayName }}</strong>（{{ selectedAccount?.username }}）重置密码。</p>
      <el-form label-position="top"><el-form-item label="新密码"><el-input v-model="resetForm.newPassword" type="password" show-password placeholder="8–72 位，包含字母和数字" /></el-form-item></el-form>
      <template #footer><el-button @click="resetVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="resetPassword">确认重置</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.account-page { padding: 30px 40px; }
.account-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 18px; margin-bottom: 20px; }
.filters { display: flex; gap: 10px; flex-wrap: wrap; }
.filters .el-input { width: 280px; }
.filters .el-select { width: 150px; }
.account-card { overflow: hidden; border: 1px solid #e2e8f2; border-radius: 14px; background: white; }
.card-heading { display: flex; justify-content: space-between; align-items: center; padding: 20px 22px; border-bottom: 1px solid #edf0f5; }
.card-heading h3 { margin: 0; font-size: 17px; }
.card-heading p { margin: 6px 0 0; font-size: 13px; color: #8a96a8; }
.card-heading > span { font-size: 13px; color: #7d899b; }
.account-name { display: flex; align-items: center; gap: 12px; }
.account-name > span { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 10px; color: #126ee8; font-weight: 700; background: #eaf3ff; }
.account-name div { display: flex; flex-direction: column; gap: 3px; }
.account-name small { color: #8c98aa; }
.pagination-row { display: flex; justify-content: flex-end; padding: 18px 22px; border-top: 1px solid #edf0f5; }
.dialog-form { margin-top: 22px; }
.reset-description { margin: 0 0 20px; color: #67758b; }
@media (max-width: 900px) { .account-page { padding: 20px; } .account-toolbar { align-items: stretch; flex-direction: column; } .account-toolbar > .el-button { align-self: flex-start; } }
</style>

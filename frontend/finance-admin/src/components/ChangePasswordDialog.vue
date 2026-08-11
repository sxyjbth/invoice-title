<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps<{ modelValue: boolean }>();
const emit = defineEmits<{ "update:modelValue": [value: boolean]; changed: [] }>();
const visible = computed({ get: () => props.modelValue, set: (value) => emit("update:modelValue", value) });
const submitting = ref(false);
const form = reactive({ currentPassword: "", newPassword: "", confirmPassword: "" });

watch(() => props.modelValue, (opened) => {
  if (opened) Object.assign(form, { currentPassword: "", newPassword: "", confirmPassword: "" });
});

async function submit() {
  if (!form.currentPassword || !form.newPassword) {
    ElMessage.warning("请填写完整密码信息");
    return;
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning("两次输入的新密码不一致");
    return;
  }
  submitting.value = true;
  try {
    const response = await fetch("/api/auth/change-password", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ currentPassword: form.currentPassword, newPassword: form.newPassword }),
    });
    const payload = await response.json().catch(() => null);
    if (!response.ok) throw new Error(payload?.message || "密码修改失败");
    ElMessage.success("密码修改成功");
    visible.value = false;
    emit("changed");
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "密码修改失败");
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="修改我的密码" width="460px" :teleported="false" destroy-on-close>
    <el-alert title="密码须为 8–72 位，并同时包含字母和数字" type="info" :closable="false" show-icon />
    <el-form label-position="top" class="password-form">
      <el-form-item label="当前密码"><el-input v-model="form.currentPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
      <el-form-item label="新密码"><el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
      <el-form-item label="确认新密码"><el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" @keyup.enter="submit" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.password-form { margin-top: 22px; }
</style>

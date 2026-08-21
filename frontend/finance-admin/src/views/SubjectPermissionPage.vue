<script setup lang="ts">
import { inject } from "vue";
import { OfficeBuilding, User } from "@element-plus/icons-vue";
import { financeLayoutKey } from "../layouts/finance-layout-context";

const {
  activePermissionProfile,
  employeeRuleId,
  openPermissionEditor,
  permissionProfiles,
  permissionSaving,
  selectPermissionProfile,
  selectedPermissionProfileId,
  switchMenu,
  updateAllEmployeesVisibility,
} = inject(financeLayoutKey)!;
</script>

<template>
  <main class="content">
    <section class="permission-config-layout" aria-label="主体权限配置">
      <aside class="permission-subject-card">
        <h2>选择主体</h2>
        <button v-for="profile in permissionProfiles" :key="profile.id" type="button" :class="{ active: selectedPermissionProfileId === profile.id }" @click="selectPermissionProfile(profile.id)">
          <strong>{{ profile.subjectName }}</strong><span>当前可见 {{ profile.visibleCount }} 人</span>
        </button>
      </aside>

      <section v-if="activePermissionProfile" class="permission-detail-card">
        <header><div><h2>{{ activePermissionProfile.subjectName }}</h2><p>配置哪些员工可以查看该主体及其已发布抬头</p></div><span>当前可见 {{ activePermissionProfile.visibleCount }} 人</span></header>
        <div class="permission-level-row">
          <span class="permission-level-icon"><el-icon><User /></el-icon></span>
          <div class="permission-level-copy"><strong>全员可见</strong><p>开启后，所有在职员工均可查看</p></div>
          <el-switch :model-value="activePermissionProfile.allEmployeesVisible" :loading="permissionSaving" :disabled="permissionSaving" aria-label="全员可见" @update:model-value="updateAllEmployeesVisibility" />
        </div>
        <div class="permission-level-row permission-level-expanded">
          <span class="permission-level-icon"><el-icon><OfficeBuilding /></el-icon></span>
          <div class="permission-level-copy"><strong>部门授权</strong><p>已选择 {{ activePermissionProfile.departments.length }} 个部门，包含子部门</p><div class="permission-tags"><span v-for="department in activePermissionProfile.departments" :key="department.id">{{ department.departmentName }} · {{ department.employeeCount }} 人</span></div></div>
          <el-button @click="openPermissionEditor('DEPARTMENT')">编辑</el-button>
        </div>
        <div class="permission-level-row permission-level-expanded">
          <span class="permission-level-icon"><el-icon><User /></el-icon></span>
          <div class="permission-level-copy"><strong>员工授权</strong><p>单独授权 {{ activePermissionProfile.employeeCount }} 名员工（允许或拒绝均优先于部门）</p><div class="employee-avatar-list"><span v-for="employee in activePermissionProfile.employeeRules.slice(0, 4)" :key="employeeRuleId(employee)" :title="employee.effect === 'DENY' ? '单独拒绝' : '单独允许'">{{ employee.employeeName.slice(0, 1) }}</span><span v-if="activePermissionProfile.employeeCount > 4">+{{ activePermissionProfile.employeeCount - 4 }}</span></div></div>
          <el-button @click="openPermissionEditor('USER')">编辑</el-button>
        </div>
        <footer><p>权限调整后实时生效，员工调整后按钉钉通讯录自动更新。</p></footer>
      </section>

      <section v-else class="permission-empty-state">
        <span class="permission-level-icon"><el-icon><OfficeBuilding /></el-icon></span><h2>暂无主体可配置权限</h2><p>请先新增并启用一个主体，再为员工或部门配置查看权限。</p><el-button type="primary" @click="switchMenu('subjects')">前往主体管理</el-button>
      </section>
    </section>
  </main>
</template>

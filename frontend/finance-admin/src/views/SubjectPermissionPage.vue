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

function selectVisibilityMode(allEmployeesVisible: boolean) {
  if (!activePermissionProfile.value || activePermissionProfile.value.allEmployeesVisible === allEmployeesVisible) return;
  void updateAllEmployeesVisibility(allEmployeesVisible);
}
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
        <div class="permission-visibility-section">
          <h3>可见范围</h3>
          <div class="permission-visibility-options" role="radiogroup" aria-label="主体可见范围">
            <button
              type="button"
              role="radio"
              aria-label="全员可见"
              :aria-checked="activePermissionProfile.allEmployeesVisible"
              :class="{ active: activePermissionProfile.allEmployeesVisible }"
              :disabled="permissionSaving"
              @click="selectVisibilityMode(true)"
            >
              <span class="permission-mode-radio" aria-hidden="true"></span>
              <span class="permission-level-icon"><el-icon><User /></el-icon></span>
              <span><strong>全员可见</strong><small>所有在职员工均可查看该主体及其已发布抬头</small></span>
            </button>
            <button
              type="button"
              role="radio"
              aria-label="部分可见"
              :aria-checked="!activePermissionProfile.allEmployeesVisible"
              :class="{ active: !activePermissionProfile.allEmployeesVisible }"
              :disabled="permissionSaving"
              @click="selectVisibilityMode(false)"
            >
              <span class="permission-mode-radio" aria-hidden="true"></span>
              <span class="permission-level-icon"><el-icon><OfficeBuilding /></el-icon></span>
              <span><strong>部分可见</strong><small>按企业、部门与员工配置可查看人员</small></span>
            </button>
          </div>
        </div>
        <div v-if="activePermissionProfile.allEmployeesVisible" class="permission-mode-summary permission-mode-summary-all">
          <span class="permission-level-icon"><el-icon><User /></el-icon></span>
          <div><strong>全员可见已开启</strong><p>该主体将对所有在职员工展示，无需再配置部门或个人权限。</p></div>
        </div>
        <div v-else class="permission-mode-summary permission-mode-summary-partial">
          <div class="permission-mode-summary-heading">
            <div><strong>已配置可见人员</strong><p>已选择 {{ activePermissionProfile.departments.length }} 个部门，个人允许或关闭规则优先于所属部门。</p></div>
            <el-button type="primary" plain @click="openPermissionEditor('DEPARTMENT')">编辑部分可见范围</el-button>
          </div>
          <div class="permission-summary-stats">
            <span><b>{{ activePermissionProfile.departments.length }}</b>个已选部门</span>
            <span><b>{{ activePermissionProfile.visibleCount }}</b>名当前可见人员</span>
            <span><b>{{ activePermissionProfile.employeeRules.length }}</b>条个人规则</span>
          </div>
          <div v-if="activePermissionProfile.departments.length" class="permission-tags">
            <span v-for="department in activePermissionProfile.departments" :key="department.id">{{ department.departmentName }} · {{ department.employeeCount }} 人</span>
          </div>
          <div v-if="activePermissionProfile.employeeRules.length" class="employee-avatar-list">
            <span v-for="employee in activePermissionProfile.employeeRules.slice(0, 6)" :key="employeeRuleId(employee)" :title="employee.effect === 'DENY' ? '单独关闭' : '单独开启'">{{ employee.employeeName.slice(0, 1) }}</span>
            <span v-if="activePermissionProfile.employeeRules.length > 6">+{{ activePermissionProfile.employeeRules.length - 6 }}</span>
          </div>
        </div>
        <footer><p>权限调整后实时生效，员工调整后按钉钉通讯录自动更新。</p></footer>
      </section>

      <section v-else class="permission-empty-state">
        <span class="permission-level-icon"><el-icon><OfficeBuilding /></el-icon></span><h2>暂无主体可配置权限</h2><p>请先新增并启用一个主体，再为员工或部门配置查看权限。</p><el-button type="primary" @click="switchMenu('subjects')">前往主体管理</el-button>
      </section>
    </section>
  </main>
</template>

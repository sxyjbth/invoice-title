<script setup lang="ts">
import { computed, inject } from "vue";
import { OfficeBuilding, User } from "@element-plus/icons-vue";
import { financeLayoutKey } from "../layouts/finance-layout-context";

const {
  activePermissionProfile,
  openPermissionEditor,
  permissionProfiles,
  permissionSaving,
  selectPermissionProfile,
  selectedPermissionEmployees,
  selectedPermissionProfileId,
  switchMenu,
  updateAllEmployeesVisibility,
} = inject(financeLayoutKey)!;

type PermissionEmployeeRule = {
  id?: number | string;
  employeeId?: number | string;
  corpCode?: string;
  corpName?: string;
  employeeName?: string;
  effect?: string;
};

function normalizeCorpCode(rule: PermissionEmployeeRule) {
  const corpCode = String(rule.corpCode ?? "").trim().toLowerCase();
  if (corpCode === "sebo" || corpCode === "walden") return corpCode;
  const corpName = String(rule.corpName ?? "").trim();
  if (corpName.includes("赛宝")) return "sebo";
  if (corpName.includes("瓦尔登")) return "walden";
  return corpCode;
}

const visibleEmployeesByCorp = computed(() => {
  const groups: Record<"sebo" | "walden", Array<{ key: string; name: string }>> = {
    sebo: [],
    walden: [],
  };
  const seen = new Set<string>();
  const rules = (selectedPermissionEmployees.value ?? []) as PermissionEmployeeRule[];

  rules.forEach((rule) => {
    if (String(rule.effect ?? "ALLOW").toUpperCase() !== "ALLOW") return;
    const corpCode = normalizeCorpCode(rule);
    if (corpCode !== "sebo" && corpCode !== "walden") return;
    const name = String(rule.employeeName ?? "").trim();
    if (!name) return;
    const key = `${corpCode}:${rule.employeeId ?? rule.id ?? name}`;
    if (seen.has(key)) return;
    seen.add(key);
    groups[corpCode].push({ key, name });
  });

  return groups;
});

const activeVisibleCount = computed(() => {
  const profile = activePermissionProfile.value;
  if (!profile) return 0;
  return profile.allEmployeesVisible ? profile.visibleCount : selectedPermissionEmployees.value.length;
});

function permissionProfileVisibleCount(profile: { id: number; visibleCount: number; allEmployeesVisible: boolean }) {
  return profile.id === activePermissionProfile.value?.id ? activeVisibleCount.value : profile.visibleCount;
}

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
          <strong>{{ profile.subjectName }}</strong><span>当前可见 {{ permissionProfileVisibleCount(profile) }} 人</span>
        </button>
      </aside>

      <section v-if="activePermissionProfile" class="permission-detail-card permission-detail-card-compact">
        <header><div><h2>{{ activePermissionProfile.subjectName }}</h2><p>配置哪些员工可以查看该主体及其已发布抬头</p></div><span>当前可见 {{ activeVisibleCount }} 人</span></header>
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
            <div><strong>已选人员</strong></div>
            <el-button type="primary" plain @click="openPermissionEditor('DEPARTMENT')">编辑部分可见范围</el-button>
          </div>
          <div class="permission-corporation-members">
            <section aria-label="赛宝已选人员">
              <header><strong>赛宝</strong><span>{{ visibleEmployeesByCorp.sebo.length }} 人</span></header>
              <div
                v-if="visibleEmployeesByCorp.sebo.length"
                class="permission-member-names permission-member-scroll"
                role="list"
                tabindex="0"
                aria-label="赛宝已选人员姓名列表"
              >
                <span v-for="employee in visibleEmployeesByCorp.sebo" :key="employee.key" role="listitem">{{ employee.name }}</span>
              </div>
              <p v-else class="permission-members-empty">暂无已选人员</p>
            </section>
            <section aria-label="瓦尔登已选人员">
              <header><strong>瓦尔登</strong><span>{{ visibleEmployeesByCorp.walden.length }} 人</span></header>
              <div
                v-if="visibleEmployeesByCorp.walden.length"
                class="permission-member-names permission-member-scroll"
                role="list"
                tabindex="0"
                aria-label="瓦尔登已选人员姓名列表"
              >
                <span v-for="employee in visibleEmployeesByCorp.walden" :key="employee.key" role="listitem">{{ employee.name }}</span>
              </div>
              <p v-else class="permission-members-empty">暂无已选人员</p>
            </section>
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

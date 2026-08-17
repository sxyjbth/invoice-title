<script setup lang="ts">
import { inject } from "vue";
import { Plus, Search } from "@element-plus/icons-vue";
import { financeLayoutKey } from "../layouts/finance-layout-context";

const {
  changeSubjectStatus,
  currentSubjectTotal,
  filteredSubjects,
  loadSubjects,
  openSubjectDialog,
  openSubjectEditor,
  openTitleBinding,
  subjectKeyword,
  subjectPageNum,
  subjectPageSize,
  subjectStatus,
} = inject(financeLayoutKey)!;
</script>

<template>
  <main class="content">
    <section class="management-toolbar">
      <el-input v-model="subjectKeyword" clearable placeholder="搜索主体名称" :prefix-icon="Search" @keyup.enter="subjectPageNum = 1; loadSubjects()" />
      <section class="status-tabs management-status-tabs" aria-label="主体状态筛选">
        <button type="button" data-status="ALL" :class="{ active: subjectStatus === 'ALL' }" @click="subjectStatus = 'ALL'; subjectPageNum = 1; loadSubjects()">全部</button>
        <button type="button" data-status="ENABLED" :class="{ active: subjectStatus === 'ENABLED' }" @click="subjectStatus = 'ENABLED'; subjectPageNum = 1; loadSubjects()">启用</button>
        <button type="button" data-status="DISABLED" :class="{ active: subjectStatus === 'DISABLED' }" @click="subjectStatus = 'DISABLED'; subjectPageNum = 1; loadSubjects()">停用</button>
      </section>
      <el-button type="primary" @click="openSubjectDialog"><el-icon><Plus /></el-icon>新增主体</el-button>
    </section>
    <section class="data-card">
      <header class="card-header"><div><h2>主体列表</h2><p>停用主体后，对应抬头及二维码将立即停止展示</p></div><span>共 {{ currentSubjectTotal }} 条</span></header>
      <div class="table-scroll">
        <table>
          <thead><tr><th>主体名称</th><th>绑定抬头</th><th>覆盖员工</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="subject in filteredSubjects" :key="subject.id">
              <td><strong>{{ subject.name }}</strong></td><td>{{ subject.boundTitleName || '未绑定' }}</td><td>{{ subject.employeeCount }} 人</td>
              <td><span class="status" :class="subject.status === 'ENABLED' ? 'status-published' : 'status-disabled'"><i />{{ subject.status === 'ENABLED' ? '启用' : '停用' }}</span></td>
              <td>{{ subject.updatedAt }}<small>{{ subject.updatedBy }}</small></td>
              <td class="row-actions"><el-button link type="primary" @click="openTitleBinding(subject)">绑定抬头</el-button><el-button link type="primary" @click="openSubjectEditor(subject)">编辑</el-button><el-button link type="primary" @click="changeSubjectStatus(subject)">{{ subject.status === 'ENABLED' ? '停用' : '启用' }}</el-button></td>
            </tr>
            <tr v-if="filteredSubjects.length === 0"><td class="empty-row" colspan="6">未找到符合条件的主体</td></tr>
          </tbody>
        </table>
      </div>
      <footer class="pagination-row" aria-label="主体管理列表分页">
        <el-pagination v-model:current-page="subjectPageNum" v-model:page-size="subjectPageSize" :page-sizes="[10,20,50,100]" :total="currentSubjectTotal" layout="total, sizes, prev, pager, next, jumper" background @current-change="loadSubjects" @size-change="subjectPageNum = 1; loadSubjects()" />
      </footer>
    </section>
  </main>
</template>

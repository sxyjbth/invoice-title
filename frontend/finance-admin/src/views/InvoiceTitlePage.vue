<script setup lang="ts">
import { inject } from "vue";
import { Plus, Search, Upload } from "@element-plus/icons-vue";
import { financeLayoutKey } from "../layouts/finance-layout-context";

const {
  activeStatus,
  currentTotal,
  filteredTitles,
  keyword,
  loadTitles,
  openImportDialog,
  openTitleEditor,
  pageNum,
  pageSize,
  resetCreateForm,
  selectStatus,
  statusClass,
  statusLabel,
  statusOptions,
} = inject(financeLayoutKey)!;
</script>

<template>
  <main class="content">
    <section class="toolbar-row" aria-label="抬头查询操作">
      <el-input v-model="keyword" clearable placeholder="搜索公司名称或纳税人识别号" :prefix-icon="Search" @keyup.enter="pageNum = 1; loadTitles()" />
      <el-button aria-label="搜索发票抬头" :icon="Search" @click="pageNum = 1; loadTitles()">筛选</el-button>
      <span class="toolbar-spacer" />
      <el-button data-testid="batch-import" @click="openImportDialog"><el-icon><Upload /></el-icon>批量导入</el-button>
      <el-button type="primary" @click="resetCreateForm"><el-icon><Plus /></el-icon>新增抬头</el-button>
    </section>

    <section class="status-tabs" aria-label="抬头状态筛选">
      <button v-for="option in statusOptions" :key="option.code" type="button" :data-status="option.code" :class="{ active: activeStatus === option.code }" @click="selectStatus(option.code)">
        {{ option.label }}<span>{{ option.count }}</span>
      </button>
    </section>

    <section class="data-card">
      <header class="card-header"><div><h2>抬头数据</h2><p>共 {{ currentTotal }} 条真实业务数据</p></div><span>共 {{ currentTotal }} 条</span></header>
      <div class="table-scroll">
        <table>
          <thead><tr><th>公司名称</th><th>纳税人识别号</th><th>展示主体</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="title in filteredTitles" :key="title.id">
              <td><strong>{{ title.companyName }}</strong><small>{{ title.bankSummary }}</small></td>
              <td class="tax-id">{{ title.taxpayerId }}</td>
              <td><span v-for="subject in title.subjects" :key="subject" class="subject-tag">{{ subject }}</span></td>
              <td><span class="status" :class="statusClass(title.status)"><i />{{ statusLabel(title.status) }}</span></td>
              <td>{{ title.updatedAt }}<small>{{ title.updatedBy }}</small></td>
              <td class="row-actions"><el-button link type="primary" @click="openTitleEditor(title)">编辑</el-button></td>
            </tr>
            <tr v-if="filteredTitles.length === 0"><td class="empty-row" colspan="6">未找到符合条件的抬头</td></tr>
          </tbody>
        </table>
      </div>
      <footer class="pagination-row" aria-label="抬头列表分页">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]" :total="currentTotal" layout="total, sizes, prev, pager, next, jumper" background @current-change="loadTitles" @size-change="pageNum = 1; loadTitles()" />
      </footer>
    </section>
  </main>
</template>

<script setup lang="ts">
import { inject } from "vue";
import FinanceAccountManagement from "../components/FinanceAccountManagement.vue";
import { financeLayoutKey } from "../layouts/finance-layout-context";

const {
  accountLoading,
  accountPageNum,
  accountPageSize,
  accountTotal,
  changeFinanceAccountPage,
  financeAccounts,
  loadFinanceAccounts,
  searchFinanceAccounts,
} = inject(financeLayoutKey)!;
</script>

<template>
  <FinanceAccountManagement
    :accounts="financeAccounts"
    :total="accountTotal"
    :loading="accountLoading"
    :page-num="accountPageNum"
    :page-size="accountPageSize"
    @refresh="loadFinanceAccounts"
    @search="searchFinanceAccounts"
    @page-change="changeFinanceAccountPage"
  />
</template>

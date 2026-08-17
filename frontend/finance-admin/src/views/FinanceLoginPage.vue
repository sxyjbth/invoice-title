<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import FinanceLoginView, { type FinanceSessionUser } from "../components/FinanceLoginView.vue";
import { routeNames } from "../router";
import { useFinanceAuthStore } from "../stores/finance-auth";

const route = useRoute();
const router = useRouter();
const auth = useFinanceAuthStore();

function handleLoggedIn(user: FinanceSessionUser) {
  auth.acceptUser(user);
  const redirect = typeof route.query.redirect === "string" ? route.query.redirect : undefined;
  void router.replace(redirect || { name: routeNames.titles });
}
</script>

<template>
  <FinanceLoginView @logged-in="handleLoggedIn" />
</template>

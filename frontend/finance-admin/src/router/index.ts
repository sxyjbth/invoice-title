import {
  createRouter,
  createWebHistory,
  type Router,
  type RouterHistory,
} from "vue-router";

export const routeNames = {
  login: "finance-login",
  titles: "finance-titles",
  subjects: "finance-subjects",
  permissions: "finance-permissions",
  accounts: "finance-accounts",
} as const;

export type FinanceMenuCode = "titles" | "subjects" | "permissions" | "accounts";

export const routeNameByMenu: Record<FinanceMenuCode, string> = {
  titles: routeNames.titles,
  subjects: routeNames.subjects,
  permissions: routeNames.permissions,
  accounts: routeNames.accounts,
};

export function createFinanceRouter(history: RouterHistory = createWebHistory(import.meta.env.BASE_URL)) {
  return createRouter({
    history,
    routes: [
      { path: "/login", name: routeNames.login, component: () => import("../views/FinanceLoginPage.vue"), meta: { title: "财务端登录" } },
      {
        path: "/",
        component: () => import("../layouts/FinanceLayout.vue"),
        meta: { requiresAuth: true },
        children: [
          { path: "", redirect: { name: routeNames.titles } },
          { path: "titles", name: routeNames.titles, component: () => import("../views/InvoiceTitlePage.vue"), meta: { title: "发票抬头管理", menuCode: "titles" } },
          { path: "subjects", name: routeNames.subjects, component: () => import("../views/InvoiceSubjectPage.vue"), meta: { title: "主体管理", menuCode: "subjects" } },
          { path: "permissions", name: routeNames.permissions, component: () => import("../views/SubjectPermissionPage.vue"), meta: { title: "主体权限", menuCode: "permissions" } },
          { path: "accounts", name: routeNames.accounts, component: () => import("../views/FinanceAccountPage.vue"), meta: { title: "财务账号", menuCode: "accounts", roles: ["SUPER_ADMIN"] } },
        ],
      },
      { path: "/:pathMatch(.*)*", redirect: () => ({ path: "/titles", replace: true }) },
    ],
  });
}

export type FinanceRouterAuth = {
  sessionReady: boolean;
  currentUser: { roleType: string } | null;
  checkSession: () => Promise<unknown>;
};

export function installFinanceRouterGuards(router: Router, auth: FinanceRouterAuth) {
  router.beforeEach(async (to) => {
    if (!auth.sessionReady) await auth.checkSession();

    if (to.meta.requiresAuth && !auth.currentUser) {
      return { name: routeNames.login, query: { redirect: to.fullPath } };
    }

    const roles = to.meta.roles as string[] | undefined;
    if (roles?.length && (!auth.currentUser || !roles.includes(auth.currentUser.roleType))) {
      return { name: routeNames.titles };
    }

    if (to.name === routeNames.login && auth.currentUser) {
      const redirect = typeof to.query.redirect === "string" ? to.query.redirect : undefined;
      return redirect || { name: routeNames.titles };
    }

    return true;
  });
}

export const router = createFinanceRouter();

import { describe, expect, it, vi } from "vitest";
import { createMemoryHistory } from "vue-router";
import { createFinanceRouter, installFinanceRouterGuards, routeNames } from "../src/router";

describe("finance admin router", () => {
  it("exposes one named URL for every finance menu", () => {
    const router = createFinanceRouter(createMemoryHistory());

    expect(router.resolve({ name: routeNames.titles }).path).toBe("/titles");
    expect(router.resolve({ name: routeNames.subjects }).path).toBe("/subjects");
    expect(router.resolve({ name: routeNames.permissions }).path).toBe("/permissions");
    expect(router.resolve({ name: routeNames.accounts }).path).toBe("/accounts");
  });

  it("renders each finance page inside one shared finance layout", () => {
    const router = createFinanceRouter(createMemoryHistory());

    const matched = router.resolve("/subjects").matched;

    expect(matched).toHaveLength(2);
    expect(matched[0].path).toBe("/");
    expect(matched[1].name).toBe(routeNames.subjects);
  });

  it("redirects the finance root to title management", async () => {
    const router = createFinanceRouter(createMemoryHistory());

    await router.push("/");
    await router.isReady();

    expect(router.currentRoute.value.name).toBe(routeNames.titles);
    expect(router.currentRoute.value.path).toBe("/titles");
  });

  it("preserves deep links to subject permissions", async () => {
    const router = createFinanceRouter(createMemoryHistory());

    await router.push("/permissions?subjectId=3");
    await router.isReady();

    expect(router.currentRoute.value.name).toBe(routeNames.permissions);
    expect(router.currentRoute.value.query.subjectId).toBe("3");
  });

  it("uses the configured public base for production history URLs", () => {
    const router = createFinanceRouter(createMemoryHistory("/invoice/finance/"));

    expect(router.resolve({ name: routeNames.subjects }).href).toBe("/invoice/finance/subjects");
  });

  it("sends anonymous deep links to login and preserves the destination", async () => {
    const router = createFinanceRouter(createMemoryHistory());
    installFinanceRouterGuards(router, {
      sessionReady: true,
      currentUser: null,
      checkSession: async () => undefined,
    });

    await router.push("/subjects?page=2");
    await router.isReady();

    expect(router.currentRoute.value.name).toBe(routeNames.login);
    expect(router.currentRoute.value.query.redirect).toBe("/subjects?page=2");
  });

  it("prevents finance users from opening super-admin account management", async () => {
    const router = createFinanceRouter(createMemoryHistory());
    installFinanceRouterGuards(router, {
      sessionReady: true,
      currentUser: { roleType: "FINANCE" },
      checkSession: async () => undefined,
    });

    await router.push("/accounts");
    await router.isReady();

    expect(router.currentRoute.value.name).toBe(routeNames.titles);
  });

  it("redirects unknown paths without forwarding catch-all params", async () => {
    const warn = vi.spyOn(console, "warn").mockImplementation(() => undefined);
    const router = createFinanceRouter(createMemoryHistory());

    await router.push("/missing/deep/link");
    await router.isReady();

    expect(router.currentRoute.value.name).toBe(routeNames.titles);
    expect(warn).not.toHaveBeenCalledWith(expect.stringContaining("pathMatch"));
    warn.mockRestore();
  });
});

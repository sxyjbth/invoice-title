import { flushPromises, mount } from "@vue/test-utils";
import ElementPlus from "element-plus";
import { createPinia } from "pinia";
import { describe, expect, it, vi } from "vitest";
import { createMemoryHistory } from "vue-router";
import AdminApp from "../src/App.vue";
import { elementPlusOptions } from "../src/element-plus";
import { createFinanceRouter } from "../src/router";

async function mountAt(path: string) {
  const router = createFinanceRouter(createMemoryHistory());
  await router.push(path);
  await router.isReady();
  const wrapper = mount(AdminApp, {
    global: { plugins: [createPinia(), [ElementPlus, elementPlusOptions], router] },
  });
  return { router, wrapper };
}

describe("finance menu routing", () => {
  it("changes the URL when a finance menu is clicked", async () => {
    const { router, wrapper } = await mountAt("/titles");
    const target = wrapper.findAll("nav a").find((item) => item.text().includes("主体管理"))!;

    expect(target.attributes("href")).toBe("/subjects");

    (target.element as HTMLAnchorElement).click();
    await flushPromises();

    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe("/subjects"));
  });

  it("renders the matching menu when a deep link is opened", async () => {
    const { wrapper } = await mountAt("/permissions");

    expect(wrapper.get('[aria-label="主体权限配置"]')).toBeTruthy();
    expect(wrapper.get("nav a.active").text()).toContain("主体权限");
  });
});

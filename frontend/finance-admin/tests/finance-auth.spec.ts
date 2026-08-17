import { flushPromises, mount } from "@vue/test-utils";
import ElementPlus from "element-plus";
import { afterEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import FinanceLoginView from "../src/components/FinanceLoginView.vue";
import FinanceAccountManagement from "../src/components/FinanceAccountManagement.vue";
import ChangePasswordDialog from "../src/components/ChangePasswordDialog.vue";
import { elementPlusOptions } from "../src/element-plus";

const global = { plugins: [[ElementPlus, elementPlusOptions]] } as any;

describe("finance login session", () => {
  it("clicking login creates only one tab session so the issued token survives refresh", async () => {
    const request = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({
      id: 1,
      username: "admin",
      displayName: "Administrator",
      roleType: "SUPER_ADMIN",
      status: "ENABLED",
    }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    }));
    const wrapper = mount(FinanceLoginView, { global });

    await wrapper.get('input[autocomplete="username"]').setValue("admin");
    await wrapper.get('input[autocomplete="current-password"]').setValue("root");
    const form = wrapper.get("form");
    await Promise.all([form.trigger("submit"), form.trigger("submit")]);
    await flushPromises();

    expect(request).toHaveBeenCalledTimes(1);
  });
});

afterEach(() => {
  document.body.innerHTML = "";
  vi.restoreAllMocks();
});

describe("财务端账号体系", () => {
  it("账号密码登录并明确忘记密码处理方式", () => {
    const wrapper = mount(FinanceLoginView, { global });

    expect(wrapper.find('input[autocomplete="username"]').exists()).toBe(true);
    expect(wrapper.find('input[autocomplete="current-password"]').exists()).toBe(true);
    expect(wrapper.text()).toContain("忘记密码请联系超级管理员重置");
  });

  it("超级管理员可分页维护财务账号", async () => {
    const wrapper = mount(FinanceAccountManagement, {
      global,
      props: {
        accounts: [{ id: 2, username: "wang.finance", displayName: "王财务", status: "ENABLED" }],
        total: 1,
        loading: false,
      },
    });
    await nextTick();

    expect(wrapper.text()).toContain("新增财务账号");
    expect(wrapper.text()).toContain("重置密码");
    expect(wrapper.text()).toContain("停用");
    expect(wrapper.find('[aria-label="财务账号列表分页"]').exists()).toBe(true);
  });

  it("新增财务账号校验必填格式并要求二次确认初始密码", async () => {
    const request = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response("{}", {
      status: 200,
      headers: { "Content-Type": "application/json" },
    }));
    const wrapper = mount(FinanceAccountManagement, {
      global,
      attachTo: document.body,
      props: { accounts: [], total: 0, loading: false },
    });

    await wrapper.findAll("button").find((item) => item.text().includes("新增财务账号"))!.trigger("click");
    await nextTick();
    expect(document.body.textContent).toContain("确认初始密码");

    await wrapper.get('input[placeholder="建议使用姓名拼音或工号"]').setValue("finance.sun");
    await wrapper.get('input[placeholder="用于页面显示和操作记录"]').setValue("孙财务");
    await wrapper.get('input[placeholder="8–72 位，包含字母和数字"]').setValue("Finance123");
    await wrapper.get('input[placeholder="请再次输入初始密码"]').setValue("Different123");
    Array.from(document.body.querySelectorAll<HTMLButtonElement>(".el-dialog button"))
      .find((button) => button.textContent?.includes("确认创建"))!.click();
    await flushPromises();

    expect(document.body.textContent).toContain("两次输入的密码不一致");
    expect(request).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it("超级管理员重置密码时也要求二次确认", async () => {
    const wrapper = mount(FinanceAccountManagement, {
      global,
      attachTo: document.body,
      props: {
        accounts: [{ id: 2, username: "wang.finance", displayName: "王财务", status: "ENABLED" }],
        total: 1,
        loading: false,
      },
    });

    await flushPromises();
    await wrapper.findAll(".el-button").find((button) => button.text().includes("重置密码"))!.trigger("click");
    await nextTick();
    expect(document.body.textContent).toContain("确认新密码");
    wrapper.unmount();
  });

  it("财务人员只能通过本人改密表单修改自己的密码", async () => {
    const wrapper = mount(ChangePasswordDialog, {
      global,
      props: { modelValue: true },
    });
    await nextTick();

    expect(wrapper.text()).toContain("当前密码");
    expect(wrapper.text()).toContain("新密码");
    expect(wrapper.text()).toContain("确认新密码");
    expect(wrapper.text()).not.toContain("选择账号");
  });
});

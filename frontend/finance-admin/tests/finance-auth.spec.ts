import { mount } from "@vue/test-utils";
import ElementPlus from "element-plus";
import { describe, expect, it } from "vitest";
import { nextTick } from "vue";
import FinanceLoginView from "../src/components/FinanceLoginView.vue";
import FinanceAccountManagement from "../src/components/FinanceAccountManagement.vue";
import ChangePasswordDialog from "../src/components/ChangePasswordDialog.vue";
import { elementPlusOptions } from "../src/element-plus";

const global = { plugins: [[ElementPlus, elementPlusOptions]] } as any;

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

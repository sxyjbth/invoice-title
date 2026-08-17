import { mount } from "@vue/test-utils";
import ElementPlus from "element-plus";
import { describe, expect, it } from "vitest";
import { defineComponent, h, inject, nextTick, type PropType } from "vue";
import FinanceAccountManagement from "../src/components/FinanceAccountManagement.vue";
import { elementPlusOptions } from "../src/element-plus";

describe("finance account timestamps", () => {
  it("renders the last login time without an ISO T separator", async () => {
    const accounts = [{
      id: 2,
      username: "wang.finance",
      displayName: "王财务",
      status: "ENABLED" as const,
      lastLoginAt: "2026-08-11T09:20:49.515",
    }];
    const TableColumnStub = defineComponent({
      props: { prop: String, label: String },
      setup(_props, { slots }) {
        const rows = inject<typeof accounts>("test-account-rows", []);
        return () => h("div", rows.map((row) => slots.default?.({ row })));
      },
    });
    const wrapper = mount(FinanceAccountManagement, {
      global: {
        plugins: [[ElementPlus, elementPlusOptions]],
        provide: { "test-account-rows": accounts },
        stubs: {
          ElTable: defineComponent({
            props: { data: Array as PropType<unknown[]> },
            setup(_props, { slots }) { return () => h("div", slots.default?.()); },
          }),
          ElTableColumn: TableColumnStub,
        },
      },
      props: {
        accounts,
        total: 1,
        loading: false,
      },
    });
    await nextTick();

    expect(wrapper.text()).toContain("2026-08-11 09:20:49");
    expect(wrapper.text()).not.toContain("2026-08-11T09:20:49.515");
  });
});

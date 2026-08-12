import { mount } from "@vue/test-utils";
import ElementPlus from "element-plus";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises } from "@vue/test-utils";
import EmployeeApp from "../src/App.vue";
import { elementPlusOptions } from "../src/element-plus";

const bundledDingTalkApi = vi.hoisted(() => ({
  getAuthCode: vi.fn().mockResolvedValue({ authCode: "sdk-auth-code" }),
}));

vi.mock("dingtalk-jsapi", () => ({
  default: bundledDingTalkApi,
}));

const global = { plugins: [[ElementPlus, elementPlusOptions]] } as any;

describe("员工端发票抬头", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
    bundledDingTalkApi.getAuthCode.mockReset().mockResolvedValue({ authCode: "sdk-auth-code" });
    delete (window as any).DingTalkPC;
    delete (window as any).DD;
    window.history.replaceState({}, "", "/?corpCode=sebo");
    (window as any).dd = {
      env: { platform: "android" },
      runtime: { permission: { requestAuthCode: vi.fn().mockResolvedValue({ code: "ding-auth-code" }) } },
    };
  });

  function mockEmployeeApis(expectedAuthCode = "ding-auth-code") {
    return vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
      const url = String(input);
      if (url.includes("/api/employee/auth/organizations")) {
        return new Response(JSON.stringify([
          { corpCode: "sebo", corpName: "赛宝绿创能源技术（上海）有限公司", corpId: "ding-sebo" },
          { corpCode: "walden", corpName: "瓦尔登环境科学研究院（北京）有限公司", corpId: "ding-walden" },
        ]), { status: 200 });
      }
      if (url.includes("/api/employee/auth/dingtalk")) {
        expect(init?.body).toBe(JSON.stringify({ corpCode: "sebo", authCode: expectedAuthCode }));
        return new Response(JSON.stringify({ dingUserId: "ding-employee-001", employeeName: "示例员工" }), { status: 200 });
      }
      if (url.includes("/api/employee/invoice-titles/1/qr-token")) {
        return new Response(JSON.stringify({ token: "qr-token", qrPath: "/employee/qr/qr-token", expiresAt: "2026-08-11T10:10:00" }), { status: 200 });
      }
      if (url.includes("/api/employee/invoice-titles")) {
        return new Response(JSON.stringify({ records: [{
          id: 1,
          companyName: "杭州赛宝卓越技术有限公司",
          taxpayerId: "91110400MADFF1HE1T",
          registeredAddress: "浙江省杭州市钱塘区临江街道纬五路3688号临江科创园6号楼12楼",
          phone: "4008696096",
          bankName: "宁波银行股份有限公司北京丰台支行",
          bankAccount: "86041110000957180",
          subjectNames: ["杭州主体"],
        }], total: 1, pageNum: 1, pageSize: 20 }), { status: 200 });
      }
      throw new Error(`unexpected request: ${url}`);
    });
  }

  it("通过钉钉免登后展示服务端授权抬头，不向接口传员工身份", async () => {
    const request = mockEmployeeApis();
    const wrapper = mount(EmployeeApp, { global });
    await flushPromises();

    expect(wrapper.text()).toContain("杭州赛宝卓越技术有限公司");
    expect(wrapper.text()).toContain("91110400MADFF1HE1T");
    expect(wrapper.findAll('[aria-label="选择主体"]')).toHaveLength(1);
    expect(wrapper.text()).not.toContain("管理");
    expect(request.mock.calls.some(([url]) => String(url).includes("/api/employee/auth/dingtalk"))).toBe(true);
    expect(request.mock.calls.some(([url]) => String(url).includes("/api/employee/invoice-titles"))).toBe(true);
    expect(request.mock.calls.some(([url]) => String(url).includes("dingUserId="))).toBe(false);
    expect((window as any).dd.runtime.permission.requestAuthCode)
      .toHaveBeenCalledWith(expect.objectContaining({ corpId: "ding-sebo" }));
  });

  it("与 sebo-meal 一致在钉钉容器没有全局 API 时使用完整 npm SDK", async () => {
    delete (window as any).dd;
    vi.spyOn(window.navigator, "userAgent", "get").mockReturnValue("Mozilla/5.0 AliApp(DingTalk/8.3.45)");
    const request = mockEmployeeApis("sdk-auth-code");

    mount(EmployeeApp, { global });
    await flushPromises();

    expect(bundledDingTalkApi.getAuthCode).toHaveBeenCalledWith({ corpId: "ding-sebo" });
    expect(request.mock.calls.some(([url]) => String(url).includes("/api/employee/auth/dingtalk"))).toBe(true);
  });

  it("与 sebo-meal 一致优先使用官方 getAuthCode 免登入口", async () => {
    const getAuthCode = vi.fn().mockResolvedValue({ authCode: "official-auth-code" });
    (window as any).dd.getAuthCode = getAuthCode;
    (window as any).dd.runtime.permission.requestAuthCode = vi.fn().mockRejectedValue(new Error("不应该调用旧入口"));
    const request = mockEmployeeApis("official-auth-code");

    mount(EmployeeApp, { global });
    await flushPromises();

    expect(getAuthCode).toHaveBeenCalledWith({ corpId: "ding-sebo" });
    expect((window as any).dd.runtime.permission.requestAuthCode).not.toHaveBeenCalled();
    expect(request.mock.calls.some(([url]) => String(url).includes("/api/employee/auth/dingtalk"))).toBe(true);
  });

  it("钉钉 PC 桥接延迟注入时等待桥接就绪后再请求免登码", async () => {
    vi.useFakeTimers();
    delete (window as any).dd;
    const request = mockEmployeeApis();

    mount(EmployeeApp, { global });
    await flushPromises();
    expect(bundledDingTalkApi.getAuthCode).not.toHaveBeenCalled();

    (window as any).DD = {
      runtime: { permission: { requestAuthCode: vi.fn().mockResolvedValue({ code: "ding-auth-code" }) } },
    };
    await vi.advanceTimersByTimeAsync(100);
    await flushPromises();

    expect(request.mock.calls.some(([url]) => String(url).includes("/api/employee/auth/dingtalk"))).toBe(true);
  });

  it("展示钉钉 JSAPI 返回的真实错误信息", async () => {
    (window as any).dd.runtime.permission.requestAuthCode = vi.fn().mockRejectedValueOnce(
      new Error("current environment is not supported"),
    );
    mockEmployeeApis();

    const wrapper = mount(EmployeeApp, { global });
    await flushPromises();

    expect(wrapper.text()).toContain("current environment is not supported");
  });

  it("点击展示二维码后显示十分钟有效的临时二维码", async () => {
    const request = mockEmployeeApis();
    const wrapper = mount(EmployeeApp, { global });
    await flushPromises();

    await wrapper.get('[data-testid="show-qr"]').trigger("click");
    await flushPromises();

    expect(wrapper.get('[role="dialog"]').text()).toContain("抬头二维码");
    expect(wrapper.get('[role="dialog"]').text()).toContain("10:00");
    expect(wrapper.get('[role="dialog"] img').attributes("alt")).toContain("二维码");
    expect(wrapper.get('[role="dialog"] img').attributes("src")).toMatch(/^data:image\/svg\+xml/);
    expect(request.mock.calls.some(([url]) => String(url).includes("/api/employee/invoice-titles/1/qr-token"))).toBe(true);
  });

  it("扫码进入时使用临时令牌加载发布快照且不要求钉钉登录", async () => {
    window.history.replaceState({}, "", "/?qrToken=public-token");
    const request = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({
      id: 3,
      titleId: 1,
      companyName: "杭州赛宝卓越技术有限公司",
      taxpayerId: "91110400MADFF1HE1T",
      registeredAddress: "浙江省杭州市钱塘区临江街道纬五路3688号临江科创园6号楼12楼",
      phone: "4008696096",
      bankName: "宁波银行股份有限公司北京丰台支行",
      bankAccount: "86041110000957180",
    }), { status: 200 }));

    const wrapper = mount(EmployeeApp, { global });
    await flushPromises();

    expect(wrapper.text()).toContain("杭州赛宝卓越技术有限公司");
    expect(request).toHaveBeenCalledWith("/api/employee/invoice-titles/qr/public-token", expect.any(Object));
    expect(request.mock.calls.some(([url]) => String(url).includes("/api/employee/auth/dingtalk"))).toBe(false);
  });
});

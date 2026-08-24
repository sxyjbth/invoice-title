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

  function mockEmployeeApis(
    expectedAuthCode = "ding-auth-code",
    expectedCorpCode = "sebo",
    titleOverrides: Record<string, unknown> = {},
  ) {
    return vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
      const url = String(input);
      if (url.includes("/api/employee/auth/organizations")) {
        return new Response(JSON.stringify([
          { corpCode: "sebo", corpName: "赛宝绿创能源技术（上海）有限公司", corpId: "ding-sebo" },
          { corpCode: "walden", corpName: "瓦尔登环境科学研究院（北京）有限公司", corpId: "ding-walden" },
        ]), { status: 200 });
      }
      if (url.includes("/api/employee/auth/dingtalk")) {
        expect(init?.body).toBe(JSON.stringify({ corpCode: expectedCorpCode, authCode: expectedAuthCode }));
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
          updatedBy: "王财务",
          updatedAt: "2026-08-12T16:30:45",
          ...titleOverrides,
        }], total: 1, pageNum: 1, pageSize: 20 }), { status: 200 });
      }
      throw new Error(`unexpected request: ${url}`);
    });
  }

  it("顶部只展示居中的发票抬头，不展示返回工作台入口", async () => {
    mockEmployeeApis();
    const wrapper = mount(EmployeeApp, { global });
    await flushPromises();

    expect(wrapper.get(".toolbar").text()).toBe("发票抬头");
    expect(wrapper.find(".back-button").exists()).toBe(false);
  });

  it("主体选择使用明确的切换按钮并由按钮打开选项", async () => {
    mockEmployeeApis();
    const wrapper = mount(EmployeeApp, { global });
    await flushPromises();

    const switchButton = wrapper.get('[aria-label="切换主体"]');
    const combobox = wrapper.get('.subject-selector [role="combobox"]');

    expect(switchButton.text()).toBe("切换");
    expect(wrapper.find(".subject-selector .el-select__caret").exists()).toBe(false);
    expect(combobox.attributes("aria-expanded")).toBe("false");

    await switchButton.trigger("click");
    await flushPromises();

    expect(combobox.attributes("aria-expanded")).toBe("true");
  });

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

  it("兼容钉钉工作台通过 corpId 参数传入企业身份", async () => {
    window.history.replaceState({}, "", "/?corpId=ding-walden");
    const request = mockEmployeeApis("ding-auth-code", "walden");

    mount(EmployeeApp, { global });
    await flushPromises();

    expect((window as any).dd.runtime.permission.requestAuthCode)
      .toHaveBeenCalledWith(expect.objectContaining({ corpId: "ding-walden" }));
    expect(request.mock.calls.some(([, init]) => init?.body === JSON.stringify({
      corpCode: "walden",
      authCode: "ding-auth-code",
    }))).toBe(true);
  });

  it("展示真实发布人和抬头更新时间", async () => {
    mockEmployeeApis();
    const wrapper = mount(EmployeeApp, { global });
    await flushPromises();

    expect(wrapper.get(".company-intro p").text())
      .toBe("由王财务发布 · 当前有效 · 2026-08-12 16:30更新");
  });

  it("可选字段为 null 时页面和复制内容均按空白处理", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    vi.spyOn(window.navigator, "clipboard", "get").mockReturnValue({ writeText } as any);
    mockEmployeeApis("ding-auth-code", "sebo", {
      registeredAddress: null,
      phone: null,
      bankName: null,
      bankAccount: null,
    });

    const wrapper = mount(EmployeeApp, { global });
    await flushPromises();

    expect(wrapper.text()).not.toContain("null");
    expect(wrapper.findAll(".field-value").slice(1).map((field) => field.text()))
      .toEqual(["", "", "", ""]);

    await wrapper.get('[aria-label="复制地址"]').trigger("click");
    await flushPromises();
    expect(writeText).toHaveBeenLastCalledWith("");

    await wrapper.findAll("button").find((button) => button.text().includes("复制全部"))!.trigger("click");
    await flushPromises();
    expect(writeText).toHaveBeenLastCalledWith([
      "公司名称：杭州赛宝卓越技术有限公司",
      "纳税人识别号：91110400MADFF1HE1T",
      "地址：",
      "电话：",
      "开户行：",
      "银行账号：",
    ].join("\n"));
  });

  it("公司名称提供与其他字段一致的单字段复制按钮", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    vi.spyOn(window.navigator, "clipboard", "get").mockReturnValue({ writeText } as any);
    mockEmployeeApis();

    const wrapper = mount(EmployeeApp, { global });
    await flushPromises();

    await wrapper.get('[aria-label="复制公司名称"]').trigger("click");
    await flushPromises();

    expect(writeText).toHaveBeenCalledWith("杭州赛宝卓越技术有限公司");
    expect(wrapper.text()).toContain("公司名称已复制");
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
      createdBy: "王财务",
      createdAt: "2026-08-12T16:30:45",
    }), { status: 200 }));

    const wrapper = mount(EmployeeApp, { global });
    await flushPromises();

    expect(wrapper.text()).toContain("杭州赛宝卓越技术有限公司");
    expect(wrapper.get(".company-intro p").text())
      .toBe("由王财务发布 · 当前有效 · 2026-08-12 16:30更新");
    expect(request).toHaveBeenCalledWith("/api/employee/invoice-titles/qr/public-token", expect.any(Object));
    expect(request.mock.calls.some(([url]) => String(url).includes("/api/employee/auth/dingtalk"))).toBe(false);
    expect(wrapper.find(".employee-header").exists()).toBe(false);
    expect(wrapper.find(".subject-selector").exists()).toBe(false);
    expect(wrapper.find('[data-testid="show-qr"]').exists()).toBe(false);
  });

  it("过期二维码接口返回 410 且响应体不可解析时展示重新获取提示", async () => {
    window.history.replaceState({}, "", "/?qrToken=expired-token");
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 410 }));

    const wrapper = mount(EmployeeApp, { global });
    await flushPromises();

    expect(wrapper.text()).toContain("二维码已过期，请重新获取二维码");
    expect(wrapper.text()).not.toContain("请求失败");
    expect(wrapper.text()).not.toContain("服务暂时不可用");
  });

  it("扫码页面在移动端安全剪贴板不可用时使用兼容复制方案", async () => {
    window.history.replaceState({}, "", "/?qrToken=public-token");
    vi.spyOn(window.navigator, "clipboard", "get").mockReturnValue(undefined as any);
    const legacyCopy = vi.fn().mockReturnValue(true);
    Object.defineProperty(document, "execCommand", {
      configurable: true,
      value: legacyCopy,
    });
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({
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

    await wrapper.findAll("button").find((button) => button.text().includes("复制全部"))!.trigger("click");
    await flushPromises();

    expect(legacyCopy).toHaveBeenCalledWith("copy");
    expect(wrapper.text()).toContain("已复制，可粘贴给开票方");
  });

  it("扫码页面在移动端拒绝 Clipboard API 时继续使用兼容复制方案", async () => {
    window.history.replaceState({}, "", "/?qrToken=public-token");
    const secureCopy = vi.fn().mockRejectedValue(new Error("NotAllowedError"));
    vi.spyOn(window.navigator, "clipboard", "get").mockReturnValue({ writeText: secureCopy } as any);
    const legacyCopy = vi.fn().mockReturnValue(true);
    Object.defineProperty(document, "execCommand", { configurable: true, value: legacyCopy });
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({
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

    await wrapper.findAll("button").find((button) => button.text().includes("复制全部"))!.trigger("click");
    await flushPromises();

    expect(secureCopy).toHaveBeenCalled();
    expect(legacyCopy).toHaveBeenCalledWith("copy");
    expect(wrapper.text()).toContain("已复制，可粘贴给开票方");
  });
});

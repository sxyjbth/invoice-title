import { flushPromises, mount } from "@vue/test-utils";
import ElementPlus from "element-plus";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { afterEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import AdminApp from "../src/App.vue";
import { elementPlusOptions } from "../src/element-plus";

const adminStyles = readFileSync(resolve(process.cwd(), "src/styles.css"), "utf8");

const global = { plugins: [[ElementPlus, elementPlusOptions]] } as any;

afterEach(() => {
  document.body.innerHTML = "";
  vi.restoreAllMocks();
});

describe("财务端发票抬头管理", () => {
  it("原生列表表格样式不影响弹窗内的 Element Plus 表格", () => {
    expect(adminStyles).not.toMatch(/(?:^|})\s*table\s*{/m);
    expect(adminStyles).toContain(".table-scroll table { width: 100%; min-width: 1030px;");
  });

  it("页面标题区域不再展示钉钉工作台面包屑", () => {
    const wrapper = mount(AdminApp, { global });

    expect(wrapper.get(".topbar").text()).not.toContain("钉钉工作台 / 财务管理");
  });

  it("页面标题区域不再展示通讯录同步提示", () => {
    const wrapper = mount(AdminApp, { global });

    expect(wrapper.get(".topbar").text()).not.toContain("钉钉通讯录已同步");
  });

  it("登录账号名称与角色相同时使用用户名区分两行信息", () => {
    const wrapper = mount(AdminApp, { global });
    const profile = wrapper.get('[aria-label="当前登录账号"]');

    expect(profile.get("strong").text()).toBe("superadmin");
    expect(profile.get("small").text()).toBe("超级管理员");
  });

  it("批量导入只作为抬头管理页面操作而不是一级导航", () => {
    const wrapper = mount(AdminApp, { global });

    expect(wrapper.get("nav").text()).not.toContain("批量导入");
    expect(wrapper.get('[data-testid="batch-import"]').text()).toContain("批量导入");
  });

  it("提供全部、已发布、草稿和已停用状态筛选", async () => {
    const wrapper = mount(AdminApp, { global });

    const statusFilter = wrapper.get('[aria-label="抬头状态筛选"]');
    expect(statusFilter.text()).toContain("全部");
    expect(statusFilter.text()).toContain("已发布");
    expect(statusFilter.text()).toContain("草稿");
    expect(statusFilter.text()).toContain("已停用");

    await statusFilter.get('[data-status="DRAFT"]').trigger("click");
    expect(wrapper.get("tbody").text()).toContain("北京示例技术服务有限公司");
    expect(wrapper.get("tbody").text()).not.toContain("杭州赛宝卓越技术有限公司");
  });

  it("分页组件使用中文文案", () => {
    const wrapper = mount(AdminApp, { global });
    const pagination = wrapper.get('[aria-label="抬头列表分页"]');
    expect(pagination.text()).toContain("共 3 条");
    expect(pagination.text()).toContain("前往");
    // Happy DOM 不会展开分页下拉项，直接校验传给 Element Plus 的中文页容量文案。
    expect(elementPlusOptions.locale.el.pagination.pagesize).toBe("条/页");
  });

  it("新增和编辑抬头打开真实表单并保存到后端", async () => {
    const request = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response("99", { status: 200 }));
    const wrapper = mount(AdminApp, { global, attachTo: document.body });

    await wrapper.findAll("button").find((item) => item.text().includes("新增抬头"))!.trigger("click");
    await nextTick();
    expect(document.body.textContent).toContain("新增发票抬头");

    const dialogInputs = Array.from(document.body.querySelectorAll<HTMLInputElement>(".el-dialog input"));
    dialogInputs[0].value = "上线新增有限公司";
    dialogInputs[0].dispatchEvent(new Event("input"));
    dialogInputs[1].value = "91330100ONLINE0002";
    dialogInputs[1].dispatchEvent(new Event("input"));
    const saveDraft = Array.from(document.body.querySelectorAll<HTMLButtonElement>(".el-dialog button"))
      .find((button) => button.textContent?.includes("保存草稿"))!;
    saveDraft.click();
    await nextTick();
    expect(request).toHaveBeenCalledWith("/api/admin/invoice-titles", expect.objectContaining({ method: "POST" }));

    const editButton = wrapper.findAll("tbody tr")[0].findAll("button").find((item) => item.text().includes("编辑"))!;
    await editButton.trigger("click");
    await nextTick();
    expect(document.body.textContent).toContain("编辑发票抬头");
    expect((document.body.querySelector(".el-dialog input") as HTMLInputElement).value).toBe("杭州赛宝卓越技术有限公司");
    wrapper.unmount();
  });

  it("历史版本列表使用分页且恢复入口不再是静态占位", async () => {
    const wrapper = mount(AdminApp, { global, attachTo: document.body });
    await wrapper.findAll("tbody tr")[0].findAll("button").find((item) => item.text().includes("查看版本"))!.trigger("click");
    await nextTick();

    expect(document.body.querySelector('[aria-label="历史版本列表分页"]')).not.toBeNull();
    expect(document.body.textContent).toContain("恢复为草稿");
    wrapper.unmount();
  });

  it("主体管理展示可维护的主体列表并使用分页", async () => {
    const wrapper = mount(AdminApp, { global });
    await wrapper.findAll("nav button").find((item) => item.text().includes("主体管理"))!.trigger("click");

    expect(wrapper.get("tbody").text()).toContain("杭州主体");
    expect(wrapper.get("tbody").text()).toContain("HZ");
    expect(wrapper.find('[aria-label="主体管理列表分页"]').exists()).toBe(true);
  });

  it("主体列表可按启用或停用状态筛选", async () => {
    const wrapper = mount(AdminApp, { global });
    await wrapper.findAll("nav button").find((item) => item.text().includes("主体管理"))!.trigger("click");

    const statusFilter = wrapper.get('[aria-label="主体状态筛选"]');
    expect(statusFilter.text()).toContain("全部");
    expect(statusFilter.text()).toContain("启用");
    expect(statusFilter.text()).toContain("停用");

    await statusFilter.get('[data-status="DISABLED"]').trigger("click");
    expect(wrapper.get("tbody").text()).not.toContain("杭州主体");
  });

  it("主体支持新增、编辑和停用操作", async () => {
    const request = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response("", { status: 200 }));
    const wrapper = mount(AdminApp, { global, attachTo: document.body });
    await wrapper.findAll("nav button").find((item) => item.text().includes("主体管理"))!.trigger("click");

    await wrapper.findAll("button").find((item) => item.text().includes("新增主体"))!.trigger("click");
    await nextTick();
    expect(document.body.textContent).toContain("新增主体");
    const cancel = Array.from(document.body.querySelectorAll<HTMLButtonElement>(".el-dialog button"))
      .find((button) => button.textContent?.includes("取消"));
    cancel?.click();
    await nextTick();

    const firstActions = wrapper.findAll("tbody tr")[0].findAll("button");
    await firstActions.find((item) => item.text().includes("编辑"))!.trigger("click");
    await nextTick();
    expect(document.body.textContent).toContain("编辑主体");
    expect((document.body.querySelector(".el-dialog input") as HTMLInputElement).value).toBe("杭州主体");
    const close = document.body.querySelector<HTMLButtonElement>(".el-dialog__headerbtn");
    close?.click();
    await nextTick();

    await firstActions.find((item) => item.text().includes("停用"))!.trigger("click");
    expect(request).toHaveBeenCalledWith(expect.stringContaining("/api/admin/subjects/1/status?status=DISABLED"), expect.objectContaining({ method: "PATCH" }));
    wrapper.unmount();
  });

  it("主体权限按主体聚合配置全员、部门和员工授权", async () => {
    const wrapper = mount(AdminApp, { global });
    await wrapper.findAll("nav button").find((item) => item.text().includes("主体权限"))!.trigger("click");

    const permissionPage = wrapper.get('[aria-label="主体权限配置"]');
    expect(permissionPage.text()).toContain("选择主体");
    expect(permissionPage.text()).toContain("当前可见 128 人");
    expect(permissionPage.text()).toContain("全员可见");
    expect(permissionPage.text()).toContain("部门授权");
    expect(permissionPage.text()).toContain("技术中心 · 86 人");
    expect(permissionPage.text()).toContain("员工授权");
    expect(permissionPage.text()).toContain("单独授权 12 名员工");
    expect(permissionPage.text()).toContain("保存权限");

    await permissionPage.findAll("button").find((item) => item.text().includes("北京主体"))!.trigger("click");
    expect(permissionPage.text()).toContain("当前可见 46 人");
  });

  it("没有主体时主体权限页面显示引导而不是白屏", async () => {
    const wrapper = mount(AdminApp, { global });
    await wrapper.findAll("nav button").find((item) => item.text().includes("主体权限"))!.trigger("click");

    (wrapper.vm as any).permissionProfiles = [];
    await nextTick();

    const permissionPage = wrapper.get('[aria-label="主体权限配置"]');
    expect(permissionPage.text()).toContain("暂无主体可配置权限");
    expect(permissionPage.text()).toContain("前往主体管理");
  });

  it("员工权限以启用开关呈现部门授权结果并支持按最终状态筛选", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ records: [{
      id: 1,
      dingUserId: "ding-employee-001",
      employeeNo: "SB0001",
      employeeName: "陈一",
      departmentId: 1,
      departmentName: "技术中心",
      mobile: "13800000001",
      permissionEnabled: true,
    }], total: 1 }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    }));
    const wrapper = mount(AdminApp, { global, attachTo: document.body });
    await wrapper.findAll("nav button").find((item) => item.text().includes("主体权限"))!.trigger("click");

    const permissionPage = wrapper.get('[aria-label="主体权限配置"]');
    await permissionPage.findAll("button").find((item) => item.text().includes("编辑") && item.element.closest(".permission-level-row")?.textContent?.includes("员工授权"))!.trigger("click");
    await nextTick();
    await flushPromises();
    expect(document.body.querySelector('input[placeholder="搜索姓名、工号、部门或手机号"]')).not.toBeNull();
    expect(document.body.textContent).toContain("权限状态");
    expect(document.body.textContent).toContain("已启用");
    expect(document.body.textContent).toContain("已关闭");
    expect(document.body.textContent).not.toContain("继承部门");
    expect(document.body.querySelector('[aria-label="陈一的查看权限"]')?.closest(".el-switch")?.classList.contains("is-checked")).toBe(true);
    expect(document.body.textContent).not.toContain("钉钉对象 ID");

    const close = document.body.querySelector<HTMLButtonElement>(".el-dialog__headerbtn");
    close?.click();
    await nextTick();
    await permissionPage.findAll("button").find((item) => item.text().includes("编辑") && item.element.closest(".permission-level-row")?.textContent?.includes("部门授权"))!.trigger("click");
    await nextTick();
    expect(document.body.textContent).toContain("从通讯录部门中选择");
    expect(document.body.textContent).not.toContain("请输入部门名称");
    wrapper.unmount();
  });

  it("员工授权弹窗确认选择后立即保存个人权限", async () => {
    const request = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ records: [{
      id: 99,
      corpCode: "sebo",
      corpName: "赛宝绿创能源技术（上海）有限公司",
      dingUserId: "ding-sun-xinyao",
      employeeNo: "R04952",
      employeeName: "孙鑫尧",
      departmentId: 99,
      departmentName: "平台开发部",
      mobile: "13936725713",
      permissionEnabled: false,
    }], total: 1 }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    }));
    const wrapper = mount(AdminApp, { global, attachTo: document.body });
    await wrapper.findAll("nav button").find((item) => item.text().includes("主体权限"))!.trigger("click");

    const permissionPage = wrapper.get('[aria-label="主体权限配置"]');
    await permissionPage.findAll("button").find((item) => item.text().includes("编辑") && item.element.closest(".permission-level-row")?.textContent?.includes("员工授权"))!.trigger("click");
    await flushPromises();
    (document.body.querySelector('[aria-label="孙鑫尧的查看权限"]') as HTMLElement).click();
    await nextTick();
    Array.from(document.body.querySelectorAll<HTMLButtonElement>(".el-dialog button"))
      .find((button) => button.textContent?.includes("确定选择"))!.click();
    await flushPromises();

    expect(request).toHaveBeenCalledWith("/api/admin/subjects/1/permission-profile", expect.objectContaining({
      method: "PUT",
      body: expect.stringContaining('"employeeId":99,"effect":"ALLOW"'),
    }));
    wrapper.unmount();
  });

  it("后端 employeeId 字段返回的既有员工授权可以正确回显", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ records: [{
      id: 99,
      corpCode: "sebo",
      corpName: "赛宝绿创能源技术（上海）有限公司",
      dingUserId: "ding-sun-xinyao",
      employeeNo: "R04952",
      employeeName: "孙鑫尧",
      departmentId: 99,
      departmentName: "平台开发部",
      mobile: "13936725713",
      permissionEnabled: true,
    }], total: 1 }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    }));
    const wrapper = mount(AdminApp, { global, attachTo: document.body });
    await wrapper.findAll("nav button").find((item) => item.text().includes("主体权限"))!.trigger("click");
    (wrapper.vm as any).permissionProfiles[0].employeeRules = [{
      employeeId: 99,
      employeeName: "孙鑫尧",
      employeeNo: "R04952",
      dingUserId: "ding-sun-xinyao",
      departmentName: "平台开发部",
      effect: "ALLOW",
    }];

    const permissionPage = wrapper.get('[aria-label="主体权限配置"]');
    await permissionPage.findAll("button").find((item) => item.text().includes("编辑") && item.element.closest(".permission-level-row")?.textContent?.includes("员工授权"))!.trigger("click");
    await flushPromises();

    expect(document.body.querySelector('[aria-label="孙鑫尧的查看权限"]')?.closest(".el-switch")?.classList.contains("is-checked")).toBe(true);
    wrapper.unmount();
  });

  it("操作日志展示可追溯的业务操作并使用分页", async () => {
    const wrapper = mount(AdminApp, { global });
    await wrapper.findAll("nav button").find((item) => item.text().includes("操作日志"))!.trigger("click");

    expect(wrapper.get("tbody").text()).toContain("发布抬头");
    expect(wrapper.get("tbody").text()).toContain("王财务");
    expect(wrapper.find('[aria-label="操作日志列表分页"]').exists()).toBe(true);
  });

  it("操作日志将操作类型代码显示为中文", async () => {
    const wrapper = mount(AdminApp, { global });
    await wrapper.findAll("nav button").find((item) => item.text().includes("操作日志"))!.trigger("click");

    (wrapper.vm as any).operationLogs[0].action = "CHANGE_PASSWORD";
    await nextTick();

    expect(wrapper.get("tbody").text()).toContain("修改密码");
    expect(wrapper.get("tbody").text()).not.toContain("CHANGE_PASSWORD");
  });

  it("操作日志列表和详情均不展示客户端 IP", async () => {
    const wrapper = mount(AdminApp, { global, attachTo: document.body });
    await wrapper.findAll("nav button").find((item) => item.text().includes("操作日志"))!.trigger("click");

    expect(wrapper.get("main").text()).not.toContain("客户端 IP");
    await wrapper.findAll("tbody button")[0].trigger("click");
    await nextTick();
    expect(document.body.textContent).not.toContain("客户端 IP");
    wrapper.unmount();
  });

  it("财务账号接口返回 401 时退出失效会话并显示登录页", async () => {
    const wrapper = mount(AdminApp, { global });
    const handleFinanceAccountResponse = (wrapper.vm as any).handleFinanceAccountResponse;

    expect(handleFinanceAccountResponse).toBeTypeOf("function");
    if (!handleFinanceAccountResponse) return;

    await handleFinanceAccountResponse(new Response('{"message":"请先登录财务管理端"}', {
      status: 401,
      headers: { "Content-Type": "application/json" },
    }));
    await nextTick();

    expect(wrapper.find(".login-page").exists()).toBe(true);
  });

  it("批量导入使用真实文件选择并为导入历史提供分页", async () => {
    const wrapper = mount(AdminApp, { global, attachTo: document.body });
    await wrapper.get('[data-testid="batch-import"]').trigger("click");

    expect(document.body.querySelector('input[type="file"][accept=".xlsx"]')).not.toBeNull();
    expect(document.body.querySelector('[aria-label="导入历史分页"]')).not.toBeNull();
    wrapper.unmount();
  });
});

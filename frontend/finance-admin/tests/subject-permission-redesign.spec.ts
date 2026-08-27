import { DOMWrapper, flushPromises, mount } from "@vue/test-utils";
import ElementPlus from "element-plus";
import { createPinia } from "pinia";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { afterEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { createMemoryHistory } from "vue-router";
import AdminApp from "../src/App.vue";
import { elementPlusOptions } from "../src/element-plus";
import FinanceLayout from "../src/layouts/FinanceLayout.vue";
import { createFinanceRouter } from "../src/router";

const adminStyles = readFileSync(resolve(process.cwd(), "src/styles.css"), "utf8");

async function mountPermissionPage() {
  const router = createFinanceRouter(createMemoryHistory());
  await router.push("/permissions");
  await router.isReady();
  const wrapper = mount(AdminApp, {
    attachTo: document.body,
    global: { plugins: [createPinia(), [ElementPlus, elementPlusOptions], router] },
  });
  await flushPromises();
  return wrapper;
}

function layoutVm(wrapper: Awaited<ReturnType<typeof mountPermissionPage>>) {
  return wrapper.getComponent(FinanceLayout).vm as any;
}

function mockDualEnterpriseDirectory(requests: Array<{ url: string; method: string; body?: unknown }> = []) {
  const organizations = [
    { corpCode: "sebo", corpName: "赛宝绿创能源技术（上海）有限公司" },
    { corpCode: "walden", corpName: "瓦尔登环境科学研究院（北京）有限公司" },
  ];
  const departments = [
    {
      id: 11,
      corpCode: "sebo",
      corpName: organizations[0].corpName,
      dingDepartmentId: "ding-dept-platform",
      departmentName: "平台开发部",
      employeeCount: 2,
    },
    {
      id: 21,
      corpCode: "walden",
      corpName: organizations[1].corpName,
      dingDepartmentId: "ding-dept-research",
      departmentName: "数智化中心",
      employeeCount: 1,
    },
  ];
  const employees = [
    {
      id: 101,
      corpCode: "sebo",
      corpName: organizations[0].corpName,
      dingUserId: "ding-user-sun-sebo",
      employeeNo: "R04952",
      employeeName: "孙鑫尧",
      departmentId: 11,
      departmentName: "平台开发部",
      mobile: "13936725713",
    },
    {
      id: 102,
      corpCode: "sebo",
      corpName: organizations[0].corpName,
      dingUserId: "ding-user-li-sebo",
      employeeNo: "R01411",
      employeeName: "李晨",
      departmentId: 11,
      departmentName: "平台开发部",
      mobile: "18223148993",
    },
    {
      id: 201,
      corpCode: "walden",
      corpName: organizations[1].corpName,
      dingUserId: "ding-user-wang-walden",
      employeeNo: "W0001",
      employeeName: "王月",
      departmentId: 21,
      departmentName: "数智化中心",
      mobile: "13800000001",
    },
  ];

  return vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
    const url = new URL(String(input), "http://localhost");
    const method = init?.method ?? "GET";
    requests.push({
      url: `${url.pathname}${url.search}`,
      method,
      body: typeof init?.body === "string" ? JSON.parse(init.body) : undefined,
    });
    const json = (value: unknown) => new Response(JSON.stringify(value), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
    if (url.pathname.endsWith("/directory/organizations")) return json(organizations);
    if (url.pathname.endsWith("/directory/departments")) {
      const keyword = url.searchParams.get("keyword") ?? "";
      const records = departments.filter((department) => !keyword || department.departmentName.includes(keyword));
      return json({ records, total: records.length });
    }
    if (url.pathname.endsWith("/directory/employees")) {
      const keyword = url.searchParams.get("keyword") ?? "";
      const departmentId = Number(url.searchParams.get("departmentId"));
      const records = employees.filter((employee) =>
        (!departmentId || employee.departmentId === departmentId)
        && (!keyword || [employee.employeeName, employee.employeeNo, employee.departmentName, employee.mobile]
          .some((value) => value.includes(keyword))));
      return json({ records, total: records.length });
    }
    if (url.pathname.endsWith("/directory/employee-selections/resolve")) {
      const body = typeof init?.body === "string" ? JSON.parse(init.body) : {};
      const selectedEmployees = employees
        .filter((employee) => !body.corpCodes?.length || body.corpCodes.includes(employee.corpCode))
        .map((employee) => ({ ...employee, departmentIds: [employee.departmentId] }));
      return json({
        selectedEmployeeCount: selectedEmployees.length,
        selectedEmployeeIds: selectedEmployees.map((employee) => employee.id),
        selectedEmployees,
        employeeGroups: [],
      });
    }
    return json({});
  });
}

function mockInitiallyPartialOrganizationDirectory() {
  const organization = {
    corpCode: "sebo",
    corpName: "赛宝绿创能源技术（上海）有限公司",
  };
  const selectedDepartment = {
    id: 11,
    corpCode: organization.corpCode,
    corpName: organization.corpName,
    dingDepartmentId: "ding-dept-selected",
    departmentName: "已授权部门",
    employeeCount: 1,
  };
  const unselectedDepartment = {
    id: 12,
    corpCode: organization.corpCode,
    corpName: organization.corpName,
    dingDepartmentId: "ding-dept-unselected",
    departmentName: "未授权部门",
    employeeCount: 1,
  };

  return vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
    const url = new URL(String(input), "http://localhost");
    const json = (value: unknown) => new Response(JSON.stringify(value), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
    if (url.pathname.endsWith("/directory/organizations")) return json([organization]);
    if (url.pathname.endsWith("/directory/departments")) {
      // 首屏通讯录分页只返回一个已授权部门；企业级状态必须补齐完整目录后再判断。
      if (url.searchParams.get("pageSize") === "100") {
        return json({ records: [selectedDepartment, unselectedDepartment], total: 2 });
      }
      return json({ records: [selectedDepartment], total: 2 });
    }
    if (url.pathname.endsWith("/directory/employees")) return json({ records: [], total: 0 });
    return json({});
  });
}

async function openPermissionEditor(wrapper: Awaited<ReturnType<typeof mountPermissionPage>>) {
  const edit = wrapper.findAll("button").find((button) => button.text().includes("编辑部分可见范围"));
  if (!edit) throw new Error("未找到编辑部分可见范围入口");
  await edit.trigger("click");
  await flushPromises();
  const dialogs = document.body.querySelectorAll<HTMLElement>('.partial-permission-dialog[aria-label="编辑可见范围"]');
  const dialog = dialogs.item(dialogs.length - 1);
  if (!dialog) throw new Error("未找到编辑可见范围弹窗");
  return dialog;
}

async function expandOrganization(dialog: HTMLElement, corpCode: "sebo" | "walden") {
  const organization = dialog.querySelector<HTMLElement>(`[data-corp-code="${corpCode}"]`);
  if (!organization) throw new Error(`未找到企业节点：${corpCode}`);
  organization.click();
  await flushPromises();
}

async function expandDepartment(dialog: HTMLElement, departmentId: number) {
  const department = dialog.querySelector<HTMLElement>(`[data-department-id="${departmentId}"]`);
  if (!department) throw new Error(`未找到部门节点：${departmentId}`);
  department.click();
  await flushPromises();
}

function departmentCheckbox(dialog: HTMLElement, departmentName: string) {
  const checkbox = dialog.querySelector<HTMLElement>(`[aria-label="选择部门 ${departmentName}"]`);
  if (!checkbox) throw new Error(`未找到部门复选框：${departmentName}`);
  return checkbox.classList.contains("el-checkbox") ? checkbox : checkbox.closest<HTMLElement>(".el-checkbox") ?? checkbox;
}

function corporationCheckbox(dialog: HTMLElement, corporationName: string) {
  const checkbox = dialog.querySelector<HTMLElement>(`[aria-label="选择企业 ${corporationName}"]`);
  if (!checkbox) throw new Error(`未找到企业复选框：${corporationName}`);
  return checkbox.classList.contains("el-checkbox") ? checkbox : checkbox.closest<HTMLElement>(".el-checkbox") ?? checkbox;
}

function employeeRow(dialog: HTMLElement, employeeName: string) {
  const row = Array.from(dialog.querySelectorAll<HTMLElement>(".permission-employee-row"))
    .find((item) => item.textContent?.includes(employeeName));
  if (!row) throw new Error(`未找到员工节点：${employeeName}`);
  return row;
}

function selectedEmployeeNames(dialog: HTMLElement) {
  return Array.from(dialog.querySelectorAll<HTMLElement>(".permission-selected-chip strong"))
    .map((item) => item.textContent?.trim());
}

async function searchDirectory(dialog: HTMLElement, keyword: string) {
  const input = dialog.querySelector<HTMLInputElement>('[aria-label="搜索部门或员工"]');
  if (!input) throw new Error("未找到权限搜索框");
  input.value = keyword;
  input.dispatchEvent(new Event("input", { bubbles: true }));
  input.dispatchEvent(new KeyboardEvent("keyup", { key: "Enter", bubbles: true }));
  await flushPromises();
}

async function selectSearchTab(dialog: HTMLElement, label: "全部" | "联系人" | "部门") {
  const tab = Array.from(dialog.querySelectorAll<HTMLButtonElement>('[role="tab"]'))
    .find((item) => item.textContent?.trim() === label);
  if (!tab) throw new Error(`未找到搜索分类：${label}`);
  tab.click();
  await flushPromises();
}

afterEach(() => {
  document.body.innerHTML = "";
  vi.restoreAllMocks();
  vi.useRealTimers();
});

describe("主体权限双企业可见范围重设计（红灯契约）", () => {
  it("员工权限规则优先使用 employeeId，避免把规则主键误当员工主键", async () => {
    const wrapper = await mountPermissionPage();

    expect(layoutVm(wrapper).employeeRuleId({ id: 999, employeeId: 101 })).toBe(101);

    wrapper.unmount();
  });

  it("权限详情卡片收窄为可用区域的 70% 并保持左对齐", async () => {
    const wrapper = await mountPermissionPage();
    const card = wrapper.get('[aria-label="主体权限配置"] .permission-detail-card');

    expect(card.classes()).toContain("permission-detail-card-compact");
    expect(adminStyles).toMatch(/\.permission-detail-card-compact\s*\{[^}]*width:\s*70%[^}]*justify-self:\s*start/s);

    wrapper.unmount();
  });

  it("部分可见首页仅按赛宝和瓦尔登双栏展示已选人员姓名", async () => {
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].allEmployeesVisible = false;
    vm.permissionProfiles[0].employeeRules = [
      { id: 101, corpCode: "sebo", corpName: "赛宝绿创能源技术（上海）有限公司", employeeName: "孙鑫尧", effect: "ALLOW" },
      { id: 201, corpCode: "walden", corpName: "瓦尔登环境科学研究院（北京）有限公司", employeeName: "王月", effect: "ALLOW" },
      { id: 999, corpCode: "sebo", corpName: "赛宝绿创能源技术（上海）有限公司", employeeName: "不应展示", effect: "DENY" },
    ];
    await nextTick();

    const partialSummary = wrapper.get(".permission-mode-summary-partial");
    expect(partialSummary.findAll(".permission-corporation-members > section")).toHaveLength(2);
    expect(partialSummary.get('[aria-label="赛宝已选人员"]').text()).toContain("孙鑫尧");
    expect(partialSummary.get('[aria-label="瓦尔登已选人员"]').text()).toContain("王月");
    expect(partialSummary.text()).not.toContain("不应展示");
    expect(partialSummary.find(".permission-summary-stats").exists()).toBe(false);
    expect(partialSummary.find(".permission-tags").exists()).toBe(false);

    wrapper.unmount();
  });

  it("双企业已选人员姓名栏在人员较多时分别独立滚动且少量人员布局不变", async () => {
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].allEmployeesVisible = false;
    vm.permissionProfiles[0].employeeRules = [
      ...Array.from({ length: 18 }, (_, index) => ({
        id: 1000 + index,
        corpCode: "sebo",
        corpName: "赛宝绿创能源技术（上海）有限公司",
        employeeName: `赛宝员工${index + 1}`,
        effect: "ALLOW",
      })),
      {
        id: 2001,
        corpCode: "walden",
        corpName: "瓦尔登环境科学研究院（北京）有限公司",
        employeeName: "瓦尔登员工1",
        effect: "ALLOW",
      },
    ];
    await nextTick();

    expect(adminStyles).toMatch(/\.permission-member-names\s*\{[^}]*max-height:\s*\d+px[^}]*overflow-y:\s*auto/s);

    const seboSection = wrapper.get('[aria-label="赛宝已选人员"]');
    const waldenSection = wrapper.get('[aria-label="瓦尔登已选人员"]');
    const seboNames = seboSection.get('.permission-member-names[aria-label="赛宝已选人员姓名列表"]');
    const waldenNames = waldenSection.get('.permission-member-names[aria-label="瓦尔登已选人员姓名列表"]');
    expect(seboNames.element).not.toBe(waldenNames.element);
    expect(seboNames.findAll("span")).toHaveLength(18);
    expect(waldenNames.findAll("span")).toHaveLength(1);
    expect(waldenNames.text()).toContain("瓦尔登员工1");

    wrapper.unmount();
  });

  it("编辑器采用左右两栏并提供企业到部门到员工的选择树和已选择标签", async () => {
    mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    const dialog = await openPermissionEditor(wrapper);

    expect(dialog.querySelector('[aria-label="企业部门员工选择树"]')).not.toBeNull();
    expect(dialog.querySelector('[aria-label="已选择人员"]')).not.toBeNull();
    const enterpriseRoots = dialog.querySelectorAll(".permission-corp-row");
    expect(enterpriseRoots).toHaveLength(2);
    expect(dialog.textContent).toContain("赛宝绿创能源技术（上海）有限公司");
    expect(dialog.textContent).toContain("瓦尔登环境科学研究院（北京）有限公司");

    await expandOrganization(dialog, "sebo");
    expect(dialog.textContent).toContain("平台开发部");
    await expandDepartment(dialog, 11);
    expect(dialog.textContent).toContain("孙鑫尧");
    expect(dialog.textContent).toContain("李晨");

    await expandOrganization(dialog, "walden");
    expect(dialog.textContent).toContain("数智化中心");
    await expandDepartment(dialog, 21);
    expect(dialog.textContent).toContain("王月");

    employeeRow(dialog, "王月").click();
    await nextTick();
    expect(selectedEmployeeNames(dialog)).toContain("王月");

    wrapper.unmount();
  });

  it("编辑器右侧已选择人员按瓦尔登在上赛宝在下分区展示", async () => {
    mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].departments = [];
    vm.permissionProfiles[0].employeeRules = [];
    const dialog = await openPermissionEditor(wrapper);

    await expandOrganization(dialog, "sebo");
    await expandDepartment(dialog, 11);
    employeeRow(dialog, "孙鑫尧").click();
    await nextTick();

    await expandOrganization(dialog, "walden");
    await expandDepartment(dialog, 21);
    employeeRow(dialog, "王月").click();
    await nextTick();

    const selectedPane = dialog.querySelector<HTMLElement>('[aria-label="已选择人员"]')!;
    const corporationSections = selectedPane.querySelectorAll<HTMLElement>(".permission-selected-corporation");
    expect(corporationSections).toHaveLength(2);
    expect(corporationSections[0].getAttribute("aria-label")).toBe("瓦尔登已选择人员");
    expect(corporationSections[1].getAttribute("aria-label")).toBe("赛宝已选择人员");
    expect(selectedEmployeeNames(corporationSections[0])).toEqual(["王月"]);
    expect(selectedEmployeeNames(corporationSections[1])).toEqual(["孙鑫尧"]);
    expect(selectedPane.querySelectorAll("details.permission-selected-corporation")).toHaveLength(0);
    expect(adminStyles).toMatch(/\.permission-selected-groups\s*\{[^}]*grid-template-rows:\s*minmax\(0,\s*1fr\)\s+minmax\(0,\s*1fr\)/s);
    expect(adminStyles).toMatch(/\.permission-selected-corporation\s+\.permission-selected-chips\s*\{[^}]*overflow-y:\s*auto[^}]*overscroll-behavior-y:\s*contain/s);

    wrapper.unmount();
  });

  it("企业节点提供前置复选框并可一次选择该企业全部员工", async () => {
    mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].departments = [];
    vm.permissionProfiles[0].employeeRules = [];
    const dialog = await openPermissionEditor(wrapper);

    const checkbox = corporationCheckbox(dialog, "赛宝绿创能源技术（上海）有限公司");
    expect(checkbox.compareDocumentPosition(dialog.querySelector('[data-corp-code="sebo"]')!)
      & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    checkbox.click();
    await flushPromises();

    expect(selectedEmployeeNames(dialog).sort()).toEqual(["孙鑫尧", "李晨"].sort());

    wrapper.unmount();
  });

  it("企业全选只发起一次批量解析请求且不再逐部门加载员工", async () => {
    const requests: Array<{ url: string; method: string; body?: unknown }> = [];
    mockDualEnterpriseDirectory(requests);
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].departments = [];
    vm.permissionProfiles[0].employeeRules = [];
    const dialog = await openPermissionEditor(wrapper);
    requests.length = 0;

    corporationCheckbox(dialog, "赛宝绿创能源技术（上海）有限公司").click();
    await flushPromises();

    const batchRequests = requests.filter((request) => request.url.endsWith("/directory/employee-selections/resolve"));
    const memberPageRequests = requests.filter((request) => request.url.includes("/directory/employees?"));
    expect(batchRequests).toHaveLength(1);
    expect(batchRequests[0]).toMatchObject({
      method: "POST",
      body: { corpCodes: ["sebo"], departmentIds: [], employeeIds: [] },
    });
    expect(memberPageRequests).toHaveLength(0);
    expect(selectedEmployeeNames(dialog).sort()).toEqual(["孙鑫尧", "李晨"].sort());

    wrapper.unmount();
  });

  it("员工节点使用前置方框复选框且不再显示选择或已选尾标", async () => {
    mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    const dialog = await openPermissionEditor(wrapper);
    await expandOrganization(dialog, "sebo");
    await expandDepartment(dialog, 11);

    const row = employeeRow(dialog, "孙鑫尧");
    const checkbox = row.querySelector<HTMLElement>('[aria-label="选择员工 孙鑫尧"]');
    expect(checkbox).not.toBeNull();
    expect(row.firstElementChild?.classList.contains("el-checkbox")).toBe(true);
    expect(row.textContent).not.toContain("选择");
    expect(row.textContent).not.toContain("已选");

    checkbox!.click();
    await nextTick();
    expect(selectedEmployeeNames(dialog)).toContain("孙鑫尧");

    wrapper.unmount();
  });

  it("权限搜索框只提供一个可见清除控件", async () => {
    mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    const dialog = await openPermissionEditor(wrapper);
    await searchDirectory(dialog, "孙");

    const input = dialog.querySelector<HTMLInputElement>('[aria-label="搜索部门或员工"]')!;
    const clearButtons = dialog.querySelectorAll<HTMLButtonElement>('[aria-label="清空搜索"]');
    expect(input.type).toBe("text");
    expect(clearButtons).toHaveLength(1);

    clearButtons[0].click();
    await flushPromises();
    expect(input.value).toBe("");

    wrapper.unmount();
  });

  it("主页当前可见人数按实际已选员工去重统计", async () => {
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].allEmployeesVisible = false;
    vm.permissionProfiles[0].visibleCount = 3;
    vm.permissionProfiles[0].employeeRules = [
      { id: 999, employeeId: 101, corpCode: "sebo", employeeName: "李晨", effect: "ALLOW" },
    ];
    await nextTick();

    const activeSubject = wrapper.get(".permission-subject-card button.active");
    const detailHeader = wrapper.get(".permission-detail-card > header");
    expect(activeSubject.text()).toContain("当前可见 1 人");
    expect(detailHeader.text()).toContain("当前可见 1 人");
    expect(wrapper.get('[aria-label="赛宝已选人员"]').text()).toContain("1 人");

    wrapper.unmount();
  });

  it("跨企业员工数值 ID 相同时仍按企业身份分别计入当前可见人数", async () => {
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].allEmployeesVisible = false;
    vm.permissionProfiles[0].visibleCount = 1;
    vm.permissionProfiles[0].departments = [];
    vm.permissionProfiles[0].employeeRules = [
      {
        id: 9001,
        employeeId: 101,
        corpCode: "sebo",
        corpName: "赛宝绿创能源技术（上海）有限公司",
        employeeName: "赛宝同号员工",
        effect: "ALLOW",
      },
      {
        id: 9002,
        employeeId: 101,
        corpCode: "walden",
        corpName: "瓦尔登环境科学研究院（北京）有限公司",
        employeeName: "瓦尔登同号员工",
        effect: "ALLOW",
      },
    ];
    await nextTick();

    expect(vm.selectedPermissionEmployees.map((employee: any) => `${employee.corpCode}:${employee.employeeName}`))
      .toEqual(["sebo:赛宝同号员工", "walden:瓦尔登同号员工"]);
    expect(wrapper.get(".permission-subject-card button.active").text()).toContain("当前可见 2 人");
    expect(wrapper.get(".permission-detail-card > header").text()).toContain("当前可见 2 人");

    wrapper.unmount();
  });

  it("企业复选框首次展示会按完整部门目录判断半选而不是按首屏误判全选", async () => {
    mockInitiallyPartialOrganizationDirectory();
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].allEmployeesVisible = false;
    vm.permissionProfiles[0].departments = [{
      id: 11,
      corpCode: "sebo",
      corpName: "赛宝绿创能源技术（上海）有限公司",
      dingDepartmentId: "ding-dept-selected",
      departmentName: "已授权部门",
      employeeCount: 1,
    }];
    vm.permissionProfiles[0].employeeRules = [];
    const dialog = await openPermissionEditor(wrapper);
    const checkbox = corporationCheckbox(dialog, "赛宝绿创能源技术（上海）有限公司");

    expect(checkbox.classList.contains("is-checked")).toBe(false);
    expect(checkbox.querySelector(".el-checkbox__input")?.classList.contains("is-indeterminate")).toBe(true);

    wrapper.unmount();
  });

  it("搜索后可在全部、联系人、部门间筛选并点击结果加入已选择人员", async () => {
    mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    const dialog = await openPermissionEditor(wrapper);
    await searchDirectory(dialog, "孙");

    const tabs = dialog.querySelector<HTMLElement>('[role="tablist"][aria-label="搜索结果类型"]');
    expect(tabs).not.toBeNull();
    expect(tabs!.textContent).toContain("全部");
    expect(tabs!.textContent).toContain("联系人");
    expect(tabs!.textContent).toContain("部门");

    const contact = dialog.querySelector<HTMLElement>('[aria-label="联系人 孙鑫尧"]');
    expect(contact).not.toBeNull();
    expect(contact!.textContent).toContain("R04952");
    expect(contact!.textContent).toContain("平台开发部");
    expect(contact!.textContent).toContain("13936725713");
    expect(contact!.textContent).toContain("赛宝绿创能源技术（上海）有限公司");
    contact!.click();
    await nextTick();
    expect(dialog.querySelector('[aria-label="已选择人员"]')!.textContent).toContain("孙鑫尧");

    await searchDirectory(dialog, "平台");
    await selectSearchTab(dialog, "联系人");
    expect(dialog.querySelector('[aria-label="联系人 孙鑫尧"]')).not.toBeNull();
    expect(dialog.querySelector('[aria-label="部门 平台开发部"]')).toBeNull();

    await selectSearchTab(dialog, "部门");
    expect(dialog.querySelector('[aria-label="联系人 孙鑫尧"]')).toBeNull();
    const department = dialog.querySelector<HTMLElement>('[aria-label="部门 平台开发部"]');
    expect(department).not.toBeNull();
    expect(department!.textContent).toContain("赛宝绿创能源技术（上海）有限公司");

    wrapper.unmount();
  });

  it("输入搜索词后无需回车即可显示联系人和部门结果", async () => {
    vi.useFakeTimers();
    mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    await wrapper.findAll("button").find((button) => button.text().includes("编辑部分可见范围"))!.trigger("click");
    await flushPromises();

    const dialog = document.body.querySelector<HTMLElement>('[aria-label="编辑可见范围"]')!;
    const input = dialog.querySelector<HTMLInputElement>('[aria-label="搜索部门或员工"]')!;
    input.value = "孙";
    input.dispatchEvent(new Event("input", { bubbles: true }));
    await vi.advanceTimersByTimeAsync(300);
    await flushPromises();

    expect(dialog.querySelector('[role="tablist"][aria-label="搜索结果类型"]')).not.toBeNull();
    expect(dialog.querySelector('[aria-label="联系人 孙鑫尧"]')).not.toBeNull();

    wrapper.unmount();
    vi.useRealTimers();
  });

  it("选择员工加入右侧，选择部门补齐全部成员并去重，右侧移除会同步左树", async () => {
    mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].departments = [];
    vm.permissionProfiles[0].employeeRules = [];
    vm.permissionProfiles[0].departmentExcludedEmployeeIds = [];
    const dialog = await openPermissionEditor(wrapper);
    await expandOrganization(dialog, "sebo");
    await expandDepartment(dialog, 11);

    employeeRow(dialog, "孙鑫尧").click();
    await nextTick();
    expect(selectedEmployeeNames(dialog)).toEqual(["孙鑫尧"]);

    departmentCheckbox(dialog, "平台开发部").click();
    await flushPromises();
    expect(selectedEmployeeNames(dialog).sort()).toEqual(["孙鑫尧", "李晨"].sort());
    expect(selectedEmployeeNames(dialog)).toHaveLength(2);
    expect(employeeRow(dialog, "孙鑫尧").classList.contains("selected")).toBe(true);
    expect(employeeRow(dialog, "李晨").classList.contains("selected")).toBe(true);

    dialog.querySelector<HTMLElement>('[aria-label="移除 孙鑫尧"]')!.click();
    await nextTick();
    expect(selectedEmployeeNames(dialog)).toEqual(["李晨"]);
    expect(employeeRow(dialog, "孙鑫尧").classList.contains("selected")).toBe(false);

    wrapper.unmount();
  });

  it("部门仅在所有成员都选中时勾选，移除任一成员后显示部分选中", async () => {
    mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].departments = [];
    vm.permissionProfiles[0].employeeRules = [];
    const dialog = await openPermissionEditor(wrapper);
    await expandOrganization(dialog, "sebo");
    await expandDepartment(dialog, 11);
    const checkbox = departmentCheckbox(dialog, "平台开发部");

    expect(checkbox.classList.contains("is-checked")).toBe(false);
    checkbox.click();
    await flushPromises();
    expect(employeeRow(dialog, "孙鑫尧").classList.contains("selected")).toBe(true);
    expect(employeeRow(dialog, "李晨").classList.contains("selected")).toBe(true);
    expect(checkbox.classList.contains("is-checked")).toBe(true);

    dialog.querySelector<HTMLElement>('[aria-label="移除 孙鑫尧"]')!.click();
    await nextTick();
    expect(checkbox.classList.contains("is-checked")).toBe(false);
    expect(checkbox.querySelector(".el-checkbox__input")?.classList.contains("is-indeterminate")).toBe(true);

    wrapper.unmount();
  });

  it("部门选择产生的继承员工也会按企业回显到首页姓名栏", async () => {
    mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].allEmployeesVisible = false;
    vm.permissionProfiles[0].departments = [];
    vm.permissionProfiles[0].employeeRules = [];
    const dialog = await openPermissionEditor(wrapper);
    await expandOrganization(dialog, "sebo");
    departmentCheckbox(dialog, "平台开发部").click();
    await flushPromises();
    Array.from(dialog.querySelectorAll<HTMLButtonElement>("button"))
      .find((button) => button.textContent?.includes("确定选择"))!.click();
    await flushPromises();
    Array.from(dialog.querySelectorAll<HTMLButtonElement>("button"))
      .find((button) => button.textContent?.trim() === "取消")!.click();
    await nextTick();

    const seboNames = wrapper.get('[aria-label="赛宝已选人员"]');
    expect(seboNames.text()).toContain("孙鑫尧");
    expect(seboNames.text()).toContain("李晨");

    wrapper.unmount();
  });

  it("赛宝与瓦尔登姓名栏各自支持键盘访问和独立纵向滚动", async () => {
    mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].allEmployeesVisible = false;
    vm.permissionProfiles[0].employeeRules = [
      { id: 101, corpCode: "sebo", corpName: "赛宝绿创能源技术（上海）有限公司", employeeName: "孙鑫尧", effect: "ALLOW" },
      { id: 201, corpCode: "walden", corpName: "瓦尔登环境科学研究院（北京）有限公司", employeeName: "王月", effect: "ALLOW" },
    ];
    await nextTick();

    const seboList = wrapper.get('[aria-label="赛宝已选人员姓名列表"]');
    const waldenList = wrapper.get('[aria-label="瓦尔登已选人员姓名列表"]');
    expect(seboList.attributes("tabindex")).toBe("0");
    expect(waldenList.attributes("tabindex")).toBe("0");
    expect(seboList.classes()).toContain("permission-member-scroll");
    expect(waldenList.classes()).toContain("permission-member-scroll");
    expect(adminStyles).toMatch(/\.permission-member-names\s*\{[^}]*max-height:\s*156px;[^}]*overflow-y:\s*auto;/s);
    expect(adminStyles).toMatch(/\.permission-member-scroll\s*\{[^}]*overscroll-behavior:\s*contain;/s);

    wrapper.unmount();
  });

  it("取消编辑时丢弃未确认的员工勾选", async () => {
    mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].departments = [];
    vm.permissionProfiles[0].employeeRules = [];
    const dialog = await openPermissionEditor(wrapper);

    await expandOrganization(dialog, "sebo");
    await expandDepartment(dialog, 11);
    employeeRow(dialog, "孙鑫尧").click();
    await nextTick();
    expect(selectedEmployeeNames(dialog)).toContain("孙鑫尧");

    const cancel = Array.from(dialog.querySelectorAll<HTMLButtonElement>("button"))
      .find((button) => button.textContent?.trim() === "取消");
    expect(cancel).toBeDefined();
    cancel!.click();
    await nextTick();

    expect(vm.permissionDialogVisible).toBe(false);
    expect(wrapper.get('[aria-label="赛宝已选人员"]').text()).not.toContain("孙鑫尧");

    wrapper.unmount();
  });

  it("关闭编辑弹窗时丢弃未确认的员工勾选", async () => {
    mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    const vm = layoutVm(wrapper);
    vm.permissionProfiles[0].departments = [];
    vm.permissionProfiles[0].employeeRules = [];
    const dialog = await openPermissionEditor(wrapper);

    await expandOrganization(dialog, "sebo");
    await expandDepartment(dialog, 11);
    employeeRow(dialog, "孙鑫尧").click();
    await nextTick();
    expect(selectedEmployeeNames(dialog)).toContain("孙鑫尧");

    const close = dialog.querySelector<HTMLButtonElement>('[aria-label="关闭此对话框"]');
    expect(close).not.toBeNull();
    await new DOMWrapper(close!).trigger("click");
    await nextTick();

    expect(dialog.closest<HTMLElement>(".el-overlay")?.style.display).toBe("none");
    expect(vm.permissionDialogVisible).toBe(false);
    expect(wrapper.get('[aria-label="赛宝已选人员"]').text()).not.toContain("孙鑫尧");

    wrapper.unmount();
  });

  it("确定选择只保存并保持弹窗，只有取消或关闭按钮才退出", async () => {
    const request = mockDualEnterpriseDirectory();
    const wrapper = await mountPermissionPage();
    let dialog = await openPermissionEditor(wrapper);

    const confirm = Array.from(dialog.querySelectorAll<HTMLButtonElement>("button"))
      .find((button) => button.textContent?.includes("确定选择"));
    expect(confirm).toBeDefined();
    confirm!.click();
    await flushPromises();
    expect(request.mock.calls.some(([input, init]) =>
      String(input).includes("/permission-profile") && init?.method === "PUT")).toBe(true);
    expect(layoutVm(wrapper).permissionDialogVisible).toBe(true);

    const cancel = Array.from(dialog.querySelectorAll<HTMLButtonElement>("button"))
      .find((button) => button.textContent?.trim() === "取消");
    expect(cancel).toBeDefined();
    cancel!.click();
    await nextTick();
    expect(layoutVm(wrapper).permissionDialogVisible).toBe(false);

    wrapper.unmount();
    document.body.innerHTML = "";

    const closeWrapper = await mountPermissionPage();
    dialog = await openPermissionEditor(closeWrapper);
    const close = dialog.querySelector<HTMLButtonElement>('[aria-label="关闭此对话框"]');
    expect(close).not.toBeNull();
    await new DOMWrapper(close!).trigger("click");
    await nextTick();
    expect(dialog.closest<HTMLElement>(".el-overlay")?.style.display).toBe("none");

    const source = readFileSync(resolve(process.cwd(), "src/layouts/FinanceLayout.vue"), "utf8");
    expect(source).toContain(':close-on-click-modal="false"');
    expect(source).toContain(':close-on-press-escape="false"');

    closeWrapper.unmount();
  });
});

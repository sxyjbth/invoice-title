<script setup lang="ts">
import { computed, onBeforeUnmount, provide, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { storeToRefs } from "pinia";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { type FinanceAccount } from "../components/FinanceAccountManagement.vue";
import ChangePasswordDialog from "../components/ChangePasswordDialog.vue";
import { buildPermissionSubjectQuery, loadPermissionProfiles } from "../utils/subject-query";
import { formatDateTime } from "../utils/date";
import { resolveApiUrl } from "../api-prefix";
import { routeNameByMenu, routeNames, type FinanceMenuCode } from "../router";
import { financeLayoutKey } from "./finance-layout-context";
import { useFinanceAuthStore } from "../stores/finance-auth";
import {
  ArrowUp,
  Document,
  Download,
  Lock,
  OfficeBuilding,
  Plus,
  Search,
  Setting,
  SwitchButton,
  Upload,
  User,
} from "@element-plus/icons-vue";

type StatusCode = "PUBLISHED" | "DRAFT" | "DISABLED";
type MenuCode = FinanceMenuCode;

const importTemplateUrl = resolveApiUrl("/api/admin/invoice-imports/template");
const route = useRoute();
const router = useRouter();
const auth = useFinanceAuthStore();
const { checking: authChecking, currentUser } = storeToRefs(auth);

type InvoiceTitle = {
  id: number;
  companyName: string;
  taxpayerId: string;
  registeredAddress?: string;
  phone?: string;
  bankName?: string;
  bankAccount?: string;
  bankSummary: string;
  subjects: string[];
  subjectIds: number[];
  status: StatusCode;
  updatedAt: string;
  updatedBy: string;
};

type InvoiceSubject = {
  id: number;
  code: string;
  name: string;
  status: "ENABLED" | "DISABLED";
  employeeCount: number;
  boundTitleId?: number | null;
  boundTitleName?: string | null;
  updatedAt: string;
  updatedBy: string;
  sortNo: number;
};

type SubjectPermission = {
  id: number;
  subjectName: string;
  targetType: "USER" | "DEPARTMENT";
  targetName: string;
  targetId: string;
  status: "ENABLED" | "DISABLED";
  source: "MANUAL" | "DING_SYNC";
  updatedAt: string;
};

type SubjectPermissionProfile = {
  id: number;
  subjectName: string;
  visibleCount: number;
  allEmployeesVisible: boolean;
  departments: DingDepartment[];
  employeeRules: EmployeeRule[];
  departmentExcludedEmployeeIds?: number[];
  partiallySelectedDepartmentIds?: number[];
  employeeCount: number;
};

type DingDepartment = {
  id: number;
  corpCode?: string;
  corpName?: string;
  dingDepartmentId: string;
  departmentName: string;
  employeeCount: number;
};

type DingOrganization = {
  corpCode: string;
  corpName: string;
};

type DingEmployee = {
  id: number;
  corpCode?: string;
  corpName?: string;
  dingUserId: string;
  employeeNo: string;
  employeeName: string;
  departmentId: number;
  departmentIds?: number[];
  departmentName: string;
  mobile: string;
  permissionEnabled?: boolean;
};

type EmployeeSelectionResolveResult = {
  selectedEmployeeCount: number;
  selectedEmployeeIds: number[];
  selectedEmployees: DingEmployee[];
  employeeGroups?: unknown[];
};

type DepartmentMemberPage = {
  departmentId: number;
  records: DingEmployee[];
  total: number;
  pageNum: number;
  pageSize: number;
  loading: boolean;
  loaded: boolean;
  error: string;
};

type EmployeeRule = DingEmployee & { employeeId?: number; effect: "ALLOW" | "DENY" };

type ImportHistory = {
  id: number;
  taskNo: string;
  originalFileName: string;
  status: "PENDING" | "VALIDATING" | "COMPLETED" | "PARTIAL_FAILED" | "FAILED";
  totalCount: number;
  successCount: number;
  failureCount: number;
  createdBy: string;
  createdAt: string;
};

type ImportRowError = {
  id: number;
  rowNo: number;
  taxpayerId?: string;
  errorCode: string;
  errorMessage: string;
};

const allNavigation: Array<{ code: MenuCode; label: string; icon: typeof Document; superAdminOnly?: boolean }> = [
  { code: "titles", label: "抬头管理", icon: Document },
  { code: "subjects", label: "主体管理", icon: OfficeBuilding },
  { code: "permissions", label: "主体权限", icon: User },
  { code: "accounts", label: "财务账号", icon: Setting, superAdminOnly: true },
];

const initialTitles: InvoiceTitle[] = [
  {
    id: 1,
    companyName: "杭州赛宝卓越技术有限公司",
    taxpayerId: "91110400MADFF1HE1T",
    bankSummary: "宁波银行 · 账户尾号 7180",
    bankName: "宁波银行股份有限公司北京丰台支行",
    bankAccount: "86041110000957180",
    registeredAddress: "浙江省杭州市钱塘区临江街道纬五路3688号临江科创园6号楼12楼",
    phone: "4008696096",
    subjects: ["杭州主体"],
    subjectIds: [1],
    status: "PUBLISHED",
    updatedAt: "2026-08-07 15:46",
    updatedBy: "王财务",
  },
  {
    id: 2,
    companyName: "北京示例技术服务有限公司",
    taxpayerId: "91110108MA01EXAMPLE",
    bankSummary: "招商银行 · 账户尾号 3028",
    subjects: ["北京主体"],
    subjectIds: [2],
    status: "DRAFT",
    updatedAt: "2026-08-06 10:22",
    updatedBy: "李会计",
  },
  {
    id: 3,
    companyName: "上海赛宝技术服务有限公司",
    taxpayerId: "91310115MA1KEXAMPLE",
    bankSummary: "浦发银行 · 账户尾号 6631",
    subjects: ["上海主体"],
    subjectIds: [3],
    status: "DISABLED",
    updatedAt: "2026-07-30 09:15",
    updatedBy: "王财务",
  },
];

const titles = ref<InvoiceTitle[]>(initialTitles);
const titleTotal = ref(initialTitles.length);
const statusCounts = reactive<Record<"ALL" | StatusCode, number>>({ ALL: 3, PUBLISHED: 1, DRAFT: 1, DISABLED: 1 });
const statusOptions = computed<Array<{ code: "ALL" | StatusCode; label: string; count: number }>>(() => [
  { code: "ALL", label: "全部", count: statusCounts.ALL },
  { code: "PUBLISHED", label: "已发布", count: statusCounts.PUBLISHED },
  { code: "DRAFT", label: "草稿", count: statusCounts.DRAFT },
]);

const subjects = ref<InvoiceSubject[]>([
  { id: 1, code: "HZ", name: "杭州主体", status: "ENABLED", employeeCount: 186, boundTitleId: 1, boundTitleName: "杭州赛宝卓越技术有限公司", updatedAt: "2026-08-07 15:46", updatedBy: "王财务", sortNo: 10 },
  { id: 2, code: "BJ", name: "北京主体", status: "ENABLED", employeeCount: 92, boundTitleId: 2, boundTitleName: "北京示例技术服务有限公司", updatedAt: "2026-08-06 10:22", updatedBy: "李会计", sortNo: 20 },
  { id: 3, code: "SH", name: "上海主体", status: "ENABLED", employeeCount: 68, boundTitleId: 3, boundTitleName: "上海赛宝技术服务有限公司", updatedAt: "2026-08-03 14:10", updatedBy: "王财务", sortNo: 30 },
  { id: 4, code: "EAST", name: "华东主体", status: "ENABLED", employeeCount: 40, updatedAt: "2026-07-29 09:35", updatedBy: "王财务", sortNo: 40 },
]);
const titleSubjectOptions = ref<InvoiceSubject[]>([...subjects.value]);
const subjectTitleOptions = ref<InvoiceTitle[]>([...titles.value]);

const permissions = ref<SubjectPermission[]>([
  { id: 1, subjectName: "杭州主体", targetType: "USER", targetName: "示例员工", targetId: "ding-employee-001", status: "ENABLED", source: "MANUAL", updatedAt: "2026-08-07 15:40" },
  { id: 2, subjectName: "华东主体", targetType: "DEPARTMENT", targetName: "华东交付中心", targetId: "ding-dept-east", status: "ENABLED", source: "DING_SYNC", updatedAt: "2026-08-07 08:30" },
  { id: 3, subjectName: "北京主体", targetType: "DEPARTMENT", targetName: "北京研发中心", targetId: "ding-dept-beijing", status: "ENABLED", source: "MANUAL", updatedAt: "2026-08-06 10:18" },
]);

const permissionProfiles = ref<SubjectPermissionProfile[]>([
  {
    id: 1,
    subjectName: "杭州主体",
    visibleCount: 128,
    allEmployeesVisible: false,
    departments: [
      { id: 1, dingDepartmentId: "ding-dept-tech", departmentName: "技术中心", employeeCount: 86 },
      { id: 2, dingDepartmentId: "ding-dept-finance", departmentName: "财务部", employeeCount: 18 },
      { id: 3, dingDepartmentId: "ding-dept-purchase", departmentName: "采购部", employeeCount: 12 },
    ],
    employeeRules: [
      { id: 1, dingUserId: "ding-employee-001", employeeNo: "SB0001", employeeName: "陈一", departmentId: 1, departmentName: "技术中心", mobile: "13800000001", effect: "ALLOW" },
      { id: 2, dingUserId: "ding-employee-002", employeeNo: "SB0002", employeeName: "李二", departmentId: 2, departmentName: "财务部", mobile: "13800000002", effect: "ALLOW" },
      { id: 3, dingUserId: "ding-employee-003", employeeNo: "SB0003", employeeName: "王三", departmentId: 3, departmentName: "采购部", mobile: "13800000003", effect: "DENY" },
      { id: 4, dingUserId: "ding-employee-004", employeeNo: "SB0004", employeeName: "赵四", departmentId: 1, departmentName: "技术中心", mobile: "13800000004", effect: "ALLOW" },
    ],
    employeeCount: 12,
  },
  {
    id: 2,
    subjectName: "北京主体",
    visibleCount: 46,
    allEmployeesVisible: false,
    departments: [{ id: 4, dingDepartmentId: "ding-dept-beijing", departmentName: "北京研发中心", employeeCount: 34 }],
    employeeRules: [],
    employeeCount: 12,
  },
  {
    id: 3,
    subjectName: "上海主体",
    visibleCount: 72,
    allEmployeesVisible: false,
    departments: [{ id: 5, dingDepartmentId: "ding-dept-shanghai", departmentName: "上海交付中心", employeeCount: 60 }],
    employeeRules: [],
    employeeCount: 12,
  },
]);

const importHistory = ref<ImportHistory[]>([
  { id: 1, taskNo: "IMP2026080509280012AB", originalFileName: "invoice-title-20260805.xlsx", status: "PARTIAL_FAILED", totalCount: 13, successCount: 12, failureCount: 1, createdBy: "王财务", createdAt: "2026-08-05 09:28" },
  { id: 2, taskNo: "IMP2026071916420034CD", originalFileName: "invoice-title-20260719.xlsx", status: "COMPLETED", totalCount: 8, successCount: 8, failureCount: 0, createdBy: "李会计", createdAt: "2026-07-19 16:42" },
]);

const testMode = import.meta.env.MODE === "test";
// 仅供 `vite --mode test` 的浏览器评估页使用；Vitest 仍走其请求桩，避免改变单元测试契约。
const permissionPreviewMode = testMode && !import.meta.env.VITEST;
const testDirectoryOrganizations: DingOrganization[] = [
  { corpCode: "sebo", corpName: "赛宝绿创能源技术（上海）有限公司" },
  { corpCode: "walden", corpName: "瓦尔登环境科学研究院（北京）有限公司" },
];
const testDirectoryDepartments: DingDepartment[] = [
  { id: 11, corpCode: "sebo", corpName: "赛宝绿创能源技术（上海）有限公司", dingDepartmentId: "sebo-platform", departmentName: "平台开发部", employeeCount: 2 },
  { id: 12, corpCode: "sebo", corpName: "赛宝绿创能源技术（上海）有限公司", dingDepartmentId: "sebo-finance", departmentName: "财务部", employeeCount: 1 },
  { id: 21, corpCode: "walden", corpName: "瓦尔登环境科学研究院（北京）有限公司", dingDepartmentId: "walden-digital", departmentName: "数智化中心", employeeCount: 2 },
  { id: 22, corpCode: "walden", corpName: "瓦尔登环境科学研究院（北京）有限公司", dingDepartmentId: "walden-administration", departmentName: "综合管理部", employeeCount: 1 },
];
const testDirectoryEmployees: DingEmployee[] = [
  { id: 101, corpCode: "sebo", corpName: "赛宝绿创能源技术（上海）有限公司", dingUserId: "sebo-sun", employeeNo: "R04952", employeeName: "孙鑫尧", departmentId: 11, departmentIds: [11], departmentName: "平台开发部", mobile: "13936725713" },
  { id: 102, corpCode: "sebo", corpName: "赛宝绿创能源技术（上海）有限公司", dingUserId: "sebo-li", employeeNo: "R01411", employeeName: "李晨", departmentId: 11, departmentIds: [11], departmentName: "平台开发部", mobile: "18223148993" },
  { id: 103, corpCode: "sebo", corpName: "赛宝绿创能源技术（上海）有限公司", dingUserId: "sebo-finance", employeeNo: "SB0103", employeeName: "王财务", departmentId: 12, departmentIds: [12], departmentName: "财务部", mobile: "13800000003" },
  { id: 201, corpCode: "walden", corpName: "瓦尔登环境科学研究院（北京）有限公司", dingUserId: "walden-wang", employeeNo: "W0001", employeeName: "王月", departmentId: 21, departmentIds: [21], departmentName: "数智化中心", mobile: "13800000001" },
  { id: 202, corpCode: "walden", corpName: "瓦尔登环境科学研究院（北京）有限公司", dingUserId: "walden-du", employeeNo: "W0002", employeeName: "杜婷婷", departmentId: 21, departmentIds: [21], departmentName: "数智化中心", mobile: "13800000002" },
  { id: 203, corpCode: "walden", corpName: "瓦尔登环境科学研究院（北京）有限公司", dingUserId: "walden-wei", employeeNo: "W0003", employeeName: "韦丽平", departmentId: 22, departmentIds: [22], departmentName: "综合管理部", mobile: "13800000004" },
];

function cloneTestDirectoryDepartments(departments = testDirectoryDepartments) {
  return departments.map((department) => ({ ...department }));
}

function cloneTestDirectoryEmployees(employees = testDirectoryEmployees) {
  return employees.map((employee) => ({
    ...employee,
    departmentIds: [...(employee.departmentIds ?? [])],
  }));
}

// 浏览器评估页直接展示双企业的已选人员，便于在不连接后端的情况下确认最终版式。
if (permissionPreviewMode && permissionProfiles.value[0]) {
  const previewEmployees = [
    testDirectoryEmployees[0],
    testDirectoryEmployees[1],
    testDirectoryEmployees[3],
  ].filter((employee): employee is DingEmployee => Boolean(employee));
  permissionProfiles.value[0] = {
    ...permissionProfiles.value[0],
    visibleCount: previewEmployees.length,
    departments: [],
    employeeRules: previewEmployees.map((employee) => ({ ...employee, effect: "ALLOW" })),
  };
}
const changePasswordVisible = ref(false);
const profileMenuVisible = ref(false);
const financeAccounts = ref<FinanceAccount[]>(testMode ? [{
  id: 2,
  username: "wang.finance",
  displayName: "王财务",
  status: "ENABLED",
  lastLoginAt: "2026-08-10 15:32",
  createdAt: "2026-08-08 09:20",
}] : []);
const accountTotal = ref(financeAccounts.value.length);
const accountLoading = ref(false);
const accountPageNum = ref(1);
const accountPageSize = ref(20);
const accountKeyword = ref("");
const accountStatus = ref("");
const navigation = computed(() => allNavigation.filter((item) => !item.superAdminOnly || currentUser.value?.roleType === "SUPER_ADMIN"));
const profileRoleLabel = computed(() => currentUser.value?.roleType === "SUPER_ADMIN" ? "超级管理员" : "财务人员");
const profileDisplayName = computed(() => {
  if (!currentUser.value) return "";
  return currentUser.value.displayName === profileRoleLabel.value
    ? currentUser.value.username
    : currentUser.value.displayName;
});

const activeMenu = computed<MenuCode>(() => (route.meta.menuCode as MenuCode | undefined) ?? "titles");
const activeStatus = ref<"ALL" | StatusCode>("ALL");
const keyword = ref("");
const pageNum = ref(1);
const pageSize = ref(10);
const importVisible = ref(false);
const createVisible = ref(false);
const editingTitleId = ref<number | null>(null);
const titleSaving = ref(false);
const importFileName = ref("");
const importFile = ref<File | null>(null);
const importFileInput = ref<HTMLInputElement | null>(null);
const importSubmitting = ref(false);
const importHistoryPageNum = ref(1);
const importHistoryPageSize = ref(10);
const importHistoryTotal = ref(2);
const importErrorTaskId = ref<number | null>(null);
const importRowErrors = ref<ImportRowError[]>([]);
const importResult = ref<ImportHistory | null>(null);
const importErrorsLoading = ref(false);
const subjectPageNum = ref(1);
const subjectPageSize = ref(20);
const subjectKeyword = ref("");
const subjectStatus = ref<"ALL" | "ENABLED" | "DISABLED">("ALL");
const subjectDialogVisible = ref(false);
const editingSubjectId = ref<number | null>(null);
const subjectTotal = ref(subjects.value.length);
const subjectSaving = ref(false);
const titleBindingVisible = ref(false);
const titleBindingSaving = ref(false);
const bindingSubject = ref<InvoiceSubject | null>(null);
const bindingTitleId = ref<number | null>(null);
const permissionPageNum = ref(1);
const permissionPageSize = ref(20);
const permissionSubject = ref("");
const permissionKeyword = ref("");
const permissionDialogVisible = ref(false);
const permissionSaving = ref(false);
const directoryLoading = ref(false);
const directoryKeyword = ref("");
const directoryPageNum = ref(1);
const directoryPageSize = ref(10);
const directoryTotal = ref(0);
const directoryEmployees = ref<DingEmployee[]>([]);
const directoryDepartments = ref<DingDepartment[]>([]);
const directoryOrganizations = ref<DingOrganization[]>([]);
const directoryCorpCode = ref("");
const departmentMemberPages = reactive<Record<string, DepartmentMemberPage>>({});
const loadedDirectoryEmployees = reactive<Record<string, DingEmployee>>({});
const loadedEmployeeDepartmentIds = reactive<Record<string, number[]>>({});
const selectedDepartmentIds = ref<number[]>([]);
const revokedDepartmentIds = ref<number[]>([]);
const reenabledEmployeeIds = ref<number[]>([]);
const departmentExcludedEmployeeIds = ref<number[]>([]);
const partiallySelectedDepartmentIds = ref<number[]>([]);
const employeeEnabledDraft = reactive<Record<string, boolean>>({});
const employeePermissionEdited = reactive<Record<string, boolean>>({});
const employeePermissionStatus = ref<"ALL" | "ENABLED" | "DISABLED">("ALL");
const directorySearchActive = ref(false);
const directoryResultType = ref<"ALL" | "USER" | "DEPARTMENT">("ALL");
const expandedDirectoryCorpCodes = ref<string[]>([]);
const fullyLoadedDirectoryCorpCodes = ref<string[]>([]);
const expandedDirectoryDepartmentIds = ref<number[]>([]);
const selectedPermissionProfileId = ref(1);
let directorySearchTimer: ReturnType<typeof setTimeout> | null = null;

const titleForm = reactive({
  companyName: "",
  taxpayerId: "",
  address: "",
  phone: "",
  bankName: "",
  bankAccount: "",
  subjectId: null as number | null,
  status: "DRAFT" as "DRAFT" | "PUBLISHED",
});
const taxpayerIdPattern = /^[0-9A-Z]{15,20}$/;
const phonePattern = /^(?:$|1[3-9]\d{9}|0\d{2,3}-?\d{7,8}(?:-\d{1,6})?|(?:400|800)-?\d{3}-?\d{4})$/;
const bankAccountPattern = /^(?:$|\d{8,32})$/;
const titleFormErrors = reactive({ companyName: "", taxpayerId: "", phone: "", bankAccount: "" });

function clearTitleFormErrors() {
  Object.assign(titleFormErrors, { companyName: "", taxpayerId: "", phone: "", bankAccount: "" });
}

function validateTitleForm() {
  clearTitleFormErrors();
  const companyName = titleForm.companyName.trim();
  const taxpayerId = titleForm.taxpayerId.trim();
  const phone = titleForm.phone.trim();
  const bankAccount = titleForm.bankAccount.trim();
  if (!companyName) titleFormErrors.companyName = "公司名称不能为空";
  if (!taxpayerId) {
    titleFormErrors.taxpayerId = "纳税人识别号不能为空";
  } else if (!taxpayerIdPattern.test(taxpayerId)) {
    titleFormErrors.taxpayerId = "纳税人识别号应为 15-20 位大写字母或数字";
  }
  if (!phonePattern.test(phone)) {
    titleFormErrors.phone = "请输入正确的手机号、固定电话或 400/800 客服电话";
  }
  if (!bankAccountPattern.test(bankAccount)) {
    titleFormErrors.bankAccount = "银行账号应为 8-32 位数字";
  }
  return !Object.values(titleFormErrors).some(Boolean);
}

const subjectForm = reactive({ name: "", status: "ENABLED" as "ENABLED" | "DISABLED", sortNo: 0 });
const permissionForm = reactive({
  subjectName: "杭州主体",
  targetType: "DEPARTMENT" as "ALL" | "USER" | "DEPARTMENT",
  targetName: "",
  targetId: "",
});

const filteredTitles = computed(() => {
  const normalized = keyword.value.trim().toLowerCase();
  return titles.value.filter((title) => {
    const statusMatches = activeStatus.value === "ALL" || title.status === activeStatus.value;
    const keywordMatches = !normalized
      || title.companyName.toLowerCase().includes(normalized)
      || title.taxpayerId.toLowerCase().includes(normalized);
    return statusMatches && keywordMatches;
  });
});

const pageTitle = computed(() => route.meta.title ?? (activeMenu.value === "titles"
  ? "发票抬头管理"
  : navigation.value.find((item) => item.code === activeMenu.value)?.label ?? "抬头管理"));
const currentTotal = computed(() => testMode
  ? (statusOptions.value.find((item) => item.code === activeStatus.value)?.count ?? filteredTitles.value.length)
  : titleTotal.value);
const filteredSubjects = computed(() => subjects.value.filter((subject) => {
  const value = subjectKeyword.value.trim().toLowerCase();
  const keywordMatches = !value || subject.name.toLowerCase().includes(value);
  const statusMatches = subjectStatus.value === "ALL" || subject.status === subjectStatus.value;
  return keywordMatches && statusMatches;
}));
const currentSubjectTotal = computed(() => testMode ? filteredSubjects.value.length : subjectTotal.value);
const filteredPermissions = computed(() => permissions.value.filter((permission) => {
  const value = permissionKeyword.value.trim().toLowerCase();
  const subjectMatches = !permissionSubject.value || permission.subjectName === permissionSubject.value;
  const keywordMatches = !value || permission.targetName.toLowerCase().includes(value) || permission.targetId.toLowerCase().includes(value);
  return subjectMatches && keywordMatches;
}));
const activePermissionProfile = computed<SubjectPermissionProfile | null>(() => permissionProfiles.value.find(
  (profile) => profile.id === selectedPermissionProfileId.value,
) ?? permissionProfiles.value[0] ?? null);
const selectedPermissionEmployees = computed<DingEmployee[]>(() => {
  const selected = new Map<string, DingEmployee>();
  activePermissionProfile.value?.employeeRules
    .filter((rule) => rule.effect === "ALLOW")
    .forEach((rule) => {
      const employeeId = employeeRuleId(rule);
      if (employeeId === null) return;
      const employee = { ...rule, id: employeeId } as DingEmployee;
      selected.set(employeeIdentityKey(employee), employee);
    });
  Object.values(loadedDirectoryEmployees).forEach((employee) => {
    const employeeKey = employeeIdentityKey(employee);
    const enabled = employeeEnabledDraft[employeeKey] ?? resolveEmployeeEnabled(employee);
    if (enabled) selected.set(employeeKey, employee);
    else selected.delete(employeeKey);
  });
  return [...selected.values()].sort((left, right) => {
    const corpCompare = String(left.corpCode ?? "").localeCompare(String(right.corpCode ?? ""));
    return corpCompare || left.employeeName.localeCompare(right.employeeName, "zh-CN");
  });
});
watch(activeMenu, (menu) => {
  if (!currentUser.value) return;
  if (menu === "accounts") void loadFinanceAccounts();
  if (menu === "titles") void Promise.all([loadTitles(), loadTitleCounts(), loadSubjects()]);
  if (menu === "subjects") void Promise.all([loadSubjects(), loadTitles()]);
  if (menu === "permissions") void initializePermissionProfiles();
}, { immediate: true });

async function readApi<T>(response: Response, fallbackMessage: string): Promise<T> {
  if (response.ok) return await response.json() as T;
  if (response.status === 401) invalidateFinanceSession();
  let message = fallbackMessage;
  try {
    const error = await response.json() as { message?: string };
    if (error.message) message = error.message;
  } catch {
    // 非 JSON 错误响应使用业务默认提示。
  }
  throw new Error(message);
}

function invalidateFinanceSession() {
  financeAccounts.value = [];
  accountTotal.value = 0;
  auth.clearSession();
  if (route.name !== routeNames.login) void router.replace({ name: routeNames.login, query: { redirect: route.fullPath } });
  ElMessage.warning("登录状态已失效，请重新登录");
}

function toTitle(record: any): InvoiceTitle {
  const account = record.bankAccount ?? "";
  return {
    id: record.id,
    companyName: record.companyName,
    taxpayerId: record.taxpayerId,
    registeredAddress: record.registeredAddress,
    phone: record.phone,
    bankName: record.bankName,
    bankAccount: account,
    bankSummary: `${record.bankName || "未填写开户行"} · 账户尾号 ${account ? account.slice(-4) : "----"}`,
    subjects: record.subjectNames ?? [],
    subjectIds: record.subjectIds ?? [],
    status: record.status,
    updatedAt: String(record.updatedAt ?? "").replace("T", " ").slice(0, 16),
    updatedBy: record.updatedBy ?? "-",
  };
}

function toSubject(record: any): InvoiceSubject {
  return {
    id: record.id,
    code: record.subjectCode,
    name: record.subjectName,
    status: record.status,
    employeeCount: record.employeeCount ?? 0,
    boundTitleId: record.boundTitleId ?? null,
    boundTitleName: record.boundTitleName ?? null,
    updatedAt: String(record.updatedAt ?? "").replace("T", " ").slice(0, 16),
    updatedBy: record.updatedBy ?? "-",
    sortNo: record.sortNo ?? 0,
  };
}

async function loadTitleCounts() {
  if (testMode) return;
  const codes: Array<"ALL" | StatusCode> = ["ALL", "PUBLISHED", "DRAFT", "DISABLED"];
  await Promise.all(codes.map(async (code) => {
    const query = new URLSearchParams({ pageNum: "1", pageSize: "1" });
    if (code !== "ALL") query.set("status", code);
    const response = await fetch(`/api/admin/invoice-titles?${query}`, { credentials: "include" });
    const result = await readApi<{ total: number }>(response, "抬头统计加载失败");
    statusCounts[code] = result.total;
  }));
}

async function loadTitles(silent = false) {
  if (testMode) return;
  const query = new URLSearchParams({ pageNum: String(pageNum.value), pageSize: String(pageSize.value) });
  if (keyword.value.trim()) query.set("keyword", keyword.value.trim());
  if (activeStatus.value !== "ALL") query.set("status", activeStatus.value);
  try {
    const response = await fetch(`/api/admin/invoice-titles?${query}`, { credentials: "include" });
    const result = await readApi<{ records: any[]; total: number }>(response, "抬头列表加载失败");
    titles.value = result.records.map(toTitle);
    titleTotal.value = result.total;
  } catch (error) {
    if (silent) throw error;
    ElMessage.error(requestErrorMessage(error, "抬头列表加载失败"));
  }
}

async function loadSubjects() {
  if (testMode) return;
  const query = new URLSearchParams({ pageNum: String(subjectPageNum.value), pageSize: String(subjectPageSize.value) });
  if (subjectKeyword.value.trim()) query.set("keyword", subjectKeyword.value.trim());
  if (subjectStatus.value !== "ALL") query.set("status", subjectStatus.value);
  try {
    const response = await fetch(`/api/admin/subjects?${query}`, { credentials: "include" });
    const result = await readApi<{ records: any[]; total: number }>(response, "主体列表加载失败");
    subjects.value = result.records.map(toSubject);
    subjectTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "主体列表加载失败");
  }
}

async function loadCoreData() {
  await Promise.all([loadTitles(), loadTitleCounts(), loadSubjects()]);
}

async function logout() {
  await auth.logout();
  void router.replace({ name: routeNames.login });
}

function openChangePasswordFromProfile() {
  profileMenuVisible.value = false;
  changePasswordVisible.value = true;
}

async function logoutFromProfile() {
  profileMenuVisible.value = false;
  await logout();
}

async function handleFinanceAccountResponse(response: Response) {
  if (response.status === 401) {
    invalidateFinanceSession();
    return null;
  }
  return await readApi<{ records: FinanceAccount[]; total: number }>(response, "财务账号加载失败");
}

async function loadFinanceAccounts() {
  if (testMode) return;
  accountLoading.value = true;
  const query = new URLSearchParams({
    pageNum: String(accountPageNum.value),
    pageSize: String(accountPageSize.value),
  });
  if (accountKeyword.value) query.set("keyword", accountKeyword.value);
  if (accountStatus.value) query.set("status", accountStatus.value);
  try {
    const response = await fetch(`/api/admin/finance-users?${query}`, { credentials: "include" });
    const result = await handleFinanceAccountResponse(response);
    if (!result) return;
    financeAccounts.value = result.records;
    accountTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "财务账号加载失败");
  } finally {
    accountLoading.value = false;
  }
}

function searchFinanceAccounts(keywordValue: string, statusValue: string) {
  accountKeyword.value = keywordValue;
  accountStatus.value = statusValue;
  accountPageNum.value = 1;
  void loadFinanceAccounts();
}

function changeFinanceAccountPage(page: number, size: number) {
  accountPageNum.value = page;
  accountPageSize.value = size;
  void loadFinanceAccounts();
}

async function switchMenu(code: MenuCode) {
  pageNum.value = 1;
  await router.push(`/${code}`);
}

defineExpose({
  applyPermissionSelection,
  bindingTitleId,
  directoryPageNum,
  handleFinanceAccountResponse,
  importFile,
  loadDirectory,
  permissionProfiles,
  readApi,
  submitImport,
  switchMenu,
});

function selectStatus(status: "ALL" | StatusCode) {
  activeStatus.value = status;
  pageNum.value = 1;
  void loadTitles();
}

function statusLabel(status: StatusCode) {
  return { PUBLISHED: "已发布", DRAFT: "草稿", DISABLED: "已停用" }[status];
}

function statusClass(status: StatusCode) {
  return `status-${status.toLowerCase()}`;
}

function openImportDialog() {
  resetImportDialogState();
  importVisible.value = true;
  void loadImportHistory();
}

function clearImportFileSelection() {
  importFile.value = null;
  importFileName.value = "";
  if (importFileInput.value) importFileInput.value.value = "";
}

function resetImportDialogState() {
  clearImportFileSelection();
  importErrorTaskId.value = null;
  importRowErrors.value = [];
  importResult.value = null;
}

function closeImportDialog() {
  resetImportDialogState();
  importVisible.value = false;
}

function requestErrorMessage(error: unknown, fallback: string) {
  if (error instanceof TypeError && /failed to fetch|networkerror|network request failed/i.test(error.message)) {
    return "无法连接导入服务，请确认后端服务正常后重试";
  }
  return error instanceof Error && error.message ? error.message : fallback;
}

function chooseImportFile() {
  importFileInput.value?.click();
}

function handleImportFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0] ?? null;
  importFile.value = file;
  importFileName.value = file?.name ?? "";
  importErrorTaskId.value = null;
  importRowErrors.value = [];
  importResult.value = null;
}

async function loadImportHistory(silent = false) {
  if (import.meta.env.MODE === "test") return;
  try {
    const response = await fetch(`/api/admin/invoice-imports?pageNum=${importHistoryPageNum.value}&pageSize=${importHistoryPageSize.value}`);
    if (!response.ok) return;
    const result = await response.json() as { records: ImportHistory[]; total: number };
    importHistory.value = result.records;
    importHistoryTotal.value = result.total;
  } catch (error) {
    if (silent) throw error;
    // 原型独立打开时保留真实演示数据；联调环境由 Vite 代理读取后端分页接口。
  }
}

/** 抬头编辑使用独立全量主体数据，不继承主体管理列表的分页和筛选条件。 */
async function loadTitleSubjectOptions() {
  if (testMode) {
    titleSubjectOptions.value = [...subjects.value];
    return;
  }
  const options: InvoiceSubject[] = [];
  const optionPageSize = 100;
  let optionPageNum = 1;
  let total = 0;
  do {
    const query = new URLSearchParams({ pageNum: String(optionPageNum), pageSize: String(optionPageSize) });
    const response = await fetch(`/api/admin/subjects?${query}`, { credentials: "include" });
    const result = await readApi<{ records: any[]; total: number }>(response, "绑定主体选项加载失败");
    options.push(...result.records.map(toSubject));
    total = result.total;
    optionPageNum += 1;
  } while (options.length < total);
  titleSubjectOptions.value = options;
}

/** 主体换绑使用独立全量抬头数据，不继承抬头管理列表的分页和筛选条件。 */
async function loadSubjectTitleOptions() {
  if (testMode) {
    subjectTitleOptions.value = [...titles.value];
    return;
  }
  const options: InvoiceTitle[] = [];
  const optionPageSize = 100;
  let optionPageNum = 1;
  let total = 0;
  do {
    const query = new URLSearchParams({ pageNum: String(optionPageNum), pageSize: String(optionPageSize) });
    const response = await fetch(`/api/admin/invoice-titles?${query}`, { credentials: "include" });
    const result = await readApi<{ records: any[]; total: number }>(response, "绑定抬头选项加载失败");
    options.push(...result.records.map(toTitle));
    total = result.total;
    optionPageNum += 1;
  } while (options.length < total);
  subjectTitleOptions.value = options;
}

async function loadImportErrors(taskId: number) {
  importErrorTaskId.value = taskId;
  importErrorsLoading.value = true;
  try {
    const query = new URLSearchParams({ taskId: String(taskId), pageNum: "1", pageSize: "100" });
    const response = await fetch(`/api/admin/invoice-imports/errors?${query}`, { credentials: "include" });
    const result = await readApi<{ records: ImportRowError[] }>(response, "导入失败原因加载失败");
    importRowErrors.value = result.records;
    return result.records;
  } catch (error) {
    importRowErrors.value = [];
    ElMessage.error(requestErrorMessage(error, "导入失败原因加载失败"));
    return [];
  } finally {
    importErrorsLoading.value = false;
  }
}

function importErrorSummary(error: ImportRowError) {
  const taxpayer = error.taxpayerId ? `（纳税人识别号：${error.taxpayerId}）` : "";
  return `第 ${error.rowNo} 行：${error.errorMessage}${taxpayer}`;
}

function importResultTitle(result: ImportHistory) {
  const counts = `成功 ${result.successCount} 条，失败 ${result.failureCount} 条`;
  return result.successCount > 0 ? `部分导入完成：${counts}` : `导入失败：${counts}`;
}

async function submitImport() {
  if (!importFile.value) return;
  importSubmitting.value = true;
  try {
    const body = new FormData();
    body.append("file", importFile.value);
    const response = await fetch("/api/admin/invoice-imports", {
      method: "POST",
      credentials: "include",
      body,
    });
    const result = await readApi<ImportHistory>(response, "导入请求失败");
    importResult.value = result;
    clearImportFileSelection();
    if (result.failureCount > 0) {
      const errors = await loadImportErrors(result.id);
      const firstReason = errors[0] ? `第 ${errors[0].rowNo} 行，${errors[0].errorMessage}` : `共有 ${result.failureCount} 条数据校验失败`;
      if (result.successCount > 0) {
        ElMessage.warning(`导入完成：成功 ${result.successCount} 条，失败 ${result.failureCount} 条；成功数据已生成草稿`);
      } else {
        ElMessage.error(`导入失败：${firstReason}`);
      }
    } else {
      importErrorTaskId.value = null;
      importRowErrors.value = [];
      ElMessage.success(`导入完成：成功 ${result.successCount} 条；数据已生成草稿`);
    }

    // 导入业务结果已经由 POST 确认，后续列表刷新失败不能覆盖成“导入请求失败”。
    const refreshResults = await Promise.allSettled([
      loadImportHistory(true),
      loadTitles(true),
      loadTitleCounts(),
    ]);
    if (refreshResults.some((item) => item.status === "rejected")) {
      ElMessage.warning("导入结果已保存，但页面数据刷新失败，请稍后重试");
    }
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, "导入失败，请检查后端服务和 Excel 内容"));
  } finally {
    importSubmitting.value = false;
  }
}

async function resetCreateForm() {
  editingTitleId.value = null;
  Object.assign(titleForm, {
    companyName: "",
    taxpayerId: "",
    address: "",
    phone: "",
    bankName: "",
    bankAccount: "",
    subjectId: null,
    status: "DRAFT",
  });
  clearTitleFormErrors();
  try {
    await loadTitleSubjectOptions();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "绑定主体选项加载失败");
    return;
  }
  createVisible.value = true;
}

async function openTitleEditor(title: InvoiceTitle) {
  editingTitleId.value = title.id;
  let detail = title;
  if (!testMode) {
    try {
      const response = await fetch(`/api/admin/invoice-titles/${title.id}`, { credentials: "include" });
      detail = toTitle(await readApi<any>(response, "抬头详情加载失败"));
      await loadTitleSubjectOptions();
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : "抬头详情加载失败");
      return;
    }
  } else {
    await loadTitleSubjectOptions();
  }
  Object.assign(titleForm, {
    companyName: detail.companyName,
    taxpayerId: detail.taxpayerId,
    address: detail.registeredAddress ?? "",
    phone: detail.phone ?? "",
    bankName: detail.bankName ?? "",
    bankAccount: detail.bankAccount ?? "",
    subjectId: detail.subjectIds[0] ?? null,
    status: detail.status === "PUBLISHED" ? "PUBLISHED" : "DRAFT",
  });
  clearTitleFormErrors();
  createVisible.value = true;
}

async function saveTitle(status: "DRAFT" | "PUBLISHED") {
  if (!validateTitleForm()) return;
  const subjectIds = titleForm.subjectId == null ? [] : [titleForm.subjectId];
  if (status === "PUBLISHED" && subjectIds.length === 0) {
    ElMessage.warning("保存并发布时请至少选择一个展示主体");
    return;
  }
  titleSaving.value = true;
  const url = editingTitleId.value
    ? `/api/admin/invoice-titles/${editingTitleId.value}`
    : "/api/admin/invoice-titles";
  try {
    const response = await fetch(url, {
      method: editingTitleId.value ? "PUT" : "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        companyName: titleForm.companyName.trim(),
        taxpayerId: titleForm.taxpayerId.trim(),
        registeredAddress: titleForm.address.trim(),
        phone: titleForm.phone.trim(),
        bankName: titleForm.bankName.trim(),
        bankAccount: titleForm.bankAccount.trim(),
        subjectIds,
        status,
      }),
    });
    if (!response.ok) await readApi(response, "抬头保存失败");
    createVisible.value = false;
    ElMessage.success(editingTitleId.value ? "抬头已更新" : "抬头已新增");
    if (!testMode) {
      await Promise.all([loadTitles(), loadTitleCounts(), loadSubjects(), loadTitleSubjectOptions(), loadSubjectTitleOptions()]);
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "抬头保存失败");
  } finally {
    titleSaving.value = false;
  }
}

function openSubjectDialog() {
  editingSubjectId.value = null;
  Object.assign(subjectForm, { name: "", status: "ENABLED", sortNo: 0 });
  subjectDialogVisible.value = true;
}

function openSubjectEditor(subject: InvoiceSubject) {
  editingSubjectId.value = subject.id;
  Object.assign(subjectForm, {
    name: subject.name,
    status: subject.status,
    sortNo: subject.sortNo,
  });
  subjectDialogVisible.value = true;
}

async function saveSubject() {
  if (!subjectForm.name.trim()) {
    ElMessage.warning("请填写主体名称");
    return;
  }
  subjectSaving.value = true;
  const url = editingSubjectId.value ? `/api/admin/subjects/${editingSubjectId.value}` : "/api/admin/subjects";
  try {
    const response = await fetch(url, {
      method: editingSubjectId.value ? "PUT" : "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        subjectName: subjectForm.name.trim(),
        status: subjectForm.status,
        sortNo: subjectForm.sortNo,
        operatorUserId: currentUser.value?.username ?? "finance",
      }),
    });
    if (!response.ok) await readApi(response, "主体保存失败");
    if (testMode) {
      const existing = subjects.value.find((item) => item.id === editingSubjectId.value);
      if (existing) Object.assign(existing, { name: subjectForm.name, status: subjectForm.status, sortNo: subjectForm.sortNo });
    } else {
      await loadSubjects();
    }
    subjectDialogVisible.value = false;
    ElMessage.success(editingSubjectId.value ? "主体已更新" : "主体已新增");
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "主体保存失败");
  } finally {
    subjectSaving.value = false;
  }
}

async function changeSubjectStatus(subject: InvoiceSubject) {
  const status = subject.status === "ENABLED" ? "DISABLED" : "ENABLED";
  const query = new URLSearchParams({ status, operatorUserId: currentUser.value?.username ?? "finance" });
  try {
    const response = await fetch(`/api/admin/subjects/${subject.id}/status?${query}`, {
      method: "PATCH",
      credentials: "include",
    });
    if (!response.ok) await readApi(response, "主体状态更新失败");
    subject.status = status;
    if (!testMode) await loadSubjects();
    ElMessage.success(status === "DISABLED" ? "主体已停用" : "主体已启用");
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "主体状态更新失败");
  }
}

async function openTitleBinding(subject: InvoiceSubject) {
  bindingSubject.value = subject;
  bindingTitleId.value = subject.boundTitleId ?? null;
  try {
    await loadSubjectTitleOptions();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "绑定抬头选项加载失败");
    return;
  }
  titleBindingVisible.value = true;
}

async function saveTitleBinding() {
  if (!bindingSubject.value || bindingTitleId.value == null) {
    ElMessage.warning("请选择要绑定的发票抬头");
    return;
  }
  titleBindingSaving.value = true;
  try {
    const response = await fetch(`/api/admin/subjects/${bindingSubject.value.id}/title-binding`, {
      method: "PUT",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        titleId: bindingTitleId.value,
        operatorUserId: currentUser.value?.username ?? "finance",
      }),
    });
    if (!response.ok) await readApi(response, "绑定抬头失败");
    const selectedTitle = subjectTitleOptions.value.find((title) => title.id === bindingTitleId.value);
    bindingSubject.value.boundTitleId = bindingTitleId.value;
    bindingSubject.value.boundTitleName = selectedTitle?.companyName ?? null;
    titleBindingVisible.value = false;
    ElMessage.success("抬头绑定成功");
    if (!testMode) {
      await Promise.all([loadSubjects(), loadTitles(), loadTitleSubjectOptions(), loadSubjectTitleOptions()]);
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "绑定抬头失败");
  } finally {
    titleBindingSaving.value = false;
  }
}

function searchDirectory() {
  clearDirectorySearchTimer();
  directoryPageNum.value = 1;
  directorySearchActive.value = Boolean(directoryKeyword.value.trim());
  permissionForm.targetType = directoryResultType.value;
  void loadDirectory();
}

function resetDirectorySearch() {
  clearDirectorySearchTimer();
  directoryKeyword.value = "";
  directoryCorpCode.value = "";
  directoryPageNum.value = 1;
  directorySearchActive.value = false;
  directoryResultType.value = "ALL";
  permissionForm.targetType = "ALL";
}

function clearDirectorySearchTimer() {
  if (directorySearchTimer === null) return;
  clearTimeout(directorySearchTimer);
  directorySearchTimer = null;
}

function scheduleDirectorySearch() {
  clearDirectorySearchTimer();
  if (!directoryKeyword.value.trim()) {
    resetDirectorySearch();
    return;
  }
  directorySearchTimer = setTimeout(() => {
    directorySearchTimer = null;
    searchDirectory();
  }, 250);
}

onBeforeUnmount(clearDirectorySearchTimer);

function changePermissionResultType() {
  directoryPageNum.value = 1;
  permissionForm.targetType = directoryResultType.value;
  void loadDirectory();
}

function mergeDirectoryDepartments(departments: DingDepartment[]) {
  const departmentKey = (department: DingDepartment) => `${department.corpCode || "default"}:${department.id}`;
  const merged = new Map(directoryDepartments.value.map((department) => [departmentKey(department), department]));
  departments.forEach((department) => merged.set(departmentKey(department), department));
  directoryDepartments.value = [...merged.values()];
}

function organizationDepartments(organization: DingOrganization) {
  return directoryDepartments.value.filter((department) => department.corpCode === organization.corpCode);
}

function isOrganizationFullySelected(organization: DingOrganization) {
  const departments = organizationDepartments(organization);
  return fullyLoadedDirectoryCorpCodes.value.includes(organization.corpCode)
    && departments.length > 0
    && departments.every((department) => isDepartmentFullySelected(department.id));
}

function isOrganizationPartiallySelected(organization: DingOrganization) {
  const departments = organizationDepartments(organization);
  if (!departments.length || isOrganizationFullySelected(organization)) return false;
  return departments.some((department) =>
    isDepartmentFullySelected(department.id) || isDepartmentPartiallySelected(department));
}

async function loadOrganizationDepartments(organization: DingOrganization) {
  if (fullyLoadedDirectoryCorpCodes.value.includes(organization.corpCode)) return;
  if (permissionPreviewMode) {
    mergeDirectoryDepartments(cloneTestDirectoryDepartments(
      testDirectoryDepartments.filter((department) => department.corpCode === organization.corpCode),
    ));
    fullyLoadedDirectoryCorpCodes.value = [...new Set([
      ...fullyLoadedDirectoryCorpCodes.value,
      organization.corpCode,
    ])];
    return;
  }
  const pageSize = 100;
  let pageNum = 1;
  let loaded = 0;
  let total = 0;
  const records: DingDepartment[] = [];
  do {
    const query = new URLSearchParams({
      pageNum: String(pageNum),
      pageSize: String(pageSize),
      corpCode: organization.corpCode,
    });
    const response = await fetch(`/api/admin/directory/departments?${query}`, { credentials: "include" });
    const result = await readApi<{ records: DingDepartment[]; total: number }>(response, "部门目录加载失败");
    const pageRecords = result.records ?? [];
    records.push(...pageRecords);
    loaded += pageRecords.length;
    total = result.total ?? loaded;
    pageNum += 1;
  } while (loaded < total && pageNum <= 100);
  mergeDirectoryDepartments(records);
  fullyLoadedDirectoryCorpCodes.value = [...new Set([
    ...fullyLoadedDirectoryCorpCodes.value,
    organization.corpCode,
  ])];
}

async function toggleOrganizationTree(organization: DingOrganization) {
  const expanded = new Set(expandedDirectoryCorpCodes.value);
  if (expanded.has(organization.corpCode)) {
    expanded.delete(organization.corpCode);
    expandedDirectoryCorpCodes.value = [...expanded];
    return;
  }
  expanded.add(organization.corpCode);
  expandedDirectoryCorpCodes.value = [...expanded];
  try {
    await loadOrganizationDepartments(organization);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "部门目录加载失败");
  }
}

async function selectDirectoryOrganization(organization: DingOrganization, selected = true) {
  try {
    await loadOrganizationDepartments(organization);
    const departments = organizationDepartments(organization);
    const result = await resolveDirectoryEmployeeSelection({ corpCodes: [organization.corpCode] });
    applyOrganizationSelection(departments, result.selectedEmployees ?? [], selected);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "企业通讯录加载失败");
  }
}

/**
 * 企业、部门或员工批量选择统一交给后端一次性解析，避免企业全选时逐部门分页拉取成员。
 */
async function resolveDirectoryEmployeeSelection(input: {
  corpCodes?: string[];
  departmentIds?: number[];
  employeeIds?: number[];
}): Promise<EmployeeSelectionResolveResult> {
  const request = {
    corpCodes: input.corpCodes ?? [],
    departmentIds: input.departmentIds ?? [],
    employeeIds: input.employeeIds ?? [],
  };
  if (permissionPreviewMode) {
    const corpCodes = new Set(request.corpCodes);
    const departmentIds = new Set(request.departmentIds);
    const employeeIds = new Set(request.employeeIds);
    const selectedEmployees = cloneTestDirectoryEmployees(testDirectoryEmployees.filter((employee) => {
      const memberships = [employee.departmentId, ...(employee.departmentIds ?? [])];
      return (!corpCodes.size || corpCodes.has(employee.corpCode ?? "default"))
        && (!departmentIds.size || memberships.some((departmentId) => departmentIds.has(departmentId)))
        && (!employeeIds.size || employeeIds.has(employee.id));
    }));
    return {
      selectedEmployeeCount: selectedEmployees.length,
      selectedEmployeeIds: selectedEmployees.map((employee) => employee.id),
      selectedEmployees,
      employeeGroups: [],
    };
  }
  const response = await fetch("/api/admin/directory/employee-selections/resolve", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  return readApi<EmployeeSelectionResolveResult>(response, "企业通讯录加载失败");
}

/**
 * 将企业选择结果集中写入响应式状态，避免每个部门、每名员工触发一次数组替换和界面重算。
 */
function applyOrganizationSelection(
  departments: DingDepartment[],
  employees: DingEmployee[],
  selected: boolean,
) {
  const selectedIds = new Set(selectedDepartmentIds.value);
  const revokedIds = new Set(revokedDepartmentIds.value);
  const partialIds = new Set(partiallySelectedDepartmentIds.value);
  departments.forEach((department) => {
    if (selected) {
      selectedIds.add(department.id);
      revokedIds.delete(department.id);
    } else {
      selectedIds.delete(department.id);
      revokedIds.add(department.id);
    }
    partialIds.delete(department.id);
  });

  const excludedEmployeeIds = new Set(departmentExcludedEmployeeIds.value);
  const reenabledIds = new Set(reenabledEmployeeIds.value);
  employees.forEach((employee) => {
    hydrateDirectoryEmployee(employee);
    const employeeKey = employeeIdentityKey(employee);
    employeeEnabledDraft[employeeKey] = selected;
    employeePermissionEdited[employeeKey] = true;
    reenabledIds.delete(employee.id);
    if (selected || !employeeDepartmentIds(employee).some((departmentId) => selectedIds.has(departmentId))) {
      excludedEmployeeIds.delete(employee.id);
    } else {
      excludedEmployeeIds.add(employee.id);
    }
  });

  selectedDepartmentIds.value = [...selectedIds];
  revokedDepartmentIds.value = [...revokedIds];
  partiallySelectedDepartmentIds.value = [...partialIds];
  departmentExcludedEmployeeIds.value = [...excludedEmployeeIds];
  reenabledEmployeeIds.value = [...reenabledIds];
}

async function loadAllDepartmentMembers(department: DingDepartment) {
  const page = departmentMemberPage(department);
  if (page.loaded && page.records.length >= page.total) {
    page.records.forEach((employee) => hydrateDirectoryEmployee(employee, department.id));
    return;
  }
  if (permissionPreviewMode) {
    page.records = cloneTestDirectoryEmployees(testDirectoryEmployees.filter((employee) =>
      (!department.corpCode || employee.corpCode === department.corpCode)
      && (employee.departmentId === department.id || employee.departmentIds?.includes(department.id))));
    page.total = page.records.length;
    page.pageNum = 1;
    page.loaded = true;
    page.error = "";
    page.records.forEach((employee) => hydrateDirectoryEmployee(employee, department.id));
    return;
  }
  page.loading = true;
  page.error = "";
  try {
    const pageSize = 100;
    let pageNum = 1;
    let loaded = 0;
    let total = 0;
    const records: DingEmployee[] = [];
    do {
      const query = new URLSearchParams({
        pageNum: String(pageNum),
        pageSize: String(pageSize),
        departmentId: String(department.id),
      });
      if (department.corpCode) query.set("corpCode", department.corpCode);
      const response = await fetch(`/api/admin/directory/employees?${query}`, { credentials: "include" });
      const result = await readApi<{ records: DingEmployee[]; total: number }>(response, "部门成员加载失败");
      const currentRecords = result.records ?? [];
      records.push(...currentRecords);
      loaded += currentRecords.length;
      total = result.total ?? loaded;
      pageNum += 1;
    } while (loaded < total && pageNum <= 100);
    page.records = [...new Map(records.map((employee) => [employee.id, employee])).values()];
    page.total = total;
    page.pageNum = 1;
    page.loaded = true;
    page.records.forEach((employee) => hydrateDirectoryEmployee(employee, department.id));
  } catch (error) {
    page.records = [];
    page.total = 0;
    page.loaded = false;
    page.error = error instanceof Error ? error.message : "部门成员加载失败";
  } finally {
    page.loading = false;
  }
}

async function toggleDepartmentTree(department: DingDepartment) {
  const expanded = new Set(expandedDirectoryDepartmentIds.value);
  if (expanded.has(department.id)) {
    expanded.delete(department.id);
    expandedDirectoryDepartmentIds.value = [...expanded];
    return;
  }
  expanded.add(department.id);
  expandedDirectoryDepartmentIds.value = [...expanded];
  await loadAllDepartmentMembers(department);
}

function departmentMemberKey(department: DingDepartment) {
  return `${department.corpCode ?? "default"}:${department.id}`;
}

function departmentMemberPage(department: DingDepartment) {
  const key = departmentMemberKey(department);
  if (!departmentMemberPages[key]) {
    departmentMemberPages[key] = {
      departmentId: department.id,
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      loading: false,
      loaded: false,
      error: "",
    };
  }
  return departmentMemberPages[key];
}

function hydrateDirectoryEmployee(employee: DingEmployee, membershipDepartmentId?: number) {
  const employeeKey = employeeIdentityKey(employee);
  loadedDirectoryEmployees[employeeKey] = employee;
  const departmentIds = new Set([
    ...(loadedEmployeeDepartmentIds[employeeKey] ?? []),
    ...(employee.departmentIds ?? []),
    employee.departmentId,
    membershipDepartmentId,
  ].filter((departmentId): departmentId is number => Number.isFinite(departmentId)));
  loadedEmployeeDepartmentIds[employeeKey] = [...departmentIds];
  if (!employeePermissionEdited[employeeKey]) {
    employeeEnabledDraft[employeeKey] = resolveEmployeeEnabled(employee);
    const explicitRule = activePermissionProfile.value?.employeeRules
      .find((rule) => employeeRuleMatches(rule, employee));
    const revokedExplicitAllow = explicitRule?.effect === "ALLOW"
      && [...departmentIds].some((departmentId) => revokedDepartmentIds.value.includes(departmentId))
      && !reenabledEmployeeIds.value.includes(employee.id);
    if (revokedExplicitAllow) employeePermissionEdited[employeeKey] = true;
  }
}

async function loadDirectoryOrganizations() {
  if (permissionPreviewMode) {
    directoryOrganizations.value = testDirectoryOrganizations.map((organization) => ({ ...organization }));
    return;
  }
  try {
    const response = await fetch("/api/admin/directory/organizations", { credentials: "include" });
    const result = await readApi<DingOrganization[]>(response, "企业目录加载失败");
    directoryOrganizations.value = Array.isArray(result) ? result : [];
  } catch (error) {
    directoryOrganizations.value = [];
    ElMessage.error(error instanceof Error ? error.message : "企业目录加载失败");
  }
}

async function loadDepartmentMembers(department: DingDepartment, force = false) {
  const page = departmentMemberPage(department);
  if (page.loaded && !force) return;
  if (permissionPreviewMode) {
    const records = cloneTestDirectoryEmployees(testDirectoryEmployees.filter((employee) =>
      (!department.corpCode || employee.corpCode === department.corpCode)
      && (employee.departmentId === department.id || employee.departmentIds?.includes(department.id))));
    const offset = (page.pageNum - 1) * page.pageSize;
    page.records = records.slice(offset, offset + page.pageSize);
    page.total = records.length;
    page.error = "";
    page.loaded = true;
    page.records.forEach((employee) => hydrateDirectoryEmployee(employee, department.id));
    return;
  }
  page.loading = true;
  page.error = "";
  const query = new URLSearchParams({
    pageNum: String(page.pageNum),
    pageSize: String(page.pageSize),
    departmentId: String(department.id),
  });
  if (department.corpCode) query.set("corpCode", department.corpCode);
  try {
    const response = await fetch(`/api/admin/directory/employees?${query}`, { credentials: "include" });
    const result = await readApi<{ records: DingEmployee[]; total: number }>(response, "部门成员加载失败");
    page.records = result.records ?? [];
    page.total = result.total ?? 0;
    page.records.forEach((employee) => {
      hydrateDirectoryEmployee(employee, department.id);
    });
    page.loaded = true;
  } catch (error) {
    page.records = [];
    page.total = 0;
    page.loaded = false;
    page.error = error instanceof Error ? error.message : "部门成员加载失败";
  } finally {
    page.loading = false;
  }
}

function handleDepartmentExpand(department: DingDepartment, expandedRows: DingDepartment[]) {
  if (expandedRows.some((row) => departmentMemberKey(row) === departmentMemberKey(department))) {
    void loadDepartmentMembers(department);
  }
}

function changeDirectoryOrganization() {
  directoryPageNum.value = 1;
  void loadDirectory();
}

function toggleDepartmentSelection(departmentId: number, selected: boolean) {
  const ids = new Set(selectedDepartmentIds.value);
  const revokedIds = new Set(revokedDepartmentIds.value);
  if (selected) {
    ids.add(departmentId);
    revokedIds.delete(departmentId);
  } else {
    ids.delete(departmentId);
    revokedIds.add(departmentId);
  }
  selectedDepartmentIds.value = [...ids];
  revokedDepartmentIds.value = [...revokedIds];
  partiallySelectedDepartmentIds.value = partiallySelectedDepartmentIds.value
    .filter((id) => id !== departmentId);

  const excludedEmployeeIds = new Set(departmentExcludedEmployeeIds.value);
  // 部门选择是强联动操作：本次勾选或取消统一覆盖该部门已加载成员的个人编辑状态。
  Object.values(loadedDirectoryEmployees)
    .filter((employee) => employeeDepartmentIds(employee).includes(departmentId))
    .forEach((employee) => {
      const employeeKey = employeeIdentityKey(employee);
      employeeEnabledDraft[employeeKey] = selected;
      employeePermissionEdited[employeeKey] = true;
      reenabledEmployeeIds.value = reenabledEmployeeIds.value.filter((employeeId) => employeeId !== employee.id);
      if (selected || !employeeDepartmentIds(employee).some((employeeDepartmentId) => ids.has(employeeDepartmentId))) {
        excludedEmployeeIds.delete(employee.id);
      } else {
        excludedEmployeeIds.add(employee.id);
      }
    });
  departmentExcludedEmployeeIds.value = [...excludedEmployeeIds];
}

/**
 * 部门复选框代表“部门内成员全部开启”，与后端保留的部门授权规则分开计算。
 * 单个成员关闭时，部门授权仍保留并通过排除记录屏蔽该成员，避免误关同部门其他成员。
 */
function isDepartmentFullySelected(departmentId: number) {
  if (!selectedDepartmentIds.value.includes(departmentId)) return false;
  if (partiallySelectedDepartmentIds.value.includes(departmentId)) return false;
  return !Object.values(loadedDirectoryEmployees).some((employee) =>
    employeeDepartmentIds(employee).includes(departmentId)
    && employeeEnabledDraft[employeeIdentityKey(employee)] === false);
}

function loadedDepartmentIdsForEmployeeId(employeeId: number) {
  return Object.entries(loadedDirectoryEmployees)
    .filter(([, employee]) => employee.id === employeeId)
    .flatMap(([employeeKey]) => loadedEmployeeDepartmentIds[employeeKey] ?? []);
}

function refreshDepartmentPartialSelection(departmentIds: number[]) {
  const partialIds = new Set(partiallySelectedDepartmentIds.value);
  const excludedIds = new Set(departmentExcludedEmployeeIds.value);
  departmentIds
    .filter((departmentId) => selectedDepartmentIds.value.includes(departmentId))
    .forEach((departmentId) => {
      const hasKnownExcludedMember = [...excludedIds].some((employeeId) =>
        loadedDepartmentIdsForEmployeeId(employeeId).includes(departmentId));
      const hasUnknownExcludedMember = [...excludedIds].some((employeeId) =>
        !loadedDepartmentIdsForEmployeeId(employeeId).length);
      if (hasKnownExcludedMember || (hasUnknownExcludedMember && partialIds.has(departmentId))) {
        partialIds.add(departmentId);
      } else {
        partialIds.delete(departmentId);
      }
    });
  partiallySelectedDepartmentIds.value = [...partialIds];
}

function employeeDepartmentIds(employee: DingEmployee) {
  return [...new Set([
    ...(employee.departmentIds ?? []),
    employee.departmentId,
    ...(loadedEmployeeDepartmentIds[employeeIdentityKey(employee)] ?? []),
  ].filter((departmentId): departmentId is number => Number.isFinite(departmentId)))];
}

function employeeInheritedBySelectedDepartment(employee: DingEmployee) {
  return employeeDepartmentIds(employee)
    .some((departmentId) => selectedDepartmentIds.value.includes(departmentId));
}

function handleEmployeePermissionChange(employee: DingEmployee, enabled: boolean) {
  employeePermissionEdited[employeeIdentityKey(employee)] = true;
  const excludedEmployeeIds = new Set(departmentExcludedEmployeeIds.value);
  if (!enabled && employeeInheritedBySelectedDepartment(employee)) excludedEmployeeIds.add(employee.id);
  else excludedEmployeeIds.delete(employee.id);
  departmentExcludedEmployeeIds.value = [...excludedEmployeeIds];

  const reenabledIds = new Set(reenabledEmployeeIds.value);
  const belongsToRevokedDepartment = employeeDepartmentIds(employee)
    .some((departmentId) => revokedDepartmentIds.value.includes(departmentId));
  if (enabled && belongsToRevokedDepartment) reenabledIds.add(employee.id);
  else reenabledIds.delete(employee.id);
  reenabledEmployeeIds.value = [...reenabledIds];

  const selectedEmployeeDepartmentIds = employeeDepartmentIds(employee)
    .filter((departmentId) => selectedDepartmentIds.value.includes(departmentId));
  if (!enabled) {
    partiallySelectedDepartmentIds.value = [...new Set([
      ...partiallySelectedDepartmentIds.value,
      ...selectedEmployeeDepartmentIds,
    ])];
  } else {
    refreshDepartmentPartialSelection(selectedEmployeeDepartmentIds);
  }
}

function isEmployeeSelected(employee: DingEmployee) {
  return employeeEnabledDraft[employeeIdentityKey(employee)] ?? resolveEmployeeEnabled(employee);
}

function isDepartmentPartiallySelected(department: DingDepartment) {
  const members = departmentMemberPage(department).records;
  if (!members.length) return partiallySelectedDepartmentIds.value.includes(department.id);
  const selectedCount = members.filter(isEmployeeSelected).length;
  return selectedCount > 0 && selectedCount < members.length;
}

function selectDirectoryEmployee(employee: DingEmployee, enabled = true) {
  hydrateDirectoryEmployee(employee, employee.departmentId);
  employeeEnabledDraft[employeeIdentityKey(employee)] = enabled;
  handleEmployeePermissionChange(employee, enabled);
}

async function selectDirectoryDepartment(department: DingDepartment, selected = true) {
  await loadAllDepartmentMembers(department);
  toggleDepartmentSelection(department.id, selected);
}

function removeSelectedDirectoryEmployee(employee: DingEmployee) {
  selectDirectoryEmployee(employee, false);
}

async function initializePermissionProfiles() {
  if (testMode) return;
  const pageSize = 100;
  let pageNum = 1;
  let total = 0;
  const permissionSubjects: InvoiceSubject[] = [];
  do {
    const query = buildPermissionSubjectQuery(pageNum, pageSize);
    const response = await fetch(`/api/admin/subjects?${query}`, { credentials: "include" });
    const result = await readApi<{ records: any[]; total: number }>(response, "权限主体列表加载失败");
    permissionSubjects.push(...result.records.map(toSubject));
    total = result.total;
    pageNum += 1;
  } while (permissionSubjects.length < total);

  permissionProfiles.value = permissionSubjects.map((subject) => ({
    id: subject.id,
    subjectName: subject.name,
    visibleCount: 0,
    allEmployeesVisible: false,
    departments: [],
    employeeRules: [],
    employeeCount: 0,
  }));
  if (permissionProfiles.value.length > 0) {
    selectedPermissionProfileId.value = permissionProfiles.value[0].id;
    const loaded = await loadPermissionProfiles(permissionProfiles.value.map((profile) => profile.id), loadPermissionProfile);
    if (!loaded) ElMessage.error("主体权限加载失败");
    const profile = activePermissionProfile.value;
    if (profile) await preparePermissionProfileEmployeeState(profile);
  } else {
    selectedPermissionProfileId.value = 0;
  }
}

async function loadPermissionProfile(subjectId: number) {
  if (testMode) return;
  const response = await fetch(`/api/admin/subjects/${subjectId}/permission-profile`, { credentials: "include" });
  const result = await readApi<any>(response, "主体权限加载失败");
  const profile: SubjectPermissionProfile = {
    id: result.subjectId,
    subjectName: result.subjectName,
    visibleCount: result.visibleCount ?? 0,
    allEmployeesVisible: Boolean(result.allEmployeeVisible),
    departments: result.departments ?? [],
    employeeRules: result.employeeRules ?? [],
    departmentExcludedEmployeeIds: result.departmentExcludedEmployeeIds ?? [],
    partiallySelectedDepartmentIds: result.partiallySelectedDepartmentIds ?? [],
    employeeCount: (result.employeeRules ?? []).length,
  };
  const index = permissionProfiles.value.findIndex((item) => item.id === profile.id);
  if (index >= 0) permissionProfiles.value[index] = profile;
}

async function selectPermissionProfile(subjectId: number) {
  selectedPermissionProfileId.value = subjectId;
  try {
    await loadPermissionProfile(subjectId);
    const profile = activePermissionProfile.value;
    if (profile) await preparePermissionProfileEmployeeState(profile);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "主体权限加载失败");
  }
}

async function loadDirectory() {
  const profile = activePermissionProfile.value;
  if (!profile) return;
  directoryLoading.value = true;
  if (permissionPreviewMode) {
    const normalizedKeyword = directoryKeyword.value.trim().toLocaleLowerCase("zh-CN");
    const matchesKeyword = (values: Array<string | undefined>) => !normalizedKeyword
      || values.some((value) => String(value ?? "").toLocaleLowerCase("zh-CN").includes(normalizedKeyword));
    const matchesCorporation = (corpCode?: string) => !directoryCorpCode.value || corpCode === directoryCorpCode.value;
    const departments = cloneTestDirectoryDepartments(testDirectoryDepartments.filter((department) =>
      matchesCorporation(department.corpCode)
      && matchesKeyword([department.departmentName, department.corpName, department.corpCode])));
    const employees = cloneTestDirectoryEmployees(testDirectoryEmployees.filter((employee) =>
      matchesCorporation(employee.corpCode)
      && matchesKeyword([
        employee.employeeName,
        employee.employeeNo,
        employee.departmentName,
        employee.mobile,
        employee.corpName,
        employee.corpCode,
      ])));

    directoryDepartments.value = permissionForm.targetType === "USER" ? [] : departments;
    directoryEmployees.value = permissionForm.targetType === "DEPARTMENT" ? [] : employees;
    directoryEmployees.value.forEach((employee) => hydrateDirectoryEmployee(employee));
    directoryTotal.value = directoryDepartments.value.length + directoryEmployees.value.length;
    directoryLoading.value = false;
    return;
  }
  const createQuery = (targetType: "USER" | "DEPARTMENT") => {
    const query = new URLSearchParams({
      pageNum: String(directoryPageNum.value),
      pageSize: String(directoryPageSize.value),
    });
    if (directoryKeyword.value.trim()) query.set("keyword", directoryKeyword.value.trim());
    if (directoryCorpCode.value) query.set("corpCode", directoryCorpCode.value);
    if (targetType === "USER") {
      query.set("subjectId", String(profile.id));
      if (employeePermissionStatus.value !== "ALL") query.set("permissionStatus", employeePermissionStatus.value);
    }
    return query;
  };
  const loadResult = async (targetType: "USER" | "DEPARTMENT") => {
    const path = targetType === "USER" ? "employees" : "departments";
    const response = await fetch(`/api/admin/directory/${path}?${createQuery(targetType)}`, { credentials: "include" });
    return readApi<{ records: any[]; total: number }>(response, "通讯录加载失败");
  };
  try {
    const departmentResult = permissionForm.targetType === "USER" ? null : await loadResult("DEPARTMENT");
    const employeeResult = permissionForm.targetType === "DEPARTMENT" ? null : await loadResult("USER");
    if (departmentResult) directoryDepartments.value = departmentResult.records;
    if (employeeResult) {
      directoryEmployees.value = employeeResult.records;
      directoryEmployees.value.forEach((employee) => {
        hydrateDirectoryEmployee(employee);
      });
    }
    directoryTotal.value = (departmentResult?.total ?? 0) + (employeeResult?.total ?? 0);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "通讯录加载失败");
  } finally {
    directoryLoading.value = false;
  }
}

function resetPermissionEmployeeState() {
  Object.keys(employeeEnabledDraft).forEach((key) => delete employeeEnabledDraft[key]);
  Object.keys(employeePermissionEdited).forEach((key) => delete employeePermissionEdited[key]);
  Object.keys(loadedDirectoryEmployees).forEach((key) => delete loadedDirectoryEmployees[key]);
  Object.keys(loadedEmployeeDepartmentIds).forEach((key) => delete loadedEmployeeDepartmentIds[key]);
}

async function hydratePermissionProfileMembers(profile: SubjectPermissionProfile) {
  if (profile.allEmployeesVisible || !profile.departments.length) return;
  await Promise.all(profile.departments.map((department) => loadAllDepartmentMembers(department)));
}

async function preparePermissionProfileEmployeeState(profile: SubjectPermissionProfile) {
  resetPermissionEmployeeState();
  await hydratePermissionProfileMembers(profile);
}

function openPermissionEditor(targetType: "USER" | "DEPARTMENT") {
  const profile = activePermissionProfile.value;
  if (!profile) return;
  void targetType;
  permissionForm.targetType = "ALL";
  permissionForm.subjectName = profile.subjectName;
  directoryKeyword.value = "";
  directoryCorpCode.value = "";
  directoryPageNum.value = 1;
  directorySearchActive.value = false;
  directoryResultType.value = "ALL";
  expandedDirectoryCorpCodes.value = [];
  fullyLoadedDirectoryCorpCodes.value = [];
  expandedDirectoryDepartmentIds.value = [];
  employeePermissionStatus.value = "ALL";
  selectedDepartmentIds.value = profile.departments.map((department) => department.id);
  revokedDepartmentIds.value = [];
  reenabledEmployeeIds.value = [];
  departmentExcludedEmployeeIds.value = [...(profile.departmentExcludedEmployeeIds ?? [])];
  partiallySelectedDepartmentIds.value = [...(profile.partiallySelectedDepartmentIds ?? [])];
  resetPermissionEmployeeState();
  permissionDialogVisible.value = true;
  void (async () => {
    await Promise.all([loadDirectoryOrganizations(), loadDirectory(), hydratePermissionProfileMembers(profile)]);
    await Promise.all(directoryOrganizations.value.map((organization) => loadOrganizationDepartments(organization)));
  })();
}

function inheritedEmployeeEnabled(employee: DingEmployee) {
  const profile = activePermissionProfile.value;
  const departmentIds = permissionDialogVisible.value
    ? selectedDepartmentIds.value
    : profile?.departments.map((department) => department.id) ?? [];
  return Boolean(profile && (profile.allEmployeesVisible
    || employeeDepartmentIds(employee).some((departmentId) => departmentIds.includes(departmentId))));
}

function employeeRuleId(rule: EmployeeRule) {
  const value = Number(rule.employeeId ?? rule.id);
  return Number.isFinite(value) ? value : null;
}

function employeeIdentityKey(employee: Pick<DingEmployee, "id" | "corpCode">) {
  return `${employee.corpCode || "default"}:${employee.id}`;
}

function employeeRuleMatches(rule: EmployeeRule, employee: DingEmployee) {
  if (employeeRuleId(rule) !== employee.id) return false;
  return !rule.corpCode || !employee.corpCode || rule.corpCode === employee.corpCode;
}

/** 部门排除或本次撤销部门优先关闭成员；其余权限取全员、部门与员工 ALLOW 规则的并集。 */
function resolveEmployeeEnabled(employee: DingEmployee) {
  if (departmentExcludedEmployeeIds.value.includes(employee.id)) return false;
  const explicitRule = activePermissionProfile.value?.employeeRules.find((rule) => employeeRuleMatches(rule, employee));
  const revokedByDepartment = employeeDepartmentIds(employee)
    .some((departmentId) => revokedDepartmentIds.value.includes(departmentId));
  const explicitlyReenabled = reenabledEmployeeIds.value.includes(employee.id);
  if (revokedByDepartment && !explicitlyReenabled) return false;
  return inheritedEmployeeEnabled(employee) || explicitRule?.effect === "ALLOW";
}

async function updateAllEmployeesVisibility(enabled: boolean) {
  const profile = activePermissionProfile.value;
  if (!profile || permissionSaving.value) return;
  const previousEnabled = profile.allEmployeesVisible;
  const previousVisibleCount = profile.visibleCount;

  // 先反馈用户点击结果；接口失败时恢复原状态。
  profile.allEmployeesVisible = enabled;
  permissionSaving.value = true;
  try {
    const response = await fetch(`/api/admin/subjects/${profile.id}/permission-profile/all-employee-visible`, {
      method: "PATCH",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ allEmployeeVisible: enabled }),
    });
    const result = await readApi<any>(response, "全员可见设置失败");
    profile.subjectName = result.subjectName ?? profile.subjectName;
    profile.allEmployeesVisible = Boolean(result.allEmployeeVisible);
    profile.visibleCount = result.visibleCount ?? 0;
    profile.departments = result.departments ?? [];
    profile.employeeRules = result.employeeRules ?? [];
    profile.departmentExcludedEmployeeIds = result.departmentExcludedEmployeeIds ?? [];
    profile.partiallySelectedDepartmentIds = result.partiallySelectedDepartmentIds ?? [];
    profile.employeeCount = profile.employeeRules.length;
    ElMessage.success(`全员可见已${profile.allEmployeesVisible ? "开启" : "关闭"}`);
  } catch (error) {
    profile.allEmployeesVisible = previousEnabled;
    profile.visibleCount = previousVisibleCount;
    ElMessage.error(error instanceof Error ? error.message : "全员可见设置失败");
  } finally {
    permissionSaving.value = false;
  }
}

async function savePermissionConfiguration(): Promise<boolean> {
  const profile = activePermissionProfile.value;
  if (!profile) return false;
  permissionSaving.value = true;
  try {
    if (permissionPreviewMode) {
      ElMessage.success(`${profile.subjectName}权限已保存并立即生效`);
      return true;
    }
    const response = await fetch(`/api/admin/subjects/${profile.id}/permission-profile`, {
      method: "PUT",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        allEmployeeVisible: profile.allEmployeesVisible,
        departmentIds: permissionDialogVisible.value
          ? selectedDepartmentIds.value
          : profile.departments.map((department) => department.id),
        revokedDepartmentIds: revokedDepartmentIds.value,
        reenabledEmployeeIds: reenabledEmployeeIds.value,
        departmentExcludedEmployeeIds: departmentExcludedEmployeeIds.value,
        employeeRules: profile.employeeRules
          .filter((rule) => rule.effect === "ALLOW")
          .map(employeeRuleId)
          .filter((employeeId): employeeId is number => employeeId !== null)
          .map((employeeId) => ({ employeeId, effect: "ALLOW" })),
      }),
    });
    if (!response.ok) await readApi(response, "权限保存失败");
    ElMessage.success(`${profile.subjectName}权限已保存并立即生效`);
    // 保存后继续停留在编辑器内，资料刷新失败也不能将已成功的保存误报为失败。
    if (!testMode) {
      try {
        await loadPermissionProfile(profile.id);
      } catch {
        ElMessage.warning("权限已保存，但最新权限信息刷新失败，请稍后重新进入查看");
      }
    }
    return true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "权限保存失败");
    return false;
  } finally {
    permissionSaving.value = false;
  }
}

async function applyPermissionSelection() {
  const profile = activePermissionProfile.value;
  if (!profile) return;
  const selected = new Map(profile.departments.map((department) => [department.id, department]));
  directoryDepartments.value.forEach((department) => {
    if (selectedDepartmentIds.value.includes(department.id)) selected.set(department.id, department);
    else selected.delete(department.id);
  });
  profile.departments = [...selected.values()];
  const rules = new Map<string, EmployeeRule>();
  profile.employeeRules
    .filter((rule) => rule.effect === "ALLOW")
    .forEach((rule) => {
      const employeeId = employeeRuleId(rule);
      if (employeeId !== null) {
        const employee = { ...rule, id: employeeId } as DingEmployee;
        rules.set(employeeIdentityKey(employee), rule);
      }
    });
  Object.entries(employeeEnabledDraft).forEach(([employeeKey, enabled]) => {
    if (!employeePermissionEdited[employeeKey]) return;
    const employee = loadedDirectoryEmployees[employeeKey];
    if (!employee) return;
    const inheritedEnabled = inheritedEmployeeEnabled(employee);
    if (!enabled || inheritedEnabled) rules.delete(employeeKey);
    else rules.set(employeeKey, { ...employee, effect: "ALLOW" });
  });
  profile.employeeRules = [...rules.values()];
  profile.departmentExcludedEmployeeIds = [...departmentExcludedEmployeeIds.value];
  profile.partiallySelectedDepartmentIds = [...partiallySelectedDepartmentIds.value];
  profile.employeeCount = profile.employeeRules.length;
  await savePermissionConfiguration();
}

provide(financeLayoutKey, {
  accountLoading, accountPageNum, accountPageSize, accountTotal, activePermissionProfile, activeStatus,
  applyPermissionSelection, bindingSubject, bindingTitleId, changeFinanceAccountPage, changeSubjectStatus,
  currentSubjectTotal, currentTotal, directoryDepartments, directoryEmployees, directoryKeyword,
  directoryLoading, directoryPageNum, directoryPageSize, directoryTotal, editingSubjectId, editingTitleId,
  employeeEnabledDraft, employeePermissionStatus, employeeRuleId, filteredSubjects, filteredTitles,
  financeAccounts, importErrorSummary, importErrorsLoading, importErrorTaskId, importFileInput,
  importFileName, importHistory, importHistoryPageNum, importHistoryPageSize, importHistoryTotal,
  importRowErrors, importSubmitting, importTemplateUrl, keyword, loadDirectory, loadFinanceAccounts,
  loadImportErrors, loadImportHistory, loadSubjects, loadTitles, openImportDialog, openPermissionEditor,
  openSubjectDialog, openSubjectEditor, openTitleBinding, openTitleEditor, pageNum, pageSize,
  permissionDialogVisible, permissionForm, permissionProfiles, permissionSaving, resetCreateForm,
  resetDirectorySearch, searchDirectory, searchFinanceAccounts,
  selectPermissionProfile, selectedDepartmentIds, selectedPermissionEmployees, selectedPermissionProfileId, selectStatus,
  statusClass, statusLabel, statusOptions, subjectDialogVisible, subjectForm, subjectKeyword,
  subjectPageNum, subjectPageSize, subjectSaving, subjects, subjectStatus, switchMenu, titleBindingSaving,
  titleBindingVisible, titleForm, titleSaving, titles, updateAllEmployeesVisibility,
});

</script>

<template>
  <div v-if="currentUser" class="admin-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">票</span>
        <div>
          <strong>发票抬头</strong>
          <small>财务管理台</small>
        </div>
      </div>

      <nav aria-label="财务管理导航">
        <RouterLink
          v-for="item in navigation"
          :key="item.code"
          :to="{ name: routeNameByMenu[item.code] }"
          :class="{ active: activeMenu === item.code }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          {{ item.label }}
        </RouterLink>
      </nav>

      <el-popover
        v-model:visible="profileMenuVisible"
        placement="top-start"
        trigger="click"
        :width="210"
        popper-class="finance-profile-popover"
      >
        <template #reference>
          <button type="button" class="finance-profile" aria-label="当前登录账号" :aria-expanded="profileMenuVisible">
            <span>{{ profileDisplayName.slice(0, 1) }}</span>
            <div><strong>{{ profileDisplayName }}</strong></div>
            <span class="finance-profile-chevron" aria-label="展开账号菜单"><el-icon><ArrowUp /></el-icon></span>
          </button>
        </template>
        <div class="finance-profile-menu" aria-label="账号操作菜单">
          <button type="button" @click="openChangePasswordFromProfile"><el-icon><Lock /></el-icon>修改密码</button>
          <button type="button" class="danger" @click="logoutFromProfile"><el-icon><SwitchButton /></el-icon>退出登录</button>
        </div>
      </el-popover>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div>
          <h1>{{ pageTitle }}</h1>
        </div>
      </header>

      <RouterView />

    </section>

    <el-dialog v-model="importVisible" title="批量导入抬头" width="620px" @close="resetImportDialogState">
      <el-tabs>
        <el-tab-pane label="上传文件">
          <div class="upload-zone" @click="chooseImportFile">
            <input ref="importFileInput" class="visually-hidden-input" type="file" accept=".xlsx" @click.stop @change="handleImportFileChange" />
            <el-icon><Upload /></el-icon>
            <strong>{{ importFileName || '点击选择 Excel 文件' }}</strong>
            <p>支持 .xlsx，单次最多 1,000 条；仅导入抬头信息，不包含主体，导入后生成草稿。</p>
          </div>
          <el-button link type="primary" tag="a" :href="importTemplateUrl"><el-icon><Download /></el-icon>下载导入模板</el-button>
          <el-alert
            v-if="importResult && importResult.failureCount > 0"
            aria-label="本次导入结果"
            class="import-error-alert"
            :type="importResult.successCount > 0 ? 'warning' : 'error'"
            :closable="false"
            show-icon
            :title="importResultTitle(importResult)"
          >
            <template #default>
              <p v-if="!importRowErrors.length">失败原因加载失败，可在“导入历史”中重新查看。</p>
              <p v-for="error in importRowErrors" :key="error.id">失败原因：{{ importErrorSummary(error) }}</p>
            </template>
          </el-alert>
        </el-tab-pane>
        <el-tab-pane label="导入历史">
          <div v-for="task in importHistory" :key="task.id" class="history-row">
            <span>{{ formatDateTime(task.createdAt) }} · {{ task.createdBy }}<small>{{ task.originalFileName }} · {{ task.taskNo }}</small></span>
            <div class="history-result">
              <strong>成功 {{ task.successCount }}，失败 {{ task.failureCount }}</strong>
              <el-button v-if="task.failureCount" link type="danger" :loading="importErrorsLoading && importErrorTaskId === task.id" @click="loadImportErrors(task.id)">查看失败原因</el-button>
            </div>
          </div>
          <el-alert
            v-if="importRowErrors.length"
            class="import-error-alert"
            type="error"
            :closable="false"
            title="失败明细"
          >
            <template #default>
              <p v-for="error in importRowErrors" :key="error.id">{{ importErrorSummary(error) }}</p>
            </template>
          </el-alert>
          <div aria-label="导入历史分页" class="history-pagination">
            <el-pagination
              v-model:current-page="importHistoryPageNum"
              v-model:page-size="importHistoryPageSize"
              :total="importHistoryTotal"
              :page-sizes="[10,20,50,100]"
              layout="total, prev, pager, next"
              size="small"
              @current-change="loadImportHistory"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="closeImportDialog">取消</el-button>
        <el-button type="primary" :disabled="!importFileName" :loading="importSubmitting" @click="submitImport">校验并导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createVisible" :title="editingTitleId ? '编辑发票抬头' : '新增发票抬头'" width="720px">
      <el-form :model="titleForm" label-position="top" :show-message="false">
        <div class="form-grid">
          <el-form-item label="公司名称" prop="companyName" required><el-input v-model="titleForm.companyName" placeholder="请输入完整公司名称" maxlength="200" @input="titleFormErrors.companyName = ''" /><span v-if="titleFormErrors.companyName" class="el-form-item__error">{{ titleFormErrors.companyName }}</span></el-form-item>
          <el-form-item label="纳税人识别号" prop="taxpayerId" required><el-input v-model="titleForm.taxpayerId" placeholder="请输入15-20位大写字母或数字" maxlength="20" @input="titleFormErrors.taxpayerId = ''" /><span v-if="titleFormErrors.taxpayerId" class="el-form-item__error">{{ titleFormErrors.taxpayerId }}</span></el-form-item>
          <el-form-item class="full" label="注册地址"><el-input v-model="titleForm.address" placeholder="请输入注册地址" /></el-form-item>
          <el-form-item label="电话" prop="phone"><el-input v-model="titleForm.phone" placeholder="请输入手机号、固定电话或400/800客服电话" maxlength="50" @input="titleFormErrors.phone = ''" /><span v-if="titleFormErrors.phone" class="el-form-item__error">{{ titleFormErrors.phone }}</span></el-form-item>
          <el-form-item label="开户行"><el-input v-model="titleForm.bankName" placeholder="请输入开户银行" /></el-form-item>
          <el-form-item label="银行账号" prop="bankAccount"><el-input v-model="titleForm.bankAccount" placeholder="请输入8-32位数字" maxlength="32" @input="titleFormErrors.bankAccount = ''" /><span v-if="titleFormErrors.bankAccount" class="el-form-item__error">{{ titleFormErrors.bankAccount }}</span></el-form-item>
          <el-form-item label="展示主体（可后期选择）">
            <el-select v-model="titleForm.subjectId" clearable placeholder="可选择一个主体">
              <el-option
                v-for="subject in titleSubjectOptions"
                :key="subject.id"
                :label="subject.status !== 'ENABLED' ? `${subject.name}（已停用）` : subject.boundTitleId && subject.boundTitleId !== editingTitleId ? `${subject.name}（已绑定${subject.boundTitleName || '其他抬头'}）` : subject.name"
                :value="subject.id"
                :disabled="subject.status !== 'ENABLED' || Boolean(subject.boundTitleId && subject.boundTitleId !== editingTitleId)"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button :loading="titleSaving" @click="saveTitle('DRAFT')">保存草稿</el-button>
        <el-button type="primary" :loading="titleSaving" @click="saveTitle('PUBLISHED')">保存并发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="subjectDialogVisible" :title="editingSubjectId ? '编辑主体' : '新增主体'" width="520px">
      <el-form label-position="top">
        <el-form-item label="主体名称" required><el-input v-model="subjectForm.name" placeholder="例如：杭州主体" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="subjectForm.status"><el-radio value="ENABLED">启用</el-radio><el-radio value="DISABLED">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="展示顺序"><el-input-number v-model="subjectForm.sortNo" :min="0" :max="9999" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="subjectDialogVisible = false">取消</el-button><el-button type="primary" :loading="subjectSaving" @click="saveSubject">保存主体</el-button></template>
    </el-dialog>

    <el-dialog
      v-model="permissionDialogVisible"
      title="编辑部分可见范围"
      width="1120px"
      class="partial-permission-dialog"
      aria-label="编辑可见范围"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <section class="permission-scope-editor" aria-label="编辑可见范围">
        <section class="permission-directory-pane" aria-label="企业部门员工选择树">
          <div class="permission-directory-search">
            <el-icon><Search /></el-icon>
            <input
              v-model="directoryKeyword"
              type="text"
              aria-label="搜索部门或员工"
              placeholder="搜索姓名、工号、部门或手机号"
              @input="scheduleDirectorySearch"
              @keyup.enter="searchDirectory"
            />
            <button v-if="directoryKeyword" type="button" aria-label="清空搜索" @click="resetDirectorySearch">×</button>
          </div>

          <template v-if="directorySearchActive">
            <div class="permission-search-tabs" role="tablist" aria-label="搜索结果类型">
              <button type="button" role="tab" :aria-selected="directoryResultType === 'ALL'" :class="{ active: directoryResultType === 'ALL' }" @click="directoryResultType = 'ALL'; changePermissionResultType()">全部</button>
              <button type="button" role="tab" :aria-selected="directoryResultType === 'USER'" :class="{ active: directoryResultType === 'USER' }" @click="directoryResultType = 'USER'; changePermissionResultType()">联系人</button>
              <button type="button" role="tab" :aria-selected="directoryResultType === 'DEPARTMENT'" :class="{ active: directoryResultType === 'DEPARTMENT' }" @click="directoryResultType = 'DEPARTMENT'; changePermissionResultType()">部门</button>
            </div>
            <div v-loading="directoryLoading" class="permission-search-results">
              <section v-if="directoryResultType !== 'DEPARTMENT'" class="permission-search-group">
                <h4>联系人</h4>
                <button
                  v-for="employee in directoryEmployees"
                  :key="`employee-${employeeIdentityKey(employee)}`"
                  type="button"
                  class="permission-contact-card"
                  :class="{ selected: isEmployeeSelected(employee) }"
                  :aria-label="`联系人 ${employee.employeeName}`"
                  @click="selectDirectoryEmployee(employee, true)"
                >
                  <span class="permission-person-avatar">{{ employee.employeeName.slice(0, 1) }}</span>
                  <span><strong>{{ employee.employeeName }}</strong><small>{{ employee.employeeNo }} · {{ employee.departmentName }}</small><small>{{ employee.mobile }} · {{ employee.corpName || employee.corpCode }}</small></span>
                </button>
              </section>
              <section v-if="directoryResultType !== 'USER'" class="permission-search-group">
                <h4>部门</h4>
                <button
                  v-for="department in directoryDepartments"
                  :key="`department-${department.id}`"
                  type="button"
                  class="permission-department-card"
                  :class="{ selected: isDepartmentFullySelected(department.id) }"
                  :aria-label="`部门 ${department.departmentName}`"
                  @click="selectDirectoryDepartment(department, true)"
                >
                  <span class="permission-department-icon"><el-icon><OfficeBuilding /></el-icon></span>
                  <span><strong>{{ department.departmentName }}</strong><small>{{ department.corpName || department.corpCode }}</small></span>
                  <small>{{ department.employeeCount }} 人</small>
                </button>
              </section>
              <p v-if="!directoryLoading && !directoryEmployees.length && !directoryDepartments.length" class="permission-empty">未找到匹配的联系人或部门</p>
            </div>
          </template>

          <div v-else v-loading="directoryLoading" class="permission-directory-tree">
            <article v-for="organization in directoryOrganizations" :key="organization.corpCode" class="permission-corp-node">
              <div class="permission-corp-row">
                <el-checkbox
                  :model-value="isOrganizationFullySelected(organization)"
                  :indeterminate="isOrganizationPartiallySelected(organization)"
                  :aria-label="`选择企业 ${organization.corpName}`"
                  @click.stop
                  @change="selectDirectoryOrganization(organization, Boolean($event))"
                />
                <button type="button" class="permission-corp-toggle" :data-corp-code="organization.corpCode" @click="toggleOrganizationTree(organization)">
                  <span class="permission-corp-logo">{{ organization.corpName.slice(0, 1) }}</span>
                  <strong>{{ organization.corpName }}</strong>
                  <span class="permission-tree-arrow" :class="{ expanded: expandedDirectoryCorpCodes.includes(organization.corpCode) }">›</span>
                </button>
              </div>
              <div v-if="expandedDirectoryCorpCodes.includes(organization.corpCode)" class="permission-department-tree">
                <article v-for="department in organizationDepartments(organization)" :key="department.id" class="permission-department-node">
                  <div class="permission-department-row">
                    <el-checkbox
                      :model-value="isDepartmentFullySelected(department.id)"
                      :indeterminate="isDepartmentPartiallySelected(department)"
                      :aria-label="`选择部门 ${department.departmentName}`"
                      @change="selectDirectoryDepartment(department, Boolean($event))"
                    />
                    <button type="button" :data-department-id="department.id" @click="toggleDepartmentTree(department)">
                      <span class="permission-tree-arrow" :class="{ expanded: expandedDirectoryDepartmentIds.includes(department.id) }">›</span>
                      <span><strong>{{ department.departmentName }}</strong><small>{{ department.employeeCount }} 人</small></span>
                    </button>
                  </div>
                  <div v-if="expandedDirectoryDepartmentIds.includes(department.id)" v-loading="departmentMemberPage(department).loading" class="permission-employee-tree">
                    <div
                      v-for="employee in departmentMemberPage(department).records"
                      :key="employeeIdentityKey(employee)"
                      class="permission-employee-row"
                      :class="{ selected: isEmployeeSelected(employee) }"
                      role="button"
                      tabindex="0"
                      @click="selectDirectoryEmployee(employee, !isEmployeeSelected(employee))"
                      @keyup.enter="selectDirectoryEmployee(employee, !isEmployeeSelected(employee))"
                    >
                      <el-checkbox
                        :model-value="isEmployeeSelected(employee)"
                        :aria-label="`选择员工 ${employee.employeeName}`"
                        @click.stop
                        @change="selectDirectoryEmployee(employee, Boolean($event))"
                      />
                      <span class="permission-person-avatar">{{ employee.employeeName.slice(0, 1) }}</span>
                      <span><strong>{{ employee.employeeName }}</strong><small>{{ employee.employeeNo }} · {{ employee.mobile }}</small></span>
                    </div>
                    <p v-if="departmentMemberPage(department).error" class="permission-empty">{{ departmentMemberPage(department).error }}</p>
                  </div>
                </article>
              </div>
            </article>
            <p v-if="!directoryLoading && !directoryOrganizations.length" class="permission-empty">暂无可选企业通讯录</p>
          </div>
        </section>

        <aside class="permission-selected-pane" aria-label="已选择人员">
          <header><strong>已选择</strong><span>（{{ selectedPermissionEmployees.length }}/5000）</span></header>
          <div class="permission-selected-chips">
            <span v-for="employee in selectedPermissionEmployees" :key="employeeIdentityKey(employee)" class="permission-selected-chip">
              <span class="permission-person-avatar">{{ employee.employeeName.slice(0, 1) }}</span>
              <span><strong>{{ employee.employeeName }}</strong></span>
              <button type="button" :aria-label="`移除 ${employee.employeeName}`" @click="removeSelectedDirectoryEmployee(employee)">×</button>
            </span>
          </div>
          <p v-if="!selectedPermissionEmployees.length" class="permission-selected-empty">从左侧选择部门或员工</p>
        </aside>
      </section>
      <template #footer><el-button @click="permissionDialogVisible = false">取消</el-button><el-button type="primary" :loading="permissionSaving" @click="applyPermissionSelection">确定选择</el-button></template>
    </el-dialog>

    <el-dialog v-model="titleBindingVisible" :title="bindingSubject ? `为${bindingSubject.name}绑定抬头` : '绑定抬头'" width="560px">
      <el-alert title="抬头与主体为一对一关系；再次绑定将替换主体和抬头两侧的原关系。" type="info" :closable="false" />
      <el-form label-position="top" class="binding-form">
        <el-form-item label="发票抬头" required>
          <el-select v-model="bindingTitleId" filterable placeholder="搜索并选择抬头公司名称" style="width: 100%">
            <el-option
              v-for="title in subjectTitleOptions"
              :key="title.id"
              :label="`${title.companyName}（${title.status === 'PUBLISHED' ? '已发布' : title.status === 'DRAFT' ? '草稿' : '已停用'}）`"
              :value="title.id"
              :disabled="title.status === 'DISABLED'"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="titleBindingVisible = false">取消</el-button><el-button type="primary" :loading="titleBindingSaving" @click="saveTitleBinding">确认绑定</el-button></template>
    </el-dialog>

    <ChangePasswordDialog v-model="changePasswordVisible" />
  </div>

  <div v-else class="auth-loading" aria-label="正在验证登录状态">
    <span class="brand-mark">票</span>
    <p>正在验证登录状态…</p>
  </div>
</template>

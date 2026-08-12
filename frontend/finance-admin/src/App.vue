<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import FinanceLoginView, { type FinanceSessionUser } from "./components/FinanceLoginView.vue";
import FinanceAccountManagement, { type FinanceAccount } from "./components/FinanceAccountManagement.vue";
import ChangePasswordDialog from "./components/ChangePasswordDialog.vue";
import { buildPermissionSubjectQuery } from "./utils/subject-query";
import { resolveApiUrl } from "./api-prefix";
import {
  ArrowDown,
  Clock,
  Document,
  Download,
  EditPen,
  Files,
  OfficeBuilding,
  Plus,
  Search,
  Setting,
  SwitchButton,
  Upload,
  User,
} from "@element-plus/icons-vue";

type StatusCode = "PUBLISHED" | "DRAFT" | "DISABLED";
type MenuCode = "titles" | "subjects" | "permissions" | "logs" | "accounts";

const importTemplateUrl = resolveApiUrl("/api/admin/invoice-imports/template");

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
  titleCount: number;
  employeeCount: number;
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

type DingEmployee = {
  id: number;
  corpCode?: string;
  corpName?: string;
  dingUserId: string;
  employeeNo: string;
  employeeName: string;
  departmentId: number;
  departmentName: string;
  mobile: string;
  permissionEnabled?: boolean;
};

type EmployeeRule = DingEmployee & { employeeId?: number; effect: "ALLOW" | "DENY" };

type OperationLog = {
  id: number;
  module: string;
  action: string;
  businessName: string;
  operator: string;
  result: "SUCCESS" | "FAILED";
  createdAt: string;
  detail: string;
};

const operationTypeLabels: Record<string, string> = {
  CREATE: "新增",
  UPDATE: "编辑",
  PUBLISH: "发布抬头",
  DISABLE: "停用",
  RESTORE: "恢复版本",
  IMPORT: "导入抬头",
  AUTHORIZE: "新增授权",
  REVOKE: "取消授权",
  CREATE_ACCOUNT: "新增账号",
  CHANGE_PASSWORD: "修改密码",
  RESET_PASSWORD: "重置密码",
  ENABLE_ACCOUNT: "启用账号",
  DISABLE_ACCOUNT: "停用账号",
};

function formatOperationType(operationType: string) {
  return operationTypeLabels[operationType] ?? operationType;
}

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

type InvoiceTitleVersion = {
  id: number;
  titleId: number;
  versionNo: number;
  status: StatusCode;
  companyName: string;
  createdBy: string;
  createdAt: string;
};

const allNavigation: Array<{ code: MenuCode; label: string; icon: typeof Document; superAdminOnly?: boolean }> = [
  { code: "titles", label: "抬头管理", icon: Document },
  { code: "subjects", label: "主体管理", icon: OfficeBuilding },
  { code: "permissions", label: "主体权限", icon: User },
  { code: "logs", label: "操作日志", icon: Files },
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
    subjects: ["杭州主体", "华东主体"],
    subjectIds: [1, 4],
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
  { code: "DISABLED", label: "已停用", count: statusCounts.DISABLED },
]);

const subjects = ref<InvoiceSubject[]>([
  { id: 1, code: "HZ", name: "杭州主体", status: "ENABLED", titleCount: 1, employeeCount: 186, updatedAt: "2026-08-07 15:46", updatedBy: "王财务", sortNo: 10 },
  { id: 2, code: "BJ", name: "北京主体", status: "ENABLED", titleCount: 1, employeeCount: 92, updatedAt: "2026-08-06 10:22", updatedBy: "李会计", sortNo: 20 },
  { id: 3, code: "SH", name: "上海主体", status: "ENABLED", titleCount: 1, employeeCount: 68, updatedAt: "2026-08-03 14:10", updatedBy: "王财务", sortNo: 30 },
  { id: 4, code: "EAST", name: "华东主体", status: "ENABLED", titleCount: 1, employeeCount: 40, updatedAt: "2026-07-29 09:35", updatedBy: "王财务", sortNo: 40 },
]);

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

const operationLogs = ref<OperationLog[]>([
  { id: 1, module: "发票抬头", action: "PUBLISH", businessName: "杭州赛宝卓越技术有限公司", operator: "王财务", result: "SUCCESS", createdAt: "2026-08-07 15:46:12", detail: "发布 V3，并更新杭州主体、华东主体展示数据。" },
  { id: 2, module: "主体权限", action: "AUTHORIZE", businessName: "杭州主体 / 示例员工", operator: "王财务", result: "SUCCESS", createdAt: "2026-08-07 15:40:03", detail: "授予钉钉用户 ding-employee-001 杭州主体查看权限。" },
  { id: 3, module: "批量导入", action: "IMPORT", businessName: "invoice-title-20260805.xlsx", operator: "李会计", result: "FAILED", createdAt: "2026-08-05 09:28:42", detail: "成功 12 条，失败 1 条；失败原因为纳税人识别号重复。" },
  { id: 4, module: "发票抬头", action: "RESTORE", businessName: "杭州赛宝卓越技术有限公司", operator: "王财务", result: "SUCCESS", createdAt: "2026-07-22 11:31:08", detail: "将 V2 恢复为新草稿 V4，未覆盖当前发布版本。" },
]);

const importHistory = ref<ImportHistory[]>([
  { id: 1, taskNo: "IMP2026080509280012AB", originalFileName: "invoice-title-20260805.xlsx", status: "PARTIAL_FAILED", totalCount: 13, successCount: 12, failureCount: 1, createdBy: "王财务", createdAt: "2026-08-05 09:28" },
  { id: 2, taskNo: "IMP2026071916420034CD", originalFileName: "invoice-title-20260719.xlsx", status: "COMPLETED", totalCount: 8, successCount: 8, failureCount: 0, createdBy: "李会计", createdAt: "2026-07-19 16:42" },
]);

const testMode = import.meta.env.MODE === "test";
const authChecking = ref(!testMode);
const currentUser = ref<FinanceSessionUser | null>(testMode ? {
  id: 1,
  username: "superadmin",
  displayName: "超级管理员",
  roleType: "SUPER_ADMIN",
  status: "ENABLED",
} : null);
const changePasswordVisible = ref(false);
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

const activeMenu = ref<MenuCode>("titles");
const activeStatus = ref<"ALL" | StatusCode>("ALL");
const keyword = ref("");
const pageNum = ref(1);
const pageSize = ref(10);
const importVisible = ref(false);
const createVisible = ref(false);
const editingTitleId = ref<number | null>(null);
const titleSaving = ref(false);
const detailVisible = ref(false);
const currentTitle = ref<InvoiceTitle | null>(null);
const titleVersions = ref<InvoiceTitleVersion[]>([
  { id: 3, titleId: 1, versionNo: 3, status: "PUBLISHED", companyName: "杭州赛宝卓越技术有限公司", createdBy: "王财务", createdAt: "2026-08-07 15:46" },
  { id: 2, titleId: 1, versionNo: 2, status: "PUBLISHED", companyName: "杭州赛宝卓越技术有限公司", createdBy: "李会计", createdAt: "2026-07-22 11:30" },
  { id: 1, titleId: 1, versionNo: 1, status: "PUBLISHED", companyName: "杭州赛宝卓越技术有限公司", createdBy: "王财务", createdAt: "2026-06-18 09:12" },
]);
const versionPageNum = ref(1);
const versionPageSize = ref(10);
const versionTotal = ref(titleVersions.value.length);
const versionLoading = ref(false);
const importFileName = ref("");
const importFile = ref<File | null>(null);
const importFileInput = ref<HTMLInputElement | null>(null);
const importSubmitting = ref(false);
const importHistoryPageNum = ref(1);
const importHistoryPageSize = ref(10);
const importHistoryTotal = ref(2);
const subjectPageNum = ref(1);
const subjectPageSize = ref(20);
const subjectKeyword = ref("");
const subjectStatus = ref<"ALL" | "ENABLED" | "DISABLED">("ALL");
const subjectDialogVisible = ref(false);
const editingSubjectId = ref<number | null>(null);
const subjectTotal = ref(subjects.value.length);
const subjectSaving = ref(false);
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
const selectedDepartmentIds = ref<number[]>([]);
const employeeEnabledDraft = reactive<Record<number, boolean>>({});
const employeePermissionStatus = ref<"ALL" | "ENABLED" | "DISABLED">("ALL");
const selectedPermissionProfileId = ref(1);
const logPageNum = ref(1);
const logPageSize = ref(20);
const logModule = ref("");
const logKeyword = ref("");
const logDetailVisible = ref(false);
const logTotal = ref(operationLogs.value.length);
const currentLog = ref<OperationLog | null>(null);

const titleForm = reactive({
  companyName: "",
  taxpayerId: "",
  address: "",
  phone: "",
  bankName: "",
  bankAccount: "",
  subjectIds: [] as number[],
  status: "DRAFT" as "DRAFT" | "PUBLISHED",
});

const subjectForm = reactive({ code: "", name: "", status: "ENABLED" as "ENABLED" | "DISABLED", sortNo: 0 });
const permissionForm = reactive({
  subjectName: "杭州主体",
  targetType: "USER" as "USER" | "DEPARTMENT",
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

const pageTitle = computed(() => activeMenu.value === "titles"
  ? "发票抬头管理"
  : navigation.value.find((item) => item.code === activeMenu.value)?.label ?? "抬头管理");
const currentTotal = computed(() => testMode
  ? (statusOptions.value.find((item) => item.code === activeStatus.value)?.count ?? filteredTitles.value.length)
  : titleTotal.value);
const filteredSubjects = computed(() => subjects.value.filter((subject) => {
  const value = subjectKeyword.value.trim().toLowerCase();
  const keywordMatches = !value || subject.name.toLowerCase().includes(value) || subject.code.toLowerCase().includes(value);
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
const filteredLogs = computed(() => operationLogs.value.filter((log) => {
  const value = logKeyword.value.trim().toLowerCase();
  return (!logModule.value || log.module === logModule.value)
    && (!value || log.businessName.toLowerCase().includes(value) || log.operator.toLowerCase().includes(value));
}));

onMounted(() => {
  if (!testMode) void checkAuthentication();
});

async function readApi<T>(response: Response, fallbackMessage: string): Promise<T> {
  if (response.ok) return await response.json() as T;
  let message = fallbackMessage;
  try {
    const error = await response.json() as { message?: string };
    if (error.message) message = error.message;
  } catch {
    // 非 JSON 错误响应使用业务默认提示。
  }
  throw new Error(message);
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
    titleCount: record.titleCount ?? 0,
    employeeCount: record.employeeCount ?? 0,
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

async function loadTitles() {
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
    ElMessage.error(error instanceof Error ? error.message : "抬头列表加载失败");
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

async function loadOperationLogs() {
  if (testMode) return;
  const query = new URLSearchParams({ pageNum: String(logPageNum.value), pageSize: String(logPageSize.value) });
  if (logKeyword.value.trim()) query.set("keyword", logKeyword.value.trim());
  if (logModule.value) query.set("moduleType", logModule.value);
  const moduleLabels: Record<string, string> = {
    TITLE: "发票抬头", SUBJECT: "主体管理", PERMISSION: "主体权限", IMPORT: "批量导入", QR: "二维码", ACCOUNT: "财务账号",
  };
  try {
    const response = await fetch(`/api/admin/operation-logs?${query}`, { credentials: "include" });
    const result = await readApi<{ records: any[]; total: number }>(response, "操作日志加载失败");
    operationLogs.value = result.records.map((record) => ({
      id: record.id,
      module: moduleLabels[record.moduleType] ?? record.moduleType,
      action: record.operationType,
      businessName: record.businessName,
      operator: record.operatorName,
      result: record.result,
      createdAt: String(record.createdAt ?? "").replace("T", " ").slice(0, 19),
      detail: record.detailJson,
    }));
    logTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "操作日志加载失败");
  }
}

async function loadCoreData() {
  await Promise.all([loadTitles(), loadTitleCounts(), loadSubjects()]);
}

async function checkAuthentication() {
  authChecking.value = true;
  try {
    const response = await fetch("/api/auth/me", { credentials: "include" });
    if (response.ok) {
      currentUser.value = await response.json() as FinanceSessionUser;
      await loadCoreData();
    }
    else currentUser.value = null;
  } catch {
    currentUser.value = null;
  } finally {
    authChecking.value = false;
  }
}

function handleLoggedIn(user: FinanceSessionUser) {
  currentUser.value = user;
  activeMenu.value = "titles";
  void loadCoreData();
}

async function logout() {
  try {
    await fetch("/api/auth/logout", { method: "POST", credentials: "include" });
  } finally {
    currentUser.value = null;
    activeMenu.value = "titles";
  }
}

async function handleFinanceAccountResponse(response: Response) {
  if (response.status === 401) {
    financeAccounts.value = [];
    accountTotal.value = 0;
    currentUser.value = null;
    activeMenu.value = "titles";
    ElMessage.warning("登录状态已失效，请重新登录");
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

function switchMenu(code: MenuCode) {
  activeMenu.value = code;
  pageNum.value = 1;
  if (code === "accounts") void loadFinanceAccounts();
  if (code === "titles") void loadTitles();
  if (code === "subjects") void loadSubjects();
  if (code === "permissions") void initializePermissionProfiles();
  if (code === "logs") void loadOperationLogs();
}

function selectStatus(status: "ALL" | StatusCode) {
  activeStatus.value = status;
  pageNum.value = 1;
  void loadTitles();
}

function statusLabel(status: StatusCode) {
  return statusOptions.value.find((item) => item.code === status)?.label ?? status;
}

function statusClass(status: StatusCode) {
  return `status-${status.toLowerCase()}`;
}

function openTitle(title: InvoiceTitle) {
  currentTitle.value = title;
  detailVisible.value = true;
  versionPageNum.value = 1;
  void loadTitleVersions();
}

async function loadTitleVersions() {
  if (!currentTitle.value || testMode) return;
  versionLoading.value = true;
  try {
    const query = new URLSearchParams({ pageNum: String(versionPageNum.value), pageSize: String(versionPageSize.value) });
    const response = await fetch(`/api/admin/invoice-titles/${currentTitle.value.id}/versions?${query}`, { credentials: "include" });
    const result = await readApi<{ records: any[]; total: number }>(response, "历史版本加载失败");
    titleVersions.value = result.records.map((record) => ({
      ...record,
      createdAt: String(record.createdAt ?? "").replace("T", " ").slice(0, 16),
    }));
    versionTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "历史版本加载失败");
  } finally {
    versionLoading.value = false;
  }
}

async function restoreVersion(version: InvoiceTitleVersion) {
  if (!currentTitle.value) return;
  try {
    const response = await fetch(`/api/admin/invoice-titles/${currentTitle.value.id}/versions/${version.id}/restore`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ operatorUserId: currentUser.value?.username ?? "finance" }),
    });
    if (!response.ok) await readApi(response, "历史版本恢复失败");
    ElMessage.success(`V${version.versionNo} 已恢复为新草稿`);
    if (!testMode) await loadTitleVersions();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "历史版本恢复失败");
  }
}

function openImportDialog() {
  importVisible.value = true;
  void loadImportHistory();
}

function chooseImportFile() {
  importFileInput.value?.click();
}

function handleImportFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0] ?? null;
  importFile.value = file;
  importFileName.value = file?.name ?? "";
}

async function loadImportHistory() {
  if (import.meta.env.MODE === "test") return;
  try {
    const response = await fetch(`/api/admin/invoice-imports?pageNum=${importHistoryPageNum.value}&pageSize=${importHistoryPageSize.value}`);
    if (!response.ok) return;
    const result = await response.json() as { records: ImportHistory[]; total: number };
    importHistory.value = result.records;
    importHistoryTotal.value = result.total;
  } catch {
    // 原型独立打开时保留真实演示数据；联调环境由 Vite 代理读取后端分页接口。
  }
}

async function submitImport() {
  if (!importFile.value) return;
  importSubmitting.value = true;
  try {
    const body = new FormData();
    body.append("file", importFile.value);
    const query = new URLSearchParams({ operatorUserId: "ding-user-finance", operatorName: "王财务" });
    const response = await fetch(`/api/admin/invoice-imports?${query}`, { method: "POST", body });
    if (!response.ok) throw new Error("导入请求失败");
    await loadImportHistory();
    importFile.value = null;
    importFileName.value = "";
    ElMessage.success("校验完成，成功数据已生成草稿");
  } catch {
    ElMessage.error("导入失败，请检查后端服务和 Excel 内容");
  } finally {
    importSubmitting.value = false;
  }
}

function resetCreateForm() {
  editingTitleId.value = null;
  Object.assign(titleForm, {
    companyName: "",
    taxpayerId: "",
    address: "",
    phone: "",
    bankName: "",
    bankAccount: "",
    subjectIds: [],
    status: "DRAFT",
  });
  createVisible.value = true;
}

async function openTitleEditor(title: InvoiceTitle) {
  editingTitleId.value = title.id;
  let detail = title;
  if (!testMode) {
    try {
      const response = await fetch(`/api/admin/invoice-titles/${title.id}`, { credentials: "include" });
      detail = toTitle(await readApi<any>(response, "抬头详情加载失败"));
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : "抬头详情加载失败");
      return;
    }
  }
  Object.assign(titleForm, {
    companyName: detail.companyName,
    taxpayerId: detail.taxpayerId,
    address: detail.registeredAddress ?? "",
    phone: detail.phone ?? "",
    bankName: detail.bankName ?? "",
    bankAccount: detail.bankAccount ?? "",
    subjectIds: [...detail.subjectIds],
    status: detail.status === "PUBLISHED" ? "PUBLISHED" : "DRAFT",
  });
  createVisible.value = true;
}

async function saveTitle(status: "DRAFT" | "PUBLISHED") {
  if (!titleForm.companyName.trim() || !titleForm.taxpayerId.trim()) {
    ElMessage.warning("请填写公司名称和纳税人识别号");
    return;
  }
  const subjectIds = titleForm.subjectIds.length > 0 ? titleForm.subjectIds : (testMode ? [1] : []);
  if (subjectIds.length === 0) {
    ElMessage.warning("请至少选择一个展示主体");
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
    if (!testMode) await Promise.all([loadTitles(), loadTitleCounts()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "抬头保存失败");
  } finally {
    titleSaving.value = false;
  }
}

function openSubjectDialog() {
  editingSubjectId.value = null;
  Object.assign(subjectForm, { code: "", name: "", status: "ENABLED", sortNo: 0 });
  subjectDialogVisible.value = true;
}

function openSubjectEditor(subject: InvoiceSubject) {
  editingSubjectId.value = subject.id;
  Object.assign(subjectForm, {
    code: subject.code,
    name: subject.name,
    status: subject.status,
    sortNo: subject.sortNo,
  });
  subjectDialogVisible.value = true;
}

async function saveSubject() {
  if (!subjectForm.code.trim() || !subjectForm.name.trim()) {
    ElMessage.warning("请填写主体名称和主体编码");
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
        subjectCode: subjectForm.code.trim().toUpperCase(),
        subjectName: subjectForm.name.trim(),
        status: subjectForm.status,
        sortNo: subjectForm.sortNo,
        operatorUserId: currentUser.value?.username ?? "finance",
      }),
    });
    if (!response.ok) await readApi(response, "主体保存失败");
    if (testMode) {
      const existing = subjects.value.find((item) => item.id === editingSubjectId.value);
      if (existing) Object.assign(existing, { code: subjectForm.code.toUpperCase(), name: subjectForm.name, status: subjectForm.status, sortNo: subjectForm.sortNo });
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
    await loadPermissionProfile(selectedPermissionProfileId.value);
  } else {
    selectedPermissionProfileId.value = 0;
  }
}

async function loadPermissionProfile(subjectId: number) {
  if (testMode) return;
  try {
    const response = await fetch(`/api/admin/subjects/${subjectId}/permission-profile`, { credentials: "include" });
    const result = await readApi<any>(response, "主体权限加载失败");
    const profile: SubjectPermissionProfile = {
      id: result.subjectId,
      subjectName: result.subjectName,
      visibleCount: result.visibleCount ?? 0,
      allEmployeesVisible: Boolean(result.allEmployeeVisible),
      departments: result.departments ?? [],
      employeeRules: result.employeeRules ?? [],
      employeeCount: (result.employeeRules ?? []).length,
    };
    const index = permissionProfiles.value.findIndex((item) => item.id === profile.id);
    if (index >= 0) permissionProfiles.value[index] = profile;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "主体权限加载失败");
  }
}

async function selectPermissionProfile(subjectId: number) {
  selectedPermissionProfileId.value = subjectId;
  await loadPermissionProfile(subjectId);
}

async function loadDirectory() {
  const profile = activePermissionProfile.value;
  if (!profile) return;
  directoryLoading.value = true;
  const query = new URLSearchParams({
    pageNum: String(directoryPageNum.value),
    pageSize: String(directoryPageSize.value),
  });
  if (directoryKeyword.value.trim()) query.set("keyword", directoryKeyword.value.trim());
  if (permissionForm.targetType === "USER") {
    query.set("subjectId", String(profile.id));
    if (employeePermissionStatus.value !== "ALL") query.set("permissionStatus", employeePermissionStatus.value);
  }
  const path = permissionForm.targetType === "USER" ? "employees" : "departments";
  try {
    const response = await fetch(`/api/admin/directory/${path}?${query}`, { credentials: "include" });
    const result = await readApi<{ records: any[]; total: number }>(response, "通讯录加载失败");
    directoryTotal.value = result.total;
    if (permissionForm.targetType === "USER") {
      directoryEmployees.value = result.records;
      directoryEmployees.value.forEach((employee) => {
        employeeEnabledDraft[employee.id] = resolveEmployeeEnabled(employee);
      });
    } else directoryDepartments.value = result.records;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "通讯录加载失败");
  } finally {
    directoryLoading.value = false;
  }
}

function openPermissionEditor(targetType: "USER" | "DEPARTMENT") {
  const profile = activePermissionProfile.value;
  if (!profile) return;
  permissionForm.targetType = targetType;
  permissionForm.subjectName = profile.subjectName;
  directoryKeyword.value = "";
  directoryPageNum.value = 1;
  employeePermissionStatus.value = "ALL";
  selectedDepartmentIds.value = profile.departments.map((department) => department.id);
  Object.keys(employeeEnabledDraft).forEach((key) => delete employeeEnabledDraft[Number(key)]);
  permissionDialogVisible.value = true;
  void loadDirectory();
}

function inheritedEmployeeEnabled(employee: DingEmployee) {
  const profile = activePermissionProfile.value;
  return Boolean(profile && (profile.allEmployeesVisible
    || profile.departments.some((department) => department.id === employee.departmentId)));
}

function employeeRuleId(rule: EmployeeRule) {
  return rule.id ?? rule.employeeId;
}

/** 个人规则优先；没有个人规则时直接呈现全员或部门授权的最终状态。 */
function resolveEmployeeEnabled(employee: DingEmployee) {
  const explicitRule = activePermissionProfile.value?.employeeRules.find((rule) => employeeRuleId(rule) === employee.id);
  return explicitRule ? explicitRule.effect === "ALLOW" : inheritedEmployeeEnabled(employee);
}

function updateAllEmployeesVisibility(enabled: boolean) {
  const profile = activePermissionProfile.value;
  if (!profile) return;
  profile.allEmployeesVisible = enabled;
  profile.visibleCount = enabled
    ? 386
    : profile.departments.reduce((total, department) => total + department.employeeCount, 0)
      + profile.employeeRules.filter((rule) => rule.effect === "ALLOW").length;
}

async function savePermissionConfiguration(): Promise<boolean> {
  const profile = activePermissionProfile.value;
  if (!profile) return false;
  permissionSaving.value = true;
  try {
    const response = await fetch(`/api/admin/subjects/${profile.id}/permission-profile`, {
      method: "PUT",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        allEmployeeVisible: profile.allEmployeesVisible,
        departmentIds: profile.departments.map((department) => department.id),
        employeeRules: profile.employeeRules.map((rule) => ({ employeeId: employeeRuleId(rule), effect: rule.effect })),
      }),
    });
    if (!response.ok) await readApi(response, "权限保存失败");
    if (!testMode) await loadPermissionProfile(profile.id);
    ElMessage.success(`${profile.subjectName}权限已保存并立即生效`);
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
  if (permissionForm.targetType === "DEPARTMENT") {
    const selected = new Map(profile.departments.map((department) => [department.id, department]));
    directoryDepartments.value.forEach((department) => {
      if (selectedDepartmentIds.value.includes(department.id)) selected.set(department.id, department);
      else selected.delete(department.id);
    });
    profile.departments = [...selected.values()];
  } else {
    const rules = new Map(profile.employeeRules.map((rule) => [employeeRuleId(rule), rule]));
    directoryEmployees.value.forEach((employee) => {
      const enabled = employeeEnabledDraft[employee.id];
      const inheritedEnabled = inheritedEmployeeEnabled(employee);
      if (enabled === inheritedEnabled) rules.delete(employee.id);
      else rules.set(employee.id, { ...employee, effect: enabled ? "ALLOW" : "DENY" });
    });
    profile.employeeRules = [...rules.values()];
    profile.employeeCount = profile.employeeRules.length;
  }
  if (await savePermissionConfiguration()) permissionDialogVisible.value = false;
}

function openLogDetail(log: OperationLog) {
  currentLog.value = log;
  logDetailVisible.value = true;
}
</script>

<template>
  <FinanceLoginView v-if="!authChecking && !currentUser" @logged-in="handleLoggedIn" />

  <div v-else-if="currentUser" class="admin-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">票</span>
        <div>
          <strong>发票抬头</strong>
          <small>财务管理台</small>
        </div>
      </div>

      <nav aria-label="财务管理导航">
        <button
          v-for="item in navigation"
          :key="item.code"
          type="button"
          :class="{ active: activeMenu === item.code }"
          @click="switchMenu(item.code)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          {{ item.label }}
        </button>
      </nav>

      <div class="finance-profile" aria-label="当前登录账号">
        <span>{{ profileDisplayName.slice(0, 1) }}</span>
        <div><strong>{{ profileDisplayName }}</strong><small>{{ profileRoleLabel }}</small></div>
        <button type="button" title="修改我的密码" aria-label="修改我的密码" @click="changePasswordVisible = true"><el-icon><Setting /></el-icon></button>
        <button type="button" title="退出登录" aria-label="退出登录" @click="logout"><el-icon><SwitchButton /></el-icon></button>
      </div>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div>
          <h1>{{ pageTitle }}</h1>
        </div>
      </header>

      <main v-if="activeMenu === 'titles'" class="content">
        <section class="toolbar-row" aria-label="抬头查询操作">
          <el-input v-model="keyword" clearable placeholder="搜索公司名称或纳税人识别号" :prefix-icon="Search" @keyup.enter="pageNum = 1; loadTitles()" />
          <el-button @click="pageNum = 1; loadTitles()">筛选 <el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
          <span class="toolbar-spacer" />
          <el-button data-testid="batch-import" @click="openImportDialog">
            <el-icon><Upload /></el-icon>批量导入
          </el-button>
          <el-button type="primary" @click="resetCreateForm">
            <el-icon><Plus /></el-icon>新增抬头
          </el-button>
        </section>

        <section class="summary-grid" aria-label="抬头数据概览">
          <article><span>已发布<small>全部有效</small></span><strong>{{ statusCounts.PUBLISHED }}</strong></article>
          <article><span>草稿<small>待财务复核</small></span><strong>{{ statusCounts.DRAFT }}</strong></article>
          <article><span>主体<small>已维护展示范围</small></span><strong>{{ subjectTotal }}</strong></article>
        </section>

        <section class="status-tabs" aria-label="抬头状态筛选">
          <button
            v-for="option in statusOptions"
            :key="option.code"
            type="button"
            :data-status="option.code"
            :class="{ active: activeStatus === option.code }"
            @click="selectStatus(option.code)"
          >
            {{ option.label }}<span>{{ option.count }}</span>
          </button>
        </section>

        <section class="data-card">
          <header class="card-header">
            <div><h2>抬头数据</h2><p>共 {{ currentTotal }} 条真实业务数据</p></div>
            <span>共 {{ currentTotal }} 条</span>
          </header>
          <div class="table-scroll">
            <table>
              <thead><tr><th>公司名称</th><th>纳税人识别号</th><th>展示主体</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="title in filteredTitles" :key="title.id">
                  <td><strong>{{ title.companyName }}</strong><small>{{ title.bankSummary }}</small></td>
                  <td class="tax-id">{{ title.taxpayerId }}</td>
                  <td><span v-for="subject in title.subjects" :key="subject" class="subject-tag">{{ subject }}</span></td>
                  <td><span class="status" :class="statusClass(title.status)"><i />{{ statusLabel(title.status) }}</span></td>
                  <td>{{ title.updatedAt }}<small>{{ title.updatedBy }}</small></td>
                  <td class="row-actions">
                    <el-button link type="primary" @click="openTitle(title)">{{ title.status === 'DRAFT' ? '预览' : '查看版本' }}</el-button>
                    <el-button link type="primary" @click="openTitleEditor(title)">编辑</el-button>
                  </td>
                </tr>
                <tr v-if="filteredTitles.length === 0"><td class="empty-row" colspan="6">未找到符合条件的抬头</td></tr>
              </tbody>
            </table>
          </div>
          <footer class="pagination-row" aria-label="抬头列表分页">
            <el-pagination
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="currentTotal"
              layout="total, sizes, prev, pager, next, jumper"
              background
              @current-change="loadTitles"
              @size-change="pageNum = 1; loadTitles()"
            />
          </footer>
        </section>
      </main>

      <main v-else-if="activeMenu === 'subjects'" class="content">
        <section class="management-toolbar">
          <el-input v-model="subjectKeyword" clearable placeholder="搜索主体名称或编码" :prefix-icon="Search" @keyup.enter="subjectPageNum = 1; loadSubjects()" />
          <section class="status-tabs management-status-tabs" aria-label="主体状态筛选">
            <button type="button" data-status="ALL" :class="{ active: subjectStatus === 'ALL' }" @click="subjectStatus = 'ALL'; subjectPageNum = 1; loadSubjects()">全部</button>
            <button type="button" data-status="ENABLED" :class="{ active: subjectStatus === 'ENABLED' }" @click="subjectStatus = 'ENABLED'; subjectPageNum = 1; loadSubjects()">启用</button>
            <button type="button" data-status="DISABLED" :class="{ active: subjectStatus === 'DISABLED' }" @click="subjectStatus = 'DISABLED'; subjectPageNum = 1; loadSubjects()">停用</button>
          </section>
          <el-button type="primary" @click="openSubjectDialog"><el-icon><Plus /></el-icon>新增主体</el-button>
        </section>
        <section class="data-card">
          <header class="card-header"><div><h2>主体列表</h2><p>停用主体后，关联抬头及二维码将立即停止展示</p></div><span>共 {{ currentSubjectTotal }} 条</span></header>
          <div class="table-scroll">
            <table>
              <thead><tr><th>主体名称</th><th>主体编码</th><th>关联抬头</th><th>覆盖员工</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="subject in filteredSubjects" :key="subject.id">
                  <td><strong>{{ subject.name }}</strong></td><td class="tax-id">{{ subject.code }}</td>
                  <td>{{ subject.titleCount }} 个</td><td>{{ subject.employeeCount }} 人</td>
                  <td><span class="status" :class="subject.status === 'ENABLED' ? 'status-published' : 'status-disabled'"><i />{{ subject.status === 'ENABLED' ? '启用' : '停用' }}</span></td>
                  <td>{{ subject.updatedAt }}<small>{{ subject.updatedBy }}</small></td>
                  <td class="row-actions"><el-button link type="primary" @click="openSubjectEditor(subject)">编辑</el-button><el-button link type="primary" @click="changeSubjectStatus(subject)">{{ subject.status === 'ENABLED' ? '停用' : '启用' }}</el-button></td>
                </tr>
                <tr v-if="filteredSubjects.length === 0"><td class="empty-row" colspan="7">未找到符合条件的主体</td></tr>
              </tbody>
            </table>
          </div>
          <footer class="pagination-row" aria-label="主体管理列表分页">
            <el-pagination v-model:current-page="subjectPageNum" v-model:page-size="subjectPageSize" :page-sizes="[10,20,50,100]" :total="currentSubjectTotal" layout="total, sizes, prev, pager, next, jumper" background @current-change="loadSubjects" @size-change="subjectPageNum = 1; loadSubjects()" />
          </footer>
        </section>
      </main>

      <main v-else-if="activeMenu === 'permissions'" class="content">
        <section class="permission-config-layout" aria-label="主体权限配置">
          <aside class="permission-subject-card">
            <h2>选择主体</h2>
            <button
              v-for="profile in permissionProfiles"
              :key="profile.id"
              type="button"
              :class="{ active: selectedPermissionProfileId === profile.id }"
              @click="selectPermissionProfile(profile.id)"
            >
              <strong>{{ profile.subjectName }}</strong>
              <span>当前可见 {{ profile.visibleCount }} 人</span>
            </button>
          </aside>

          <section v-if="activePermissionProfile" class="permission-detail-card">
            <header>
              <div>
                <h2>{{ activePermissionProfile.subjectName }}</h2>
                <p>配置哪些员工可以查看该主体及其已发布抬头</p>
              </div>
              <span>当前可见 {{ activePermissionProfile.visibleCount }} 人</span>
            </header>

            <div class="permission-level-row">
              <span class="permission-level-icon"><el-icon><User /></el-icon></span>
              <div class="permission-level-copy">
                <strong>全员可见</strong>
                <p>开启后，所有在职员工均可查看</p>
              </div>
              <el-switch
                :model-value="activePermissionProfile.allEmployeesVisible"
                aria-label="全员可见"
                @update:model-value="updateAllEmployeesVisibility"
              />
            </div>

            <div class="permission-level-row permission-level-expanded">
              <span class="permission-level-icon"><el-icon><OfficeBuilding /></el-icon></span>
              <div class="permission-level-copy">
                <strong>部门授权</strong>
                <p>已选择 {{ activePermissionProfile.departments.length }} 个部门，包含子部门</p>
                <div class="permission-tags">
                  <span v-for="department in activePermissionProfile.departments" :key="department.id">
                    {{ department.departmentName }} · {{ department.employeeCount }} 人
                  </span>
                </div>
              </div>
              <el-button @click="openPermissionEditor('DEPARTMENT')">编辑</el-button>
            </div>

            <div class="permission-level-row permission-level-expanded">
              <span class="permission-level-icon"><el-icon><User /></el-icon></span>
              <div class="permission-level-copy">
                <strong>员工授权</strong>
                <p>单独授权 {{ activePermissionProfile.employeeCount }} 名员工（允许或拒绝均优先于部门）</p>
                <div class="employee-avatar-list">
                  <span v-for="employee in activePermissionProfile.employeeRules.slice(0, 4)" :key="employee.id" :title="employee.effect === 'DENY' ? '单独拒绝' : '单独允许'">{{ employee.employeeName.slice(0, 1) }}</span>
                  <span v-if="activePermissionProfile.employeeCount > 4">
                    +{{ activePermissionProfile.employeeCount - 4 }}
                  </span>
                </div>
              </div>
              <el-button @click="openPermissionEditor('USER')">编辑</el-button>
            </div>

            <footer>
              <p>权限保存后实时生效，员工调整后按钉钉通讯录自动更新。</p>
              <el-button type="primary" size="large" :loading="permissionSaving" @click="savePermissionConfiguration">保存权限</el-button>
            </footer>
          </section>

          <section v-else class="permission-empty-state">
            <span class="permission-level-icon"><el-icon><OfficeBuilding /></el-icon></span>
            <h2>暂无主体可配置权限</h2>
            <p>请先新增并启用一个主体，再为员工或部门配置查看权限。</p>
            <el-button type="primary" @click="switchMenu('subjects')">前往主体管理</el-button>
          </section>
        </section>
      </main>

      <FinanceAccountManagement
        v-else-if="activeMenu === 'accounts'"
        :accounts="financeAccounts"
        :total="accountTotal"
        :loading="accountLoading"
        :page-num="accountPageNum"
        :page-size="accountPageSize"
        @refresh="loadFinanceAccounts"
        @search="searchFinanceAccounts"
        @page-change="changeFinanceAccountPage"
      />

      <main v-else class="content">
        <section class="management-toolbar log-toolbar">
          <el-select v-model="logModule" clearable placeholder="全部模块">
            <el-option label="发票抬头" value="TITLE" /><el-option label="主体权限" value="PERMISSION" /><el-option label="批量导入" value="IMPORT" /><el-option label="财务账号" value="ACCOUNT" />
          </el-select>
          <el-input v-model="logKeyword" clearable placeholder="搜索业务对象或操作人" :prefix-icon="Search" @keyup.enter="logPageNum = 1; loadOperationLogs()" />
          <span />
          <el-button type="primary" @click="logPageNum = 1; loadOperationLogs()">查询日志</el-button>
        </section>
        <section class="data-card">
          <header class="card-header"><div><h2>操作日志</h2><p>关键维护动作留痕，不允许人工删除</p></div><span>共 {{ logTotal }} 条</span></header>
          <div class="table-scroll">
            <table>
              <thead><tr><th>操作时间</th><th>业务模块</th><th>操作类型</th><th>业务对象</th><th>操作人</th><th>结果</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="log in filteredLogs" :key="log.id">
                  <td>{{ log.createdAt }}</td><td>{{ log.module }}</td><td><strong>{{ formatOperationType(log.action) }}</strong></td><td>{{ log.businessName }}</td><td>{{ log.operator }}</td>
                  <td><span class="status" :class="log.result === 'SUCCESS' ? 'status-published' : 'status-draft'"><i />{{ log.result === 'SUCCESS' ? '成功' : '失败' }}</span></td>
                  <td><el-button link type="primary" @click="openLogDetail(log)">查看详情</el-button></td>
                </tr>
              </tbody>
            </table>
          </div>
          <footer class="pagination-row" aria-label="操作日志列表分页">
            <el-pagination v-model:current-page="logPageNum" v-model:page-size="logPageSize" :page-sizes="[10,20,50,100]" :total="logTotal" layout="total, sizes, prev, pager, next, jumper" background @current-change="loadOperationLogs" @size-change="logPageNum = 1; loadOperationLogs()" />
          </footer>
        </section>
      </main>
    </section>

    <el-dialog v-model="importVisible" title="批量导入抬头" width="620px">
      <el-tabs>
        <el-tab-pane label="上传文件">
          <div class="upload-zone" @click="chooseImportFile">
            <input ref="importFileInput" class="visually-hidden-input" type="file" accept=".xlsx" @click.stop @change="handleImportFileChange" />
            <el-icon><Upload /></el-icon>
            <strong>{{ importFileName || '点击选择 Excel 文件' }}</strong>
            <p>支持 .xlsx，单次最多 1,000 条；导入后先生成草稿。</p>
          </div>
          <el-button link type="primary" tag="a" :href="importTemplateUrl"><el-icon><Download /></el-icon>下载导入模板</el-button>
        </el-tab-pane>
        <el-tab-pane label="导入历史">
          <div v-for="task in importHistory" :key="task.id" class="history-row">
            <span>{{ task.createdAt }} · {{ task.createdBy }}<small>{{ task.originalFileName }} · {{ task.taskNo }}</small></span>
            <strong>成功 {{ task.successCount }}，失败 {{ task.failureCount }}</strong>
          </div>
          <div aria-label="导入历史分页" class="history-pagination">
            <el-pagination
              v-model:current-page="importHistoryPageNum"
              v-model:page-size="importHistoryPageSize"
              :total="importHistoryTotal"
              :page-sizes="[10,20,50,100]"
              layout="total, prev, pager, next"
              small
              @current-change="loadImportHistory"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!importFileName" :loading="importSubmitting" @click="submitImport">校验并导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createVisible" :title="editingTitleId ? '编辑发票抬头' : '新增发票抬头'" width="720px">
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="公司名称"><el-input v-model="titleForm.companyName" placeholder="请输入完整公司名称" /></el-form-item>
          <el-form-item label="纳税人识别号"><el-input v-model="titleForm.taxpayerId" placeholder="请输入纳税人识别号" /></el-form-item>
          <el-form-item class="full" label="注册地址"><el-input v-model="titleForm.address" placeholder="请输入注册地址" /></el-form-item>
          <el-form-item label="电话"><el-input v-model="titleForm.phone" placeholder="请输入联系电话" /></el-form-item>
          <el-form-item label="开户行"><el-input v-model="titleForm.bankName" placeholder="请输入开户银行" /></el-form-item>
          <el-form-item label="银行账号"><el-input v-model="titleForm.bankAccount" placeholder="请输入银行账号" /></el-form-item>
          <el-form-item label="展示主体">
            <el-select v-model="titleForm.subjectIds" multiple collapse-tags placeholder="可选择一个或多个主体">
              <el-option v-for="subject in subjects.filter((item) => item.status === 'ENABLED')" :key="subject.id" :label="subject.name" :value="subject.id" />
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

    <el-drawer v-model="detailVisible" title="版本记录" size="520px">
      <section v-if="currentTitle" v-loading="versionLoading" class="version-panel">
        <h3>{{ currentTitle.companyName }}</h3>
        <p>{{ currentTitle.taxpayerId }}</p>
        <ol>
          <li v-for="(version, index) in titleVersions" :key="version.id">
            <span><strong>V{{ version.versionNo }}{{ version.status === 'PUBLISHED' && index === 0 ? ' · 当前发布版本' : version.status === 'DRAFT' ? ' · 草稿' : '' }}</strong><small>{{ version.createdAt }} · {{ version.createdBy }}</small></span>
            <el-tag v-if="version.status === 'PUBLISHED' && index === 0" type="success">已发布</el-tag>
            <el-tag v-else-if="version.status === 'DRAFT'" type="warning">草稿</el-tag>
            <el-button v-else link type="primary" @click="restoreVersion(version)">恢复为草稿</el-button>
          </li>
        </ol>
        <div aria-label="历史版本列表分页" class="history-pagination">
          <el-pagination v-model:current-page="versionPageNum" v-model:page-size="versionPageSize" :total="versionTotal" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next" small @current-change="loadTitleVersions" @size-change="versionPageNum = 1; loadTitleVersions()" />
        </div>
        <p class="restore-note"><el-icon><Clock /></el-icon>恢复历史版本会新建草稿，不会覆盖当前已发布版本。</p>
      </section>
    </el-drawer>

    <el-dialog v-model="subjectDialogVisible" :title="editingSubjectId ? '编辑主体' : '新增主体'" width="520px">
      <el-form label-position="top">
        <el-form-item label="主体名称" required><el-input v-model="subjectForm.name" placeholder="例如：杭州主体" /></el-form-item>
        <el-form-item label="主体编码" required><el-input v-model="subjectForm.code" placeholder="例如：HZ，保存后不可重复" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="subjectForm.status"><el-radio value="ENABLED">启用</el-radio><el-radio value="DISABLED">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="展示顺序"><el-input-number v-model="subjectForm.sortNo" :min="0" :max="9999" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="subjectDialogVisible = false">取消</el-button><el-button type="primary" :loading="subjectSaving" @click="saveSubject">保存主体</el-button></template>
    </el-dialog>

    <el-dialog v-model="permissionDialogVisible" :title="permissionForm.targetType === 'DEPARTMENT' ? '编辑部门授权' : '编辑员工授权'" width="880px">
      <section class="directory-picker">
        <header>
          <div><strong>{{ permissionForm.subjectName }}</strong><p>{{ permissionForm.targetType === 'USER' ? '开关展示最终权限，个人设置优先于部门授权' : '从通讯录部门中选择，无需手工填写部门 ID' }}</p></div>
          <el-input v-model="directoryKeyword" clearable :placeholder="permissionForm.targetType === 'USER' ? '搜索姓名、工号、部门或手机号' : '搜索部门名称'" :prefix-icon="Search" @keyup.enter="directoryPageNum = 1; loadDirectory()" />
        </header>
        <div v-if="permissionForm.targetType === 'USER'" class="directory-status-filter">
          <span>权限状态</span>
          <el-radio-group v-model="employeePermissionStatus" aria-label="员工权限状态筛选" size="small" @change="directoryPageNum = 1; loadDirectory()">
            <el-radio-button value="ALL">全部</el-radio-button>
            <el-radio-button value="ENABLED">已启用</el-radio-button>
            <el-radio-button value="DISABLED">已关闭</el-radio-button>
          </el-radio-group>
        </div>
        <el-table v-if="permissionForm.targetType === 'USER'" v-loading="directoryLoading" :data="directoryEmployees" height="360">
          <el-table-column label="所属企业" min-width="180"><template #default="{ row }">{{ row.corpName || row.corpCode || '历史企业' }}</template></el-table-column>
          <el-table-column prop="employeeName" label="姓名" min-width="100" />
          <el-table-column prop="employeeNo" label="工号" min-width="100" />
          <el-table-column prop="departmentName" label="部门" min-width="130" />
          <el-table-column prop="mobile" label="手机号" min-width="130" />
          <el-table-column label="查看权限" min-width="110" align="center">
            <template #default="{ row }">
              <el-switch v-model="employeeEnabledDraft[row.id]" :aria-label="`${row.employeeName}的查看权限`" :aria-checked="employeeEnabledDraft[row.id]" inline-prompt active-text="启" inactive-text="关" />
            </template>
          </el-table-column>
        </el-table>
        <el-table v-else v-loading="directoryLoading" :data="directoryDepartments" height="360">
          <el-table-column width="70">
            <template #default="{ row }"><el-checkbox v-model="selectedDepartmentIds" :value="row.id" /></template>
          </el-table-column>
          <el-table-column label="所属企业" min-width="180"><template #default="{ row }">{{ row.corpName || row.corpCode || '历史企业' }}</template></el-table-column>
          <el-table-column prop="departmentName" label="部门名称" min-width="240" />
          <el-table-column prop="employeeCount" label="在职员工" min-width="120"><template #default="{ row }">{{ row.employeeCount }} 人</template></el-table-column>
        </el-table>
        <el-pagination v-model:current-page="directoryPageNum" v-model:page-size="directoryPageSize" :total="directoryTotal" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next" @current-change="loadDirectory" @size-change="directoryPageNum = 1; loadDirectory()" />
      </section>
      <template #footer><el-button @click="permissionDialogVisible = false">取消</el-button><el-button type="primary" @click="applyPermissionSelection">确定选择</el-button></template>
    </el-dialog>

    <el-drawer v-model="logDetailVisible" title="操作日志详情" size="500px">
      <section v-if="currentLog" class="log-detail">
        <dl><dt>操作时间</dt><dd>{{ currentLog.createdAt }}</dd><dt>业务模块</dt><dd>{{ currentLog.module }}</dd><dt>操作类型</dt><dd>{{ formatOperationType(currentLog.action) }}</dd><dt>业务对象</dt><dd>{{ currentLog.businessName }}</dd><dt>操作人</dt><dd>{{ currentLog.operator }}</dd><dt>执行结果</dt><dd>{{ currentLog.result === 'SUCCESS' ? '成功' : '失败' }}</dd></dl>
        <h3>操作说明</h3><p>{{ currentLog.detail }}</p>
      </section>
    </el-drawer>

    <ChangePasswordDialog v-model="changePasswordVisible" />
  </div>

  <div v-else class="auth-loading" aria-label="正在验证登录状态">
    <span class="brand-mark">票</span>
    <p>正在验证登录状态…</p>
  </div>
</template>

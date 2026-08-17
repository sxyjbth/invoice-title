<script setup lang="ts">
import { computed, provide, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { storeToRefs } from "pinia";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { type FinanceAccount } from "../components/FinanceAccountManagement.vue";
import ChangePasswordDialog from "../components/ChangePasswordDialog.vue";
import { buildPermissionSubjectQuery, loadPermissionProfiles } from "../utils/subject-query";
import { resolveApiUrl } from "../api-prefix";
import { routeNameByMenu, routeNames, type FinanceMenuCode } from "../router";
import { financeLayoutKey } from "./finance-layout-context";
import { useFinanceAuthStore } from "../stores/finance-auth";
import {
  Document,
  Download,
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
  { id: 1, code: "HZ", name: "杭州主体", status: "ENABLED", employeeCount: 186, boundTitleId: 1, boundTitleName: "杭州赛宝卓越技术有限公司", updatedAt: "2026-08-07 15:46", updatedBy: "王财务", sortNo: 10 },
  { id: 2, code: "BJ", name: "北京主体", status: "ENABLED", employeeCount: 92, boundTitleId: 2, boundTitleName: "北京示例技术服务有限公司", updatedAt: "2026-08-06 10:22", updatedBy: "李会计", sortNo: 20 },
  { id: 3, code: "SH", name: "上海主体", status: "ENABLED", employeeCount: 68, boundTitleId: 3, boundTitleName: "上海赛宝技术服务有限公司", updatedAt: "2026-08-03 14:10", updatedBy: "王财务", sortNo: 30 },
  { id: 4, code: "EAST", name: "华东主体", status: "ENABLED", employeeCount: 40, updatedAt: "2026-07-29 09:35", updatedBy: "王财务", sortNo: 40 },
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

const importHistory = ref<ImportHistory[]>([
  { id: 1, taskNo: "IMP2026080509280012AB", originalFileName: "invoice-title-20260805.xlsx", status: "PARTIAL_FAILED", totalCount: 13, successCount: 12, failureCount: 1, createdBy: "王财务", createdAt: "2026-08-05 09:28" },
  { id: 2, taskNo: "IMP2026071916420034CD", originalFileName: "invoice-title-20260719.xlsx", status: "COMPLETED", totalCount: 8, successCount: 8, failureCount: 0, createdBy: "李会计", createdAt: "2026-07-19 16:42" },
]);

const testMode = import.meta.env.MODE === "test";
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
const loadedDirectoryEmployees = reactive<Record<number, DingEmployee>>({});
const selectedDepartmentIds = ref<number[]>([]);
const employeeEnabledDraft = reactive<Record<number, boolean>>({});
const employeePermissionStatus = ref<"ALL" | "ENABLED" | "DISABLED">("ALL");
const selectedPermissionProfileId = ref(1);

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

const subjectForm = reactive({ name: "", status: "ENABLED" as "ENABLED" | "DISABLED", sortNo: 0 });
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

async function loadCoreData() {
  await Promise.all([loadTitles(), loadTitleCounts(), loadSubjects()]);
}

async function logout() {
  await auth.logout();
  void router.replace({ name: routeNames.login });
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
  return statusOptions.value.find((item) => item.code === status)?.label ?? status;
}

function statusClass(status: StatusCode) {
  return `status-${status.toLowerCase()}`;
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
    ElMessage.error(error instanceof Error ? error.message : "导入失败原因加载失败");
    return [];
  } finally {
    importErrorsLoading.value = false;
  }
}

function importErrorSummary(error: ImportRowError) {
  const taxpayer = error.taxpayerId ? `（纳税人识别号：${error.taxpayerId}）` : "";
  return `第 ${error.rowNo} 行：${error.errorMessage}${taxpayer}`;
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
    await Promise.all([loadImportHistory(), loadTitles(), loadTitleCounts()]);
    importFile.value = null;
    importFileName.value = "";
    if (importFileInput.value) importFileInput.value.value = "";
    if (result.failureCount > 0) {
      const errors = await loadImportErrors(result.id);
      const firstReason = errors[0] ? `第 ${errors[0].rowNo} 行，${errors[0].errorMessage}` : `共有 ${result.failureCount} 条数据校验失败`;
      ElMessage.error(`导入失败：${firstReason}`);
    } else {
      importErrorTaskId.value = null;
      importRowErrors.value = [];
      ElMessage.success(`导入完成：成功 ${result.successCount} 条；数据已生成草稿`);
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "导入失败，请检查后端服务和 Excel 内容");
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
  const subjectIds = [...titleForm.subjectIds];
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
    if (!testMode) await Promise.all([loadTitles(), loadTitleCounts()]);
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

function openTitleBinding(subject: InvoiceSubject) {
  bindingSubject.value = subject;
  bindingTitleId.value = subject.boundTitleId ?? null;
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
    const selectedTitle = titles.value.find((title) => title.id === bindingTitleId.value);
    bindingSubject.value.boundTitleId = bindingTitleId.value;
    bindingSubject.value.boundTitleName = selectedTitle?.companyName ?? null;
    titleBindingVisible.value = false;
    ElMessage.success("抬头绑定成功");
    if (!testMode) await Promise.all([loadSubjects(), loadTitles()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "绑定抬头失败");
  } finally {
    titleBindingSaving.value = false;
  }
}

function searchDirectory() {
  directoryPageNum.value = 1;
  void loadDirectory();
}

function resetDirectorySearch() {
  directoryKeyword.value = "";
  directoryPageNum.value = 1;
  void loadDirectory();
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
    employeeCount: (result.employeeRules ?? []).length,
  };
  const index = permissionProfiles.value.findIndex((item) => item.id === profile.id);
  if (index >= 0) permissionProfiles.value[index] = profile;
}

async function selectPermissionProfile(subjectId: number) {
  selectedPermissionProfileId.value = subjectId;
  try {
    await loadPermissionProfile(subjectId);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "主体权限加载失败");
  }
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
        loadedDirectoryEmployees[employee.id] = employee;
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
  Object.keys(loadedDirectoryEmployees).forEach((key) => delete loadedDirectoryEmployees[Number(key)]);
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
    Object.entries(employeeEnabledDraft).forEach(([employeeId, enabled]) => {
      const employee = loadedDirectoryEmployees[Number(employeeId)];
      if (!employee) return;
      const inheritedEnabled = inheritedEmployeeEnabled(employee);
      if (enabled === inheritedEnabled) rules.delete(employee.id);
      else rules.set(employee.id, { ...employee, effect: enabled ? "ALLOW" : "DENY" });
    });
    profile.employeeRules = [...rules.values()];
    profile.employeeCount = profile.employeeRules.length;
  }
  if (await savePermissionConfiguration()) permissionDialogVisible.value = false;
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
  resetDirectorySearch, savePermissionConfiguration, searchDirectory, searchFinanceAccounts,
  selectPermissionProfile, selectedDepartmentIds, selectedPermissionProfileId, selectStatus,
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

      <div class="finance-profile" aria-label="当前登录账号">
        <span>{{ profileDisplayName.slice(0, 1) }}</span>
        <div><strong>{{ profileDisplayName }}</strong></div>
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

      <RouterView />

    </section>

    <el-dialog v-model="importVisible" title="批量导入抬头" width="620px">
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
            v-if="importRowErrors.length"
            class="import-error-alert"
            type="error"
            :closable="false"
            show-icon
            title="导入失败"
          >
            <template #default>
              <p v-for="error in importRowErrors" :key="error.id">导入失败：{{ importErrorSummary(error) }}</p>
            </template>
          </el-alert>
        </el-tab-pane>
        <el-tab-pane label="导入历史">
          <div v-for="task in importHistory" :key="task.id" class="history-row">
            <span>{{ task.createdAt }} · {{ task.createdBy }}<small>{{ task.originalFileName }} · {{ task.taskNo }}</small></span>
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
          <el-form-item label="展示主体（可后期选择）">
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

    <el-dialog v-model="subjectDialogVisible" :title="editingSubjectId ? '编辑主体' : '新增主体'" width="520px">
      <el-form label-position="top">
        <el-form-item label="主体名称" required><el-input v-model="subjectForm.name" placeholder="例如：杭州主体" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="subjectForm.status"><el-radio value="ENABLED">启用</el-radio><el-radio value="DISABLED">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="展示顺序"><el-input-number v-model="subjectForm.sortNo" :min="0" :max="9999" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="subjectDialogVisible = false">取消</el-button><el-button type="primary" :loading="subjectSaving" @click="saveSubject">保存主体</el-button></template>
    </el-dialog>

    <el-dialog v-model="permissionDialogVisible" :title="permissionForm.targetType === 'DEPARTMENT' ? '编辑部门授权' : '编辑员工授权'" width="880px">
      <section class="directory-picker">
        <header>
          <div><strong>{{ permissionForm.subjectName }}</strong><p>{{ permissionForm.targetType === 'USER' ? '开关展示最终权限，个人设置优先于部门授权' : '从通讯录部门中选择，无需手工填写部门 ID' }}</p></div>
          <div class="directory-search-actions">
            <el-input v-model="directoryKeyword" clearable :placeholder="permissionForm.targetType === 'USER' ? '搜索姓名、工号、部门或手机号' : '搜索部门名称'" :prefix-icon="Search" @keyup.enter="searchDirectory" />
            <el-button type="primary" :icon="Search" @click="searchDirectory">搜索</el-button>
            <el-button @click="resetDirectorySearch">重置</el-button>
          </div>
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

    <el-dialog v-model="titleBindingVisible" :title="bindingSubject ? `为${bindingSubject.name}绑定抬头` : '绑定抬头'" width="560px">
      <el-alert title="一个主体只能绑定一个抬头，再次绑定将替换原关系。" type="info" :closable="false" />
      <el-form label-position="top" class="binding-form">
        <el-form-item label="发票抬头" required>
          <el-select v-model="bindingTitleId" filterable placeholder="搜索并选择抬头公司名称" style="width: 100%">
            <el-option v-for="title in titles.filter((item) => item.status !== 'DISABLED')" :key="title.id" :label="`${title.companyName}（${title.status === 'PUBLISHED' ? '已发布' : '草稿'}）`" :value="title.id" />
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

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { ArrowLeft, CopyDocument, Grid, Refresh, Close } from "@element-plus/icons-vue";
import QRCode from "qrcode";
import { requestAuthCode$ as requestAuthCodeApi } from "dingtalk-jsapi/api/runtime/permission/requestAuthCode";
import closeNavigationApi from "dingtalk-jsapi/api/biz/navigation/close";

type InvoiceTitle = {
  id: number;
  companyName: string;
  taxpayerId: string;
  registeredAddress: string;
  phone: string;
  bankName: string;
  bankAccount: string;
  subjectNames: string[];
};

type PageResult<T> = { records: T[]; total: number; pageNum: number; pageSize: number };
type QrToken = { token: string; accessPath: string; expiresAt: string };
type InvoiceField = { label: string; value: string };
type DingTalkOrganization = { corpCode: string; corpName: string; corpId: string };

const titles = ref<InvoiceTitle[]>([]);
const selectedSubject = ref("");
const loading = ref(true);
const errorMessage = ref("");
const qrVisible = ref(false);
const seconds = ref(600);
const toast = ref("");
const qrImageUrl = ref("");
let countdownTimer: number | undefined;
let toastTimer: number | undefined;

const subjects = computed(() => Array.from(new Set(titles.value.flatMap((title) => title.subjectNames || []))));
const currentTitle = computed(() => titles.value.find((title) => title.subjectNames?.includes(selectedSubject.value)) || titles.value[0]);
const companyName = computed(() => currentTitle.value?.companyName || "");
const taxpayerId = computed(() => currentTitle.value?.taxpayerId || "");
const fields = computed<InvoiceField[]>(() => currentTitle.value ? [
  { label: "纳税人识别号", value: currentTitle.value.taxpayerId },
  { label: "地址", value: currentTitle.value.registeredAddress },
  { label: "电话", value: currentTitle.value.phone },
  { label: "开户行", value: currentTitle.value.bankName },
  { label: "银行账号", value: currentTitle.value.bankAccount },
] : []);

const formattedTime = computed(() => {
  const minutes = Math.floor(seconds.value / 60).toString().padStart(2, "0");
  const remainder = (seconds.value % 60).toString().padStart(2, "0");
  return `${minutes}:${remainder}`;
});

async function api<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(init?.headers || {}) },
    ...init,
  });
  if (!response.ok) {
    const payload = await response.json().catch(() => ({ message: "服务暂时不可用" }));
    throw new Error(payload.message || `请求失败（${response.status}）`);
  }
  return response.json() as Promise<T>;
}

async function resolveDingTalkOrganization(): Promise<DingTalkOrganization> {
  const organizations = await api<DingTalkOrganization[]>("/api/employee/auth/organizations");
  const params = new URLSearchParams(window.location.search);
  const requestedCorpCode = params.get("corpCode") || import.meta.env.VITE_DINGTALK_CORP_CODE;
  if (requestedCorpCode) {
    const matched = organizations.find((item) => item.corpCode === requestedCorpCode);
    if (!matched) throw new Error(`未接入的钉钉企业：${requestedCorpCode}`);
    return matched;
  }
  if (organizations.length === 1) return organizations[0];
  throw new Error("工作台入口缺少企业参数 corpCode，请联系管理员检查应用地址");
}

function requestDingTalkAuthCode(corpId: string): Promise<string> {
  const queryCode = new URLSearchParams(window.location.search).get("authCode");
  const dd = (window as any).dd;
  const injectedRequestAuthCode = dd?.runtime?.permission?.requestAuthCode;
  const requestAuthCode = typeof injectedRequestAuthCode === "function"
    ? injectedRequestAuthCode
    : requestAuthCodeApi;
  if (typeof requestAuthCode !== "function") {
    if (queryCode) return Promise.resolve(queryCode);
    return Promise.reject(new Error("请从钉钉工作台打开“发票抬头”应用"));
  }

  return new Promise((resolve, reject) => {
    const options = {
      corpId,
      success: (result: { code?: string }) => result?.code ? resolve(result.code) : reject(new Error("钉钉未返回免登码")),
      fail: (error: { errorMessage?: string }) => reject(new Error(error?.errorMessage || "获取钉钉免登码失败")),
    };
    const returned = requestAuthCode(options);
    if (returned?.then) {
      returned.then(options.success, options.fail);
    }
  });
}

async function initialize() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const qrToken = new URLSearchParams(window.location.search).get("qrToken");
    if (qrToken) {
      const snapshot = await api<Omit<InvoiceTitle, "subjectNames">>(`/api/employee/invoice-titles/qr/${encodeURIComponent(qrToken)}`);
      titles.value = [{ ...snapshot, subjectNames: ["二维码抬头"] }];
    } else {
      const organization = await resolveDingTalkOrganization();
      const authCode = await requestDingTalkAuthCode(organization.corpId);
      await api("/api/employee/auth/dingtalk", {
        method: "POST",
        body: JSON.stringify({ corpCode: organization.corpCode, authCode }),
      });
      const page = await api<PageResult<InvoiceTitle>>("/api/employee/invoice-titles?pageNum=1&pageSize=100");
      titles.value = page.records;
    }
    selectedSubject.value = subjects.value[0] || "";
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载发票抬头失败";
  } finally {
    loading.value = false;
  }
}

function notify(message: string) {
  toast.value = message;
  if (toastTimer) window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => (toast.value = ""), 2200);
}

async function copyText(value: string, message = "已复制") {
  try {
    await navigator.clipboard?.writeText(value);
  } catch {
    // 钉钉容器未授权剪贴板时仍保留可见反馈。
  }
  notify(message);
}

function copyAll() {
  const content = [
    `公司名称：${companyName.value}`,
    ...fields.value.map((field) => `${field.label}：${field.value}`),
  ].join("\n");
  return copyText(content, "已复制，可粘贴给开票方");
}

function startCountdown() {
  if (countdownTimer) window.clearInterval(countdownTimer);
  countdownTimer = window.setInterval(() => {
    seconds.value = Math.max(0, seconds.value - 1);
    if (seconds.value === 0 && countdownTimer) window.clearInterval(countdownTimer);
  }, 1000);
}

async function createQrTokenAndImage() {
  if (!currentTitle.value) return;
  const token = await api<QrToken>(`/api/employee/invoice-titles/${currentTitle.value.id}/qr-token`, {
    method: "POST",
    body: "{}",
  });
  const qrUrl = new URL(window.location.href);
  qrUrl.search = "";
  qrUrl.hash = "";
  qrUrl.searchParams.set("qrToken", token.token);
  const svg = await QRCode.toString(qrUrl.toString(), {
    errorCorrectionLevel: "M",
    width: 320,
    margin: 2,
    color: { dark: "#07132f", light: "#ffffff" },
  });
  qrImageUrl.value = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

async function openQr() {
  seconds.value = 600;
  qrImageUrl.value = "";
  qrVisible.value = true;
  try {
    await createQrTokenAndImage();
    startCountdown();
  } catch (error) {
    closeQr();
    notify(error instanceof Error ? error.message : "二维码生成失败");
  }
}

function closeQr() {
  qrVisible.value = false;
  if (countdownTimer) window.clearInterval(countdownTimer);
}

async function refreshQr() {
  seconds.value = 600;
  qrImageUrl.value = "";
  await createQrTokenAndImage();
  startCountdown();
  notify("二维码已刷新，有效期重新计时");
}

function returnWorkbench() {
  const close = (window as any).dd?.biz?.navigation?.close || closeNavigationApi;
  if (typeof close === "function") close();
  else window.history.back();
}

watch(selectedSubject, () => closeQr());
onMounted(initialize);
onBeforeUnmount(() => {
  if (countdownTimer) window.clearInterval(countdownTimer);
  if (toastTimer) window.clearTimeout(toastTimer);
});
</script>

<template>
  <div class="employee-app">
    <header class="employee-header">
      <div class="toolbar">
        <button type="button" class="back-button" @click="returnWorkbench">
          <el-icon><ArrowLeft /></el-icon>
          返回工作台
        </button>
        <strong>发票抬头</strong>
        <span aria-hidden="true" />
      </div>

      <div v-if="subjects.length" class="subject-selector" aria-label="选择主体">
        <el-select v-model="selectedSubject" size="large" aria-label="可查看主体">
          <el-option v-for="subject in subjects" :key="subject" :label="subject" :value="subject" />
        </el-select>
      </div>
    </header>

    <main v-if="loading" class="empty-state">
      <div class="empty-mark">票</div>
      <h1>正在验证钉钉身份</h1>
      <p>正在加载您有权查看的发票抬头…</p>
    </main>

    <main v-else-if="errorMessage" class="empty-state">
      <div class="empty-mark">!</div>
      <h1>暂时无法打开</h1>
      <p>{{ errorMessage }}</p>
      <el-button type="primary" @click="initialize">重新加载</el-button>
    </main>

    <main v-else-if="currentTitle" class="title-content">
      <section class="company-intro">
        <h1>{{ companyName }}</h1>
        <p>已由财务发布 · 当前有效</p>
      </section>

      <section class="title-fields" aria-label="发票抬头信息">
        <div v-for="field in fields" :key="field.label" class="field-row">
          <div>
            <span class="field-label">{{ field.label }}</span>
            <span class="field-value">{{ field.value }}</span>
          </div>
          <el-button link type="primary" :aria-label="`复制${field.label}`" @click="copyText(field.value, `${field.label}已复制`)">
            复制
          </el-button>
        </div>
      </section>

      <section class="employee-actions" aria-label="抬头操作">
        <el-button type="primary" size="large" @click="copyAll">
          <el-icon><CopyDocument /></el-icon>
          复制全部
        </el-button>
        <el-button data-testid="show-qr" plain type="primary" size="large" @click="openQr">
          <el-icon><Grid /></el-icon>
          展示二维码
        </el-button>
        <p>信息仅用于开具发票，请勿公开转发</p>
      </section>
    </main>

    <main v-else class="empty-state">
      <div class="empty-mark">空</div>
      <h1>暂无可用发票抬头</h1>
      <p>如需查看，请联系财务确认主体权限及抬头发布状态。</p>
    </main>

    <div v-if="qrVisible" class="sheet-backdrop" role="presentation" @click.self="closeQr">
      <section class="qr-sheet" role="dialog" aria-modal="true" aria-label="抬头二维码">
        <div class="sheet-handle" aria-hidden="true" />
        <h2>抬头二维码</h2>
        <button type="button" class="qr-close" aria-label="关闭二维码" @click="closeQr">
          <el-icon><Close /></el-icon>
        </button>
        <strong>{{ companyName }}</strong>
        <p class="qr-tax-id">{{ taxpayerId }}</p>
        <div class="qr-image-frame" :class="{ loading: !qrImageUrl }">
          <img v-if="qrImageUrl" :src="qrImageUrl" alt="临时发票抬头二维码" />
          <span v-else>正在生成二维码…</span>
        </div>
        <p class="qr-validity">二维码10分钟内有效</p>
        <strong class="qr-countdown">{{ formattedTime }}</strong>
        <el-button type="primary" size="large" class="qr-copy" @click="copyAll">
          <el-icon><CopyDocument /></el-icon>
          复制全部
        </el-button>
        <el-button link type="primary" class="refresh-action" @click="refreshQr">
          <el-icon><Refresh /></el-icon>
          刷新二维码
        </el-button>
        <p class="qr-privacy">仅用于本次开票，请勿转发</p>
      </section>
    </div>

    <div v-if="toast" class="toast" role="status">{{ toast }}</div>
  </div>
</template>

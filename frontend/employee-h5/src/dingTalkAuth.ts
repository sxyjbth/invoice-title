import dingTalkJsApi from "dingtalk-jsapi";

interface DingTalkAuthCodeResult {
  code?: string;
  authCode?: string;
}

interface DingTalkAuthError {
  errorMessage?: string;
  message?: string;
}

interface DingTalkJsApi {
  ready?: (callback: () => void) => void;
  getAuthCode?: (options: { corpId: string }) => Promise<DingTalkAuthCodeResult>;
  requestAuthCode?: (options: { corpId: string }) => Promise<DingTalkAuthCodeResult>;
  channel?: {
    permission?: {
      requestAuthCode: DingTalkAuthCodeRequester;
    };
  };
  runtime?: {
    permission?: {
      requestAuthCode: DingTalkAuthCodeRequester;
    };
  };
}

type DingTalkAuthCodeRequester = (options: {
  corpId: string;
  onSuccess?: (result: DingTalkAuthCodeResult) => void;
  onFail?: (error: DingTalkAuthError) => void;
}) => Promise<DingTalkAuthCodeResult> | void;

declare global {
  interface Window {
    dd?: DingTalkJsApi;
    DingTalkPC?: DingTalkJsApi;
    DD?: DingTalkJsApi;
  }
}

/**
 * 获取钉钉 H5 微应用免登码。
 *
 * 该适配逻辑与 sebo-meal 已运行的员工免登实现保持一致，兼容移动端、PC 端、
 * 延迟注入的全局 JSAPI，以及钉钉 npm SDK。
 */
export async function requestDingTalkAuthCode(corpId: string): Promise<string> {
  const dingTalkApi = await waitForDingTalkApi();
  if (!dingTalkApi || !isDingTalkAuthAvailable(dingTalkApi)) {
    throw new Error("钉钉免登组件未就绪，请在钉钉应用内重新打开");
  }

  await waitForDingTalkReady(dingTalkApi);
  return requestAuthCode(dingTalkApi, corpId);
}

function isDingTalkAuthAvailable(dingTalkApi: DingTalkJsApi | undefined) {
  return !!(
    dingTalkApi?.getAuthCode
    || dingTalkApi?.runtime?.permission?.requestAuthCode
    || dingTalkApi?.channel?.permission?.requestAuthCode
    || dingTalkApi?.requestAuthCode
  );
}

function requestAuthCode(dingTalkApi: DingTalkJsApi, corpId: string): Promise<string> {
  if (dingTalkApi.getAuthCode) {
    return dingTalkApi.getAuthCode({ corpId }).then(readAuthCode);
  }
  if (dingTalkApi.runtime?.permission?.requestAuthCode) {
    return requestAuthCodeByPermission(dingTalkApi.runtime.permission.requestAuthCode, corpId);
  }
  if (dingTalkApi.channel?.permission?.requestAuthCode) {
    return requestAuthCodeByPermission(dingTalkApi.channel.permission.requestAuthCode, corpId);
  }
  if (dingTalkApi.requestAuthCode) {
    return dingTalkApi.requestAuthCode({ corpId }).then(readAuthCode);
  }
  return Promise.reject(new Error("钉钉免登组件未就绪，请在钉钉应用内重新打开"));
}

function requestAuthCodeByPermission(requester: DingTalkAuthCodeRequester, corpId: string): Promise<string> {
  return new Promise((resolve, reject) => {
    try {
      const result = requester({
        corpId,
        onSuccess: (authResult) => {
          try {
            resolve(readAuthCode(authResult));
          } catch (error) {
            reject(error);
          }
        },
        onFail: (error) => reject(new Error(error.errorMessage || error.message || "钉钉免登授权失败")),
      });
      if (result && typeof result.then === "function") {
        result.then((authResult) => resolve(readAuthCode(authResult))).catch(reject);
      }
    } catch (error) {
      reject(error);
    }
  });
}

function readAuthCode(result: DingTalkAuthCodeResult) {
  const authCode = result.code || result.authCode;
  if (!authCode) throw new Error("钉钉免登授权码为空，请重新进入应用");
  return authCode;
}

function getDingTalkApi() {
  const scope = globalThis as typeof globalThis & {
    dd?: DingTalkJsApi;
    DingTalkPC?: DingTalkJsApi;
    DD?: DingTalkJsApi;
  };
  return scope.dd || scope.DingTalkPC || scope.DD || getBundledDingTalkApi();
}

function getBundledDingTalkApi() {
  return isDingTalkContainer() ? (dingTalkJsApi as unknown as DingTalkJsApi) : undefined;
}

function isDingTalkContainer() {
  return typeof navigator !== "undefined"
    && /DingTalk|dingtalk|AliApp\(DingTalk/i.test(navigator.userAgent);
}

function waitForDingTalkReady(dingTalkApi: DingTalkJsApi): Promise<void> {
  if (!dingTalkApi.ready) return Promise.resolve();
  return new Promise((resolve) => dingTalkApi.ready?.(resolve));
}

function waitForDingTalkApi(timeoutMs = 3000): Promise<DingTalkJsApi | undefined> {
  const existingApi = getDingTalkApi();
  if (existingApi) return Promise.resolve(existingApi);

  const startedAt = Date.now();
  return new Promise((resolve) => {
    const timer = globalThis.setInterval(() => {
      const api = getDingTalkApi();
      if (api || Date.now() - startedAt >= timeoutMs) {
        globalThis.clearInterval(timer);
        resolve(api);
      }
    }, 100);
  });
}

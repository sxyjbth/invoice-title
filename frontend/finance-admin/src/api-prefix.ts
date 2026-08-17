function normalizePrefix(prefix: string) {
  const value = prefix.trim();
  if (!value || value === "/") return "";
  return `/${value.replace(/^\/+|\/+$/g, "")}`;
}

export const FINANCE_SESSION_HEADER = "X-Invoice-Finance-Session";
export const FINANCE_SESSION_STORAGE_KEY = "invoice-title.finance-session";

type TabStorage = Pick<Storage, "getItem" | "setItem" | "removeItem">;

export function resolveApiUrl(path: string, prefix = import.meta.env.VITE_API_BASE_PREFIX || "") {
  if (path !== "/api" && !path.startsWith("/api/")) return path;
  return `${normalizePrefix(prefix)}${path}`;
}

export function createApiFetch(
  nativeFetch: typeof fetch,
  prefix = import.meta.env.VITE_API_BASE_PREFIX || "",
  storage: TabStorage | undefined = typeof window === "undefined" ? undefined : window.sessionStorage,
) {
  return (async (input: RequestInfo | URL, init?: RequestInit) => {
    const originalPath = typeof input === "string" ? input : "";
    const resolved = typeof input === "string" ? resolveApiUrl(input, prefix) : input;
    if (originalPath !== "/api" && !originalPath.startsWith("/api/")) {
      return nativeFetch(resolved, init);
    }

    const headers = new Headers(init?.headers);
    // 请求头即使为空也必须发送，后端据此明确忽略浏览器共享的旧 Cookie 会话。
    headers.set(FINANCE_SESSION_HEADER, storage?.getItem(FINANCE_SESSION_STORAGE_KEY) ?? "");
    const response = await nativeFetch(resolved, { ...init, headers });
    const issuedToken = response.headers.get(FINANCE_SESSION_HEADER);
    if (issuedToken) {
      storage?.setItem(FINANCE_SESSION_STORAGE_KEY, issuedToken);
    }
    if (response.ok && originalPath === "/api/auth/logout") {
      storage?.removeItem(FINANCE_SESSION_STORAGE_KEY);
    }
    return response;
  }) as typeof fetch;
}

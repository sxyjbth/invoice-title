function normalizePrefix(prefix: string) {
  const value = prefix.trim();
  if (!value || value === "/") return "";
  return `/${value.replace(/^\/+|\/+$/g, "")}`;
}

export function resolveApiUrl(path: string, prefix = import.meta.env.VITE_API_BASE_PREFIX || "") {
  if (path !== "/api" && !path.startsWith("/api/")) return path;
  return `${normalizePrefix(prefix)}${path}`;
}

export function createApiFetch(nativeFetch: typeof fetch, prefix = import.meta.env.VITE_API_BASE_PREFIX || "") {
  return ((input: RequestInfo | URL, init?: RequestInit) => {
    const resolved = typeof input === "string" ? resolveApiUrl(input, prefix) : input;
    return nativeFetch(resolved, init);
  }) as typeof fetch;
}


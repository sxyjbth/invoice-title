import { describe, expect, it, vi } from "vitest";
import { createApiFetch, resolveApiUrl } from "../src/api-prefix";

function memoryStorage(): Storage {
  const values = new Map<string, string>();
  return {
    get length() { return values.size; },
    clear: () => values.clear(),
    getItem: (key) => values.get(key) ?? null,
    key: (index) => Array.from(values.keys())[index] ?? null,
    removeItem: (key) => values.delete(key),
    setItem: (key, value) => values.set(key, value),
  };
}

describe("finance api prefix", () => {
  it("prefixes api fetches and download links consistently", async () => {
    const nativeFetch = vi.fn().mockResolvedValue(new Response("{}"));
    const wrapped = createApiFetch(nativeFetch as typeof fetch, "/invoice/");

    await wrapped("/api/auth/me", { credentials: "include" });

    expect(nativeFetch.mock.calls[0][0]).toBe("/invoice/api/auth/me");
    const request = nativeFetch.mock.calls[0][1] as RequestInit;
    expect(request.credentials).toBe("include");
    expect(new Headers(request.headers).get("X-Invoice-Finance-Session")).toBe("");
    expect(resolveApiUrl("/api/admin/invoice-imports/template", "/invoice/"))
      .toBe("/invoice/api/admin/invoice-imports/template");
  });

  it("stores the login token in the supplied tab storage and sends it on later requests", async () => {
    const storage = memoryStorage();
    const nativeFetch = vi.fn()
      .mockResolvedValueOnce(new Response("{}", {
        status: 200,
        headers: { "X-Invoice-Finance-Session": "tab-one-token" },
      }))
      .mockResolvedValueOnce(new Response("{}", { status: 200 }));
    const wrapped = createApiFetch(nativeFetch as typeof fetch, "/invoice/", storage);

    await wrapped("/api/auth/login", { method: "POST" });
    await wrapped("/api/auth/me");

    expect(storage.getItem("invoice-title.finance-session")).toBe("tab-one-token");
    const secondRequest = nativeFetch.mock.calls[1][1] as RequestInit;
    expect(new Headers(secondRequest.headers).get("X-Invoice-Finance-Session")).toBe("tab-one-token");
  });

  it("sends an empty tab-session header before login so another tab cookie is ignored", async () => {
    const storage = memoryStorage();
    const nativeFetch = vi.fn().mockResolvedValue(new Response("", { status: 401 }));
    const wrapped = createApiFetch(nativeFetch as typeof fetch, "", storage);

    await wrapped("/api/auth/me");

    const request = nativeFetch.mock.calls[0][1] as RequestInit;
    expect(new Headers(request.headers).get("X-Invoice-Finance-Session")).toBe("");
  });

  it("clears only the supplied tab storage after logout", async () => {
    const firstTab = memoryStorage();
    const secondTab = memoryStorage();
    firstTab.setItem("invoice-title.finance-session", "first-token");
    secondTab.setItem("invoice-title.finance-session", "second-token");
    const nativeFetch = vi.fn().mockResolvedValue(new Response("", { status: 200 }));
    const firstFetch = createApiFetch(nativeFetch as typeof fetch, "", firstTab);

    await firstFetch("/api/auth/logout", { method: "POST" });

    expect(firstTab.getItem("invoice-title.finance-session")).toBeNull();
    expect(secondTab.getItem("invoice-title.finance-session")).toBe("second-token");
  });
});

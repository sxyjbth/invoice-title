import { describe, expect, it, vi } from "vitest";
import { createApiFetch, resolveApiUrl } from "../src/api-prefix";

describe("employee api prefix", () => {
  it("prefixes only same-origin api paths", async () => {
    const nativeFetch = vi.fn().mockResolvedValue(new Response("{}"));
    const wrapped = createApiFetch(nativeFetch as typeof fetch, "/invoice");

    await wrapped("/api/employee/invoice-titles");
    await wrapped("https://example.com/api/external");

    expect(nativeFetch).toHaveBeenNthCalledWith(1, "/invoice/api/employee/invoice-titles", undefined);
    expect(nativeFetch).toHaveBeenNthCalledWith(2, "https://example.com/api/external", undefined);
    expect(resolveApiUrl("/api/test", "")).toBe("/api/test");
  });
});


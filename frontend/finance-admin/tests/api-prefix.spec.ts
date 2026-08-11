import { describe, expect, it, vi } from "vitest";
import { createApiFetch, resolveApiUrl } from "../src/api-prefix";

describe("finance api prefix", () => {
  it("prefixes api fetches and download links consistently", async () => {
    const nativeFetch = vi.fn().mockResolvedValue(new Response("{}"));
    const wrapped = createApiFetch(nativeFetch as typeof fetch, "/invoice/");

    await wrapped("/api/auth/me", { credentials: "include" });

    expect(nativeFetch).toHaveBeenCalledWith("/invoice/api/auth/me", { credentials: "include" });
    expect(resolveApiUrl("/api/admin/invoice-imports/template", "/invoice/"))
      .toBe("/invoice/api/admin/invoice-imports/template");
  });
});


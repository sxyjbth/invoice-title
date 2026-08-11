import { describe, expect, it } from "vitest";
import { formatDateTime } from "../src/utils/date";

describe("日期时间展示", () => {
  it("将后端 ISO 本地时间格式化为不带 T 和毫秒的中文页面时间", () => {
    expect(formatDateTime("2026-08-11T09:20:49.515")).toBe("2026-08-11 09:20:49");
    expect(formatDateTime("2026-08-11 09:20:49")).toBe("2026-08-11 09:20:49");
    expect(formatDateTime(undefined)).toBe("-");
  });
});

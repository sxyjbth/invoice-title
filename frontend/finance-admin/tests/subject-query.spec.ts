import { describe, expect, it } from "vitest";
import { buildPermissionSubjectQuery } from "../src/utils/subject-query";

describe("主体权限页主体查询", () => {
  it("独立加载全部主体且不继承主体管理页的状态筛选", () => {
    const query = buildPermissionSubjectQuery(2, 100);

    expect(query.get("pageNum")).toBe("2");
    expect(query.get("pageSize")).toBe("100");
    expect(query.has("status")).toBe(false);
    expect(query.has("keyword")).toBe(false);
  });
});

import { describe, expect, it } from "vitest";
import * as subjectQuery from "../src/utils/subject-query";

const { buildPermissionSubjectQuery } = subjectQuery;

describe("主体权限页主体查询", () => {
  it("独立加载全部主体且不继承主体管理页的状态筛选", () => {
    const query = buildPermissionSubjectQuery(2, 100);

    expect(query.get("pageNum")).toBe("2");
    expect(query.get("pageSize")).toBe("100");
    expect(query.has("status")).toBe(false);
    expect(query.has("keyword")).toBe(false);
  });

  it("批量加载部分失败时等待所有主体完成并只返回一次聚合失败结果", async () => {
    const loadPermissionProfiles = (subjectQuery as Record<string, unknown>).loadPermissionProfiles;
    expect(loadPermissionProfiles).toBeTypeOf("function");
    if (typeof loadPermissionProfiles !== "function") return;

    const loadedSubjectIds: number[] = [];
    const loaded = await (loadPermissionProfiles as (
      subjectIds: number[],
      loader: (subjectId: number) => Promise<void>,
    ) => Promise<boolean>)([1, 2, 3], async (subjectId) => {
      loadedSubjectIds.push(subjectId);
      if (subjectId !== 2) throw new Error("主体权限加载失败");
    });

    expect(loadedSubjectIds).toEqual([1, 2, 3]);
    expect(loaded).toBe(false);
  });
});

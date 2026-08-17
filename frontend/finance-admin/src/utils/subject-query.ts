/** 主体权限页使用独立查询，避免继承主体管理页的关键字或状态筛选。 */
export function buildPermissionSubjectQuery(pageNum: number, pageSize: number) {
  return new URLSearchParams({ pageNum: String(pageNum), pageSize: String(pageSize) });
}

/** 等待全部主体加载完成，并将多个请求失败聚合为一个布尔结果。 */
export async function loadPermissionProfiles<T>(
  subjects: T[],
  loadProfile: (subject: T) => Promise<void>,
): Promise<boolean> {
  const results = await Promise.allSettled(subjects.map(loadProfile));
  return results.every((result) => result.status === "fulfilled");
}

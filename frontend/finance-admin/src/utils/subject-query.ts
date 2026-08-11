/** 主体权限页使用独立查询，避免继承主体管理页的关键字或状态筛选。 */
export function buildPermissionSubjectQuery(pageNum: number, pageSize: number) {
  return new URLSearchParams({ pageNum: String(pageNum), pageSize: String(pageSize) });
}

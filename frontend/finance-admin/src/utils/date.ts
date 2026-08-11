/** 将后端 LocalDateTime/ISO 字符串转换为页面统一的本地日期时间格式。 */
export function formatDateTime(value?: string | null): string {
  if (!value) return "-";
  return value.replace("T", " ").replace(/\.\d+$/, "").slice(0, 19);
}

import "vue-router";

export {};

declare module "vue-router" {
  interface RouteMeta {
    title?: string;
    menuCode?: "titles" | "subjects" | "permissions" | "accounts";
    requiresAuth?: boolean;
    roles?: string[];
  }
}

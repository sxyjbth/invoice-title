import type { InjectionKey } from "vue";

export type FinanceLayoutContext = Record<string, any>;

export const financeLayoutKey: InjectionKey<FinanceLayoutContext> = Symbol("finance-layout");

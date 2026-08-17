import { createApp } from "vue";
import ElementPlus from "element-plus";
import { createPinia } from "pinia";
import "element-plus/dist/index.css";
import "./styles.css";
import App from "./App.vue";
import { elementPlusOptions } from "./element-plus";
import { createApiFetch } from "./api-prefix";
import { installFinanceRouterGuards, router } from "./router";
import { useFinanceAuthStore } from "./stores/finance-auth";

window.fetch = createApiFetch(window.fetch.bind(window));

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(ElementPlus, elementPlusOptions);

installFinanceRouterGuards(router, useFinanceAuthStore(pinia));
app.use(router);

app.mount("#app");

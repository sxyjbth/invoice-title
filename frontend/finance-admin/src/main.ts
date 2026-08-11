import { createApp } from "vue";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "./styles.css";
import App from "./App.vue";
import { elementPlusOptions } from "./element-plus";
import { createApiFetch } from "./api-prefix";

window.fetch = createApiFetch(window.fetch.bind(window));

createApp(App).use(ElementPlus, elementPlusOptions).mount("#app");

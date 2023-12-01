import { defineConfig, mergeConfig } from "vite";
import defaultConfig from "./../../vite.vue.config.js";
import { comlink } from "vite-plugin-comlink";

export default mergeConfig(
  defaultConfig,
  defineConfig({
    plugins: [comlink()],
    build: {
      lib: {
        entry: "./src/index.ts",
        name: "shareworkerimpl",
      },
    },
    worker: {
      plugins: [comlink()],
    },
  }),
);

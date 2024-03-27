import { defineConfig, mergeConfig } from "vite";
import defaultConfig from "../../vite.vue.config.js";

export default mergeConfig(
  defaultConfig,
  defineConfig({
    build: {
      outDir: "dist/main",
      lib: {
        entry: "./src/electron/main/index.ts",
        name: "electronstoragemain",
      },
      rollupOptions: {
        external: ["electron", "node:path", "node:fs"],
      },
    },
  }),
);

import { defineConfig, mergeConfig } from "vite";
import defaultConfig from "../../vite.vue.config.js";

export default mergeConfig(
  defaultConfig,
  defineConfig({
    build: {
      outDir: "dist/preload",
      lib: {
        entry: "./src/electron/preload/index.ts",
        name: "electronstoragepreload",
      },
      rollupOptions: {
        external: ["electron"],
      },
    },
  }),
);

import { defineConfig, mergeConfig } from "vite";
import defaultConfig from "./../vite.vue.config.js";
import { resolve } from "path";

export default mergeConfig(
  defaultConfig,
  defineConfig({
    build: {
      lib: {
        entry: resolve(__dirname, "src/index.ts"),
        name: "skin",
      },
      rollupOptions: {
        // make sure to externalize deps that shouldn't be bundled
        // into your library
        external: ["vue"],
        output: {
          // Provide global variables to use in the UMD build
          // for externalized deps
          globals: {
            vue: "Vue",
          },
        },
      },
    },
  }),
);

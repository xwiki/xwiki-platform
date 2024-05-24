import { defineConfig } from "vite";
import Vue from "@vitejs/plugin-vue";
import Inspect from "vite-plugin-inspect";
import dts from "vite-plugin-dts";
import vuetify from "vite-plugin-vuetify";
import { comlink } from "vite-plugin-comlink";

import { resolve } from "path";

export default defineConfig({
  build: {
    sourcemap: true,
    input: {
      main: resolve(__dirname, "index.html"),
    },
  },
  plugins: [
    Vue({
      include: [/\.vue$/, /\.md$/],
      template: {
        compilerOptions: {
          isCustomElement: (tag) =>
            tag.startsWith("sl-") || tag.startsWith("solid-"),
        },
      },
    }),
    vuetify(),
    dts({
      insertTypesEntry: true,
    }),
    // https://github.com/antfu/vite-plugin-inspect
    Inspect({
      // change this to enable inspect for debugging
      enabled: true,
    }),
    comlink(),
  ],
  worker: {
    plugins: () => [comlink()],
  },
  optimizeDeps: {
    esbuildOptions: {
      tsconfigRaw: {
        compilerOptions: {
          // Workaround for a vite bug (see https://github.com/vitejs/vite/issues/13736)
          experimentalDecorators: true,
        },
      },
    },
  },
});

import { defineConfig } from 'vite';
import Vue from '@vitejs/plugin-vue';
import Inspect from 'vite-plugin-inspect';
import dts from "vite-plugin-dts";
import vuetify from 'vite-plugin-vuetify';

// import path from 'path';
import { ModuleFormat } from 'rollup';

export default defineConfig({
  build: {
    lib: {
      entry: './src/index.ts',
      // the proper extensions will be added
      fileName: (format : ModuleFormat) => { if (format=="es") 
                                                return 'main.bundle.dev.js' 
                                            else 
                                                return 'main.bundle.js' },
      formats : ["es", "iife"],
    },
    rollupOptions: {
      // make sure to externalize deps that shouldn't be bundled
      // into your library
      external: ['vue', 'vuetify'],
      output: {
        // Provide global variables to use in the UMD build
        // for externalized deps
        globals: {
          vue: 'Vue',
        },
      },
    },
  },
  plugins: [
    Vue({
      include: [/\.vue$/, /\.md$/],
      template: {
        compilerOptions: {
          isCustomElement: tag => (tag.startsWith('sl-') || tag.startsWith('solid-'))
        }
      }
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
  ]
});


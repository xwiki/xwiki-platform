import { defineConfig } from 'vite';
import Inspect from 'vite-plugin-inspect';
import dts from "vite-plugin-dts";

// import path from 'path';
import { ModuleFormat } from 'rollup';

export default defineConfig({
  build: {
    lib: {
      entry: './src/index.ts',
      name: 'menubuttons',
      // the proper extensions will be added
      fileName: (format : ModuleFormat) => { if (format=="es") 
                                                return 'main.bundle.dev.js' 
                                            else 
                                                return 'main.bundle.js' },
      formats : ["es", "iife"],
    },
  },
  plugins: [
    // https://github.com/antfu/vite-plugin-inspect
    Inspect({
      // change this to enable inspect for debugging
      enabled: true,
    }),
    dts({
       insertTypesEntry: true,
     }),
  ],
});


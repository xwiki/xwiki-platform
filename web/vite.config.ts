/*
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

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

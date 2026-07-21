/**
 * See the NOTICE file distributed with this work for additional
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

import react from "@vitejs/plugin-react";
import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));

// https://vite.dev/config/
export default defineConfig({
  build: {
    outDir: "../../../target/node-dist",
    lib: {
      entry: resolve(__dirname, "src/main.js"),
      fileName: (format, entryName) => `${entryName}.${format}.js`,
      formats: ["es"],
    },
    rollupOptions: {
      external: [
        "jquery",
        "vue",
        "vue-i18n",
        "xwiki-platform-localization-webjar",
        "@xwiki/platform-component-manager-default",
      ],
    },
    sourcemap: true,
  },
  define: {
    // Oxc replaces whole expressions only, so the entry below does not cover
    // this one, on which React selects its production build.
    "process.env.NODE_ENV": JSON.stringify("production"),
    // Define process to avoid runtime error with jQuery.
    "process.env": "{}",
  },
  plugins: [react(), vue()],
});

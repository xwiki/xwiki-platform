/*
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

import {defineConfig} from 'vite'
import vue2 from '@vitejs/plugin-vue2'
import {resolve} from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue2()],
  build: {
    lib: {
      entry: resolve(__dirname, "src/main.js"),
      fileName: "main",
      name: 'xwiki-platform-notifications-webjar',
      formats: ['umd']
    },
    rollupOptions: {
      // make sure to externalize deps that shouldn't be bundled into your library
      external: ["vue", "jquery", "xwiki-livedata-vue"],
      output: {
        // Provide global variables to use in the UMD build for externalized deps
        globals: {
          vue: "Vue",
          jquery: "$"
        },
      },
    },
  },
});

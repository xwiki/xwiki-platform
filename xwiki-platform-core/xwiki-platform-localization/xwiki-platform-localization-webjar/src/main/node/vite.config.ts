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

import { generateWebjarNodeConfig } from "@xwiki/platform-tool-viteconfig";
import { mergeConfig } from "vite";
import { resolve } from "node:path";

export default mergeConfig(
  generateWebjarNodeConfig(import.meta.url, [
    "@xwiki/platform-localization-default",
    "@xwiki/platform-localization-resolver-xwiki-rest",
  ]),
  {
    build: {
      rollupOptions: {
        input: {
          index: resolve(__dirname, "src/index.ts"),
          l10n: resolve(__dirname, "src/l10n.ts"),
        },
      },
    },
  },
);

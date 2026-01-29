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

import { generateConfigVue } from "../../../../../../xwiki-platform-node/src/main/node/vite.config.ts";
import { defineConfig, mergeConfig } from "vite";

const defaults = generateConfigVue(import.meta.url);

// Exclude @xwiki/platform-api from external dependencies because it is currently not distributed as a webjar.
// TODO: See XWIKI-XYZ
// We proceed by mutation of the default configuration because the merge strategy of vite does not propose a
// substractive operation (i.e., it's only possible to add new externals, but not to remove them).
defaults.build.rollupOptions.external =
  defaults.build.rollupOptions.external.filter(
    (it) => it !== "@xwiki/platform-api",
  );

export default mergeConfig(
  defaults,
  defineConfig({
    build: {
      outDir: "../../../target/node-dist",
    },
  }),
);

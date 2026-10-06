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
import type { LibraryOptions } from "vite";

const config = generateWebjarNodeConfig(import.meta.url);

// The displayer templates load the bundle as a classic script (see src/index.ts for why), and the top-level names of
// a classic script are globals. Emitted as an ES module, the minified functions would land on window under names
// such as `$`, overwriting the page's own. The "iife" format wraps them in a function scope instead.
config.build!.lib = {
  ...(config.build!.lib as LibraryOptions),
  // Unused, since the entry point exports nothing, but required by the "iife" format.
  name: "xwikiRecords",
  formats: ["iife"],
  fileName: () => "index.js",
};

export default config;

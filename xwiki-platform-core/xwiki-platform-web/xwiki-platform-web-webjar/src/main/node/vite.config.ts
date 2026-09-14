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

import { defineConfig } from "vite";

const minify = process.env.MINIFY === "true";

// The entry point to build. The "iife" format supports a single entry per build, so each of them is built by its own
// Vite pass, selected through this environment variable.
const entry = process.env.ENTRY ?? "entityReference";

// Note that the shared generateWebjarNodeConfig() helper is deliberately not used here: it forces the "es" format,
// while these bundles have to be classic scripts, and it defines "define.amd" as false, which would remove the AMD
// registration done by the entry points.
export default defineConfig({
  build: {
    // The webjar-node packaging copies this directory to META-INF/resources/webjars/<artifactId>/<version>/.
    outDir: "../../../target/node-dist",
    // Keep the minified and the non minified builds of each entry point side by side.
    emptyOutDir: false,
    minify: minify ? "esbuild" : false,
    sourcemap: minify,
    lib: {
      entry: `src/${entry}.ts`,
      // Unused, since the entry points export nothing, but required by the "iife" format.
      name: entry,
      formats: ["iife"],
      fileName: () => (minify ? `${entry}.min.js` : `${entry}.js`),
    },
  },
});

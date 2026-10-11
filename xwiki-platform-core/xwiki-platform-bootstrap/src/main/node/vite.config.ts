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

import { readFileSync } from "node:fs";
import { defineConfig } from "vite";

const minify = process.env.MINIFY === "true";

// Keep generating the banner/
const pkg = JSON.parse(
  readFileSync(new URL("./package.json", import.meta.url), "utf8"),
) as { version: string };
const banner =
  "/*!\n" +
  ` * Bootstrap v${pkg.version} ()\n` +
  ` * Copyright 2011-${new Date().getFullYear()} \n` +
  " * Licensed under the MIT license\n" +
  " */";

export default defineConfig({
  esbuild: {
    // Make sure the banner is not removed when prettifying the sources.
    legalComments: "inline",
  },
  build: {
    outDir: "../../../target/node-dist",
    // Keep both builds + the maven-copied less/ and fonts/ side by side.
    emptyOutDir: false,
    minify: minify ? "esbuild" : false,
    sourcemap: minify,
    lib: {
      entry: "index.js",
      name: "xwikiBootstrap",
      formats: ["iife"],
      fileName: () => (minify ? "js/xwiki-bootstrap.min.js" : "js/xwiki-bootstrap.js"),
    },
    rollupOptions: {
      output: {
        banner,
      },
    },
  },
});

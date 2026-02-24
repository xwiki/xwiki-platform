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
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import type { UserConfig } from "vite";

function generateWebjarNodeConfig(
  path: string,
  toBundle: string[] = [],
): UserConfig {
  const __dirname = dirname(fileURLToPath(path));
  return defineConfig({
    build: {
      outDir: "../../../target/node-dist",
      lib: {
        entry: resolve(__dirname, "src/index.ts"),
        fileName: (format, entryName) => `${entryName}.${format}.js`,
        formats: ["es"],
      },
      sourcemap: true,
      rollupOptions: {
        external: (id) => {
          // Force the inclusion of direct or transitive dependencies to keep bundled.
          if (
            toBundle.find((s) => id === s || id.startsWith(`${s}/`)) !==
            undefined
          ) {
            return false;
          }

          // Keep files from current package internal (relative imports)
          if (id.startsWith(".") || id.startsWith("/")) {
            return false;
          }

          // Keep absolute paths to your source files internal
          const srcPath = resolve(__dirname, "src");
          if (id.startsWith(srcPath)) {
            return false;
          }

          // Externalize everything else (node_modules dependencies)
          return true;
        },
      },
    },
  });
}

export { generateWebjarNodeConfig };

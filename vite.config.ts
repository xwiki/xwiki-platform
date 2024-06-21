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

import { defineConfig, mergeConfig, UserConfig } from "vite";
import dts from "vite-plugin-dts";
import { readFileSync } from "node:fs";
import { basename, dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import vue from "@vitejs/plugin-vue";
import cssInjectedByJsPlugin from "vite-plugin-css-injected-by-js";

function pathsComputation(path: string) {
  const dir = dirname(fileURLToPath(path));
  const packageDirName = basename(dir);
  const pkg = JSON.parse(
    readFileSync(resolve(dir, "package.json"), { encoding: "utf-8" }),
  );
  return { packageDirName, pkg };
}

export function generateConfig(path: string): UserConfig {
  const { packageDirName, pkg } = pathsComputation(path);

  const libFileName = (format: string) => `index.${format}.js`;

  return defineConfig({
    build: {
      sourcemap: true,
      lib: {
        entry: "./src/index.ts",
        name: `cristal_${packageDirName}`,
        fileName: libFileName,
      },
      rollupOptions: {
        external: Object.keys(pkg.dependencies || {}),
      },
    },
    plugins: [
      dts({
        insertTypesEntry: true,
      }),
    ],
  });
}

export function generateConfigVue(path: string): Record<string, any> {
  return mergeConfig(
    generateConfig(path),
    defineConfig({
      build: {
        cssCodeSplit: true,
        rollupOptions: {
          // external: Object.keys(pkg.dependencies || {}),
          output: {
            globals: {
              vue: "Vue",
            },
          },
        },
      },
      plugins: [
        vue({
          template: {
            compilerOptions: {
              isCustomElement: (tag) =>
                tag.startsWith("sl-") || tag.startsWith("solid-"),
            },
          },
        }),
        // This plugin is useful to make the CSS of a given module loaded by the
        // module itself, allowing CSS to be loaded even when Cristal is
        // imported in an external project.
        cssInjectedByJsPlugin(),
      ],
    }),
  );
}

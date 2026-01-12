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

import vue from "@vitejs/plugin-vue";
import { defineConfig, mergeConfig } from "vite";
import cssInjectedByJsPlugin from "vite-plugin-css-injected-by-js";
import dts from "vite-plugin-dts";
import { copyFileSync, existsSync, readFileSync } from "node:fs";
import { basename, dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import type { UserConfig } from "vite";

function pathsComputation(path: string) {
    const dir = dirname(fileURLToPath(path));
    const packageDirName = basename(dir);
    const pkg = JSON.parse(
        readFileSync(resolve(dir, "package.json"), { encoding: "utf-8" }),
    );
    return { packageDirName, pkg };
}

/**
 *
 * @param path - the path of the build module
 * @param distPath - an optional parameter in case the target directory is not the default dist
 * @param entryRoot - an optional parameter in case the entry point is not the default src/index.ts
 */
function generateConfig(
    path: string,
    distPath: string = "dist",
    entryRoot: string = "./src/",
): UserConfig {
    const { packageDirName, pkg } = pathsComputation(path);

    const libFileName = (format: string) => `index.${format}.js`;

    return defineConfig({
        build: {
            sourcemap: true,
            lib: {
                entry: `${entryRoot}/index.ts`,
                name: `cristal_${packageDirName}`,
                fileName: libFileName,
            },
            rollupOptions: {
                external: [
                    ...Object.keys(pkg.dependencies || {}),
                    ...Object.keys(pkg.peerDependencies || {}),
                ],
            },
        },
        plugins: [
            dts({
                insertTypesEntry: true,
                entryRoot,
                afterBuild: () => {
                    // publint suggests having a specific extensions for the exported types for each kind of module system
                    // (esm/cjs). The goal is to make sure packages are supported by all consumers.
                    const originTypeFile = `${distPath}/index.d.ts`;
                    // First check if the module is producing types.
                    if (existsSync(originTypeFile)) {
                        copyFileSync(originTypeFile, `${distPath}/index.d.cts`);
                    }
                },
            }),
        ],
    });
}

function generateConfigVue(path: string): Record<string, any> {
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
                vue(),
                // This plugin is useful to make the CSS of a given module loaded by the
                // module itself, allowing CSS to be loaded even when Cristal is
                // imported in an external project.
                cssInjectedByJsPlugin(),
            ],
        }),
    );
}

export { generateConfig, generateConfigVue };

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
import remapping from "@jridgewell/remapping";
import vue from "@vitejs/plugin-vue";
import { defineConfig, mergeConfig } from "vite";
import cssInjectedByJsPlugin from "vite-plugin-css-injected-by-js";
import dts from "vite-plugin-dts";
import {
  copyFileSync,
  existsSync,
  readFileSync,
  readdirSync,
  writeFileSync,
} from "node:fs";
import { basename, dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import type { UserConfig } from "vite";

/**
 * Vite plugin to resolve sourcemap chains during the bundle step.
 * When reexporting components from packages built in a previous step, Vite
 * will minify the results of this previous build which might already be
 * minified. In this scenario, this plugin will attempt to find the sourcemaps
 * from the previous build and apply them to this build's sourcemaps, so that
 * the sources can still be derived from the newly minified files.
 * @param buildOutDir - the directory that contains sourcemaps to flatten
 * @returns the Vite plugin for {@link buildOutDir}
 */
function flattenSourceMaps(buildOutDir: string) {
  return {
    name: "flatten-sourcemaps",
    async closeBundle() {
      // We list all the source maps in the output directory.
      const mapFiles = readdirSync(buildOutDir, {
        recursive: true,
        withFileTypes: true,
      })
        .filter((file) => file.name.endsWith(".map"))
        .map((file) => join(file.parentPath, file.name));

      for (const mapFile of mapFiles) {
        const map = JSON.parse(readFileSync(mapFile, "utf-8"));

        // remapping's loader function needs to return the map's content for
        // each source recursively listed in the sourcemap chain.
        const flattened = remapping(map, (sourceFile) => {
          // We need to resolve the relative paths.
          const absolutePath = resolve(dirname(mapFile), sourceFile);

          try {
            return JSON.parse(readFileSync(absolutePath + ".map", "utf-8"));
          } catch {
            return null;
          }
        });

        // We save the flattened map.
        writeFileSync(mapFile, JSON.stringify(flattened));
      }
    },
  };
}

function generateWebjarNodeConfig(
  path: string,
  toBundle: string[] = [],
): UserConfig {
  const WEBJAR_NODE_OUT_DIR = "../../../target/node-dist";
  const __dirname = dirname(fileURLToPath(path));
  return defineConfig({
    build: {
      outDir: WEBJAR_NODE_OUT_DIR,
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
    plugins: [flattenSourceMaps(WEBJAR_NODE_OUT_DIR)],
  });
}

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
          // publint suggests having a specific extensions for the exported types for each kind of module
          // system (esm/cjs). The goal is to make sure packages are supported by all consumers.
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

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function generateConfigVue(path: string): Record<string, any> {
  const baseConfig = generateConfig(path);
  return mergeConfig(
    baseConfig,
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
        ...(baseConfig.plugins ?? []),
        vue(),
        // This plugin is useful to make the CSS of a given module loaded by the
        // module itself, allowing CSS to be loaded even when Cristal is
        // imported in an external project.
        cssInjectedByJsPlugin(),
      ],
    }),
  );
}

export { generateConfig, generateConfigVue, generateWebjarNodeConfig };

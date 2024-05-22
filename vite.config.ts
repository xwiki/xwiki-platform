import { defineConfig, mergeConfig } from "vite";
import dts from "vite-plugin-dts";
import { readFileSync } from "node:fs";
import { basename, dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import vue from "@vitejs/plugin-vue";

function pathsComputation(path: string) {
  const dir = dirname(fileURLToPath(path));
  const packageDirName = basename(dir);
  const pkg = JSON.parse(
    readFileSync(resolve(dir, "package.json"), { encoding: "utf-8" }),
  );
  return { packageDirName, pkg };
}

export function generateConfig(path: string) {
  const { packageDirName, pkg } = pathsComputation(path);

  const libFileName = (format: string) => `index.${format}.js`;

  return defineConfig({
    build: {
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

export function generateConfigVue(path: string) {
  return mergeConfig(
    generateConfig(path),
    defineConfig({
      build: {
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
      ],
    }),
  );
}

/**
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
import js from "@eslint/js";
import { defineConfig } from "eslint/config";
// @ts-expect-error no types are available for this package
import headers from "eslint-plugin-headers";
import importPlugin from "eslint-plugin-import";
import eslintPluginPrettierRecommended from "eslint-plugin-prettier/recommended";
// @ts-expect-error no types are available for this package
import pluginPromise from "eslint-plugin-promise";
import eslintPluginTsdoc from "eslint-plugin-tsdoc";
import pluginVue from "eslint-plugin-vue";
import globals from "globals";
import tseslint from "typescript-eslint";
import vueEslintParser from "vue-eslint-parser";
import type { Config } from "@eslint/config-helpers";

const licenseText =
  "See the LICENSE file distributed with this work for additional\n" +
  "information regarding copyright ownership.\n" +
  "\n" +
  "This is free software; you can redistribute it and/or modify it\n" +
  "under the terms of the GNU Lesser General Public License as\n" +
  "published by the Free Software Foundation; either version 2.1 of\n" +
  "the License, or (at your option) any later version.\n" +
  "\n" +
  "This software is distributed in the hope that it will be useful,\n" +
  "but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
  "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU\n" +
  "Lesser General Public License for more details.\n" +
  "\n" +
  "You should have received a copy of the GNU Lesser General Public\n" +
  "License along with this software; if not, write to the Free\n" +
  "Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA\n" +
  "02110-1301 USA, or see the FSF site: http://www.fsf.org.";
const eslintConfig: Config[] = defineConfig([
  {
    files: ["**/*.{js,mjs,cjs,ts,mts,cts,vue}"],
    plugins: { js },
    extends: ["js/recommended"],
    languageOptions: { globals: { ...globals.browser, ...globals.node } },
  },
  tseslint.configs.recommended,
  pluginVue.configs["flat/essential"],
  {
    files: ["**/*.vue"],
    languageOptions: { parserOptions: { parser: tseslint.parser } },
    rules: {
      "vue/no-deprecated-slot-attribute": [
        "error",
        {
          ignore: ["/^sl-/"],
        },
      ],
    },
  },
  importPlugin.flatConfigs.recommended,
  pluginPromise.configs["flat/recommended"],
  {
    plugins: {
      tsdoc: eslintPluginTsdoc,
    },
    rules: {
      "tsdoc/syntax": "error",
    },
  },
  eslintPluginPrettierRecommended,
  {
    files: ["**/*.{ts,tsx}"],
    extends: [
      importPlugin.flatConfigs.recommended,
      importPlugin.flatConfigs.typescript,
    ],
    // other configs...
  },
  {
    files: ["**/*.{js,mjs,cjs,ts,tsx,vue}"],
    settings: {
      "import/parsers": {
        espree: [".js", ".cjs", ".mjs", ".jsx"],
      },
      "import/resolver": {
        typescript: true,
        node: true,
      },
    },
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module",
    },
    rules: {
      "import/order": [
        "error",
        {
          groups: [
            "index",
            "sibling",
            "parent",
            "internal",
            "external",
            "builtin",
            "object",
            "type",
          ],
          alphabetize: {
            order: "asc",
          },
          named: { import: true, export: true },
        },
      ],
      "import/export": "error",
      "import/group-exports": "error",
      "import/consistent-type-specifier-style": ["error", "prefer-top-level"],
      curly: "error",
      "max-statements": "error",
      "import/no-cycle": "error",
    },
  },
  {
    plugins: {
      headers,
    },
    rules: {
      "headers/header-format": [
        "error",
        {
          source: "string",
          content: licenseText,
        },
      ],
    },
  },
  {
    plugins: {
      headers,
    },
    files: ["**/*.vue"],
    rules: {
      "headers/header-format": [
        "error",
        {
          source: "string",
          content: licenseText,
          enableVueSupport: true,
        },
      ],
    },
    languageOptions: {
      parser: vueEslintParser,
    },
  },
]);
export default eslintConfig;

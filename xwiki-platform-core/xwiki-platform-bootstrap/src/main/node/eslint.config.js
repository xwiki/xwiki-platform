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

import js from "@eslint/js";
import globals from "globals";
import tseslint from "typescript-eslint";

// Minimal linting, matching the style of the pre-fork code base.
export default [
  {
    ignores: [
      "node_modules/**",
      "tests/vendor/**",
      "tests/visual/**",
      "tests/index.html",
    ],
  },
  js.configs.recommended,
  {
    files: ["js/**/*.js", "tests/unit/**/*.js"],
    languageOptions: {
      ecmaVersion: 2018,
      sourceType: "script",
      globals: {
        ...globals.browser,
        jQuery: "readonly",
        $: "readonly",
        QUnit: "readonly",
      },
    },
    rules: {
      // These rules are custom and match the code style of this module before the fork.
      "no-unused-vars": "off",
      "no-empty": "off",
      "no-cond-assign": "off",
      "no-unexpected-multiline": "off",
    },
  },
  {
    files: ["index.js", "*.config.js"],
    languageOptions: {
      ecmaVersion: "latest",
      sourceType: "module",
      globals: { ...globals.node, ...globals.browser },
    },
  },
  {
    files: ["*.config.ts", "tests/playwright/**/*.ts"],
    languageOptions: {
      ecmaVersion: "latest",
      sourceType: "module",
      globals: { ...globals.node, ...globals.browser },
      parser: tseslint.parser,
    },
  },
];

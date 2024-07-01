/* eslint-env node */
require("@rushstack/eslint-patch/modern-module-resolution");

module.exports = {
  "root": true,
  "extends": [
    "plugin:vue/essential",
    "eslint:recommended",
    "@vue/eslint-config-prettier"
  ],
  "ignorePatterns": ["vite.config.js"]
}

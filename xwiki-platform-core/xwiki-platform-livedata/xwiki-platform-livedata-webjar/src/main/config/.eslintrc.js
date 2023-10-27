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

module.exports = {
  root: true,
  env: {
    node: true,
    'jest/globals': true
  },
  'extends': [
    'plugin:vue/recommended',
    'eslint:recommended'
  ],
  plugins: ['jest'],
  parserOptions: {
    parser: '@babel/eslint-parser'
  },
  rules: {
    'camelcase': 'error',
    'max-params': ['error', 5],
    'max-depth': ['error', 3],
    'max-statements': ['error', 20],
    'complexity': ['error', 10],
    'max-len': ['error', 120],
    'no-console': ['warn', { allow: ['warn', 'error'] }],
    'no-debugger': 'warn',
  },
  globals: {
    define: 'readonly',
    // We need to be able to set the public path at runtime.
    '__webpack_public_path__': 'writable',
    XWiki: 'writable'
  }
};

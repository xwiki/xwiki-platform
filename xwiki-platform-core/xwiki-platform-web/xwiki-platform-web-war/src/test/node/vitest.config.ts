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

import { defineConfig } from "vitest/config";
import { fileURLToPath } from "node:url";

export default defineConfig({
  resolve: {
    // Let the tests import the legacy JavaScript files they cover (with the ?raw suffix) without counting directories.
    // Vite resolves relative aliases from the importing file, so the paths must be absolute.
    alias: {
      "@webapp": fileURLToPath(new URL("../../main/webapp", import.meta.url)),
      // Filled by the unpack-prototype-for-javascript-tests execution in the pom.xml of this module.
      "@test-webjars": fileURLToPath(
        new URL("../../../target/test-webjars", import.meta.url),
      ),
    },
  },
  test: {
    environment: "jsdom",
  },
});

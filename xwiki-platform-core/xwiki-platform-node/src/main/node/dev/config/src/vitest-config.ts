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
import type { UserConfig } from "vite";

const userConfig: UserConfig = defineConfig({
  test: {
    reporters: ["junit"],
    outputFile: "target/unit-tests.xml",
    passWithNoTests: true,
    // Bound the number of concurrent worker processes each Vitest run may spawn. Without this, Vitest defaults to
    // roughly one worker per CPU core; combined with the packages Nx runs in parallel, the resulting jsdom worker
    // processes exhaust the memory of the CI agents, which get killed by the OOM killer.
    maxWorkers: 2,
  },
});
export default userConfig;

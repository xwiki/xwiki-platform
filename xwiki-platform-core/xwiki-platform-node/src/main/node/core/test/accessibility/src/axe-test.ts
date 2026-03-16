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
import { assertAxe } from "./assert-axe";
import { test } from "vitest";
import type { AxeFeatures } from "./axe-features";
import type { TestAPI } from "vitest";

/**
 * An extended test method with axe core accessibility test integrated.
 * @since 18.2.0RC1
 * @public
 */
const it: TestAPI = test.extend<AxeFeatures>({
  // Fixture 1: owns mounting and exposes the wrapper
  wrapper: [
    () => {
      throw new Error(
        "Override the wrapper fixture with the component under test",
      );
    },
  ],

  // Fixture 2: depends on wrapper, runs axe after each test
  axeCheck: [
    async ({ wrapper }, use) => {
      await use();

      if (wrapper) {
        await assertAxe(wrapper);
        wrapper.unmount();
      }
    },
    { auto: true },
  ],
});

export { it, it as test };

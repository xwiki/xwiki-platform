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
import { run } from "axe-core";
import { expect } from "vitest";
import type { VueWrapper } from "@vue/test-utils";

/**
 * Run an axe core check on the provided vue component wrapper and check for the absence of issues.
 * @param wrapper - the vue wrapper to assert for accessibility
 * @since 18.2.0RC1
 * @public
 */
async function assertAxe(wrapper: VueWrapper): Promise<void> {
  const axeRun = await run(wrapper.element, {
    runOnly: {
      type: "tags",
      // Note: this list needs to be in sync with org.xwiki.test.ui.WCAGContext.VALIDATE_TAGS
      values: ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"],
    },
  });
  expect(axeRun.violations).toEqual([]);
}

export { assertAxe };

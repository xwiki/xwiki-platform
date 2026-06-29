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
import { it } from "./axe-test";
import type { WrapperFeature } from "./axe-features";
import type { VueWrapper } from "@vue/test-utils";

/**
 * Execute a test and checks for the accessibility of the tested component with axe core at the end.
 * @param name - the name of the test
 * @param wrapper - the vue wrapper to test
 * @param assertions - a function performing assertions on the component
 * @since 18.2.0RC1
 * @public
 */
function runTest(
  name: string,
  wrapper: VueWrapper,
  assertions: (wrapper: VueWrapper) => Promise<void> | void,
): void {
  const extend = it.extend<WrapperFeature>({
    // eslint-disable-next-line no-empty-pattern
    wrapper: async ({}, use) => {
      await use(wrapper);
    },
  });

  // For some reason this scope call is mandatory to make the rest work, otherwise the wrapper is not initialized by
  // the extends above.
  extend.scoped({});

  extend(name, async ({ wrapper }) => {
    await assertions(wrapper);
  });
}

export { runTest };

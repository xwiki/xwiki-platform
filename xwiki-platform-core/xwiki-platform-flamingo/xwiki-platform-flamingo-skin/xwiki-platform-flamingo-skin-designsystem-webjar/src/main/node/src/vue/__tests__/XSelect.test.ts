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
import XSelect from "../XSelect.vue";
import {
  runTest,
  shallowMountHelper,
} from "@xwiki/platform-test-accessibility";
import { describe, expect } from "vitest";
import type { SelectProps } from "@xwiki/platform-dsapi";

const accessibilityMount = shallowMountHelper(XSelect);
describe("XSelect", () => {
  runTest(
    "render minimal props",
    accessibilityMount({
      props: {
        label: "Select Label",
        items: ["i1"],
      } satisfies SelectProps,
    }),
    (wrapper) => {
      expect(wrapper.html()).toEqual(`<dl>
  <dt><label for="v-0">Select Label</label>
    <!--v-if-->
  </dt>
  <dd><select id="v-0">
      <option value="i1">i1</option>
    </select></dd>
</dl>`);
    },
  );

  runTest(
    "render with hint",
    accessibilityMount({
      props: {
        label: "Select Label",
        items: ["i1"],
        help: "The help",
      } satisfies SelectProps,
    }),
    (wrapper) => {
      expect(wrapper.html()).toEqual(`<dl>
  <dt><label for="v-0">Select Label</label><span class="xHint">The help</span></dt>
  <dd><select id="v-0">
      <option value="i1">i1</option>
    </select></dd>
</dl>`);
    },
  );

  runTest(
    "render required",
    accessibilityMount({
      props: {
        label: "Select Label",
        items: ["i1"],
        required: true,
      } satisfies SelectProps,
    }),
    (wrapper) => {
      expect(wrapper.html()).toEqual(`<dl>
  <dt><label for="v-0">Select Label</label>
    <!--v-if-->
  </dt>
  <dd><select id="v-0" required="">
      <option value="i1">i1</option>
    </select></dd>
</dl>`);
    },
  );
});

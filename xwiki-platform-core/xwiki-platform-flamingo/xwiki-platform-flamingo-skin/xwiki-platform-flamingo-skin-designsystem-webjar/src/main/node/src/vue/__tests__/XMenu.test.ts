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
import XMenu from "../XMenu.vue";
import {
  runTest,
  shallowMountHelper,
} from "@xwiki/platform-test-accessibility";
import { describe, expect } from "vitest";

const getAccessibilityMount = shallowMountHelper(XMenu, {
  global: {
    stubs: {
      "wrap-if": false,
      "x-menu-label": false,
      "x-menu-item": false,
    },
  },
});
describe("XMenu", () => {
  runTest(
    "render no props",
    getAccessibilityMount({
      slots: {
        default: `<x-menu-label>menu label</x-menu-label>
      <x-menu-item>menu content</x-menu-item>`,
      },
    }),
    (wrapper) => {
      expect(wrapper.find("div").classes()).toContain("open");
      expect(wrapper.find("div > ul").classes()).toContain("dropdown-menu");
    },
  );
});

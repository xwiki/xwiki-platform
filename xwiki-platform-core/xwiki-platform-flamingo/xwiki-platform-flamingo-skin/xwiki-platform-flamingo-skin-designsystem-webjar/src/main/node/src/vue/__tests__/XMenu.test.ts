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
import XBtn from "../XBtn.vue";
import XMenu from "../XMenu.vue";
import XMenuItem from "../XMenuItem.vue";
import XMenuLabel from "../XMenuLabel.vue";
import {
  runTest,
  shallowMountHelper,
} from "@xwiki/platform-test-accessibility";
import { describe } from "vitest";
import Module from "module";

function getAccessibilityMount() {
  // Override require used globally but resolved to node's required by default.
  Module.prototype.require = () => {};
  return shallowMountHelper(XMenu, {
    global: {
      stubs: {
        // Use the real implementation instead of stubvs for XMenuItem and XMenuLabel
        XMenuItem,
        XMenuLabel,
        XBtn,
      },
    },
  });
}

const accessibilityMount = getAccessibilityMount();
describe("XMenu", () => {
  runTest(
    "render no props",
    accessibilityMount({
      slots: {
        activator: `<x-btn variant="primary">menu button</x-btn>`,
        default: `<x-menu-label>menu label</x-menu-label>
      <x-menu-item>menu content</x-menu-item>`,
      },
    }),
    (wrapper) => {
      console.log(wrapper.html());
    },
  );
});

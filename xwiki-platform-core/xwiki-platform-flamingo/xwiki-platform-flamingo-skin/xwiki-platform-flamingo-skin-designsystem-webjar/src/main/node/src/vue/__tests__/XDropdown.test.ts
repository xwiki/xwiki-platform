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
import XDropdown from "../XDropdown.vue";
import XMenu from "../XMenu.vue";
import XMenuItem from "../XMenuItem.vue";
import XMenuLabel from "../XMenuLabel.vue";
import { mountHelper, runTest } from "@xwiki/platform-test-accessibility";
import { describe, expect } from "vitest";
import Module from "module";

function getAccessibilityMount() {
  Module.prototype.require = () => {};
  return mountHelper(XDropdown, {
    global: {
      components: {
        XMenu,
        XMenuItem,
        XMenuLabel,
      },
    },
  });
}

const accessibilityMount = getAccessibilityMount();
describe("XDropdown", () => {
  runTest(
    "render no props",
    accessibilityMount({
      slots: {
        activator: "button text",
        default: `<x-menu><x-menu-label>menu label</x-menu-label>
      <x-menu-item>menu content</x-menu-item>
</x-menu>`,
      },
    }),
    (wrapper) => {
      expect(wrapper.html())
        .toEqual(`<div class="dropdown"><button class="btn btn-default dropdown-toggle" id="v-0" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">button text &nbsp;<span class="caret"></span></button>
  <ul class="dropdown-menu" aria-labelledby="v-0">
    <li class="dropdown-header">menu label</li>
    <li>menu content</li>
  </ul>
</div>`);
    },
  );
});

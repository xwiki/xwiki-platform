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
import XDialog from "../XDialog.vue";
import { mountHelper, runTest } from "@xwiki/platform-test-accessibility";
import { describe, expect } from "vitest";
import Module from "module";
import type { DialogProps } from "@xwiki/platform-dsapi";

function getAccessibilityMount() {
  // Override require used globally but resolved to node's required by default.
  Module.prototype.require = () => {};
  return mountHelper(XDialog);
}

const accessibilityMount = getAccessibilityMount();
describe("XDialog", () => {
  runTest(
    "render minimal props",
    accessibilityMount({
      props: {
        title: "Dialog Title",
      } satisfies DialogProps,
      slots: {
        default: "Some Text",
      },
    }),
    async () => {
      expect(document.body.innerHTML).toEqual(
        `<div data-v-app=""><span data-v-f91168ea=""></span><!--teleport start--><!--teleport end--></div><div data-v-f91168ea="" class="modal fade" tabindex="-1" role="dialog"><div data-v-f91168ea="" class="modal-dialog" role="document"><div data-v-f91168ea="" class="modal-content"><div data-v-f91168ea="" class="modal-header"><button data-v-f91168ea="" type="button" class="close" data-dismiss="modal" aria-label="Close"><span data-v-f91168ea="" aria-hidden="true">×</span></button><h4 data-v-f91168ea="" class="modal-title">Dialog Title</h4></div><div data-v-f91168ea="" class="modal-body">Some Text</div><!--v-if--></div></div></div>`,
      );
    },
  );
});

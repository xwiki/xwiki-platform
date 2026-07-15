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
import {
  runTest,
  shallowMountHelper,
} from "@xwiki/platform-test-accessibility";
import { describe, expect } from "vitest";
import type { BtnProps } from "@xwiki/platform-dsapi";

const accessibilityMount = shallowMountHelper(XBtn);
describe("XBtn", () => {
  runTest(
    "render empty",
    accessibilityMount({
      slots: {
        default: "Some Text",
      },
    }),
    (wrapper) => {
      expect(wrapper.html()).toBe(
        '<button class="btn btn-default">Some Text</button>',
      );
    },
  );
  runTest(
    "render variant primary",
    accessibilityMount({
      props: {
        variant: "primary",
      } satisfies BtnProps,
      slots: {
        default: "Some Text",
      },
    }),
    (wrapper) => {
      expect(wrapper.html()).toBe(
        '<button class="btn btn-primary">Some Text</button>',
      );
    },
  );

  runTest(
    "render size small",
    accessibilityMount({
      props: {
        size: "small",
      } satisfies BtnProps,
      slots: {
        default: "Some Text",
      },
    }),
    (wrapper) => {
      expect(wrapper.html()).toBe(
        '<button class="btn btn-sm btn-default">Some Text</button>',
      );
    },
  );
});

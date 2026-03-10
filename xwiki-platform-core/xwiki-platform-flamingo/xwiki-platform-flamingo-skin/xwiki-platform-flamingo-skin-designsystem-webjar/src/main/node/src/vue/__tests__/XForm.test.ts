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
import XForm from "../XForm.vue";
import {
  runTest,
  shallowMountHelper,
} from "@xwiki/platform-test-accessibility";
import { describe, expect } from "vitest";
import type { FormProps } from "@xwiki/platform-dsapi";

const accessibilityMount = shallowMountHelper(XForm);
describe("XForm", () => {
  let submited = false;
  runTest(
    "render minimal props",
    accessibilityMount({
      props: {
        onFormSubmit: () => {
          submited = true;
        },
      } satisfies FormProps,
      slots: {
        default: "Some Text",
      },
    }),
    (wrapper) => {
      expect(wrapper.html()).toEqual(`<form class="xform">Some Text</form>`);
      expect(submited).toBeFalsy();
      wrapper.find("form").trigger("submit");
      expect(submited).toBeTruthy();
    },
  );
});

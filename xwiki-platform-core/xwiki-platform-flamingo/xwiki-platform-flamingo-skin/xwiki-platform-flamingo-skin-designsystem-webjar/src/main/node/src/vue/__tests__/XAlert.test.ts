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
import XAlert from "../XAlert.vue";
import {
  assertAxe,
  runTest,
  shallowMountHelper,
} from "@xwiki/platform-test-accessibility";
import { describe, expect } from "vitest";
import type { AlertProps } from "@xwiki/platform-dsapi";

const accessibilityMount = shallowMountHelper(XAlert);
describe("XAlert", () => {
  runTest(
    "render with minimal configuration",
    accessibilityMount({
      props: {
        type: "info",
      } satisfies AlertProps,
      slots: {
        default: "Some Text",
      },
    }),
    (wrapper) => {
      expect(wrapper.find("div").classes()).toEqual(["box", "infomessage"]);
      expect(wrapper.find("div").text()).toEqual("Some Text");
      expect(wrapper.find("button.close").exists()).toBe(false);
    },
  );

  runTest(
    "render with a title",
    accessibilityMount({
      props: {
        type: "error",
        title: "The Title",
      } satisfies AlertProps,
    }),
    (wrapper) => {
      expect(wrapper.find("div").classes()).toEqual(["box", "errormessage"]);
      expect(wrapper.find("div > strong").text()).toEqual("The Title");
      expect(wrapper.find("button.close").exists()).toBe(false);
    },
  );

  runTest(
    "render closable",
    accessibilityMount({
      props: {
        type: "warning",
        closable: true,
      } satisfies AlertProps,
    }),

    (wrapper) => {
      expect(wrapper.find("div").classes()).toEqual([
        "box",
        "warningmessage",
        "alert-dismissible",
        "fade",
        "in",
      ]);
      expect(wrapper.find("button.close").exists()).toBe(true);
    },
  );

  let actionCalled;
  runTest(
    "render with actions",
    accessibilityMount({
      props: {
        type: "success",
        actions: [
          {
            name: "action1",
            callback() {
              actionCalled = true;
            },
          },
        ],
      } satisfies AlertProps,
    }),
    async (wrapper) => {
      actionCalled = false;

      expect(wrapper.find("div").classes()).toEqual(["box", "successmessage"]);
      const actionButton = wrapper.find("x-btn-stub");
      expect(actionButton.exists()).toBe(true);
      expect(actionCalled).toBe(false);
      await assertAxe(wrapper);
      await actionButton.trigger("click");
      expect(actionCalled).toBe(true);
    },
  );
});

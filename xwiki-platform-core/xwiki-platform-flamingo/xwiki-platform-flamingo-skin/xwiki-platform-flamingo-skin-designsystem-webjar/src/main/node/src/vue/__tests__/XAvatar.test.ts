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
import XAvatar from "../XAvatar.vue";
import {
  runTest,
  shallowMountHelper,
} from "@xwiki/platform-test-accessibility";
import { describe, expect, vi } from "vitest";
import type { AvatarProps } from "@xwiki/platform-dsapi";

function initMount() {
  vi.stubGlobal("XWiki", {
    contextPath: "http://test",
  });
  return shallowMountHelper(XAvatar);
}

const accessibilityMount = initMount();
describe("XAvatar", () => {
  runTest(
    "render with minimal configuration",
    accessibilityMount({
      props: {
        name: "UserName",
      } satisfies AvatarProps,
    }),
    (wrapper) => {
      const img = wrapper.find("img");
      expect(img.classes()).toEqual(["avatar", "avatar_50"]);
      expect(img.attributes("src")).toEqual(
        "http://test/bin/skin/resources/icons/xwiki/noavatar.png",
      );
      expect(img.attributes("alt")).toEqual("UserName");
      expect(img.attributes("title")).toEqual("UserName");
    },
  );

  runTest(
    "render with size",
    accessibilityMount({
      props: {
        name: "UserName",
        size: "42px",
      } satisfies AvatarProps,
    }),
    (wrapper) => {
      const img = wrapper.find("img");
      expect(img.classes()).toEqual(["avatar"]);
      expect(img.attributes("src")).toEqual(
        "http://test/bin/skin/resources/icons/xwiki/noavatar.png",
      );
      expect(img.attributes("alt")).toEqual("UserName");
      expect(img.attributes("title")).toEqual("UserName");
      expect(img.attributes("width")).toEqual("42px");
    },
  );
});

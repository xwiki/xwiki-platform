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
import XBreadcrumb from "../XBreadcrumb.vue";
import {
  runTest,
  shallowMountHelper,
} from "@xwiki/platform-test-accessibility";
import { describe, expect } from "vitest";
import type { BreadcrumbProps } from "@xwiki/platform-dsapi";

const accessibilityMount = shallowMountHelper(XBreadcrumb);
describe("XBreadcrumb", () => {
  runTest("render empty", accessibilityMount(), (wrapper) => {
    expect(wrapper.html()).toBe('<ol class="breadcrumb"></ol>');
  });

  runTest(
    "render with one item, no url",
    accessibilityMount({
      props: {
        items: [
          {
            label: "l1",
          },
        ],
      } satisfies BreadcrumbProps,
    }),
    (wrapper) => {
      expect(wrapper.html()).toBe(`<ol class="breadcrumb">
  <li class="active">l1</li>
</ol>`);
    },
  );

  runTest(
    "render with one item, with url",
    accessibilityMount({
      props: {
        items: [
          {
            label: "l1",
            url: "https://test.com",
          },
        ],
      } satisfies BreadcrumbProps,
    }),
    (wrapper) => {
      expect(wrapper.html()).toBe(`<ol class="breadcrumb">
  <li class="active"><a href="https://test.com">l1</a></li>
</ol>`);
    },
  );

  runTest(
    "render with two items",
    accessibilityMount({
      props: {
        items: [
          {
            label: "l1",
          },
          {
            label: "l2",
          },
        ],
      } satisfies BreadcrumbProps,
    }),
    (wrapper) => {
      expect(wrapper.html()).toBe(`<ol class="breadcrumb">
  <li class="">l1</li>
  <li class="active">l2</li>
</ol>`);
    },
  );
});

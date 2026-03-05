/*
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

import DisplayerHtml from "./DisplayerHtml.vue";
import { initWrapper } from "./displayerTestsHelper";
import { afterEach, describe, expect, it } from "vitest";
import sinon from "sinon";

describe("DisplayerHtml.vue", () => {
  afterEach(function() {
    // completely restore all fakes created through the sandbox
    sinon.restore();
  });

  it("Renders an entry in view mode", () => {
    const wrapper = initWrapper(DisplayerHtml, {
      props: {
        entry: {
          color: "<strong>some content</strong>",
        },
      },
      logic: {
        isContentTrusted: () => true,
      },
    });

    const htmlWrapper = wrapper.find(".html-wrapper");
    expect(htmlWrapper.classes()).toContain("html-wrapper");
    expect(htmlWrapper.text()).toBe("some content");
    expect(htmlWrapper.find("strong")).not.toBeUndefined;
  });

  it("Renders an entry in view mode with untrusted content", () => {
    const wrapper = initWrapper(DisplayerHtml, {
      props: {
        entry: {
          color: "<strong>some content<script>console.log(\"hello world\")</script></strong>",
        },
      },
      logic: {
        isContentTrusted: () => false,
      },
    });
    expect(wrapper.find(".html-wrapper > *").html()).toBe("<strong>some content</strong>");
  });
});

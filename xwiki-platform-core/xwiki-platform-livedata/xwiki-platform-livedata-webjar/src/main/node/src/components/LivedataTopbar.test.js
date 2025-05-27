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
import { mount } from "@vue/test-utils";
import LivedataTopbar from "./LivedataTopbar.vue";
import { describe, expect, it } from "vitest";

/**
 * Vue component initializer for the `LivedataTopbar` component. Calls `mount()` from
 * `@vue/test-utils` with preconfigured values.
 *
 * The default options passed to mounts are:
 * ```
 * {
 *   slots: {
 *     left: 'content left',
 *     right: 'content right',
 *   }
 * }
 * ```
 *
 * @returns {*} a wrapper for the LivedataBottombar component
 */
function initWrapper() {
  return mount(LivedataTopbar, {
    slots: {
      left: "content left",
      right: "content right",
    },
  });
}

describe("LivedataTopbar.vue", () => {
  it("Render and display the default slot", () => {
    const wrapper = initWrapper();
    expect(wrapper.attributes("class")).toContain("livedata-topbar");
    expect(wrapper.find(".livedata-topbar-left").text()).toBe("content left");
    expect(wrapper.find(".livedata-topbar-right").text()).toBe("content right");
  });
});

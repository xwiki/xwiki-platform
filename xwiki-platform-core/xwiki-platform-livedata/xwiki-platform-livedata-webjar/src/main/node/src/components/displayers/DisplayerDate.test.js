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

import DisplayerDate from "./DisplayerDate.vue";
import { initWrapper } from "./displayerTestsHelper";
import { afterEach, describe, expect, it, vi } from "vitest";
import flushPromises from "flush-promises";
import sinon from "sinon";

vi.mock("@/services/require.js", function() {
  return {
    async loadById() {
      return () => {
        return {
          format() {
            return "formatted date";
          },
        };
      };
    },
  };
});

describe("DisplayerDate.vue", () => {
  afterEach(function() {
    // completely restore all fakes created through the sandbox
    sinon.restore();
  });

  it("Renders an entry in view mode", async () => {
    const wrapper = initWrapper(DisplayerDate, {});
    await flushPromises();
    expect(wrapper.text()).toMatch("formatted date");
  });

  it("Switch to edit mode", async () => {
    const wrapper = initWrapper(DisplayerDate, {});
    await flushPromises();
    await wrapper.setProps({ isView: false });

    const input = wrapper.find(".editor-date");
    expect(input.attributes("value")).toBe("formatted date");
    expect(input.element).toBe(document.activeElement)
  });
});

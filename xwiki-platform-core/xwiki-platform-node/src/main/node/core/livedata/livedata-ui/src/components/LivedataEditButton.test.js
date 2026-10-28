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

import LivedataEditButton from "./LivedataEditButton.vue";
import { mount } from "@vue/test-utils";
import flushPromises from "flush-promises";
import { spy } from "sinon";
import { describe, expect, it } from "vitest";
import { ref } from "vue";

/**
 * Vue component initializer for `LivedataEditButton` components. Calls `mount()` with preconfigured
 * values.
 *
 * @param editMode - whether the Live Data starts in edit mode
 * @returns a map containing a wrapper for `LivedataEditButton` components and a mock of the logic
 */
function initWrapper({ editMode = false } = {}) {
  global.XWiki = {
    contextPath: "",
  };

  const isEditMode = ref(editMode);
  const logic = {
    isEditMode: () => isEditMode.value,
    enableEditMode: spy(() => {
      isEditMode.value = true;
    }),
    disableEditMode: spy(() => {
      isEditMode.value = false;
    }),
    updateEntries: spy(() => Promise.resolve()),
  };

  const wrapper = mount(LivedataEditButton, {
    global: {
      provide: {
        logic,
      },
      mocks: {
        $t: (key) => key,
      },
      stubs: {
        XWikiIcon: true,
      },
    },
  });
  return { wrapper, logic };
}

describe("LivedataEditButton.vue", () => {
  it("Is labelled and not pressed by default", () => {
    const { wrapper } = initWrapper();
    const button = wrapper.find("button");

    expect(button.attributes("title")).toBe("livedata.action.editMode");
    expect(button.attributes("aria-label")).toBe("livedata.action.editMode");
    expect(button.attributes("aria-pressed")).toBe("false");
    expect(button.classes()).not.toContain("active");
  });

  it("Is pressed when the Live Data is in edit mode", () => {
    const { wrapper } = initWrapper({ editMode: true });
    const button = wrapper.find("button");

    expect(button.attributes("aria-pressed")).toBe("true");
    expect(button.classes()).toContain("active");
  });

  it("Enables the edit mode on click and refreshes the entries", async () => {
    const { wrapper, logic } = initWrapper();

    await wrapper.find("button").trigger("click");
    await flushPromises();

    expect(logic.enableEditMode.callCount).toBe(1);
    expect(logic.disableEditMode.callCount).toBe(0);
    expect(logic.updateEntries.callCount).toBe(1);

    const button = wrapper.find("button");
    expect(button.attributes("aria-pressed")).toBe("true");
    expect(button.classes()).toContain("active");
  });

  it("Disables the edit mode on click when it is already enabled", async () => {
    const { wrapper, logic } = initWrapper({ editMode: true });

    await wrapper.find("button").trigger("click");
    await flushPromises();

    expect(logic.disableEditMode.callCount).toBe(1);
    expect(logic.enableEditMode.callCount).toBe(0);
    expect(logic.updateEntries.callCount).toBe(1);

    const button = wrapper.find("button");
    expect(button.attributes("aria-pressed")).toBe("false");
    expect(button.classes()).not.toContain("active");
  });

  it("Disables the edit mode when unmounted", async () => {
    const { wrapper, logic } = initWrapper({ editMode: true });

    wrapper.unmount();
    await flushPromises();

    expect(logic.disableEditMode.callCount).toBe(1);
    expect(logic.updateEntries.callCount).toBe(1);
  });
});

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

import LivedataActions from "./LivedataActions.vue";
import LivedataDropdownMenu from "./LivedataDropdownMenu.vue";
import LivedataEditButton from "./LivedataEditButton.vue";
import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import { ref } from "vue";

/**
 * Vue component initializer for `LivedataActions` components. Calls `mount()` with preconfigured
 * values.
 *
 * @param hasEditMode - whether the source of the Live Data supports the edit mode
 * @returns a wrapper for `LivedataActions` components
 */
function initWrapper({ hasEditMode = false } = {}) {
  global.XWiki = {
    contextPath: "",
  };

  const editModeSupported = ref(hasEditMode);
  return {
    editModeSupported,
    wrapper: mount(LivedataActions, {
      global: {
        provide: {
          logic: {
            hasEditMode: () => editModeSupported.value,
          },
        },
        mocks: {
          $t: (key) => key,
        },
        stubs: {
          LivedataDropdownMenu: true,
          LivedataEditButton: true,
        },
      },
    }),
  };
}

describe("LivedataActions.vue", () => {
  it("Displays the dropdown menu when the source has no edit mode", () => {
    const { wrapper } = initWrapper();

    expect(wrapper.findComponent(LivedataDropdownMenu).exists()).toBe(true);
    expect(wrapper.findComponent(LivedataEditButton).exists()).toBe(false);
  });

  it("Displays the edit button when the source has an edit mode", () => {
    const { wrapper } = initWrapper({ hasEditMode: true });

    expect(wrapper.findComponent(LivedataEditButton).exists()).toBe(true);
    expect(wrapper.findComponent(LivedataDropdownMenu).exists()).toBe(true);
  });

  it("Displays the edit button before the dropdown menu", () => {
    const { wrapper } = initWrapper({ hasEditMode: true });
    const actions = wrapper.findAll("li");

    expect(actions).toHaveLength(2);
    expect(actions.at(0).findComponent(LivedataEditButton).exists()).toBe(true);
    expect(actions.at(1).findComponent(LivedataDropdownMenu).exists()).toBe(
      true,
    );
  });

  it("Displays the edit button as soon as the source declares an edit mode", async () => {
    const { wrapper, editModeSupported } = initWrapper();

    expect(wrapper.findComponent(LivedataEditButton).exists()).toBe(false);

    editModeSupported.value = true;
    await wrapper.vm.$nextTick();

    expect(wrapper.findComponent(LivedataEditButton).exists()).toBe(true);
  });
});

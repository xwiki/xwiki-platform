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

import LivedataDropdownMenu from "./LivedataDropdownMenu.vue";
import { mount } from "@vue/test-utils";
import { spy } from "sinon";
import { describe, expect, it } from "vitest";
import { ref } from "vue";

/**
 * Vue component initializer for `LivedataDropdownMenu` components. Calls `mount()` with
 * preconfigured values.
 *
 * @param maximized - whether the Live Data starts maximized
 * @returns a map containing a wrapper for `LivedataDropdownMenu` components and mocks of
 *   logic.changeLayout and logic.toggleMaximized
 */
function initWrapper({ maximized = false } = {}) {
  global.XWiki = {
    contextPath: "",
  };

  const changeLayout = spy();
  const isMaximized = ref(maximized);
  const toggleMaximized = spy(() => {
    isMaximized.value = !isMaximized.value;
  });
  const wrapper = mount(LivedataDropdownMenu, {
    global: {
      provide: {
        logic: {
          currentLayoutId: ref("cards"),
          data: {
            meta: {
              layouts: [{ id: "table" }, { id: "cards" }],
            },
          },
          changeLayout: changeLayout,
          isMaximized: () => isMaximized.value,
          toggleMaximized: toggleMaximized,
        },
      },
      mocks: {
        $t: (key) => key,
      },
      stubs: {
        XWikiIcon: true,
      },
    },
  });
  return { wrapper, changeLayout, toggleMaximized };
}

// Since the <li> elements does not have distinguishing attributes, we look for the layout items by
// looking at the elements located after the second separator (class dropdown-header). The lis
// passed here are both group lis and item lis. Group lis contain a title and their own item lis.
function findSecondDropdownHeaderIndex(lis) {
  var dropdownHeadersCptr = 0;
  var liIdx = 0;
  for (; liIdx < lis.length; liIdx++) {
    const li = lis.at(liIdx);
    if (li.findAll(".dropdown-header").length > 0) {
      dropdownHeadersCptr++;
    }
    if (dropdownHeadersCptr >= 2) {
      break;
    }
  }
  return liIdx;
}

describe("LivedataDropdownMenu.vue", () => {
  it("Current layout is greyed out", () => {
    const { wrapper } = initWrapper();
    const lis = wrapper.findAll("li");
    const dropDownHeaderIndex = findSecondDropdownHeaderIndex(lis);

    const tableLayout = lis.at(dropDownHeaderIndex + 1);
    const cardsLayout = lis.at(dropDownHeaderIndex + 2);

    expect(tableLayout.classes()).not.toContain("disabled");
    expect(cardsLayout.classes()).toContain("disabled");
  });

  it("Clicking on an enabled layout changes the layout", () => {
    const { wrapper, changeLayout } = initWrapper();
    const lis = wrapper.findAll("li");
    const dropDownHeaderIndex = findSecondDropdownHeaderIndex(lis);
    const tableLayout = lis.at(dropDownHeaderIndex + 1);

    tableLayout.find("a").trigger("click");

    expect(changeLayout.callCount).toBe(1);
    expect(changeLayout.calledWith("table")).toBe(true);
  });

  it("Clicking on a disabled layout does nothing", () => {
    const { wrapper, changeLayout } = initWrapper();
    const lis = wrapper.findAll("li");
    const dropDownHeaderIndex = findSecondDropdownHeaderIndex(lis);
    const cardsLayout = lis.at(dropDownHeaderIndex + 2);

    cardsLayout.find("a").trigger("click");

    expect(changeLayout.callCount).toBe(0);
  });

  it("Is not expanded by default", () => {
    const { wrapper } = initWrapper();
    expect(
      wrapper.find('[data-toggle="dropdown"]').attributes("aria-expanded"),
    ).toBe("false");
  });

  it("Offers to maximize the Live Data when it is not maximized", () => {
    const { wrapper } = initWrapper();
    expect(wrapper.find(".livedata-action-maximize").text()).toBe(
      "livedata.action.maximize",
    );
  });

  it("Offers to minimize the Live Data when it is maximized", () => {
    const { wrapper } = initWrapper({ maximized: true });
    expect(wrapper.find(".livedata-action-maximize").text()).toBe(
      "livedata.action.minimize",
    );
  });

  it("Clicking on the maximize action switches to the maximized view", async () => {
    const { wrapper, toggleMaximized } = initWrapper();

    await wrapper.find(".livedata-action-maximize").trigger("click");

    expect(toggleMaximized.callCount).toBe(1);
    expect(wrapper.find(".livedata-action-maximize").text()).toBe(
      "livedata.action.minimize",
    );
  });
});

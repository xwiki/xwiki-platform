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
import $ from "jquery";
import { spy } from "sinon";
import { afterEach, describe, expect, it } from "vitest";
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
        jQuery: $,
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

/**
 * Opens the dropdown of the given wrapper, with its button located at the given position in a
 * viewport that is 1000 pixels wide and a menu that is 300 pixels high.
 *
 * @param wrapper - the wrapper of the `LivedataDropdownMenu` component
 * @param buttonRect - the position of the dropdown button in the viewport
 * @param viewportHeight - the height of the viewport, in pixels
 */
async function openMenu(wrapper, buttonRect, viewportHeight = 800) {
  Object.defineProperty(document.documentElement, "clientWidth", {
    configurable: true,
    value: 1000,
  });
  Object.defineProperty(document.documentElement, "clientHeight", {
    configurable: true,
    value: viewportHeight,
  });
  wrapper.find(".dropdown-toggle").element.getBoundingClientRect = () =>
    buttonRect;
  Object.defineProperty(
    wrapper.find(".dropdown-menu").element,
    "offsetHeight",
    {
      configurable: true,
      value: 300,
    },
  );
  $(wrapper.find(".livedata-dropdown-menu").element).trigger(
    "shown.bs.dropdown",
  );
  await wrapper.vm.$nextTick();
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

  describe("Menu position", () => {
    afterEach(() => {
      delete document.documentElement.clientWidth;
      delete document.documentElement.clientHeight;
    });

    it("Displays the menu below its button, outside of the clipping containers", async () => {
      const { wrapper } = initWrapper();

      await openMenu(wrapper, { top: 100, bottom: 130, right: 900 });

      const menuStyle = wrapper.find(".dropdown-menu").element.style;
      expect(menuStyle.position).toBe("fixed");
      expect(menuStyle.top).toBe("130px");
      expect(menuStyle.bottom).toBe("auto");
      expect(menuStyle.right).toBe("100px");
    });

    it("Displays the menu above its button when there is no room below", async () => {
      const { wrapper } = initWrapper();

      await openMenu(wrapper, { top: 600, bottom: 630, right: 900 });

      const menuStyle = wrapper.find(".dropdown-menu").element.style;
      expect(menuStyle.position).toBe("fixed");
      expect(menuStyle.top).toBe("auto");
      expect(menuStyle.bottom).toBe("200px");
    });

    it("Limits the menu height to the room left in a viewport too small for it", async () => {
      const { wrapper } = initWrapper();

      await openMenu(wrapper, { top: 100, bottom: 130, right: 900 }, 330);

      const menuStyle = wrapper.find(".dropdown-menu").element.style;
      expect(menuStyle.top).toBe("130px");
      expect(menuStyle.maxHeight).toBe("200px");
      expect(menuStyle.overflowY).toBe("auto");
    });

    it("Moves the menu along with its button when scrolling", async () => {
      const { wrapper } = initWrapper();
      await openMenu(wrapper, { top: 100, bottom: 130, right: 900 });

      wrapper.find(".dropdown-toggle").element.getBoundingClientRect = () => ({
        top: 50,
        bottom: 80,
        right: 900,
      });
      document.body.dispatchEvent(new Event("scroll"));
      await wrapper.vm.$nextTick();

      expect(wrapper.find(".dropdown-menu").element.style.top).toBe("80px");
    });

    it("Resets the menu position when it is closed", async () => {
      const { wrapper } = initWrapper();
      await openMenu(wrapper, { top: 100, bottom: 130, right: 900 });

      $(wrapper.find(".livedata-dropdown-menu").element).trigger(
        "hidden.bs.dropdown",
      );
      await wrapper.vm.$nextTick();

      expect(wrapper.find(".dropdown-menu").element.style.position).toBe("");
    });
  });
});

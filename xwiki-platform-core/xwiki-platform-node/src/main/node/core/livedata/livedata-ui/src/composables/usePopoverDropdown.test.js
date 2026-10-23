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

import { usePopoverDropdown } from "./usePopoverDropdown";
import { mount } from "@vue/test-utils";
import { spy } from "sinon";
import { describe, expect, it } from "vitest";
import { h, ref } from "vue";

/**
 * Mounts a component calling `usePopoverDropdown` with a menu of two items, the first one disabled,
 * and records the calls to the popover methods of the menu, which the test DOM doesn't implement.
 *
 * @param closeOnClick - whether the menu closes on any click inside it
 * @returns a map containing the wrapper of the component, the result of `usePopoverDropdown`, and
 *   spies of the showPopover and hidePopover methods of the menu
 */
function initWrapper(closeOnClick) {
  let dropdown;
  const wrapper = mount(
    {
      setup() {
        const menu = ref(null);
        dropdown = usePopoverDropdown(menu, closeOnClick);
        return () =>
          h("ul", { ref: menu }, [
            h("li", { class: "disabled" }, h("a", { href: "#" }, "Disabled")),
            h("li", h("a", { href: "#", class: "first" }, "First")),
          ]);
      },
    },
    { attachTo: document.body },
  );
  const showPopover = spy();
  const hidePopover = spy();
  Object.assign(wrapper.element, { showPopover, hidePopover });
  return { wrapper, dropdown, showPopover, hidePopover };
}

describe("usePopoverDropdown", () => {
  it("Gives a distinct menu identifier to each dropdown", () => {
    let first, second;
    mount({
      setup() {
        first = usePopoverDropdown(ref(null));
        second = usePopoverDropdown(ref(null));
        return () => null;
      },
    });

    expect(first.menuId).toBeTruthy();
    expect(first.menuId).not.toBe(second.menuId);
  });

  it("Follows the open state of the menu", () => {
    const { wrapper, dropdown } = initWrapper();
    expect(dropdown.isOpen.value).toBe(false);

    dropdown.onMenuToggle({ newState: "open" });
    expect(dropdown.isOpen.value).toBe(true);

    dropdown.onMenuToggle({ newState: "closed" });
    expect(dropdown.isOpen.value).toBe(false);
    wrapper.unmount();
  });

  it("Closes the menu on a click inside it", () => {
    const { wrapper, dropdown, hidePopover } = initWrapper();

    dropdown.onMenuClick();

    expect(hidePopover.callCount).toBe(1);
    wrapper.unmount();
  });

  it("Keeps the menu open on a click inside it while asked to", () => {
    const closeOnClick = ref(false);
    const { wrapper, dropdown, hidePopover } = initWrapper(closeOnClick);

    dropdown.onMenuClick();
    expect(hidePopover.callCount).toBe(0);

    closeOnClick.value = true;
    dropdown.onMenuClick();
    expect(hidePopover.callCount).toBe(1);
    wrapper.unmount();
  });

  it("Opens a closed menu and focuses its first enabled item on the arrow keys", () => {
    const { wrapper, dropdown, showPopover } = initWrapper();

    dropdown.onToggleArrowKey();

    expect(showPopover.callCount).toBe(1);
    expect(document.activeElement).toBe(wrapper.find(".first").element);
    wrapper.unmount();
  });

  it("Only focuses the first enabled item of an open menu on the arrow keys", () => {
    const { wrapper, dropdown, showPopover } = initWrapper();
    dropdown.onMenuToggle({ newState: "open" });

    dropdown.onToggleArrowKey();

    expect(showPopover.callCount).toBe(0);
    expect(document.activeElement).toBe(wrapper.find(".first").element);
    wrapper.unmount();
  });

  it("Keeps Escape from the page while the menu is open", () => {
    const { wrapper, dropdown } = initWrapper();
    const closedEscape = new KeyboardEvent("keydown", { key: "Escape" });
    const openEscape = new KeyboardEvent("keydown", { key: "Escape" });
    const stopped = [closedEscape, openEscape].map((event) =>
      spy(event, "stopPropagation"),
    );

    dropdown.onToggleEscape(closedEscape);
    dropdown.onMenuToggle({ newState: "open" });
    dropdown.onToggleEscape(openEscape);

    expect(stopped.map((stop) => stop.callCount)).toEqual([0, 1]);
    wrapper.unmount();
  });
});

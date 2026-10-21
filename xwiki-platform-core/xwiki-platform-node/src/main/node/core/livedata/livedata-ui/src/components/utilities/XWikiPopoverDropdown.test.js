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

import XWikiPopoverDropdown from "./XWikiPopoverDropdown.vue";
import { mount } from "@vue/test-utils";
import { spy } from "sinon";
import { describe, expect, it } from "vitest";
import { h } from "vue";

/**
 * Mounts a `XWikiPopoverDropdown` component with a toggle label and two menu items.
 *
 * @param props - the props of the component
 * @returns the wrapper of the component
 */
function initWrapper(props = {}) {
  return mount(XWikiPopoverDropdown, {
    props,
    slots: {
      toggle: "Actions",
      default: '<li><a href="#" class="first">First</a></li><li>Second</li>',
    },
  });
}

/**
 * Mounts a `XWikiPopoverDropdown` component in the document, so that its elements can get the focus,
 * and records the calls to the showPopover method of its menu, which the test DOM doesn't implement.
 *
 * @returns a map containing the wrapper of the component and a spy of the showPopover method
 */
function initAttachedWrapper() {
  const wrapper = initWrapper();
  document.body.appendChild(wrapper.element);
  const showPopover = spy();
  wrapper.find(".dropdown-menu").element.showPopover = showPopover;
  return { wrapper, showPopover };
}

/**
 * Dispatches the beforetoggle event that the browser fires when the menu popover opens or closes.
 *
 * @param wrapper - the wrapper of the component
 * @param newState - the new state of the popover, "open" or "closed"
 */
async function toggleMenu(wrapper, newState) {
  const event = new Event("beforetoggle");
  event.newState = newState;
  wrapper.find(".dropdown-menu").element.dispatchEvent(event);
  await wrapper.vm.$nextTick();
}

describe("XWikiPopoverDropdown.vue", () => {
  it("Displays a Bootstrap dropdown toggle and menu", () => {
    const wrapper = initWrapper({ toggleTitle: "More actions" });
    const toggle = wrapper.find("button");

    expect(toggle.classes()).toEqual(["dropdown-toggle", "btn", "btn-default"]);
    expect(toggle.attributes("type")).toBe("button");
    expect(toggle.attributes("title")).toBe("More actions");
    expect(toggle.text()).toBe("Actions");
    expect(wrapper.findAll(".dropdown-menu > li").length).toBe(2);
  });

  it("Applies the given toggle and menu classes", () => {
    const wrapper = initWrapper({
      toggleClass: "btn btn-primary",
      menuClass: "dropdown-menu-right",
    });

    expect(wrapper.find(".dropdown-toggle").classes()).toContain("btn-primary");
    expect(wrapper.find(".dropdown-menu").classes()).toContain(
      "dropdown-menu-right",
    );
  });

  it("Opens the menu as a popover of its toggle", () => {
    const wrapper = initWrapper();
    const menu = wrapper.find(".dropdown-menu");

    expect(menu.attributes()).toHaveProperty("popover");
    expect(wrapper.find(".dropdown-toggle").attributes("popovertarget")).toBe(
      menu.attributes("id"),
    );
  });

  it("Gives a distinct menu to each dropdown", () => {
    const wrapper = mount({
      render: () => [h(XWikiPopoverDropdown), h(XWikiPopoverDropdown)],
    });
    const [first, second] = wrapper.findAll(".dropdown-toggle");

    expect(first.attributes("popovertarget")).not.toBe(
      second.attributes("popovertarget"),
    );
  });

  it("Is marked as open and expanded while the menu is open", async () => {
    const wrapper = initWrapper();
    const toggle = wrapper.find(".dropdown-toggle");
    expect(toggle.attributes("aria-expanded")).toBe("false");

    await toggleMenu(wrapper, "open");
    expect(wrapper.classes()).toContain("open");
    expect(toggle.attributes("aria-expanded")).toBe("true");

    await toggleMenu(wrapper, "closed");
    expect(wrapper.classes()).not.toContain("open");
    expect(toggle.attributes("aria-expanded")).toBe("false");
  });

  it("Closes the menu on a click inside it", async () => {
    const wrapper = initWrapper();
    const hidePopover = spy();
    wrapper.find(".dropdown-menu").element.hidePopover = hidePopover;

    await wrapper.find(".first").trigger("click");

    expect(hidePopover.callCount).toBe(1);
  });

  it("Keeps the menu open on a click inside it when asked to", async () => {
    const wrapper = initWrapper({ closeOnClick: false });
    const hidePopover = spy();
    wrapper.find(".dropdown-menu").element.hidePopover = hidePopover;

    await wrapper.find(".first").trigger("click");

    expect(hidePopover.callCount).toBe(0);
  });

  describe("Keyboard", () => {
    it("Keeps only Escape from the Bootstrap dropdown script", async () => {
      const wrapper = initWrapper();
      // The Bootstrap dropdown script listens to the keys reaching the document.
      const parentKeydown = spy();
      wrapper.element.addEventListener("keydown", parentKeydown);
      const menu = wrapper.find(".dropdown-menu");

      for (const key of ["Escape", "ArrowUp", "ArrowDown", " "]) {
        await menu.trigger("keydown", { key });
      }

      expect(parentKeydown.args.map(([event]) => event.key)).toEqual([
        "ArrowUp",
        "ArrowDown",
        " ",
      ]);
    });

    it("Opens the menu and focuses its first item on the arrow keys", async () => {
      const { wrapper, showPopover } = initAttachedWrapper();

      await wrapper
        .find(".dropdown-toggle")
        .trigger("keydown", { key: "ArrowDown" });

      expect(showPopover.callCount).toBe(1);
      expect(document.activeElement).toBe(wrapper.find(".first").element);
      wrapper.unmount();
    });

    it("Only focuses the first item of an open menu on the arrow keys", async () => {
      const { wrapper, showPopover } = initAttachedWrapper();
      await toggleMenu(wrapper, "open");

      await wrapper
        .find(".dropdown-toggle")
        .trigger("keydown", { key: "ArrowUp" });

      expect(showPopover.callCount).toBe(0);
      expect(document.activeElement).toBe(wrapper.find(".first").element);
      wrapper.unmount();
    });

    it("Keeps Escape on its toggle from the page while the menu is open", async () => {
      const wrapper = initWrapper();
      // The page keyboard shortcuts listen to the keys reaching the document.
      const parentKeydown = spy();
      wrapper.element.addEventListener("keydown", parentKeydown);
      const toggle = wrapper.find(".dropdown-toggle");

      await toggle.trigger("keydown", { key: "Escape" });
      await toggleMenu(wrapper, "open");
      await toggle.trigger("keydown", { key: "Escape" });

      expect(parentKeydown.callCount).toBe(1);
    });
  });
});

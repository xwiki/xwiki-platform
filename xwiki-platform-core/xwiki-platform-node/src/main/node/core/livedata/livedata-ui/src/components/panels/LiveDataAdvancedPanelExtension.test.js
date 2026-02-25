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
import LiveDataAdvancedPanelExtension from "./LiveDataAdvancedPanelExtension.vue";
import { mount } from "@vue/test-utils";
import { nextTick } from "vue";
import { describe, expect, it } from "vitest";

/**
 * Vue Component initializer for LiveDataAdvancedPanelExtension component.
 *
 * @param container the container to use for the extension
 * @returns {*} a wrapper for the LiveDataAdvancedPanelExtension component
 */
function initWrapper({ container }) {
  global.XWiki = { contextPath: "http://localhost/" };
  return mount(LiveDataAdvancedPanelExtension, {
    global: {
      provide: {
        logic: {
          openedPanels: ["extensionPanel"],
          uniqueArrayHas(uniqueArray, item) {
            return uniqueArray.includes(item);
          },
        },
      },
    },
    props: {
      panel: {
        id: "extensionPanel",
        title: "Test Panel",
        container: container,
        component: "LiveDataAdvancedPanelExtension",
        icon: "camera",
      },
    },
    stubs: { XWikiIcon: true },
  });
}

describe("LiveDataAdvancedPanelExtension.vue", () => {
  const container = document.createElement("span");
  container.classList.add("span-test");
  container.textContent = "Hello World!";

  it("Displays the given container", async () => {
    const wrapper = initWrapper({ container });
    expect(wrapper.find(".panel-heading").text()).toBe("Test Panel");
    expect(wrapper.find(".extension-body > *").element).toBe(container);
  });

  it("Re-displays the given container after toggling the visibility", async () => {
    const wrapper = initWrapper({ container });
    expect(wrapper.find(".extension-body > *").element).toBe(container);
    await wrapper.find(".collapse-button").trigger("click");
    await nextTick();
    expect(wrapper.find(".span-test").exists()).toBe(false);
    await wrapper.find(".collapse-button").trigger("click");
    await nextTick();
    expect(wrapper.find(".span-test").exists()).toBe(true);
  });

  it("Updates the container when it is exchanged", async () => {
    const wrapper = initWrapper({ container });
    expect(wrapper.find(".span-test").exists()).toBe(true);
    const newContainer = document.createElement("p");
    newContainer.textContent = "Changed!";
    newContainer.classList.add("span-test-bis");
    wrapper.vm.panel.container = newContainer;
    await nextTick();
    expect(wrapper.find(".span-test").exists()).toBe(false);
    expect(wrapper.find(".span-test-bis").exists()).toBe(true);
  });
});

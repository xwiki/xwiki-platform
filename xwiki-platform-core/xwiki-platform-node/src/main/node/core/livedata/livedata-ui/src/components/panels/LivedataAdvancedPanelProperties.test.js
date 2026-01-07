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
import LivedataAdvancedPanelProperties from "./LivedataAdvancedPanelProperties.vue";
import { mount } from "@vue/test-utils";
import _ from "lodash-es";
import { describe, expect, it } from "vitest";
import { nextTick } from "vue";

/**
 * Vue Component initializer for LiveDataAdvancedPanelProperties component.
 *
 * @param provide (optional) an object that is merged on top of the default provide parameter.
 * @returns {*} a wrapper for the LiveDataAdvancedPanelProperties component
 */
function initWrapper({ provide } = {}) {
  global.XWiki = { contextPath: "http://localhost/" };
  let propertyIsVisible = true;
  return mount(LivedataAdvancedPanelProperties, {
    props: {
      panel: {
        id: "propertiesPanel",
        title: "Properties",
        component: "LivedataAdvancedPanelProperties",
        icon: "list-bullets",
      },
    },

    global: {
      // renderStubDefaultSlot: true,
      provide: _.merge({
        logic: {
          openedPanels: ["propertiesPanel"],
          uniqueArrayHas(uniqueArray, item) {
            return uniqueArray.includes(item);
          },
          data: {
            query: {
              properties: ["id"],
            },
          },
          isPropertyVisible() {
            return propertyIsVisible;
          },
          setPropertyVisible(propertyId, visible) {
            propertyIsVisible = visible;
          },
          getPropertyDescriptors() {
            return [{ id: "id", name: "Property Name" }];
          },
        },
      }, provide),
      stubs: {
        XWikiIcon: {
          name: "XWikiIcon",
          props: {
            iconDescriptor: Object,
          },
          template: "<i>{{ iconDescriptor.name }}</i>",
        },
      },
    },
  });
}

describe("LivedataAdvancedPanelProperties.vue", () => {
  it("Displays the title and the icon", async () => {
    const wrapper = initWrapper();
    expect(wrapper.find(".panel-heading .title").text()).toBe("list-bullets Properties");
  });

  it("Displays the properties", async () => {
    const wrapper = initWrapper();
    expect(wrapper.find(".property .property-name").text()).toBe("Property Name");
  });

  it("Toggles the visibility on click", async () => {
    const wrapper = initWrapper();
    expect(wrapper.find("input[type = checkbox]").element.checked).toBe(true);
    await wrapper.find(".visibility input").setChecked(false);
    expect(wrapper.vm.logic.isPropertyVisible("id")).toBeFalsy();
    await nextTick();
    expect(wrapper.find("input[type = checkbox]").element.checked).toBe(false);
  });
});

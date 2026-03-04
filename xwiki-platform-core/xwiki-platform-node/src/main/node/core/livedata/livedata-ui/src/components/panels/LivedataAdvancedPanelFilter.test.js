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
import LivedataAdvancedPanelFilter from "./LivedataAdvancedPanelFilter.vue";
import { mount } from "@vue/test-utils";
import _ from "lodash-es";
import { describe, expect, it } from "vitest";

/**
 * Vue Component initializer for LiveDataAdvancedPanelFilter component.
 *
 * @param provide (optional) an object that is merged on top of the default provide parameter.
 * @returns {*} a wrapper for the LiveDataAdvancedPanelFilter component
 */
function initWrapper({ provide } = {}) {

  return mount(LivedataAdvancedPanelFilter, {
    props: {
      panel: {
        id: "filterPanel",
        title: "Filter",
        component: "LivedataAdvancedPanelFilter",
        icon: "filter",
      },
    },
    global: {
      provide: _.merge({
        logic: {
          openedPanels: ["filterPanel"],
          uniqueArrayHas(uniqueArray, item) {
            return uniqueArray.includes(item);
          },
          getUnfilteredProperties() {
            return [];
          },
          getFilterableProperties() {
            return ["id"];
          },
          data: {
            query: {
              filters: [],
            },
          },
        },
      }, provide),
      stubs: {
        XWikiIcon: {
          props: {
            iconDescriptor: Object,
          },
          template: "<i>{{ iconDescriptor.name }}</i>",
        },
      }, mocks: {
        $t: (key) => key,
      },
    },
  });
}

describe("LivedataAdvancedPanelFilter.vue", () => {
  it("Displays the title and the icon", async () => {
    const wrapper = initWrapper();
    expect(wrapper.find(".panel-heading .title").text()).toBe("filter Filter");
  });

  it("Displays no message when filterable properties exist", async () => {
    const wrapper = initWrapper();
    expect(wrapper.find(".text-muted").text()).toBe("livedata.panel.filter.noneFilterable");
    expect(wrapper.find(".text-muted").attributes("style")).toBe("display: none;");
  });

  it("Displays a message when no filterable properties exist", async () => {
    const wrapper = initWrapper({
      provide: {
        logic: {
          getFilterableProperties() {
            return [];
          },
        },
      },
    });
    expect(wrapper.find(".text-muted").element.tagName).toBe("DIV");
  });
});

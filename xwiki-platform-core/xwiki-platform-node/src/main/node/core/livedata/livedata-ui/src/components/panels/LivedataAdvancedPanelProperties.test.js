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
import LivedataAdvancedPanelProperties from "./LivedataAdvancedPanelProperties.vue";
import { mount } from "@vue/test-utils";
import { assertAxe } from "@xwiki/platform-test-accessibility";
import _ from "lodash-es";
import { describe, expect, it } from "vitest";
import { nextTick, reactive } from "vue";

/**
 * Vue Component initializer for LiveDataAdvancedPanelProperties component.
 *
 * @param provide - (optional) an object that is merged on top of the default provide parameter.
 * @returns a wrapper for the LiveDataAdvancedPanelProperties component
 */
function initWrapper({ provide } = {}) {
  global.XWiki = { contextPath: "http://localhost/" };
  let propertyIsVisible = true;
  return mount(LivedataAdvancedPanelProperties, {
    attachTo: document.body,
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
      provide: _.merge(
        {
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
        },
        provide,
      ),
      stubs: {
        XWikiIcon: {
          name: "XWikiIcon",
          props: {
            iconDescriptor: Object,
          },
          template: "<i>{{ iconDescriptor.name }}</i>",
        },
      },
      mocks: {
        $t: (key) => {
          const map = {
            "livedata.panel.heading.actions.collapse.hint": "Collapse",
            "livedata.panel.heading.actions.close.hint": "Close",
            "livedata.panel.properties.useCover": "Use cover",
            "livedata.panel.properties.cover": "Cover",
            "livedata.panel.properties.cover.hint": "Use as the card cover",
          };
          return map[key] || "unexpected key";
        },
      },
    },
  });
}

describe("LivedataAdvancedPanelProperties.vue", () => {
  it("Displays the title and the icon", async () => {
    const wrapper = initWrapper();
    expect(wrapper.find(".panel-heading .title").text()).toBe(
      "list-bullets Properties",
    );
    await assertAxe(wrapper);
  });

  it("Displays the properties", async () => {
    const wrapper = initWrapper();
    expect(wrapper.find(".property .property-name").text()).toBe(
      "Property Name",
    );
    await assertAxe(wrapper);
  });

  it("Toggles the visibility on click", async () => {
    const wrapper = initWrapper();
    expect(wrapper.find("input[type = checkbox]").element.checked).toBe(true);
    await wrapper.find(".visibility input").setChecked(false);
    expect(wrapper.vm.logic.isPropertyVisible("id")).toBeFalsy();
    await nextTick();
    expect(wrapper.find("input[type = checkbox]").element.checked).toBe(false);
    await assertAxe(wrapper);
  });

  it("Doesn't offer the card cover outside of the cards layout", () => {
    const wrapper = initWrapper({
      provide: { logic: { currentLayoutId: { value: "table" } } },
    });
    expect(wrapper.find(".use-cover").exists()).toBe(false);
    expect(wrapper.find("input[type = radio]").exists()).toBe(false);
  });

  describe("Card cover", () => {
    /**
     * @returns a properties panel in the cards layout, with two properties, the second one holding an image
     */
    function initCardsWrapper() {
      const cardsDescriptor = reactive({ id: "cards", coverProperty: "" });
      const wrapper = initWrapper({
        provide: {
          logic: {
            currentLayoutId: { value: "cards" },
            data: {
              id: "ld",
              query: { properties: ["title", "thumbnail"] },
              data: {
                entries: [{ title: "A", thumbnail: '<img src="/a.png">' }],
              },
            },
            getLayoutDescriptor: (layoutId) =>
              layoutId === "cards" ? cardsDescriptor : undefined,
            getPropertyDescriptor: (id) => ({ id, visible: true }),
            getPropertyDescriptors: () => [
              { id: "title", name: "Title" },
              { id: "thumbnail", name: "Thumbnail" },
            ],
          },
        },
      });
      return { wrapper, cardsDescriptor };
    }

    it("Only displays the cover selection when the cover is used", async () => {
      const { wrapper } = initCardsWrapper();
      const useCover = wrapper.find(".use-cover input");
      expect(useCover.element.checked).toBe(false);
      expect(wrapper.find(".use-cover").text()).toBe("Use cover");
      expect(wrapper.findAll("input[type = radio]")).toHaveLength(0);
      await assertAxe(wrapper);
    });

    it("Selects the property holding an image when enabling the cover", async () => {
      const { wrapper, cardsDescriptor } = initCardsWrapper();
      await wrapper.find(".use-cover input").setChecked(true);
      expect(cardsDescriptor.coverProperty).toBe("thumbnail");
      const radios = wrapper.findAll("input[type = radio]");
      expect(radios).toHaveLength(2);
      expect(radios.map((radio) => radio.element.checked)).toStrictEqual([
        false,
        true,
      ]);
      expect(radios[0].attributes("name")).toBe("ld-card-cover");
      await assertAxe(wrapper);
    });

    it("Changes and removes the cover", async () => {
      const { wrapper, cardsDescriptor } = initCardsWrapper();
      await wrapper.find(".use-cover input").setChecked(true);
      await wrapper.findAll("input[type = radio]")[0].setChecked(true);
      expect(cardsDescriptor.coverProperty).toBe("title");
      await wrapper.find(".use-cover input").setChecked(false);
      expect(cardsDescriptor.coverProperty).toBe("");
      expect(wrapper.findAll("input[type = radio]")).toHaveLength(0);
      // The last cover is selected again when enabling the cover back.
      await wrapper.find(".use-cover input").setChecked(true);
      expect(cardsDescriptor.coverProperty).toBe("title");
    });
  });
});

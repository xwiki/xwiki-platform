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
import LayoutCardsCard from "./LayoutCardsCard.vue";
import LayoutCardsCover from "./LayoutCardsCover.vue";
import { shallowMount } from "@vue/test-utils";
import _ from "lodash-es";
import { describe, expect, it } from "vitest";
import { nextTick } from "vue";

/**
 * Vue Component initializer for LayoutCardsCard component. Since this component creates a deep
 * hierarchy of sub-components, we use `shallowMount` to initialize the wrapper.
 *
 * The default option object is merged with the `option` parameter.
 *
 * @param options - an optional option object use to customize the initialized component
 * @returns a shallow wrapper for the LayoutCardsCard component
 */
function initWrapper(options = {}) {
  return shallowMount(
    LayoutCardsCard,
    _.merge(
      {
        global: {
          provide: {
            logic: {
              getEntryId: (e) => e.id,
              isEditMode: () => false,
              isSelectionEnabled: () => false,
              getLayoutDescriptor: () => {
                return { titleProperty: "title" };
              },
              isPropertyVisible: () => false,
              getPropertyDescriptors: () => {
                return [];
              },
              data: {
                query: {
                  properties: [],
                },
              },
            },
          },
          mocks: {
            $t: (key) => key,
          },
        },
      },
      options,
    ),
  );
}

describe("LayoutCardsCard.vue", () => {
  it("Root element had an index and an id", async () => {
    const wrapper = initWrapper({
      propsData: {
        entry: {
          id: "idA",
          title: "title",
        },
        entryIdx: 1,
      },
    });
    expect(wrapper.attributes("data-livedata-entry-index")).toBe("1");
    expect(wrapper.attributes("data-livedata-entry-id")).toBe("idA");
  });

  it("Has no cover by default", () => {
    const wrapper = initWrapper({
      propsData: { entry: { id: "idA", title: "title" }, entryIdx: 0 },
    });
    expect(wrapper.findComponent(LayoutCardsCover).exists()).toBe(false);
    expect(wrapper.classes()).not.toContain("card-with-cover");
  });

  describe("With a cover", () => {
    /**
     * @param entry - the entry displayed by the card
     * @returns a card whose cover property is "thumbnail" and title property is "title"
     */
    function initCoverWrapper(entry) {
      return initWrapper({
        propsData: { entry, entryIdx: 0 },
        global: {
          provide: {
            logic: {
              getLayoutDescriptor: () => ({
                titleProperty: "title",
                coverProperty: "thumbnail",
              }),
              isPropertyVisible: () => true,
              getPropertyDescriptor: (id) => ({ id, visible: true }),
              data: { query: { properties: ["title", "thumbnail"] } },
            },
          },
        },
      });
    }

    it("Displays the image of the cover property", () => {
      const wrapper = initCoverWrapper({
        id: "idA",
        title: "Title A",
        thumbnail: '<img src="/a.png" alt="A">',
      });
      expect(wrapper.classes()).toContain("card-with-cover");
      expect(wrapper.findComponent(LayoutCardsCover).props()).toMatchObject({
        image: { src: "/a.png", alt: "A" },
      });
      // The title is still displayed below the image.
      expect(wrapper.find("h2").exists()).toBe(true);
    });

    it("Displays the title enlarged when there is no image", () => {
      const wrapper = initCoverWrapper({
        id: "idA",
        title: "Title A",
        thumbnail: "",
      });
      expect(wrapper.findComponent(LayoutCardsCover).props()).toMatchObject({
        image: undefined,
        textPropertyId: "title",
      });
      // The title is not displayed twice.
      expect(wrapper.find("h2").exists()).toBe(false);
    });

    it("Falls back on the text when the image fails to load", async () => {
      const wrapper = initCoverWrapper({
        id: "idA",
        title: "Title A",
        thumbnail: "/missing.png",
      });
      const cover = wrapper.findComponent(LayoutCardsCover);
      expect(cover.props("image")).toEqual({ src: "/missing.png", alt: "" });
      cover.vm.$emit("imageError");
      await nextTick();
      expect(cover.props("image")).toBeUndefined();
      expect(wrapper.find("h2").exists()).toBe(false);
    });
  });
});

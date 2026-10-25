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
import LayoutList from "./LayoutList.vue";
import LayoutListItem from "./LayoutListItem.vue";
import LivedataActions from "../../LivedataActions.vue";
import LivedataPagination from "../../LivedataPagination.vue";
import { shallowMount } from "@vue/test-utils";
import _ from "lodash-es";
import { describe, expect, it } from "vitest";
import { h, nextTick } from "vue";

/**
 * Vue Component initializer for LayoutList component. Since this component creates a deep
 * hierarchy of sub-components, we use `shallowMount` to initialize the wrapper.
 *
 * @param options - an optional option object use to customize the initialized component
 * @param afterEntryFetchWrapper - wrapper for the afterEntryFetch event. The callback is stored in
 *   the callback field of the object passed in parameter
 * @returns a shallow wrapper for the LayoutList component
 */
function initWrapper({ options, afterEntryFetchWrapper } = {}) {
  return shallowMount(
    LayoutList,
    _.merge(
      {
        global: {
          provide: {
            logic: {
              getEntryId: (e) => e.id,
              getEntryKeys: (entries) =>
                entries.map((entry, idx) => `key-${entry.id}-${idx}`),
              canAddEntry: () => false,
              addEntry: () => {},
              isSelectionEnabled: () => false,
              // Only the second property is visible.
              getPropertyDescriptor: (propertyId) => ({
                id: propertyId,
                visible: propertyId === "title",
              }),
              onEvent: (eventName, callback) => {
                if (afterEntryFetchWrapper) {
                  afterEntryFetchWrapper.callback = callback;
                }
              },
              data: {
                query: {
                  properties: ["id", "title"],
                },
                data: {
                  entries: [],
                },
              },
            },
          },
          mocks: {
            $t: (key) => key,
          },
          renderStubDefaultSlot: true,
          stubs: {
            // Render the named slots of the top bar, holding the pagination.
            LivedataTopbar: {
              setup(props, { slots }) {
                return () => h("div", [slots.left?.(), slots.right?.()]);
              },
            },
          },
        },
      },
      options,
    ),
  );
}

describe("LayoutList.vue", () => {
  it("Renders when no entries", async () => {
    const afterEntryFetchWrapper = {};
    const wrapper = initWrapper({ afterEntryFetchWrapper });
    // The message is only displayed once the entries are fetched.
    expect(wrapper.find(".noentries-list").exists()).toBe(false);
    afterEntryFetchWrapper.callback();
    await nextTick();
    expect(wrapper.find(".noentries-list").text()).toBe(
      "livedata.bottombar.noEntries",
    );
  });

  it("Renders the entries with the first visible property", async () => {
    const afterEntryFetchWrapper = {};
    const wrapper = initWrapper({
      options: {
        global: {
          provide: {
            logic: {
              data: {
                data: {
                  entries: [{ id: 1 }, { id: 2 }],
                },
              },
            },
          },
        },
      },
      afterEntryFetchWrapper,
    });
    afterEntryFetchWrapper.callback();
    await nextTick();
    const items = wrapper.findAllComponents(LayoutListItem);
    expect(items.length).toBe(2);
    expect(items.at(1).props()).toStrictEqual({
      entry: { id: 2 },
      entryIdx: 1,
      propertyId: "title",
      secondaryPropertyIds: [],
    });
    expect(wrapper.find(".noentries-list").exists()).toBe(false);
  });

  it("Renders the other visible properties as secondary properties", () => {
    const wrapper = initWrapper({
      options: {
        global: {
          provide: {
            logic: {
              getPropertyDescriptor: (propertyId) => ({
                id: propertyId,
                visible: propertyId !== "hidden",
              }),
              data: {
                query: { properties: ["title", "hidden", "year", "genre"] },
                data: { entries: [{ id: 1 }] },
              },
            },
          },
        },
      },
    });
    expect(wrapper.findComponent(LayoutListItem).props()).toMatchObject({
      propertyId: "title",
      secondaryPropertyIds: ["year", "genre"],
    });
  });

  it("Displays a marker before the entries only when they display a single property", () => {
    const initList = (properties) =>
      initWrapper({
        options: {
          global: {
            provide: {
              logic: {
                getPropertyDescriptor: (propertyId) => ({
                  id: propertyId,
                  visible: true,
                }),
                data: { query: { properties }, data: { entries: [{ id: 1 }] } },
              },
            },
          },
        },
      });
    expect(initList(["title"]).find(".layout-list-root").classes()).toContain(
      "layout-list-single",
    );
    expect(
      initList(["title", "year"]).find(".layout-list-root").classes(),
    ).not.toContain("layout-list-single");
  });

  it("Displays the actions of the Live Data in the top bar, like the other layouts", () => {
    // They include the edit mode button, and the dropdown menu, with the same spacing as in the other layouts.
    expect(initWrapper().findComponent(LivedataActions).exists()).toBe(true);
  });

  it("Offers to add an entry only when it is possible", async () => {
    expect(initWrapper().find(".layout-list-new-item").exists()).toBe(false);

    let added = 0;
    const wrapper = initWrapper({
      options: {
        global: {
          provide: {
            logic: {
              canAddEntry: () => true,
              addEntry: () => {
                added++;
              },
            },
          },
        },
      },
    });
    const newItem = wrapper.find(".layout-list-new-item");
    expect(newItem.text()).toContain("livedata.action.addEntry");
    await newItem.find("a").trigger("click");
    expect(added).toBe(1);
  });

  it("Has pagination at the top and at the bottom", () => {
    const wrapper = initWrapper();
    // Left and right of the top bar, and the bottom bar.
    expect(wrapper.findAllComponents(LivedataPagination).length).toBe(3);
  });
});

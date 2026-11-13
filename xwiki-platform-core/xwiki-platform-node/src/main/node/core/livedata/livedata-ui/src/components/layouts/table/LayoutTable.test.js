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
import LayoutTable from "./LayoutTable.vue";
import LayoutTableRow from "./LayoutTableRow.vue";
import displayerMixin from "../../displayers/displayerMixin";
import { LiveDataLogic } from "../../services/LiveDataLogic";
import { jsonMerge } from "../../services/jsonMerge";
import { shallowMount } from "@vue/test-utils";
import { componentStore } from "@xwiki/platform-livedata-componentstore";
import flushPromises from "flush-promises";
import _ from "lodash-es";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { h, nextTick } from "vue";

vi.mock("vue-i18n", () => ({
  useI18n: () => ({ t: (key) => key }),
}));

// Count the descriptor merges, which are made when a displayer descriptor is resolved.
vi.mock("../../services/jsonMerge", async (importOriginal) => {
  const { jsonMerge } = await importOriginal();
  return { jsonMerge: vi.fn(jsonMerge) };
});

/**
 * Vue Component initializer for LayoutTable component. Since this component creates a deep
 * hierarchy of sub-components, we use `shallowMount` to initialize the wrapper.
 *
 * The default option object is:
 * ```
 * {
 *   provide: {
 *     logic: {
 *       canAddEntry: () => false,
 *       getEntryId: (e) => e.id,
 *       getEntryKeys: (entries) => entries.map((e) => `entry-${e.id}`),
 *       data: {
 *         data: {
 *           entries: []
 *         }
 *       }
 *     }
 *   },
 *   mocks: {
 *     $t: (key) => key
 *   }
 * }
 * ```
 * The default option object is merged with the `option` parameter.
 *
 * @param options - an optional option object use to customize the initialized component
 * @param afterEntryFetchWrapper - wrapper for the afterEntryFetch event. The callback is stored in
 *   the callback field of the object passed in parameter
 * @returns a shallow wrapper for the LayoutTable component
 */
function initWrapper({ options, afterEntryFetchWrapper }) {
  return shallowMount(
    LayoutTable,
    _.merge(
      {
        global: {
          provide: {
            logic: {
              canAddEntry: () => false,
              getEntryId: (e) => e.id,
              getEntryKeys: (entries) => entries.map((e) => `entry-${e.id}`),
              onEvent: (eventName, callback) => {
                if (afterEntryFetchWrapper) {
                  afterEntryFetchWrapper.callback = callback;
                }
              },
              getPropertyDescriptors: () => [],
              data: {
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
        },
      },
      options,
    ),
  );
}

describe("LayoutTable.vue", () => {
  it("Renders when no entries", async () => {
    const afterEntryFetchWrapper = {};
    const wrapper = initWrapper({ afterEntryFetchWrapper });
    // Before afterEntryFetch is fired, the message is not displayed, and is only displayed once
    // data are fetched but the number of entries is still 0.
    expect(wrapper.findAll(".noentries-table").length).toBe(0);
    // Manual trigger of the afterEntryFetch event.
    afterEntryFetchWrapper.callback();
    await nextTick();
    expect(wrapper.find(".noentries-table").text()).toBe(
      "livedata.bottombar.noEntries",
    );
  });

  it("Provides a non-zero property count as CSS variable", async () => {
    const wrapper = initWrapper({
      options: {
        global: {
          provide: {
            logic: {
              getPropertyDescriptors: () => [{ id: "prop1" }],
            },
          },
        },
      },
    });
    expect(
      wrapper
        .find(".layout-table")
        .element.style.getPropertyValue("--livedata-property-count"),
    ).toBe("1");
  });

  it("Renders with some entries", async () => {
    const afterEntryFetchWrapper = {};
    const wrapper = initWrapper({
      options: {
        global: {
          provide: {
            logic: {
              data: {
                data: {
                  entries: [{ id: 1 }],
                },
              },
            },
          },
        },
      },
      afterEntryFetchWrapper,
    });
    // Manual trigger of the afterEntryFetch event.
    afterEntryFetchWrapper.callback();
    await nextTick();
    const rows = wrapper.findAllComponents(LayoutTableRow);
    expect(rows.length).toBe(1);
    expect(rows.at(0).props()).toStrictEqual({
      entry: { id: 1 },
      entryIdx: 0,
      propertyDescriptors: [],
    });
    expect(wrapper.find(".noentries-card").exists()).toBe(false);
  });
});

const PROPERTIES = ["name", "status", "count"];
const ROWS = 10;
const CELLS = ROWS * PROPERTIES.length;

// The names of the components of the rows updated since the last reset. They are expected to render once, when mounted.
let rowUpdates = [];

const TestDisplayer = {
  name: "TestDisplayer",
  mixins: [displayerMixin],
  render() {
    return h("span", { "data-extra": this.config.extra }, this.value);
  },
};

/**
 * @param displayerId - the id of the displayer of all the properties
 * @returns the live data logic, with no entries displayed yet, the table layout displaying it, with everything but
 *   the rows and their cells stubbed, and a function displaying a page of entries
 */
function mountTable(displayerId) {
  const liveDataSource = { getEntries: vi.fn() };
  const logic = new LiveDataLogic(
    liveDataSource,
    JSON.stringify({
      query: { properties: PROPERTIES, sort: [], filters: [], limit: ROWS },
      data: { count: 0, entries: [] },
      meta: {
        defaultLayout: "table",
        layouts: [{ id: "table" }],
        propertyDescriptors: PROPERTIES.map((id) => ({ id, visible: true })),
        propertyTypes: [],
        displayers: [{ id: displayerId, extra: "config" }],
        defaultDisplayer: displayerId,
        entryDescriptor: { idProperty: "id" },
        selection: { enabled: false },
        actions: [],
      },
    }),
    true,
    () => Promise.resolve({}),
  );
  // The logic sends its events through the element of the live data.
  logic.setElement(document.createElement("div"));
  const wrapper = shallowMount(LayoutTable, {
    global: {
      provide: { logic },
      mixins: [
        {
          updated() {
            if (this.$el.closest?.("tbody")) {
              rowUpdates.push(this.$options.name);
            }
          },
        },
      ],
      mocks: { $t: (key) => key },
      stubs: {
        LayoutTableRow: false,
        LivedataDisplayer: false,
        TestDisplayer: false,
      },
    },
  });
  // Display a page of entries, with distinct ids from one page to another.
  const displayPage = (page) => {
    liveDataSource.getEntries.mockResolvedValue({
      count: ROWS * 2,
      entries: Array.from({ length: ROWS }, (_value, index) => ({
        id: `${page}-${index}`,
        name: `name ${page}-${index}`,
        status: "open",
        count: `${index}`,
      })),
    });
    return logic.updateEntries();
  };
  return { wrapper, displayPage };
}

describe("LayoutTable.vue rendering of the cells", () => {
  // Each test uses its own displayer, since the component store keeps the loaded components.
  let displayerId;
  let testCount = 0;

  beforeEach(() => {
    displayerId = `testDisplayer${testCount++}`;
    rowUpdates = [];
    jsonMerge.mockClear();
    componentStore.register(
      "displayer",
      displayerId,
      async () => TestDisplayer,
    );
  });

  it("renders each cell once and resolves the displayer once per property on the first load", async () => {
    const { wrapper, displayPage } = mountTable(displayerId);

    await displayPage(1);
    await flushPromises();

    expect(wrapper.findAll("td.cell span[data-extra='config']").length).toBe(
      CELLS,
    );
    // No cell is rendered empty first, and a second time once its displayer is loaded.
    expect(rowUpdates).toStrictEqual([]);
    // Two merges per property: the property and type configurations, then the default displayer configuration.
    expect(jsonMerge).toHaveBeenCalledTimes(PROPERTIES.length * 2);
  });

  it("renders each new cell once on a page change", async () => {
    const { wrapper, displayPage } = mountTable(displayerId);
    await displayPage(1);
    rowUpdates = [];
    jsonMerge.mockClear();

    await displayPage(2);
    await flushPromises();

    expect(wrapper.find("td.cell span").text()).toBe("name 2-0");
    expect(rowUpdates).toStrictEqual([]);
    expect(jsonMerge).not.toHaveBeenCalled();
  });

  it("loads the displayer of the cells when it is registered after the entries are displayed", async () => {
    const { wrapper, displayPage } = mountTable(`${displayerId}Late`);

    const displayed = displayPage(1);
    await flushPromises();
    expect(wrapper.findAll("td.cell").length).toBe(CELLS);
    expect(wrapper.findAll("td.cell span").length).toBe(0);

    componentStore.register(
      "displayer",
      `${displayerId}Late`,
      async () => TestDisplayer,
    );
    await displayed;
    await flushPromises();

    expect(wrapper.findAll("td.cell span[data-extra='config']").length).toBe(
      CELLS,
    );
  });
});

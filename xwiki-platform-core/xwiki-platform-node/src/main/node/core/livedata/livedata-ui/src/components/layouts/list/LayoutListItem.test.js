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
import LayoutListItem from "./LayoutListItem.vue";
import LivedataEntrySelector from "../../LivedataEntrySelector.vue";
import LivedataDisplayer from "../../displayers/LivedataDisplayer.vue";
import { shallowMount } from "@vue/test-utils";
import _ from "lodash-es";
import { describe, expect, it } from "vitest";

/**
 * Vue Component initializer for LayoutListItem component.
 *
 * @param options - an optional option object use to customize the initialized component
 * @param props - the props overriding the default ones (they are not deep merged, so that they can be unset)
 * @returns a shallow wrapper for the LayoutListItem component
 */
function initWrapper(options = {}, props = {}) {
  return shallowMount(
    LayoutListItem,
    _.merge(
      {
        props: {
          entry: { id: "idA", title: "Title A" },
          entryIdx: 1,
          propertyId: "title",
          ...props,
        },
        global: {
          provide: {
            logic: {
              getEntryId: (e) => e.id,
              isSelectionEnabled: () => false,
            },
          },
        },
      },
      options,
    ),
  );
}

describe("LayoutListItem.vue", () => {
  it("Root element has an index and an id", () => {
    const wrapper = initWrapper();
    expect(wrapper.element.tagName).toBe("LI");
    expect(wrapper.attributes("data-livedata-entry-index")).toBe("1");
    expect(wrapper.attributes("data-livedata-entry-id")).toBe("idA");
  });

  it("Displays the given property", () => {
    const wrapper = initWrapper();
    const displayers = wrapper.findAllComponents(LivedataDisplayer);
    expect(displayers.length).toBe(1);
    expect(displayers.at(0).props()).toMatchObject({
      propertyId: "title",
      entry: { id: "idA", title: "Title A" },
    });
  });

  it("Provides a read-only logic to the displayer", () => {
    const wrapper = initWrapper({
      global: { provide: { logic: { isEditable: () => true } } },
    });
    const providedLogic = wrapper.vm.$.provides.logic;
    expect(providedLogic.isEditable({ entry: {}, propertyId: "title" })).toBe(
      false,
    );
    expect(providedLogic.getEntryId({ id: "idB" })).toBe("idB");
  });

  it("Displays nothing when there is no property", () => {
    const wrapper = initWrapper({}, { propertyId: undefined });
    expect(wrapper.findComponent(LivedataDisplayer).exists()).toBe(false);
  });

  it("Displays the entry selector when the selection is enabled", () => {
    expect(initWrapper().findComponent(LivedataEntrySelector).exists()).toBe(
      false,
    );
    const wrapper = initWrapper({
      global: { provide: { logic: { isSelectionEnabled: () => true } } },
    });
    expect(wrapper.findComponent(LivedataEntrySelector).exists()).toBe(true);
  });
});

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
import LivedataPersistentConfiguration from "./LivedataPersistentConfiguration.vue";
import { mount } from "@vue/test-utils";
import LZString from "lz-string";
import { encode } from "u-node";
import { describe, expect, it } from "vitest";
import { reactive, ref } from "vue";

/**
 * Mount the component with a minimal logic, without saving the config anywhere.
 *
 * @returns the wrapper and the logic
 */
function initWrapper() {
  const data = reactive({
    id: "ld",
    query: {
      properties: ["a", "b"],
      filters: [],
      sort: [],
      offset: 0,
      limit: 15,
    },
    meta: {
      filters: [],
      layouts: [{ id: "table" }, { id: "list" }],
      propertyDescriptors: [
        { id: "a", visible: true },
        { id: "b", visible: true },
      ],
    },
  });
  const logic = {
    data,
    currentLayoutId: ref("table"),
    getLayoutIds: () => data.meta.layouts.map((layout) => layout.id),
    getLayoutDescriptor: (layoutId) =>
      data.meta.layouts.find((layout) => layout.id === layoutId),
    getPropertyDescriptor: (propertyId) =>
      data.meta.propertyDescriptors.find(
        (property) => property.id === propertyId,
      ),
    isPropertyVisible: (propertyId) =>
      logic.getPropertyDescriptor(propertyId).visible,
    setPropertyVisible: (propertyId, visible) => {
      logic.getPropertyDescriptor(propertyId).visible = visible;
    },
  };
  const wrapper = mount(LivedataPersistentConfiguration, {
    props: { urlSearchParam: false, localStorage: false },
    global: { provide: { logic } },
  });
  return { wrapper, logic };
}

describe("LivedataPersistentConfiguration.vue", () => {
  it("Saves and restores the property displayed by the List layout", () => {
    const { wrapper, logic } = initWrapper();
    logic.getLayoutDescriptor("list").displayedProperty = "b";
    const encodedConfig = wrapper.vm.encodedConfig;

    delete logic.getLayoutDescriptor("list").displayedProperty;
    wrapper.vm.loadConfig(wrapper.vm.decodeConfig(encodedConfig));

    expect(logic.getLayoutDescriptor("list").displayedProperty).toBe("b");
  });

  it("Loads a config saved with the first encoding version", () => {
    const { wrapper, logic } = initWrapper();
    const v1Config = {
      $_filters: [],
      $_sort: [],
      $_offset: 30,
      $_limit: 15,
      $_currentLayoutId: "list",
      $_propertyOrder: ["b", "a"],
      $_propertyVisibility: ["a"],
    };
    const encodedConfig = LZString.compressToEncodedURIComponent(
      encode(wrapper.vm.coders[0], v1Config),
    );

    wrapper.vm.loadConfig(wrapper.vm.decodeConfig(encodedConfig));

    expect(logic.data.query.offset).toBe(30);
    expect(logic.currentLayoutId.value).toBe("list");
    expect(logic.data.query.properties).toStrictEqual(["b", "a"]);
    expect(logic.isPropertyVisible("a")).toBe(false);
    expect(logic.getLayoutDescriptor("list").displayedProperty).toBeUndefined();
  });
});

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
import LayoutListPropertyPicker from "./LayoutListPropertyPicker.vue";
import { LIST_LAYOUT_ID } from "./listLayout.js";
import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import { reactive } from "vue";

/**
 * Vue Component initializer for LayoutListPropertyPicker component.
 *
 * @param properties - the property descriptors, in the query order
 * @returns the wrapper and the reactive data of the logic
 */
function initWrapper(properties) {
  const data = reactive({
    id: "ld",
    query: { properties: properties.map((property) => property.id) },
    meta: {
      layouts: [{ id: LIST_LAYOUT_ID }],
      propertyDescriptors: properties,
    },
  });
  const logic = {
    data,
    getLayoutDescriptor: (layoutId) =>
      data.meta.layouts.find((layout) => layout.id === layoutId),
    getPropertyDescriptor: (propertyId) =>
      data.meta.propertyDescriptors.find(
        (property) => property.id === propertyId,
      ),
    getPropertyDescriptors: () =>
      data.query.properties.map((propertyId) =>
        logic.getPropertyDescriptor(propertyId),
      ),
  };
  const wrapper = mount(LayoutListPropertyPicker, {
    global: {
      provide: { logic },
      mocks: { $t: (key) => key },
    },
  });
  return { wrapper, data };
}

describe("LayoutListPropertyPicker.vue", () => {
  it("Is hidden when there is only one property", () => {
    const { wrapper } = initWrapper([{ id: "a", name: "A", visible: true }]);
    expect(wrapper.find("select").exists()).toBe(false);
  });

  it("Lists the properties and selects the displayed one", () => {
    const { wrapper } = initWrapper([
      { id: "a", name: "A" },
      { id: "b", name: "B", visible: true },
    ]);
    const options = wrapper.findAll("option");
    expect(options.map((option) => option.text())).toStrictEqual(["A", "B"]);
    expect(wrapper.find("select").element.value).toBe("b");
    expect(wrapper.find("label").attributes("for")).toBe(
      wrapper.find("select").attributes("id"),
    );
  });

  it("Changes the displayed property without changing the visibility", async () => {
    const { wrapper, data } = initWrapper([
      { id: "a", name: "A", visible: true },
      { id: "b", name: "B", visible: true },
    ]);
    await wrapper.find("select").setValue("b");
    expect(data.meta.layouts[0].displayedProperty).toBe("b");
    expect(
      data.meta.propertyDescriptors.map((property) => property.visible),
    ).toStrictEqual([true, true]);
    expect(wrapper.find("select").element.value).toBe("b");
  });
});

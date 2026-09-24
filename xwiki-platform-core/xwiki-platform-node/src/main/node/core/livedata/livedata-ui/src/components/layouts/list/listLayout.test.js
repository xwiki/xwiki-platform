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
import {
  LIST_LAYOUT_ID,
  getListPropertyId,
  setListPropertyId,
} from "./listLayout.js";
import { describe, expect, it } from "vitest";

/**
 * Create a minimal logic with the given properties and List layout descriptor.
 *
 * @param properties - the property descriptors, in the query order
 * @param layoutDescriptor - the descriptor of the List layout, or undefined when the layout is not available
 * @returns the logic
 */
function initLogic(properties, layoutDescriptor = { id: LIST_LAYOUT_ID }) {
  return {
    data: {
      query: {
        properties: properties.map((property) => property.id),
      },
    },
    getLayoutDescriptor: (layoutId) =>
      layoutId === LIST_LAYOUT_ID ? layoutDescriptor : undefined,
    getPropertyDescriptor: (propertyId) =>
      properties.find((property) => property.id === propertyId),
  };
}

describe("listLayout.js", () => {
  it("Returns the configured displayed property", () => {
    const logic = initLogic([{ id: "a", visible: true }, { id: "b" }], {
      id: LIST_LAYOUT_ID,
      displayedProperty: "b",
    });
    expect(getListPropertyId(logic)).toBe("b");
  });

  it("Falls back on the first visible property", () => {
    const logic = initLogic([{ id: "a" }, { id: "b", visible: true }], {
      id: LIST_LAYOUT_ID,
      displayedProperty: "unknown",
    });
    expect(getListPropertyId(logic)).toBe("b");
  });

  it("Falls back on the first property when none is visible", () => {
    const logic = initLogic([{ id: "a" }, { id: "b" }], undefined);
    expect(getListPropertyId(logic)).toBe("a");
  });

  it("Returns undefined when there are no properties", () => {
    expect(getListPropertyId(initLogic([]))).toBeUndefined();
  });

  it("Sets the displayed property without changing the property visibility", () => {
    const properties = [{ id: "a", visible: true }, { id: "b" }];
    const logic = initLogic(properties);
    setListPropertyId(logic, "b");
    expect(getListPropertyId(logic)).toBe("b");
    expect(properties).toStrictEqual([{ id: "a", visible: true }, { id: "b" }]);
  });

  it("Ignores unknown properties", () => {
    const logic = initLogic([{ id: "a" }, { id: "b", visible: true }]);
    setListPropertyId(logic, "unknown");
    expect(getListPropertyId(logic)).toBe("b");
  });
});

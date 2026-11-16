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
  getListPropertyId,
  getListSecondaryPropertyIds,
} from "./listLayout.js";
import { describe, expect, it } from "vitest";

/**
 * Create a minimal logic with the given properties.
 *
 * @param properties - the property descriptors, in the query order
 * @returns the logic
 */
function initLogic(properties) {
  return {
    data: {
      query: {
        properties: properties.map((property) => property.id),
      },
    },
    getPropertyDescriptor: (propertyId) =>
      properties.find((property) => property.id === propertyId),
  };
}

describe("listLayout.js", () => {
  describe("getListPropertyId", () => {
    it("Returns the first visible property in the configured order", () => {
      const logic = initLogic([
        { id: "a" },
        { id: "b", visible: true },
        { id: "c", visible: true },
      ]);
      expect(getListPropertyId(logic)).toBe("b");
      // Follows the order changes.
      logic.data.query.properties = ["c", "a", "b"];
      expect(getListPropertyId(logic)).toBe("c");
    });

    it("Falls back on the first property when none is visible", () => {
      expect(getListPropertyId(initLogic([{ id: "a" }, { id: "b" }]))).toBe(
        "a",
      );
    });

    it("Returns undefined when there are no properties", () => {
      expect(getListPropertyId(initLogic([]))).toBeUndefined();
    });
  });

  describe("getListSecondaryPropertyIds", () => {
    it("Returns the other visible properties in the configured order", () => {
      const logic = initLogic([
        { id: "a" },
        { id: "b", visible: true },
        { id: "c", visible: true },
        { id: "d" },
        { id: "e", visible: true },
      ]);
      expect(getListSecondaryPropertyIds(logic)).toStrictEqual(["c", "e"]);
      // Follows the order changes.
      logic.data.query.properties = ["e", "a", "b", "c", "d"];
      expect(getListSecondaryPropertyIds(logic)).toStrictEqual(["b", "c"]);
    });

    it("Returns no property when only one property is visible", () => {
      expect(
        getListSecondaryPropertyIds(
          initLogic([{ id: "a" }, { id: "b", visible: true }]),
        ),
      ).toStrictEqual([]);
    });

    it("Returns no property when none is visible", () => {
      expect(
        getListSecondaryPropertyIds(initLogic([{ id: "a" }, { id: "b" }])),
      ).toStrictEqual([]);
    });
  });
});

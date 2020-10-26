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



define([
  "testConfig",
  "Logic",
], function (
  testConfig,
  Logic
) {

  const Utils = Logic.Utils;

  const logic = new Logic({ config: testConfig });
  console.log("testConfig", testConfig);
  console.log("logic", logic);


  describe("Utils", () => {

    describe("Deep Equals", () => {

      const testObject = {
        int: 3,
        sub: { arr: [1, 2] },
      };

      it("Primitives", () => {
        expect(Utils.isDeepEqual(2, 2)).toBe(true);
        expect(Utils.isDeepEqual(2, 3)).toBe(false);
        expect(Utils.isDeepEqual(0, -0)).toBe(false);
        expect(Utils.isDeepEqual(NaN, NaN)).toBe(true);
        expect(Utils.isDeepEqual("abc", "abc")).toBe(true);
        expect(Utils.isDeepEqual("", "")).toBe(true);
        expect(Utils.isDeepEqual("abc", " abc ")).toBe(false);
        expect(Utils.isDeepEqual(undefined, undefined)).toBe(true);
        expect(Utils.isDeepEqual(null, null)).toBe(true);
        expect(Utils.isDeepEqual(undefined, null)).toBe(false);
        expect(Utils.isDeepEqual(undefined, null)).toBe(false);
      });

      it("Nested objects", () => {
        expect(Utils.isDeepEqual(testObject, {
          int: 3,
          sub: { arr: [1, 2] },
        })).toBe(true);
      });

      it("Object Property order", () => {
        expect(Utils.isDeepEqual(testObject, {
          sub: { arr: [1, 2] },
          int: 3,
        })).toBe(true);
      });

      it("Array Property order", () => {
        expect(Utils.isDeepEqual(testObject, {
          sub: { arr: [2, 1] },
          int: 3,
        })).toBe(false);
      });

      it("Missing properties", () => {
        expect(Utils.isDeepEqual(testObject, {
          sub: { arr: [1, 2] },
        })).toBe(false);
        expect(Utils.isDeepEqual(testObject, {
          int: 3,
          otherProp: 4,
          sub: { arr: [1, 2] },
        })).toBe(false);
      });

    }); // END describe: "Deep Equals"


    describe("Unique Array", () => {

      const testArr = [1, 2, 3];

      it("Has", () => {
        expect(Utils.uniqueArrayHas(testArr, 1)).toBe(true);
        expect(Utils.uniqueArrayHas(testArr, 4)).toBe(false);
        expect(Utils.uniqueArrayHas(testArr, undefined)).toBe(false);
      });

      it("uniqueArrayAdd", () => {
        Utils.uniqueArrayAdd(testArr, 4);
        expect(Utils.isDeepEqual(testArr, [1, 2, 3, 4])).toBe(true);

        // Do not add already existing items
        Utils.uniqueArrayAdd(testArr, 4);
        expect(Utils.isDeepEqual(testArr, [1, 2, 3, 4])).toBe(true);
      });

      it("uniqueArrayRemove", () => {
        Utils.uniqueArrayRemove(testArr, 4);
        expect(Utils.isDeepEqual(testArr, [1, 2, 3])).toBe(true);

        // Do not try to remove non existing item
        Utils.uniqueArrayRemove(testArr, 4);
        expect(Utils.isDeepEqual(testArr, [1, 2, 3])).toBe(true);
      });

      it("uniqueArrayToggle", () => {
        Utils.uniqueArrayToggle(testArr, 4);
        expect(Utils.isDeepEqual(testArr, [1, 2, 3, 4])).toBe(true);

        Utils.uniqueArrayToggle(testArr, 4);
        expect(Utils.isDeepEqual(testArr, [1, 2, 3])).toBe(true);

        Utils.uniqueArrayToggle(testArr, 4, false);
        expect(Utils.isDeepEqual(testArr, [1, 2, 3])).toBe(true);
      });

    }); // END describe "Unique Array"

  }); // END describe: "Utils"


  describe("Logic", () => {

    it("It Launches! :D", () => {
      expect(logic.config.id).toBe("LD0");
    });


    describe("Layout", () => {

      it("getIds", () => {
        expect(Utils.isDeepEqual(
          logic.layout.getIds(),
          ["table", "cards"],
        )).toBe(true);
      });

      it("getDescriptor", () => {

        expect(Utils.isDeepEqual(
          logic.layout.getDescriptor(),
          { id: "table", name: "Table" },
        )).toBe(true);

        expect(Utils.isDeepEqual(
          logic.layout.getDescriptor({ layoutId: "cards" }),
          { id: "cards", name: "Cards", titleProperty: "doc_title" },
        )).toBe(true);

        expect(Utils.isDeepEqual(
          logic.layout.getDescriptor({ layoutId: "nonExisting" }),
          undefined
        )).toBe(true);

      });

    });




  });


});

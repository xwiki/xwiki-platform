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
  CARDS_LAYOUT_ID,
  getCoverImage,
  getCoverPropertyId,
  getCoverTextPropertyId,
  getResizedImageSource,
  setCoverPropertyId,
} from "./cardsLayout.js";
import { describe, expect, it } from "vitest";

/**
 * Create a minimal logic.
 *
 * @param properties - the property descriptors, in the query order
 * @param entries - the displayed entries
 * @param cardsDescriptor - the descriptor of the cards layout
 * @returns the logic
 */
function initLogic(
  properties,
  entries = [],
  cardsDescriptor = { id: CARDS_LAYOUT_ID },
) {
  return {
    data: {
      query: { properties: properties.map((property) => property.id) },
      data: { entries },
    },
    getLayoutDescriptor: (layoutId) =>
      layoutId === CARDS_LAYOUT_ID ? cardsDescriptor : undefined,
    getPropertyDescriptor: (propertyId) =>
      properties.find((property) => property.id === propertyId),
    // The properties are visible unless they are explicitly hidden.
    isPropertyVisible: (propertyId) =>
      properties.find((property) => property.id === propertyId)?.visible !==
      false,
  };
}

describe("cardsLayout.js", () => {
  describe("cover property", () => {
    it("Has no cover by default", () => {
      expect(getCoverPropertyId(initLogic([{ id: "a" }]))).toBeUndefined();
    });

    it("Returns the configured cover", () => {
      const logic = initLogic([{ id: "a" }, { id: "b" }], [], {
        id: CARDS_LAYOUT_ID,
        coverProperty: "b",
      });
      expect(getCoverPropertyId(logic)).toBe("b");
    });

    it("Ignores a cover that is not a property", () => {
      const logic = initLogic([{ id: "a" }], [], {
        id: CARDS_LAYOUT_ID,
        coverProperty: "unknown",
      });
      expect(getCoverPropertyId(logic)).toBeUndefined();
    });

    it("Ignores a hidden cover", () => {
      const logic = initLogic([{ id: "a" }, { id: "b", visible: false }], [], {
        id: CARDS_LAYOUT_ID,
        coverProperty: "b",
      });
      expect(getCoverPropertyId(logic)).toBeUndefined();
    });

    it("Sets and removes the cover", () => {
      const logic = initLogic([{ id: "a", visible: true }, { id: "b" }]);
      setCoverPropertyId(logic, "b");
      expect(getCoverPropertyId(logic)).toBe("b");
      setCoverPropertyId(logic, "unknown");
      expect(getCoverPropertyId(logic)).toBe("b");
      setCoverPropertyId(logic, "");
      expect(getCoverPropertyId(logic)).toBeUndefined();
    });

    it("Does nothing when there is no cards layout", () => {
      const logic = initLogic([{ id: "a" }], [], null);
      setCoverPropertyId(logic, "a");
      expect(getCoverPropertyId(logic)).toBeUndefined();
    });
  });

  describe("getCoverImage", () => {
    it("Uses the URL of an image", () => {
      expect(
        getCoverImage(" /xwiki/bin/download/A/B/photo.JPG?rev=1.1 "),
      ).toEqual({ src: "/xwiki/bin/download/A/B/photo.JPG?rev=1.1", alt: "" });
      expect(getCoverImage("https://example.org/a.webp")).toEqual({
        src: "https://example.org/a.webp",
        alt: "",
      });
      expect(getCoverImage("data:image/png;base64,AAAA")).toEqual({
        src: "data:image/png;base64,AAAA",
        alt: "",
      });
    });

    it("Uses the first image of an HTML value", () => {
      expect(
        getCoverImage(
          '<p>Some text <img src="/a.png" alt="First"> <img src="/b.png"></p>',
        ),
      ).toEqual({ src: "/a.png", alt: "First" });
    });

    it("Ignores the values without an image", () => {
      expect(getCoverImage("Some text")).toBeUndefined();
      expect(getCoverImage("/xwiki/bin/view/A/B")).toBeUndefined();
      expect(getCoverImage("<p>No image</p>")).toBeUndefined();
      expect(getCoverImage(42)).toBeUndefined();
      expect(getCoverImage(undefined)).toBeUndefined();
    });

    it("Ignores the images that cannot be displayed safely", () => {
      expect(getCoverImage("javascript:alert(1).png")).toBeUndefined();
      expect(getCoverImage('<img src="javascript:alert(1)">')).toBeUndefined();
      expect(
        getCoverImage('<img src="data:text/html,<script></script>">'),
      ).toBeUndefined();
    });
  });

  describe("getCoverTextPropertyId", () => {
    const logic = initLogic([
      { id: "cover" },
      { id: "title" },
      { id: "first", visible: true },
    ]);

    it("Uses the text of the cover property", () => {
      const entry = { cover: "Cover text", title: "Title" };
      expect(getCoverTextPropertyId(logic, entry, "cover", "title")).toBe(
        "cover",
      );
    });

    it("Falls back on the title, then on the first visible property", () => {
      // The cover only holds an image without text.
      const entry = { cover: '<img src="/a.png">', title: "Title", first: "1" };
      expect(getCoverTextPropertyId(logic, entry, "cover", "title")).toBe(
        "title",
      );
      expect(getCoverTextPropertyId(logic, entry, "cover", undefined)).toBe(
        "first",
      );
      expect(
        getCoverTextPropertyId(logic, { first: " " }, "cover", "title"),
      ).toBe("cover");
    });

    it("Doesn't display the URL of an image that failed to load", () => {
      const entry = { cover: "/missing.png", title: "Title" };
      expect(getCoverTextPropertyId(logic, entry, "cover", "title")).toBe(
        "title",
      );
    });
  });

  describe("getResizedImageSource", () => {
    const origin = window.location.origin;

    it("Requests the attachments of the wiki at the given width, rounded up", () => {
      expect(
        getResizedImageSource("/xwiki/bin/download/A/B/photo.jpg?rev=1.1", 352),
      ).toBe("/xwiki/bin/download/A/B/photo.jpg?rev=1.1&width=400");
      expect(
        getResizedImageSource(`${origin}/xwiki/bin/download/A/B/c.png`, 700),
      ).toBe(`${origin}/xwiki/bin/download/A/B/c.png?width=700`);
    });

    it("Replaces the size of a thumbnail", () => {
      expect(
        getResizedImageSource(
          "/xwiki/bin/download/XWiki/U/avatar.png?width=50&height=50",
          704,
        ),
      ).toBe("/xwiki/bin/download/XWiki/U/avatar.png?width=800");
    });

    it("Leaves the other images untouched", () => {
      const external = "https://example.org/download/a.png";
      expect(getResizedImageSource(external, 352)).toBe(external);
      expect(getResizedImageSource("/xwiki/resources/a.png", 352)).toBe(
        "/xwiki/resources/a.png",
      );
      expect(getResizedImageSource("data:image/png;base64,AA", 352)).toBe(
        "data:image/png;base64,AA",
      );
    });

    it("Leaves the image untouched when the width is unknown", () => {
      expect(getResizedImageSource("/xwiki/bin/download/A/B/c.png")).toBe(
        "/xwiki/bin/download/A/B/c.png",
      );
    });
  });
});

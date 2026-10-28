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
// @vitest-environment jsdom

import { UniAstToReactJsxConverter } from "../converter";
import { Fragment, createElement } from "react";
import { flushSync } from "react-dom";
import { createRoot } from "react-dom/client";
import { describe, expect, test } from "vitest";
import { mock } from "vitest-mock-extended";
import type { MacroEditableZoneRef } from "../converter";
import type { EntityReference } from "@xwiki/platform-model-api";
import type {
  RemoteURLParser,
  RemoteURLSerializer,
} from "@xwiki/platform-model-remote-url-api";
import type {
  Block,
  InlineContent,
  ListItem,
} from "@xwiki/platform-uniast-api";

const imageReference = mock<EntityReference>();

const remoteURLParser = mock<RemoteURLParser>();
remoteURLParser.parse.mockReturnValue(imageReference);

const remoteURLSerializer = mock<RemoteURLSerializer>();
remoteURLSerializer.serialize
  .calledWith(imageReference)
  .mockReturnValue("/xwiki/bin/download/Space/Page/image.png");

const converter = new UniAstToReactJsxConverter(
  remoteURLParser,
  remoteURLSerializer,
);

const editableZoneRef: MacroEditableZoneRef = { type: "block", ref: null };

function text(content: string): InlineContent {
  return { type: "text", content, styles: {} };
}

function paragraph(...content: InlineContent[]): Block {
  return { type: "paragraph", content, styles: {} };
}

function listItem(
  content: string | Block[],
  options: Pick<ListItem, "number" | "checked"> = {},
): ListItem {
  return {
    ...options,
    content: typeof content === "string" ? [paragraph(text(content))] : content,
    styles: {},
  };
}

function list(...items: ListItem[]): Block {
  return { type: "list", items, styles: {} };
}

/** Renders the given blocks into a detached container and returns it for DOM assertions. */
function render(...blocks: Block[]): HTMLElement {
  const elements = converter.blocksToReactJSX(blocks, editableZoneRef);
  if (elements instanceof Error) {
    throw elements;
  }
  const container = document.createElement("div");
  const root = createRoot(container);
  flushSync(() => {
    root.render(createElement(Fragment, null, ...elements));
  });
  return container;
}

/** Lists the tag names of the element children of the given element, e.g. `["P", "UL"]`. */
function childTags(element: Element): string[] {
  return Array.from(element.children, (child) => child.tagName);
}

describe("UniAstToReactJsxConverter", () => {
  test("renders a list without numbers as a bulleted list", () => {
    const container = render(list(listItem("first"), listItem("second")));

    const ul = container.querySelector(":scope > ul")!;
    expect(ul).not.toBeNull();
    expect(ul.hasAttribute("start")).toBe(false);
    expect(Array.from(ul.children, (li) => li.textContent)).toEqual([
      "first",
      "second",
    ]);
  });

  test("renders a numbered list starting at the number of its first item", () => {
    const container = render(
      list(listItem("third", { number: 3 }), listItem("fourth", { number: 4 })),
    );

    const ol = container.querySelector(":scope > ol")!;
    expect(ol).not.toBeNull();
    expect(ol.getAttribute("start")).toBe("3");
    expect(Array.from(ol.children, (li) => li.textContent)).toEqual([
      "third",
      "fourth",
    ]);
  });

  test("renders a task list item with a read-only checkbox", () => {
    const container = render(
      list(
        listItem("done", { checked: true }),
        listItem("todo", { checked: false }),
        listItem("plain"),
      ),
    );

    const checkboxes = Array.from(
      container.querySelectorAll<HTMLInputElement>("li > input"),
    );
    expect(checkboxes.map((checkbox) => checkbox.type)).toEqual([
      "checkbox",
      "checkbox",
    ]);
    expect(checkboxes.map((checkbox) => checkbox.checked)).toEqual([
      true,
      false,
    ]);
    expect(checkboxes.every((checkbox) => checkbox.readOnly)).toBe(true);
  });

  test("renders the blocks of a list item, including a nested list", () => {
    const container = render(
      list(
        listItem([paragraph(text("parent")), list(listItem("child"))]),
        listItem("sibling"),
      ),
    );

    const items = container.querySelectorAll(":scope > ul > li");
    expect(items).toHaveLength(2);
    expect(childTags(items[0])).toEqual(["P", "UL"]);
    expect(items[0].querySelector(":scope > ul > li > p")!.textContent).toBe(
      "child",
    );
    expect(childTags(items[1])).toEqual(["P"]);
  });

  test("renders a break as a horizontal rule", () => {
    const container = render(
      paragraph(text("before")),
      { type: "break" },
      paragraph(text("after")),
    );

    expect(childTags(container)).toEqual(["P", "HR", "P"]);
  });

  test("renders the CSS classes of a block", () => {
    const container = render({
      type: "paragraph",
      content: [text("styled")],
      styles: { cssClasses: ["box", "infomessage"] },
    });

    expect(container.querySelector("p")!.className).toBe("box infomessage");
  });

  test("renders an inline image with an external URL", () => {
    const container = render(
      paragraph({
        type: "image",
        target: { type: "external", url: "https://www.xwiki.org/logo.png" },
        alt: "Logo",
        widthPx: 100,
        heightPx: 50,
      }),
    );

    const img = container.querySelector("p > img")!;
    expect(img.getAttribute("src")).toBe("https://www.xwiki.org/logo.png");
    expect(img.getAttribute("alt")).toBe("Logo");
    expect(img.getAttribute("width")).toBe("100");
    expect(img.getAttribute("height")).toBe("50");
  });

  test("renders an inline image targeting an attachment", () => {
    const container = render(
      paragraph({
        type: "image",
        target: {
          type: "internal",
          rawReference: "Space.Page@image.png",
          parsedReference: imageReference,
        },
      }),
    );

    expect(container.querySelector("p > img")!.getAttribute("src")).toBe(
      "/xwiki/bin/download/Space/Page/image.png",
    );
  });
});

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
import { UniAstToReactJsxConverter } from "../converter";
import { describe, expect, test } from "vitest";
import { mock } from "vitest-mock-extended";
import type { MacroEditableZoneRef } from "../converter";
import type {
  RemoteURLParser,
  RemoteURLSerializer,
} from "@xwiki/platform-model-remote-url-api";
import type { Block, ListItem } from "@xwiki/platform-uniast-api";

const converter = new UniAstToReactJsxConverter(
  mock<RemoteURLParser>(),
  mock<RemoteURLSerializer>(),
);

const editableZoneRef: MacroEditableZoneRef = { type: "block", ref: null };

function listItem(text: string, number?: number): ListItem {
  return {
    number,
    content: [
      {
        type: "paragraph",
        styles: {},
        content: [{ type: "text", content: text, styles: {} }],
      },
    ],
    styles: {},
  };
}

function convertList(items: ListItem[]) {
  const list: Block = { type: "list", items, styles: {} };
  const elements = converter.blocksToReactJSX([list], editableZoneRef);
  if (elements instanceof Error) {
    throw elements;
  }
  // Each converted block is wrapped in a keyed fragment.
  return elements[0].props.children;
}

describe("UniAstToReactJsxConverter", () => {
  test("renders a numbered list starting at the number of its first item", () => {
    const list = convertList([listItem("third", 3), listItem("fourth", 4)]);

    expect(list.type).toBe("ol");
    expect(list.props.start).toBe(3);
  });

  test("renders a list without numbers as a bulleted list", () => {
    const list = convertList([listItem("first"), listItem("second")]);

    expect(list.type).toBe("ul");
    expect(list.props.start).toBeUndefined();
  });
});

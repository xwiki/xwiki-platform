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
import { XWikiBlockNoteIterator } from "./XWikiBlockNoteIterator";
import {
  DefaultBlockNoteIterator,
  blockNoteIteratorName,
  getMacroCalls,
} from "@xwiki/platform-editors-blocknote-react";
import { Container } from "inversify";
import { describe, expect, it } from "vitest";

const errorCall = { name: "error", parameters: {}, content: "Error" };

// A block macro whose title parameter and content are editable in-place, each holding an inline macro.
function boxCall() {
  return {
    name: "box",
    parameters: {
      title: [
        {
          type: "xwikiInlineMacro",
          props: { id: "n1", call: { name: "info", parameters: {} } },
        },
      ],
      // Not editable in-place.
      cssClass: "important",
    },
    content: [
      {
        type: "paragraph",
        content: [
          {
            type: "xwikiInlineMacro",
            props: { id: "n2", call: errorCall },
          },
        ],
      },
    ],
  };
}

/**
 * @param content - the content to iterate
 * @returns the visited nodes, as their type and the type of the macro node holding them, if any
 */
function visit(content) {
  const visited = [];
  new XWikiBlockNoteIterator().iterate(content, {
    visit: (node, macro) => {
      visited.push([node.type ?? node, macro?.props?.id ?? macro?.id]);
      return false;
    },
  });
  return visited;
}

describe("XWikiBlockNoteIterator", () => {
  it("enters the parameters and the content of the macro calls, after visiting the macro node", () => {
    const content = [
      { id: "b1", type: "xwikiMacroBlock", props: { call: boxCall() } },
      { id: "p1", type: "paragraph", content: ["text"] },
    ];

    expect(visit(content)).toEqual([
      ["xwikiMacroBlock", undefined],
      ["xwikiInlineMacro", "b1"],
      ["paragraph", "b1"],
      ["xwikiInlineMacro", "b1"],
      ["paragraph", undefined],
      ["text", undefined],
    ]);
  });

  it("parses the call of a top-level macro to enter it", () => {
    const content = [
      {
        id: "b1",
        type: "xwikiMacroBlock",
        props: { call: JSON.stringify(boxCall()) },
      },
      // Invalid calls are not entered.
      { id: "b2", type: "xwikiMacroBlock", props: { call: "{not JSON" } },
    ];

    expect(getMacroCalls(content, new XWikiBlockNoteIterator())).toEqual([
      { id: "b1", call: JSON.stringify(boxCall()) },
      // The call of a nested macro is an object, part of the call of its parent, and its id is a property.
      { id: "n1", call: JSON.stringify({ name: "info", parameters: {} }) },
      { id: "n2", call: JSON.stringify(errorCall) },
    ]);
  });

  it("doesn't enter a macro call when the visitor skips its children", () => {
    const content = [
      { id: "b1", type: "xwikiMacroBlock", props: { call: boxCall() } },
    ];
    const visited = [];
    new XWikiBlockNoteIterator().iterate(content, {
      visit: (node) => {
        visited.push(node.type);
        return true;
      },
    });

    expect(visited).toEqual(["xwikiMacroBlock"]);
  });

  it("replaces the default iterator", () => {
    const container = new Container();
    DefaultBlockNoteIterator.bind(container);

    XWikiBlockNoteIterator.bind(container);

    expect(container.get(blockNoteIteratorName)).toBeInstanceOf(
      XWikiBlockNoteIterator,
    );
  });
});

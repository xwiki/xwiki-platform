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

import { createBlockNoteSchema } from "../../blocknote";
import { blockOutputToHTML, inlineOutputToHTML } from "../../blocknote/macro";
import { BlockNoteEditor } from "@blocknote/core";
import { beforeAll, describe, expect, test } from "vitest";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type AnyEditor = BlockNoteEditor<any, any, any>;

/** Parses an HTML fragment and returns the container element for structural assertions. */
function parse(html: string): HTMLElement {
  const container = document.createElement("div");
  container.innerHTML = html;
  return container;
}

describe("blockOutputToHTML (mixed inline/block group content)", () => {
  let editor: AnyEditor;

  beforeAll(() => {
    editor = BlockNoteEditor.create({
      schema: createBlockNoteSchema([]),
    }) as AnyEditor;
  });

  test("renders an {{info}}-like box with mixed inline and block children without crashing", () => {
    // A group (the info box) whose children mix a block, bare-string and styled-text inline content, and a nested
    // group: the structure BlockNote's schema rejects (inline as a sibling of blocks) and that used to crash
    // blocksToHTMLLossy.
    const output = [
      {
        type: "xwikiGroup",
        props: { xwikiParameters: { class: "box infomessage" } },
        children: [
          { type: "paragraph", props: {}, content: "icon", children: [] },
          "\n",
          { type: "text", text: "Information", styles: {} },
          {
            type: "xwikiGroup",
            props: { xwikiParameters: { class: "box-title" } },
            children: [
              {
                type: "heading",
                props: { level: 2 },
                content: "Note",
                children: [],
              },
            ],
          },
        ],
      },
    ];

    const html = blockOutputToHTML(editor, output, {
      name: "info",
      parameters: {},
    });

    // The box and its nested groups are rendered as plain DIVs carrying their xwikiParameters (the box class that the
    // group block spec otherwise drops).
    const box = parse(html).querySelector<HTMLElement>("div.box.infomessage");
    expect(box).not.toBeNull();
    expect(box!.querySelector("div.box-title")).not.toBeNull();
    expect(html).toContain("Information");

    // Zero wrapper: the inline run ("Information") is a direct text child of the box, not wrapped in a <p>, and no
    // .xwiki-raw container leaked into the output.
    const inlineText = Array.from(box!.childNodes).some(
      (node) =>
        node.nodeType === node.TEXT_NODE &&
        node.textContent?.includes("Information"),
    );
    expect(inlineText).toBe(true);
    expect(html).not.toContain("xwiki-raw");
  });

  test("renders a group trapped inside a plain block (list item) with no wrapper", () => {
    // A group nested inside a list item is swept through blocksToHTMLLossy as part of the list item; the sanitize +
    // unwrap fallback must still produce a wrapper-free group DIV.
    const output = [
      {
        type: "bulletListItem",
        props: {},
        content: "item",
        children: [
          {
            type: "xwikiGroup",
            props: { xwikiParameters: { class: "nested-box" } },
            children: [
              { type: "paragraph", props: {}, content: "deep", children: [] },
            ],
          },
        ],
      },
    ];

    const html = blockOutputToHTML(editor, output, {
      name: "html",
      parameters: {},
    });

    expect(parse(html).querySelector("div.nested-box")).not.toBeNull();
    expect(html).toContain("deep");
    expect(html).not.toContain("xwiki-raw");
  });

  test("applies xwikiParameters (object form) of a styled text inside macro output", () => {
    // Inside macro output the processor never serializes the text style, so xwikiParameters is a raw object.
    const output = [
      {
        type: "xwikiGroup",
        props: { xwikiParameters: { class: "box" } },
        children: [
          {
            type: "text",
            text: "label",
            styles: { xwikiParameters: { class: "sr-only" } },
          },
        ],
      },
    ];

    const html = blockOutputToHTML(editor, output, {
      name: "info",
      parameters: {},
    });

    const span = parse(html).querySelector<HTMLElement>("span.sr-only");
    expect(span).not.toBeNull();
    expect(span!.textContent).toBe("label");
  });
});

describe("XWikiParametersStyle", () => {
  let editor: AnyEditor;

  beforeAll(() => {
    editor = BlockNoteEditor.create({
      schema: createBlockNoteSchema([]),
    }) as AnyEditor;
  });

  test("applies xwikiParameters (JSON string form) as span attributes on export", () => {
    // In the top-level document the processor serializes the text style value to a JSON string.
    const html = editor.blocksToHTMLLossy([
      {
        type: "paragraph",
        content: [
          {
            type: "text",
            text: "x",
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            styles: { xwikiParameters: '{"class":"foo"}' } as any,
          },
        ],
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } as any,
    ]);

    const span = parse(html).querySelector<HTMLElement>("span.foo");
    expect(span).not.toBeNull();
    expect(span!.textContent).toBe("x");
  });
});

describe("inlineOutputToHTML (inline output with non-inline nodes)", () => {
  let editor: AnyEditor;

  beforeAll(() => {
    editor = BlockNoteEditor.create({
      schema: createBlockNoteSchema([]),
    }) as AnyEditor;
  });

  test("renders inline output that mixes an xwikiRaw image with styled text (no placeholder)", () => {
    // Inline output can carry an xwikiRaw node (an inline image rendered as raw HTML); it must not be treated as
    // inline content wholesale (which crashes blocksToHTMLLossy and yields the macro placeholder).
    const output = [
      {
        type: "xwikiRaw",
        props: {
          syntax: "html/5.0",
          text: '<img src="accept.png" alt="Icon" />',
        },
      },
      {
        type: "text",
        text: "Success",
        styles: { xwikiParameters: { class: "box successmessage one" } },
      },
      { type: "xwikiEditable" },
    ] as unknown as Parameters<typeof inlineOutputToHTML>[1];

    const html = inlineOutputToHTML(editor, output, {
      name: "success",
      parameters: {},
      content: "body",
    });

    expect(html).not.toBe("");
    const root = parse(html);
    expect(root.querySelector("img")).not.toBeNull();
    const span = root.querySelector<HTMLElement>("span.successmessage");
    expect(span).not.toBeNull();
    expect(span!.textContent).toBe("Success");
    expect(html).toContain("body");
    expect(html).not.toContain("xwiki-raw");
  });

  test("renders plain all-inline output", () => {
    const output = [
      { type: "text", text: "hello", styles: {} },
    ] as unknown as Parameters<typeof inlineOutputToHTML>[1];

    const html = inlineOutputToHTML(editor, output, {
      name: "x",
      parameters: {},
    });

    expect(html).toContain("hello");
    expect(parse(html).querySelector("img")).toBeNull();
  });
});

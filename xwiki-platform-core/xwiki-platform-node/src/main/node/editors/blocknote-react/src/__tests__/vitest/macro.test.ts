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
import { MacroOutput } from "../../blocknote/macroOutput";
import { BlockNoteEditor } from "@blocknote/core";
import { createElement } from "react";
import { flushSync } from "react-dom";
import { createRoot } from "react-dom/client";
import { beforeAll, describe, expect, test } from "vitest";
import type { MacroCall } from "../../blocknote/utils";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Nodes = any[];

/** Renders a macro output through {@link MacroOutput} into a detached container and returns it for DOM assertions. */
function render(nodes: Nodes, call: MacroCall, inline = false): HTMLElement {
  const container = document.createElement("div");
  const root = createRoot(container);
  flushSync(() => {
    root.render(createElement(MacroOutput, { nodes, call, inline }));
  });
  return container;
}

describe("MacroOutput (mixed inline/block group content)", () => {
  test("renders an {{info}}-like box with mixed inline and block children without crashing", () => {
    // A group (the info box) whose children mix a block, bare-string and styled-text inline content, and a nested
    // group: the structure BlockNote's schema rejects (inline as a sibling of blocks), which React renders freely.
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

    const container = render(output, { name: "info", parameters: {} });

    // The box and its nested groups are rendered as plain DIVs carrying their xwikiParameters (the box class).
    const box = container.querySelector<HTMLElement>("div.box.infomessage");
    expect(box).not.toBeNull();
    expect(box!.querySelector("div.box-title")).not.toBeNull();
    expect(container.textContent).toContain("Information");

    // Zero wrapper: the inline run ("Information") is a direct text child of the box, not wrapped in a <p>.
    const inlineText = Array.from(box!.childNodes).some(
      (node) =>
        node.nodeType === node.TEXT_NODE &&
        node.textContent?.includes("Information"),
    );
    expect(inlineText).toBe(true);
  });

  test("renders a group nested inside a plain block (list item)", () => {
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

    const container = render(output, { name: "html", parameters: {} });

    expect(container.querySelector("ul")).not.toBeNull();
    expect(container.querySelector("div.nested-box")).not.toBeNull();
    expect(container.textContent).toContain("deep");
  });

  test("applies xwikiParameters (object form) of a styled text inside macro output", () => {
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

    const container = render(output, { name: "info", parameters: {} });

    const span = container.querySelector<HTMLElement>("span.sr-only");
    expect(span).not.toBeNull();
    expect(span!.textContent).toBe("label");
  });

  test("substitutes an editable parameter marker matched case-insensitively", () => {
    // The xwikiEditable marker carries the descriptor's canonical parameter name ("title") while the macro call keeps
    // the case the user wrote ("tiTle"); the substitution must still find the value.
    const output = [{ type: "xwikiEditable", name: "title" }];

    const container = render(output, {
      name: "info",
      parameters: { tiTle: "Hello" },
    });

    expect(container.textContent).toContain("Hello");
  });

  test("groups consecutive numbered list items into an ordered list", () => {
    const output = [
      { type: "numberedListItem", props: {}, content: "one", children: [] },
      { type: "numberedListItem", props: {}, content: "two", children: [] },
    ];

    const container = render(output, { name: "x", parameters: {} });

    const list = container.querySelector("ol");
    expect(list).not.toBeNull();
    expect(list!.querySelectorAll("li")).toHaveLength(2);
  });
});

describe("MacroOutput (inline output with non-inline nodes)", () => {
  test("renders inline output that mixes an xwikiRaw image with styled text (no placeholder)", () => {
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
    ];

    const container = render(
      output,
      { name: "success", parameters: {}, content: "body" },
      true,
    );

    expect(container.querySelector("img")).not.toBeNull();
    const span = container.querySelector<HTMLElement>("span.successmessage");
    expect(span).not.toBeNull();
    expect(span!.textContent).toBe("Success");
    expect(container.textContent).toContain("body");
  });

  test("renders plain all-inline output", () => {
    const output = [{ type: "text", text: "hello", styles: {} }];

    const container = render(output, { name: "x", parameters: {} }, true);

    expect(container.textContent).toContain("hello");
    expect(container.querySelector("img")).toBeNull();
  });
});

describe("MacroOutput (nested macros)", () => {
  test("renders a nested block macro inside a verbatim macro content", () => {
    // An {{info}} box whose content is a nested {{error}} macro: the inner macro carries its own call + output and must
    // render (its output's own xwikiEditable substituted against its own call), not blank.
    const errorMacro = {
      type: "xwikiMacroBlock",
      props: {
        call: {
          name: "error",
          parameters: {},
          content: [{ type: "paragraph", props: {}, content: "test" }],
        },
        output: [
          {
            type: "xwikiGroup",
            props: { xwikiParameters: { class: "box errormessage" } },
            children: [{ type: "xwikiEditable" }],
          },
        ],
      },
    };
    const output = [
      {
        type: "xwikiGroup",
        props: { xwikiParameters: { class: "box infomessage" } },
        children: [{ type: "xwikiEditable" }],
      },
    ];
    const call: MacroCall = {
      name: "info",
      parameters: {},
      content: [
        { type: "paragraph", props: {}, content: "before" },
        errorMacro,
        { type: "paragraph", props: {}, content: "after" },
      ],
    };

    const container = render(output, call);

    const infoBox = container.querySelector<HTMLElement>("div.box.infomessage");
    expect(infoBox).not.toBeNull();
    // The nested error box renders inside the info box, with its own content substituted.
    expect(infoBox!.querySelector("div.box.errormessage")).not.toBeNull();
    expect(container.textContent).toContain("before");
    expect(container.textContent).toContain("after");
    expect(container.textContent).toContain("test");
  });

  test("renders a nested inline macro inside a paragraph's content", () => {
    const inlineMacro = {
      type: "xwikiInlineMacro",
      props: {
        call: { name: "color", parameters: { c: "red" }, content: "x" },
        output: [
          { type: "text", text: "E:", styles: {} },
          { type: "xwikiEditable" },
        ],
      },
    };
    const call: MacroCall = {
      name: "box",
      parameters: {},
      content: [
        {
          type: "paragraph",
          props: {},
          content: ["some ", inlineMacro, " end"],
        },
      ],
    };
    const output = [{ type: "xwikiEditable" }];

    const container = render(output, call);

    const nested = container.querySelector<HTMLElement>(
      '[data-macro-name="color"]',
    );
    expect(nested).not.toBeNull();
    expect(nested!.textContent).toBe("E:x");
    expect(container.textContent).toContain("some ");
    expect(container.textContent).toContain("end");
  });

  test("stops recursing at the nesting limit instead of overflowing the stack", () => {
    // Build a chain of macros nested through their content deeper than MAX_NESTING (100).
    let content: unknown = [
      { type: "paragraph", props: {}, content: "deepest" },
    ];
    for (let i = 0; i < 150; i++) {
      content = [
        {
          type: "xwikiMacroBlock",
          props: {
            call: { name: `m${i}`, parameters: {}, content },
            output: [
              {
                type: "xwikiGroup",
                props: {},
                children: [{ type: "xwikiEditable" }],
              },
            ],
          },
        },
      ];
    }
    const call: MacroCall = { name: "root", parameters: {}, content };
    const output = [{ type: "xwikiEditable" }];

    expect(() => render(output, call)).not.toThrow();
  });
});

describe("XWikiParametersStyle", () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let editor: BlockNoteEditor<any, any, any>;

  beforeAll(() => {
    editor = BlockNoteEditor.create({
      schema: createBlockNoteSchema([]),
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    }) as BlockNoteEditor<any, any, any>;
  });

  test("applies xwikiParameters (JSON string form) as span attributes on export", () => {
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

    const container = document.createElement("div");
    container.innerHTML = html;
    const span = container.querySelector<HTMLElement>("span.foo");
    expect(span).not.toBeNull();
    expect(span!.textContent).toBe("x");
  });
});

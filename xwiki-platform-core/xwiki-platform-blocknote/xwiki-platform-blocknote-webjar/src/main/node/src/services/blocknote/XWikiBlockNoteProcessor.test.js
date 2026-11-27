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
import { XWikiBlockNoteProcessor } from "./XWikiBlockNoteProcessor";
import {
  getLinkMetadata,
  parseMetadata,
  setLinkMetadata,
  stringifyMetadata,
  stripLinkMetadata,
} from "@xwiki/platform-editors-blocknote-react";
import { beforeEach, describe, expect, it } from "vitest";

// A block macro node with an object call and an array output.
function blockMacro() {
  return {
    id: "m1",
    type: "xwikiMacroBlock",
    props: {
      call: { name: "info", parameters: { title: "Note" }, content: "hi" },
      output: [{ type: "paragraph", props: {}, content: "hi", children: [] }],
    },
    children: [],
  };
}

// A paragraph holding an inline macro (with an object call and an inline content output) between two text runs.
function documentWithInlineMacro() {
  return [
    {
      id: "p1",
      type: "paragraph",
      props: {},
      content: [
        { type: "text", text: "first ", styles: {} },
        {
          type: "xwikiInlineMacro",
          props: {
            call: {
              name: "success",
              parameters: { title: "OK" },
              content: "done",
            },
            output: [{ type: "text", text: "done", styles: {} }],
          },
        },
        { type: "text", text: " line", styles: {} },
      ],
      children: [],
    },
  ];
}

// A block macro whose content holds a nested macro call, with its own output (the content is editable in-place).
function blockMacroWithNestedMacro() {
  return {
    id: "m2",
    type: "xwikiMacroBlock",
    props: {
      call: {
        name: "info",
        parameters: {},
        content: [
          {
            type: "xwikiMacroBlock",
            props: {
              call: { name: "velocity", parameters: {}, content: "$x" },
              output: [{ type: "paragraph", content: "secret" }],
            },
          },
        ],
      },
      output: [{ type: "xwikiEditable" }],
    },
    children: [],
  };
}

describe("XWikiBlockNoteProcessor", () => {
  let processor;

  beforeEach(() => {
    processor = new XWikiBlockNoteProcessor(new XWikiBlockNoteIterator());
  });

  // Builds a document with a single paragraph holding a single link with the given props.
  function documentWithLink(linkProps) {
    return [
      {
        id: "p1",
        type: "paragraph",
        props: {},
        content: [
          {
            type: "link",
            props: linkProps,
            href: "/xwiki/bin/view/Space/Page",
            content: [{ type: "text", text: "label", styles: {} }],
          },
        ],
        children: [],
      },
    ];
  }

  function firstLink(content) {
    return content[0].content[0];
  }

  describe("load", () => {
    it("moves the link metadata to the link URL", () => {
      const reference = {
        type: "doc",
        typed: true,
        reference: "Space.Page",
        parameters: {},
      };
      const document = processor.load(
        JSON.stringify(
          documentWithLink({
            xwikiReference: reference,
            xwikiParameters: { class: "myclass" },
            xwikiFreestanding: true,
          }),
        ),
      );

      const link = firstLink(document.content);
      // The BlockNote link schema has no props: they must be removed.
      expect(link.props).toBeUndefined();
      expect(stripLinkMetadata(link.href)).toBe("/xwiki/bin/view/Space/Page");
      expect(getLinkMetadata(link.href)).toEqual({
        xwikiReference: reference,
        xwikiParameters: { class: "myclass" },
        xwikiFreestanding: true,
      });
    });

    it("normalizes bare string elements in link content into text objects", () => {
      const doc = documentWithLink({});
      firstLink(doc).content = ["label"];
      const document = processor.load(JSON.stringify(doc));
      expect(firstLink(document.content).content).toEqual([
        { type: "text", text: "label", styles: {} },
      ]);
    });

    it("leaves the URL of a link without metadata untouched", () => {
      const document = processor.load(JSON.stringify(documentWithLink({})));
      const link = firstLink(document.content);
      expect(link.props).toBeUndefined();
      expect(link.href).toBe("/xwiki/bin/view/Space/Page");
    });

    it("moves the block metadata to the metadata property of the block", () => {
      const document = processor.load(
        JSON.stringify([
          {
            id: "p1",
            type: "paragraph",
            props: {
              textAlignment: "center",
              xwikiParameters: { class: "box" },
            },
            content: "text",
            children: [],
          },
        ]),
      );

      const { props } = document.content[0];
      expect(props.textAlignment).toBe("center");
      expect(props).not.toHaveProperty("xwikiParameters");
      expect(parseMetadata(props.metadata)).toEqual({
        xwikiParameters: { class: "box" },
      });
    });

    it("ignores the metadata property already held by a loaded block", () => {
      const forged = stringifyMetadata({
        xwikiParameters: { onclick: "alert(1)" },
      });
      const document = processor.load(
        JSON.stringify([
          {
            id: "p1",
            type: "paragraph",
            props: { metadata: forged, xwikiParameters: { class: "box" } },
            content: "text",
            children: [],
          },
          {
            id: "p2",
            type: "paragraph",
            props: { metadata: forged },
            content: "text",
            children: [],
          },
        ]),
      );

      expect(parseMetadata(document.content[0].props.metadata)).toEqual({
        xwikiParameters: { class: "box" },
      });
      expect(document.content[1].props).not.toHaveProperty("metadata");
      const saved = JSON.parse(processor.save(document));
      expect(saved[0].props).toEqual({ xwikiParameters: { class: "box" } });
      expect(saved[1].props).toEqual({});
    });

    // Loads a link whose URL already holds some (forged) metadata, with the given props.
    function loadLinkWithForgedMetadata(linkProps) {
      const doc = documentWithLink(linkProps);
      firstLink(doc).href = setLinkMetadata("/xwiki/bin/view/Space/Page", {
        xwikiReference: {
          type: "url",
          typed: true,
          reference: "https://evil.example.org/",
        },
      });
      return processor.load(JSON.stringify(doc));
    }

    it("replaces the metadata already held by the URL of a loaded link", () => {
      const document = loadLinkWithForgedMetadata({
        xwikiParameters: { class: "box" },
      });

      const link = firstLink(document.content);
      expect(stripLinkMetadata(link.href)).toBe("/xwiki/bin/view/Space/Page");
      expect(getLinkMetadata(link.href)).toEqual({
        xwikiParameters: { class: "box" },
      });
      expect(firstLink(JSON.parse(processor.save(document))).props).toEqual({
        xwikiParameters: { class: "box" },
      });
    });

    it("removes the metadata already held by the URL of a loaded link without metadata", () => {
      const document = loadLinkWithForgedMetadata({});

      expect(firstLink(document.content).href).toBe(
        "/xwiki/bin/view/Space/Page",
      );
      const saved = firstLink(JSON.parse(processor.save(document)));
      expect(saved.props).toBeUndefined();
      expect(saved.href).toBe("/xwiki/bin/view/Space/Page");
    });
  });

  describe("load then save round-trip", () => {
    it("restores the full link metadata and the original href", () => {
      const linkProps = {
        xwikiReference: {
          type: "doc",
          typed: true,
          reference: "Space.Page",
          parameters: {},
        },
        xwikiParameters: { class: "myclass" },
        xwikiFreestanding: true,
      };
      const document = processor.load(
        JSON.stringify(documentWithLink(linkProps)),
      );

      const saved = JSON.parse(processor.save(document));
      const link = firstLink(saved);
      expect(link.props).toEqual(linkProps);
      expect(link.href).toBe("/xwiki/bin/view/Space/Page");
    });

    it("restores a link that only has a resource reference", () => {
      const linkProps = {
        xwikiReference: {
          type: "url",
          typed: false,
          reference: "http://example.com",
          parameters: {},
        },
      };
      const document = processor.load(
        JSON.stringify(documentWithLink(linkProps)),
      );

      const saved = JSON.parse(processor.save(document));
      const link = firstLink(saved);
      expect(link.props).toEqual(linkProps);
      expect(link.href).toBe("/xwiki/bin/view/Space/Page");
    });

    it("restores the block metadata, and drops the empty metadata property", () => {
      const document = processor.load(
        JSON.stringify([
          {
            id: "p1",
            type: "paragraph",
            props: { xwikiParameters: { class: "box" } },
            content: "text",
            children: [],
          },
          {
            id: "p2",
            type: "paragraph",
            props: {},
            content: "text",
            children: [],
          },
        ]),
      );
      // The editor gives the default value to the metadata property of the blocks without metadata.
      document.content[1].props.metadata = "";

      const saved = JSON.parse(processor.save(document));
      expect(saved[0].props).toEqual({ xwikiParameters: { class: "box" } });
      expect(saved[1].props).toEqual({});
    });

    it("leaves a link without metadata unchanged", () => {
      const document = processor.load(JSON.stringify(documentWithLink({})));
      const saved = JSON.parse(processor.save(document));
      const link = firstLink(saved);
      expect(link.props).toBeUndefined();
      expect(link.href).toBe("/xwiki/bin/view/Space/Page");
    });
  });

  describe("generated label", () => {
    // A link without a label is rendered with an empty content and its generated label stored in the
    // xwikiGeneratedLabel property.
    function documentWithGeneratedLabel(generatedLabel) {
      return [
        {
          id: "p1",
          type: "paragraph",
          props: {},
          content: [
            {
              type: "link",
              props: {
                xwikiReference: {
                  type: "doc",
                  typed: true,
                  reference: "Space.Page",
                  parameters: {},
                },
                xwikiGeneratedLabel: generatedLabel,
              },
              href: "/xwiki/bin/view/Space/Page",
              content: [],
            },
          ],
          children: [],
        },
      ];
    }

    it("shows the generated label as the link content on load", () => {
      const document = processor.load(
        JSON.stringify(documentWithGeneratedLabel("Page")),
      );
      const link = firstLink(document.content);
      expect(link.content).toEqual([
        { type: "text", text: "Page", styles: {} },
      ]);
    });

    it("keeps the marker on save when the generated label is not changed", () => {
      const document = processor.load(
        JSON.stringify(documentWithGeneratedLabel("Page")),
      );
      const saved = JSON.parse(processor.save(document));
      const link = firstLink(saved);
      expect(link.props.xwikiGeneratedLabel).toBe("Page");
      expect(link.props.xwikiReference.reference).toBe("Space.Page");
    });

    it("drops the marker on save when the user changed the generated label", () => {
      const document = processor.load(
        JSON.stringify(documentWithGeneratedLabel("Page")),
      );
      // Simulate the user editing the label.
      firstLink(document.content).content = [
        { type: "text", text: "Custom", styles: {} },
      ];
      const saved = JSON.parse(processor.save(document));
      const link = firstLink(saved);
      expect(link.props.xwikiGeneratedLabel).toBeUndefined();
      expect(link.props.xwikiReference.reference).toBe("Space.Page");
      expect(link.content).toEqual([
        { type: "text", text: "Custom", styles: {} },
      ]);
    });
  });

  describe("macros", () => {
    it("moves the output of a block macro to the output of the document macro calls on load", () => {
      const macro = blockMacro();
      const document = processor.load(JSON.stringify([macro]));
      const loaded = document.content[0];
      expect(loaded.props.call).toBe(JSON.stringify(macro.props.call));
      expect(loaded.props).not.toHaveProperty("output");
      // The output corresponds to a specific macro call.
      expect(document.macroOutputs).toEqual({
        m1: { call: loaded.props.call, output: macro.props.output },
      });
    });

    it("binds the output of a block macro without id to a generated block id", () => {
      const macro = blockMacro();
      delete macro.id;
      const document = processor.load(JSON.stringify([macro]));
      const loaded = document.content[0];
      expect(loaded.id).toBeTruthy();
      expect(document.macroOutputs[loaded.id]).toEqual({
        call: loaded.props.call,
        output: macro.props.output,
      });
    });

    it("moves the output of an inline macro to the output of the document macro calls on load", () => {
      const doc = documentWithInlineMacro();
      const inlineMacro = doc[0].content[1];
      const document = processor.load(JSON.stringify(doc));
      const loaded = document.content[0].content[1];
      expect(loaded.props.call).toBe(JSON.stringify(inlineMacro.props.call));
      expect(loaded.props).not.toHaveProperty("output");
      expect(document.macroOutputs[loaded.props.id]).toEqual({
        call: loaded.props.call,
        output: inlineMacro.props.output,
      });
    });

    it("moves the output of a macro nested in the macro content to the output of the document macro calls on load", () => {
      const macro = blockMacroWithNestedMacro();
      const document = processor.load(JSON.stringify([macro]));
      // The macro output must not be part of the edited content (it can be shared with other users).
      expect(JSON.stringify(document.content)).not.toContain("secret");
      const call = JSON.parse(document.content[0].props.call);
      const nestedMacro = call.content[0];
      expect(nestedMacro.props).not.toHaveProperty("output");
      expect(document.macroOutputs[nestedMacro.props.id]).toEqual({
        call: JSON.stringify(nestedMacro.props.call),
        output: [{ type: "paragraph", content: "secret" }],
      });
    });

    it("binds the output of a macro holding nested macros to its call as it is in the loaded content", () => {
      const document = processor.load(
        JSON.stringify([blockMacroWithNestedMacro()]),
      );
      // The nested macro call, part of the call of its parent, is loaded (its output moved, an id given) before its
      // parent's output is bound to the call of its parent, so that the editor finds the output of both.
      expect(document.macroOutputs.m2).toEqual({
        call: document.content[0].props.call,
        output: [{ type: "xwikiEditable" }],
      });
    });

    it("restores the call object of a block macro on save round-trip, without the output", () => {
      const macro = blockMacro();
      const document = processor.load(JSON.stringify([macro]));
      // The editor gives the default value to the metadata property of the macro blocks.
      document.content[0].props.metadata = "";
      const saved = JSON.parse(processor.save(document));
      expect(saved[0].props).toEqual({ call: macro.props.call });
    });

    it("restores the call object of an inline macro on save round-trip, without the output and the id", () => {
      const doc = documentWithInlineMacro();
      const inlineMacro = doc[0].content[1];
      const document = processor.load(JSON.stringify(doc));
      const saved = JSON.parse(processor.save(document));
      expect(saved[0].content[1].props).toEqual({
        call: inlineMacro.props.call,
      });
    });

    it("removes the id of the macros nested in the macro content on save", () => {
      const macro = blockMacroWithNestedMacro();
      const document = processor.load(JSON.stringify([macro]));
      const saved = JSON.parse(processor.save(document));
      expect(saved[0].props.call.content[0].props).toEqual({
        call: { name: "velocity", parameters: {}, content: "$x" },
      });
    });

    // What the edited content needs to be rendered again, after its macro calls are modified.
    describe("rendering", () => {
      it("remembers that a macro without output was rendered, and gives an id to the macros not rendered", () => {
        const macro = blockMacro();
        macro.props.output = [];
        const doc = documentWithInlineMacro();
        delete doc[0].content[1].props.output;
        const document = processor.load(JSON.stringify([macro, ...doc]));
        // An empty output is kept, so that the macro call isn't rendered again.
        expect(document.macroOutputs).toEqual({
          m1: { call: document.content[0].props.call, output: [] },
        });
        // A macro call that wasn't rendered gets an id nevertheless, to keep its output once it's rendered.
        expect(document.content[1].content[1].props.id).toBeTruthy();
      });

      it("keeps the id of each macro call as a macro parameter, when asked", () => {
        const document = processor.load(
          JSON.stringify([
            ...documentWithInlineMacro(),
            blockMacroWithNestedMacro(),
          ]),
        );
        const inlineMacroId = document.content[0].content[1].props.id;
        const nestedMacroId = JSON.parse(document.content[1].props.call)
          .content[0].props.id;
        const saved = JSON.parse(processor.save(document, { macroIds: true }));
        expect(saved[0].content[1].props).toEqual({
          call: {
            name: "success",
            parameters: { title: "OK", xwikiBlockNoteMacroId: inlineMacroId },
            content: "done",
          },
        });
        expect(saved[1].props.call.parameters).toEqual({
          xwikiBlockNoteMacroId: "m2",
        });
        expect(saved[1].props.call.content[0].props).toEqual({
          call: {
            name: "velocity",
            parameters: { xwikiBlockNoteMacroId: nestedMacroId },
            content: "$x",
          },
        });
      });
    });
  });

  describe("code block", () => {
    function codeBlock(props) {
      return {
        id: "c1",
        type: "codeBlock",
        props,
        content: [{ type: "text", text: "test", styles: {} }],
        children: [],
      };
    }

    it("moves the verbatim and new line flags to the block metadata on load and restores them on save", () => {
      const props = {
        language: "java",
        xwikiLeadingNewLine: false,
        xwikiTrailingNewLine: false,
        xwikiVerbatim: true,
      };
      const document = processor.load(JSON.stringify([codeBlock(props)]));
      // The BlockNote code block schema doesn't support these props.
      const { metadata, ...loaded } = document.content[0].props;
      expect(loaded).toEqual({ language: "java" });
      expect(parseMetadata(metadata)).toEqual({
        xwikiLeadingNewLine: false,
        xwikiTrailingNewLine: false,
        xwikiVerbatim: true,
      });

      const saved = JSON.parse(processor.save(document));
      expect(saved[0].props).toEqual(props);
    });

    it("doesn't add the verbatim and new line flags on save when they are missing", () => {
      const document = processor.load(
        JSON.stringify([codeBlock({ language: "text" })]),
      );
      const saved = JSON.parse(processor.save(document));
      expect(saved[0].props).toEqual({});
    });
  });
});

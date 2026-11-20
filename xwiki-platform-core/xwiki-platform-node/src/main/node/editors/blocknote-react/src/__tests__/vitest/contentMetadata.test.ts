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
import {
  XWIKI_METADATA_KEYS,
  XWIKI_PARAMETERS,
  XWIKI_REFERENCE,
} from "../../features/xwikiRendering/metadataKeys";
import {
  getBlockMetadata,
  getLinkMetadata,
  parseMetadata,
  setBlockMetadata,
  setLinkMetadata,
  stringifyMetadata,
  stripLinkMetadata,
} from "../../misc/contentMetadata";
import { PastedMetadataFilterExtension } from "../../misc/pastedMetadata";
import { BlockNoteEditor } from "@blocknote/core";
import { withCollaboration } from "@blocknote/core/yjs";
import { describe, expect, test } from "vitest";
import { Awareness } from "y-protocols/awareness";
import { Doc } from "yjs";
import type { EditorType } from "../../blocknote";

// jsdom doesn't implement the clipboard events, which ProseMirror creates when pasting HTML programmatically.
globalThis.ClipboardEvent ??= class extends Event {
  clipboardData = null;
} as unknown as typeof ClipboardEvent;

const PAGE_REFERENCE = {
  type: "doc",
  typed: true,
  reference: "Some.Page",
  parameters: {},
};

function createEditor(doc?: Doc): EditorType {
  const options = {
    schema: createBlockNoteSchema(),
    extensions: [
      PastedMetadataFilterExtension({ keys: [...XWIKI_METADATA_KEYS] }),
    ],
  };
  const editor = BlockNoteEditor.create(
    doc
      ? withCollaboration({
          ...options,
          collaboration: {
            fragment: doc.getXmlFragment("content"),
            provider: { awareness: new Awareness(doc) },
            user: { name: "Alice", color: "#000" },
          },
        })
      : options,
  ) as unknown as EditorType;
  const element = document.createElement("div");
  document.body.appendChild(element);
  editor.mount(element);
  return editor;
}

// Wait for the binding of the collaboration session, and for the end of the undo capture timeout (so that the next
// change is a separate undo step).
const settle = () => new Promise((resolve) => setTimeout(resolve, 600));

describe("parseMetadata and stringifyMetadata", () => {
  test("serialize the metadata as a JSON object", () => {
    const metadata = { xwikiReference: PAGE_REFERENCE, xwikiVerbatim: true };
    expect(parseMetadata(stringifyMetadata(metadata))).toEqual(metadata);
  });

  test("serialize the metadata without entries as the empty string", () => {
    expect(stringifyMetadata({})).toBe("");
    expect(stringifyMetadata({ xwikiVerbatim: undefined })).toBe("");
    expect(parseMetadata("")).toEqual({});
  });

  test("ignore what isn't a JSON object", () => {
    expect(parseMetadata(undefined)).toEqual({});
    expect(parseMetadata("not JSON")).toEqual({});
    expect(parseMetadata("[1, 2]")).toEqual({});
    expect(parseMetadata("null")).toEqual({});
  });
});

describe("link metadata", () => {
  const metadata = {
    xwikiReference: PAGE_REFERENCE,
    xwikiGeneratedLabel: "Pâge – ünicode",
  };

  test.each([
    "https://www.xwiki.org/xwiki/bin/view/Main/?key=value#section",
    "/xwiki/bin/view/Some/Page",
    "Page",
    "#section",
    "?key=value",
    "mailto:alice@example.org",
  ])("is kept in the link URL [%s]", (href) => {
    const withMetadata = setLinkMetadata(href, metadata);
    expect(withMetadata).not.toBe(href);
    expect(getLinkMetadata(withMetadata)).toEqual(metadata);
    expect(stripLinkMetadata(withMetadata)).toBe(href);
  });

  test("replaces the metadata held by the link URL", () => {
    const href = setLinkMetadata("https://www.xwiki.org/", metadata);
    const changed = setLinkMetadata(href, { xwikiFreestanding: true });
    expect(getLinkMetadata(changed)).toEqual({ xwikiFreestanding: true });
  });

  test("leaves the URL of a link without metadata untouched", () => {
    expect(setLinkMetadata("https://www.xwiki.org", {})).toBe(
      "https://www.xwiki.org",
    );
    expect(getLinkMetadata("https://www.xwiki.org")).toEqual({});
  });
});

describe("block metadata", () => {
  test("can be kept in all the block types", () => {
    const editor = createEditor();
    const metadata = stringifyMetadata({
      xwikiParameters: { class: "box" },
    });
    const types = [
      "paragraph",
      "heading",
      "quote",
      "bulletListItem",
      "numberedListItem",
      "checkListItem",
      "toggleListItem",
      "codeBlock",
      "image",
      "file",
      "audio",
      "video",
      "divider",
      "table",
    ];
    editor.replaceBlocks(
      editor.document,
      types.map((type) => ({
        type,
        props: { metadata },
        ...(type === "table"
          ? {
              content: {
                type: "tableContent",
                rows: [{ cells: ["cell"] }],
              },
            }
          : {}),
      })) as never,
    );

    expect(
      editor.document.map((block) => [
        block.type,
        getBlockMetadata(block).xwikiParameters,
      ]),
    ).toEqual(types.map((type) => [type, { class: "box" }]));
  });

  test("keeps the other entries when an entry is set", () => {
    const editor = createEditor();
    editor.replaceBlocks(editor.document, [
      { id: "p1", type: "paragraph", content: "text" },
    ]);

    setBlockMetadata(editor, "p1", XWIKI_PARAMETERS, { class: "box" });
    setBlockMetadata(editor, "p1", XWIKI_REFERENCE, PAGE_REFERENCE);
    expect(getBlockMetadata(editor.getBlock("p1")!)).toEqual({
      xwikiParameters: { class: "box" },
      xwikiReference: PAGE_REFERENCE,
    });

    setBlockMetadata(editor, "p1", XWIKI_PARAMETERS, undefined);
    expect(getBlockMetadata(editor.getBlock("p1")!)).toEqual({
      xwikiReference: PAGE_REFERENCE,
    });
  });

  // An image whose reference is kept in its metadata.
  async function createImage(doc?: Doc): Promise<EditorType> {
    const editor = createEditor(doc);
    await settle();
    editor.replaceBlocks(editor.document, [
      {
        id: "img1",
        type: "image",
        props: {
          url: "https://example.org/a.png",
          metadata: stringifyMetadata({ xwikiReference: PAGE_REFERENCE }),
        },
      },
    ] as never);
    await settle();
    return editor;
  }

  function getImage(editor: EditorType): unknown[] {
    const block = editor.getBlock("img1")!;
    return [
      (block.props as { url: string }).url,
      getBlockMetadata(block).xwikiReference,
    ];
  }

  test.each([
    ["without", undefined],
    ["with", new Doc()],
  ])(
    "is restored by undo / redo, %s realtime collaboration",
    async (_, doc) => {
      const editor = await createImage(doc);

      // Change the image, as the image editor does.
      const other = { ...PAGE_REFERENCE, reference: "Other.Page" };
      editor.transact(() => {
        editor.updateBlock("img1", {
          props: { url: "https://example.org/b.png" },
        });
        setBlockMetadata(editor, "img1", XWIKI_REFERENCE, other);
      });
      const image = () => getImage(editor);
      expect(image()).toEqual(["https://example.org/b.png", other]);

      editor.undo();
      expect(image()).toEqual(["https://example.org/a.png", PAGE_REFERENCE]);

      editor.redo();
      expect(image()).toEqual(["https://example.org/b.png", other]);
    },
  );
});

describe("pasted metadata", () => {
  // Copy some content, with its metadata, from an editor, and paste it in another.
  async function copyPaste(blocks: unknown[]): Promise<EditorType> {
    const source = createEditor();
    source.replaceBlocks(source.document, blocks as never);
    const html = await source.blocksToFullHTML(source.document);
    const target = createEditor();
    target.pasteHTML(html);
    return target;
  }

  test("keeps only the block metadata entries accepted by their key", async () => {
    const editor = await copyPaste([
      {
        type: "paragraph",
        props: {
          metadata: stringifyMetadata({
            xwikiParameters: { onclick: "alert(1)" },
            xwikiVerbatim: true,
            unknown: "value",
          }),
        },
        content: "text",
      },
    ]);

    const paragraph = editor.document.find(
      (block) => block.type === "paragraph" && block.content.length,
    )!;
    expect(getBlockMetadata(paragraph)).toEqual({ xwikiVerbatim: true });
  });

  test.each([
    [PAGE_REFERENCE, "https://www.xwiki.org/xwiki/bin/view/Some/Page"],
    [
      { type: "url", typed: false, reference: "https://www.xwiki.org/" },
      "https://www.xwiki.org/",
    ],
  ])(
    "keeps the references to wiki resources and to what is displayed: %o",
    async (reference, url) => {
      const editor = await copyPaste([
        {
          type: "image",
          props: {
            url,
            metadata: stringifyMetadata({ xwikiReference: reference }),
          },
        },
      ]);

      const image = editor.document.find((block) => block.type === "image")!;
      expect(getBlockMetadata(image)).toEqual({ xwikiReference: reference });
    },
  );

  test("drops the references to something else than what is displayed", async () => {
    const editor = await copyPaste([
      {
        type: "image",
        props: {
          url: "https://www.xwiki.org/logo.png",
          metadata: stringifyMetadata({
            xwikiReference: {
              type: "url",
              typed: true,
              reference: "https://evil.example.org/logo.png",
            },
          }),
        },
      },
    ]);

    const image = editor.document.find((block) => block.type === "image")!;
    expect(getBlockMetadata(image)).toEqual({});
  });

  test("filters the metadata of the pasted links", async () => {
    const href = "https://www.xwiki.org/xwiki/bin/view/Some/Page";
    const editor = await copyPaste([
      {
        type: "paragraph",
        content: [
          {
            type: "link",
            href: setLinkMetadata(href, {
              xwikiReference: PAGE_REFERENCE,
              xwikiParameters: { style: "position: fixed" },
              xwikiFreestanding: true,
            }),
            content: "label",
          },
        ],
      },
    ]);

    const link = editor.document
      .flatMap((block) => block.content as { type: string; href?: string }[])
      .find((content) => content?.type === "link")!;
    expect(stripLinkMetadata(link.href!)).toBe(href);
    expect(getLinkMetadata(link.href!)).toEqual({
      xwikiReference: PAGE_REFERENCE,
      xwikiFreestanding: true,
    });
  });
});

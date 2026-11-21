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
import { XWikiMacroRenderer } from "./XWikiMacroRenderer";
import { DefaultBlockNoteIterator } from "../blocknote/DefaultBlockNoteIterator";
import { XWikiBlockNoteProcessor } from "../blocknote/XWikiBlockNoteProcessor";
import {
  createBlockMetadataStore,
  getMacroOutput,
} from "@xwiki/platform-editors-blocknote-react";
import { toCristalEntityReference } from "@xwiki/platform-model-xwiki";
import { EntityType, Model } from "@xwiki/platform-xwiki-model-api";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const infoCall = { name: "info", parameters: {}, content: "Information" };
const errorCall = { name: "error", parameters: {}, content: "Error" };
const boxCall = {
  name: "box",
  parameters: {},
  content: [{ type: "xwikiMacroBlock", props: { call: errorCall, id: "n1" } }],
};

function createRenderer(options = { restricted: false }) {
  const iterator = new DefaultBlockNoteIterator();
  return new XWikiMacroRenderer(
    new XWikiBlockNoteProcessor(iterator),
    iterator,
    options,
  );
}

// The edited content: a block macro whose content holds a nested macro, and a paragraph holding an inline macro.
function editedContent() {
  return [
    {
      id: "b1",
      type: "xwikiMacroBlock",
      props: { call: JSON.stringify(boxCall) },
      children: [],
    },
    {
      id: "p1",
      type: "paragraph",
      props: {},
      content: [
        {
          type: "xwikiInlineMacro",
          props: { call: JSON.stringify(infoCall), id: "i1" },
        },
      ],
      children: [],
    },
  ];
}

// The rendered content, as sent back by the server: each macro call has its output, and its id as a parameter.
function renderedContent() {
  return [
    {
      id: "b1",
      type: "xwikiMacroBlock",
      props: {
        call: {
          name: "box",
          parameters: { xwikiblocknotemacroid: "b1" },
          content: [
            {
              type: "xwikiMacroBlock",
              props: {
                call: {
                  ...errorCall,
                  parameters: { xwikiBlockNoteMacroId: "n1" },
                },
                output: [{ type: "paragraph", content: "Error" }],
              },
            },
          ],
        },
        output: [{ type: "xwikiEditable" }],
      },
      children: [],
    },
    {
      id: "p1",
      type: "paragraph",
      props: {},
      content: [
        {
          type: "xwikiInlineMacro",
          props: {
            call: {
              ...infoCall,
              parameters: { xwikiBlockNoteMacroId: "i1" },
            },
            // The macro produced no output.
          },
        },
      ],
      children: [],
    },
  ];
}

describe("XWikiMacroRenderer", () => {
  let requests;
  let respond;
  let instance;

  beforeEach(() => {
    requests = [];
    respond = () => ({ ok: true, json: async () => renderedContent() });
    global.fetch = vi.fn(async (url, init) => {
      requests.push({ url, parameters: init.body });
      return respond();
    });
    global.XWiki = {
      Document: class {
        constructor(reference) {
          this.reference = reference;
        }

        getURL(action, parameters) {
          return `/${action}/${Model.serialize(this.reference)}?${parameters}`;
        }
      },
    };
    document.documentElement.dataset.xwikiFormToken = "token";
    instance = {
      editor: { document: editedContent() },
      metadata: createBlockMetadataStore(),
      options: {
        syntax: "xwiki/2.1",
        sourceDocumentReference: toCristalEntityReference(
          Model.resolve("wiki:Space.Source", EntityType.DOCUMENT),
        ),
      },
    };
  });

  afterEach(() => {
    delete document.documentElement.dataset.xwikiFormToken;
  });

  it("renders the edited content for its source document, with the id of each macro call", async () => {
    await createRenderer({
      restricted: true,
      locale: "fr",
      collaboration: "wiki%3ASpace.Source/fr",
    }).render(instance);

    expect(requests).toHaveLength(1);
    const { url, parameters } = requests[0];
    expect(url).toBe(
      "/get/wiki:Space.Source?xpage=blocknote%2Frender&language=fr",
    );
    expect(Object.fromEntries(parameters)).toMatchObject({
      syntax: "xwiki/2.1",
      restricted: "true",
      form_token: "token",
      // The content is rendered with the rights of the realtime collaboration session participants.
      collaboration: "wiki%3ASpace.Source/fr",
    });
    // The id of each macro call is sent as a macro parameter.
    const sent = JSON.parse(parameters.get("content"));
    expect(
      [
        sent[0].props.call,
        sent[0].props.call.content[0].props.call,
        sent[1].content[0].props.call,
      ].map((call) => call.parameters.xwikiBlockNoteMacroId),
    ).toEqual(["b1", "n1", "i1"]);
  });

  it("keeps the output of each rendered macro call", async () => {
    await createRenderer().render(instance);

    const { metadata } = instance;
    expect(getMacroOutput(metadata, "b1", JSON.stringify(boxCall))).toEqual([
      { type: "xwikiEditable" },
    ]);
    expect(getMacroOutput(metadata, "n1", JSON.stringify(errorCall))).toEqual([
      { type: "paragraph", content: "Error" },
    ]);
    // The macro call was rendered, without output.
    expect(getMacroOutput(metadata, "i1", JSON.stringify(infoCall))).toEqual(
      [],
    );
  });

  it("ignores the output of a macro call modified while the content is rendered", async () => {
    const modifiedCall = JSON.stringify({ ...infoCall, content: "Modified" });
    respond = () => {
      // Modified (e.g. by another user editing in realtime) while being rendered.
      instance.editor.document[1].content[0].props.call = modifiedCall;
      return { ok: true, json: async () => renderedContent() };
    };

    await createRenderer().render(instance);

    expect(getMacroOutput(instance.metadata, "i1", modifiedCall)).toBe(
      undefined,
    );
    // The output is kept for the macro call that was rendered.
    expect(
      getMacroOutput(instance.metadata, "i1", JSON.stringify(infoCall)),
    ).toEqual([]);
  });

  it("fails when the edited content can't be rendered", async () => {
    respond = () => ({ ok: false, status: 500, statusText: "Server Error" });

    await expect(createRenderer().render(instance)).rejects.toThrow(
      "Failed to render the macro calls: 500 Server Error",
    );
    expect(
      getMacroOutput(instance.metadata, "b1", JSON.stringify(boxCall)),
    ).toBe(undefined);
  });

  it("doesn't render content without macro calls", async () => {
    instance.editor.document = [
      { id: "p1", type: "paragraph", props: {}, content: [], children: [] },
    ];

    await createRenderer().render(instance);

    expect(requests).toHaveLength(0);
  });
});

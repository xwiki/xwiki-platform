/**
 * See the LICENSE file distributed with this work for additional
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
import { DefaultMacrosAstToHtmlConverter } from "../default-macros-ast-to-html-converter";
import {
  AttachmentReference,
  DocumentReference,
  SpaceReference,
} from "@xwiki/cristal-model-api";
import { describe, expect, test } from "vitest";
import { any, mock } from "vitest-mock-extended";
import type {
  MacroBlockStyles,
  MacroInlineContent,
} from "@xwiki/cristal-macros-api";
import type {
  ModelReferenceParser,
  ModelReferenceParserProvider,
} from "@xwiki/cristal-model-reference-api";
import type {
  RemoteURLSerializer,
  RemoteURLSerializerProvider,
} from "@xwiki/cristal-model-remote-url-api";
import type { Container } from "inversify";

// eslint-disable-next-line max-statements
function init() {
  const modelReferenceParserProvider = mock<ModelReferenceParserProvider>();
  const remoteURLSerializerProvider = mock<RemoteURLSerializerProvider>();

  const containerMock = mock<Container>();

  containerMock.get
    .calledWith("ModelReferenceParserProvider")
    .mockReturnValue(modelReferenceParserProvider);

  containerMock.get
    .calledWith("RemoteURLSerializerProvider")
    .mockReturnValue(remoteURLSerializerProvider);

  const modelReferenceParser = mock<ModelReferenceParser>();
  const remoteURLSerializer = mock<RemoteURLSerializer>();

  modelReferenceParserProvider.get.mockReturnValue(modelReferenceParser);

  remoteURLSerializerProvider.get.mockReturnValue(remoteURLSerializer);

  const attachmentReference = new AttachmentReference(
    "image.png",
    new DocumentReference("B", new SpaceReference(undefined, "A")),
  );

  modelReferenceParser.parse
    .calledWith("A.B@image.png", any())
    .mockReturnValue(attachmentReference);

  remoteURLSerializer.serialize
    .calledWith(attachmentReference)
    .mockReturnValue("https://my.site/A/B/image.png");

  return { modelReferenceParserProvider, remoteURLSerializerProvider };
}

describe("MacrosAstToHtmlConverter", () => {
  const { modelReferenceParserProvider, remoteURLSerializerProvider } = init();

  const converter = new DefaultMacrosAstToHtmlConverter(
    modelReferenceParserProvider,
    remoteURLSerializerProvider,
  );

  function expectInlineContentToBe(
    inlineContents: MacroInlineContent[],
    expectedHtml: string,
  ) {
    expect(converter.inlineContentsToHTML(inlineContents)).toBe(expectedHtml);

    expect(
      converter.blocksToHTML([
        { type: "paragraph", styles: {}, content: inlineContents },
      ]),
    ).toBe(`<p>${expectedHtml}</p>`);
  }

  test("empty AST", () => {
    expectInlineContentToBe([], "");
  });

  test("simple text", () => {
    expectInlineContentToBe(
      [{ type: "text", content: "Hello world!", styles: {} }],
      "Hello world!",
    );
  });

  test("styled text", () => {
    expectInlineContentToBe(
      [
        {
          type: "text",
          content: "Hello world!",
          styles: {
            bold: true,
            italic: true,
            backgroundColor: "#000000",
            textColor: "#FFFFFF",
            code: true,
            strikethrough: true,
            underline: true,
          },
        },
      ],
      '<pre><span style="background-color: #000000;"><span style="color: #FFFFFF;"><u style="text-decoration: underline;"><s style="text-decoration: italic;"><em style="font-style: italic;"><strong style="font-weight: bold;">Hello world!</strong></em></s></u></span></span></pre>',
    );
  });

  test("text chaining", () => {
    expectInlineContentToBe(
      [
        { type: "text", content: "Hello", styles: { bold: true } },
        { type: "text", content: "World", styles: {} },
        { type: "text", content: "!", styles: { underline: true } },
      ],
      '<strong style="font-weight: bold;">Hello</strong>World<u style="text-decoration: underline;">!</u>',
    );
  });

  test("internal link", () => {
    expectInlineContentToBe(
      [
        {
          type: "link",
          content: [
            { type: "text", content: "I am an internal link", styles: {} },
          ],
          target: { type: "internal", rawReference: "A.B@image.png" },
        },
      ],
      '<a href="https://my.site/A/B/image.png">I am an internal link</a>',
    );
  });

  test("external link", () => {
    expectInlineContentToBe(
      [
        {
          type: "link",
          content: [
            { type: "text", content: "I am an external link", styles: {} },
          ],
          target: { type: "external", url: "https://perdu.com" },
        },
      ],
      '<a href="https://perdu.com">I am an external link</a>',
    );
  });

  test("all block types", () => {
    const content: MacroInlineContent[] = [
      { type: "text", content: "Hello!", styles: {} },
    ];
    const styles: MacroBlockStyles = { cssClasses: ["class1", "class2"] };

    const res = converter.blocksToHTML([
      { type: "paragraph", content, styles },
      { type: "heading", level: 1, content, styles },
      { type: "heading", level: 2, content, styles },
      { type: "heading", level: 3, content, styles },
      { type: "heading", level: 4, content, styles },
      { type: "heading", level: 5, content, styles },
      { type: "heading", level: 6, content, styles },
      {
        type: "list",
        numbered: false,
        items: [
          { content, styles },
          { checked: false, content, styles },
          { checked: true, content, styles },
        ],
        styles,
      },
      {
        type: "list",
        numbered: true,
        items: [
          { content, styles },
          { checked: false, content, styles },
          { checked: true, content, styles },
        ],
        styles,
      },
      {
        type: "quote",
        content: [{ type: "paragraph", content, styles }],
        styles,
      },
      { type: "code", content: "Some code here" },
      { type: "code", content: "Some code here", language: "some-language" },
      {
        type: "table",
        columns: [{}, { headerCell: { content, styles } }, { widthPx: 200 }],
        rows: [
          [
            { content, styles },
            { content, styles },
            { content, styles, rowSpan: 1 },
          ],
          [{ content, styles, colSpan: 2, rowSpan: 3 }],
        ],
        styles,
      },
      {
        type: "image",
        target: { type: "internal", rawReference: "A.B@image.png" },
      },
      {
        type: "image",
        target: { type: "internal", rawReference: "A.B@image.png" },
        alt: "Some alt caption",
        widthPx: 100,
        heightPx: 200,
      },
      {
        type: "image",
        target: { type: "external", url: "https://picsum.photos/536/354" },
      },
      {
        type: "rawHtml",
        html: "<script></script><style></style>",
      },
      {
        type: "macroBlockEditableArea",
      },
      {
        type: "paragraph",
        content: [
          {
            type: "inlineMacroEditableArea",
          },
        ],
        styles,
      },
    ]);

    expect(res).toBe(
      [
        '<p class="class1 class2">Hello!</p>',
        '<h1 class="class1 class2">Hello!</h1>',
        '<h2 class="class1 class2">Hello!</h2>',
        '<h3 class="class1 class2">Hello!</h3>',
        '<h4 class="class1 class2">Hello!</h4>',
        '<h5 class="class1 class2">Hello!</h5>',
        '<h6 class="class1 class2">Hello!</h6>',
        '<ul class="class1 class2">',
        "<li>Hello!</li>",
        '<li><input type="checkbox" checked="false" readonly="true">Hello!</li>',
        '<li><input type="checkbox" checked="true" readonly="true">Hello!</li>',
        "</ul>",
        '<ol class="class1 class2">',
        '<li>Hello!</li><li><input type="checkbox" checked="false" readonly="true">Hello!</li>',
        '<li><input type="checkbox" checked="true" readonly="true">Hello!</li>',
        "</ol>",
        '<blockquote class="class1 class2"><p class="class1 class2">Hello!</p></blockquote>',
        "<pre>Some code here</pre>",
        "<pre>Some code here</pre>",
        '<table class="class1 class2"><colgroup><col width=""><col width=""><col width="200px"></colgroup><thead>Hello!</thead><th class="class1 class2"></th><tbody><tr><td class="class1 class2">Hello!</td><td class="class1 class2">Hello!</td><td rowspan="1" class="class1 class2">Hello!</td></tr>,<tr><td colspan="2" rowspan="3" class="class1 class2">Hello!</td></tr></tbody></table>',
        '<img src="https://my.site/A/B/image.png">',
        '<img src="https://my.site/A/B/image.png" alt="Some alt caption" width="100px" height="200px">',
        '<img src="https://picsum.photos/536/354">',
        "<script></script><style></style>",
        "<!-- Macro block editable aera -->",
        '<p class="class1 class2">',
        "<!-- Macro inline editable aera --></p>",
      ].join(""),
    );
  });
});

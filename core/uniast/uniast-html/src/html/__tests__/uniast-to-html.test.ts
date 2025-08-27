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

import { UniAstToHTMLConverter } from "../uniast-to-html";
import {
  AttachmentReference,
  DocumentReference,
  EntityType,
  SpaceReference,
} from "@xwiki/cristal-model-api";
import { createConverterContext } from "@xwiki/cristal-uniast-utils";
import { Container } from "inversify";
import { describe, expect, test } from "vitest";
import { mock } from "vitest-mock-extended";
import type {
  ModelReferenceHandlerProvider,
  ModelReferenceParser,
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/cristal-model-reference-api";
import type {
  RemoteURLParserProvider,
  RemoteURLSerializer,
  RemoteURLSerializerProvider,
} from "@xwiki/cristal-model-remote-url-api";
import type { ConverterContext } from "@xwiki/cristal-uniast-api";

// eslint-disable-next-line max-statements
function getConverterContext(): ConverterContext {
  const modelReferenceParserProvider = mock<ModelReferenceParserProvider>();
  const remoteURLSerializerProvider = mock<RemoteURLSerializerProvider>();
  const modelReferenceParser = mock<ModelReferenceParser>();
  const remoteURLSerializer = mock<RemoteURLSerializer>();

  const containerMock = mock<Container>();

  containerMock.get
    .calledWith("ModelReferenceParserProvider")
    .mockReturnValue(modelReferenceParserProvider);

  containerMock.get
    .calledWith("ModelReferenceSerializerProvider")
    .mockReturnValue(mock<ModelReferenceSerializerProvider>());

  containerMock.get
    .calledWith("RemoteURLParserProvider")
    .mockReturnValue(mock<RemoteURLParserProvider>());
  containerMock.get
    .calledWith("RemoteURLSerializerProvider")
    .mockReturnValue(remoteURLSerializerProvider);

  containerMock.get
    .calledWith("ModelReferenceHandlerProvider")
    .mockReturnValue(mock<ModelReferenceHandlerProvider>());

  modelReferenceParserProvider.get.mockReturnValue(modelReferenceParser);
  remoteURLSerializerProvider.get.mockReturnValue(remoteURLSerializer);

  const attachmentReference = new AttachmentReference(
    "image.png",
    new DocumentReference("B", new SpaceReference(undefined, "A")),
  );
  modelReferenceParser.parse
    .calledWith("A.B@image.png", EntityType.ATTACHMENT)
    .mockReturnValue(attachmentReference);

  remoteURLSerializer.serialize
    .calledWith(attachmentReference)
    .mockReturnValue("https://my.site/A/B/image.png");

  return createConverterContext(containerMock);
}

// eslint-disable-next-line max-statements
describe("UniAstToHTMLConverter", () => {
  const converterContext = getConverterContext();

  const uniAstToHTMLConverter = new UniAstToHTMLConverter(converterContext);

  test("empty ast", () => {
    const res = uniAstToHTMLConverter.toHtml({ blocks: [] });
    expect(res).toBe("");
  });

  test("simple text", () => {
    const res = uniAstToHTMLConverter.toHtml({
      blocks: [
        {
          type: "paragraph",
          styles: {},
          content: [
            {
              type: "text",
              styles: {},
              content: "test",
            },
          ],
        },
      ],
    });
    expect(res).toBe("<p>test</p>");
  });

  test("bold text", () => {
    const res = uniAstToHTMLConverter.toHtml({
      blocks: [
        {
          type: "paragraph",
          styles: {},
          content: [
            {
              type: "text",
              styles: {
                bold: true,
              },
              content: "test",
            },
          ],
        },
      ],
    });
    expect(res).toBe("<p><strong>test</strong></p>");
  });

  test("strikethrough and italic text", () => {
    const res = uniAstToHTMLConverter.toHtml({
      blocks: [
        {
          type: "paragraph",
          styles: {},
          content: [
            {
              type: "text",
              styles: {
                strikethrough: true,
                italic: true,
              },
              content: "test",
            },
          ],
        },
      ],
    });
    expect(res).toBe("<p><s><em>test</em></s></p>");
  });

  test("text with special char", () => {
    const res = uniAstToHTMLConverter.toHtml({
      blocks: [
        {
          type: "paragraph",
          styles: {},
          content: [
            {
              type: "text",
              styles: {},
              content: "<bold>escape me </bold>",
            },
          ],
        },
      ],
    });
    expect(res).toBe("<p>&lt;bold&gt;escape me &lt;/bold&gt;</p>");
  });

  test("text with special char", () => {
    const res = uniAstToHTMLConverter.toHtml({
      blocks: [
        {
          type: "paragraph",
          styles: {},
          content: [
            {
              type: "text",
              styles: {},
              content: "<bold>escape me </bold>",
            },
          ],
        },
      ],
    });
    expect(res).toBe("<p>&lt;bold&gt;escape me &lt;/bold&gt;</p>");
  });

  test.each([1, 2, 3, 4, 5, 6])("Heading level $level", (level) => {
    expect(
      uniAstToHTMLConverter.toHtml({
        // @ts-expect-error level is an union but we know the provided values are fine.
        blocks: [{ type: "heading", level, content: [], styles: {} }],
      }),
    ).toBe(`<h${level}></h${level}>`);
  });

  test("list", () => {
    const res = uniAstToHTMLConverter.toHtml({
      blocks: [
        {
          type: "list",
          styles: {},
          items: [
            {
              content: [
                {
                  type: "paragraph",
                  content: [
                    {
                      type: "text",
                      content: "item A",
                      styles: {},
                    },
                    {
                      type: "text",
                      content: "item A bis",
                      styles: {},
                    },
                  ],
                  styles: {},
                },
              ],
              styles: {},
            },
            {
              content: [
                {
                  type: "paragraph",
                  content: [
                    {
                      type: "text",
                      content: "item B",
                      styles: {},
                    },
                  ],
                  styles: {},
                },
              ],
              styles: {},
            },
          ],
        },
      ],
    });
    expect(res).toBe(
      "<ul><li><p>item Aitem A bis</p></li><li><p>item B</p></li></ul>",
    );
  });

  test("quote", () => {
    const res = uniAstToHTMLConverter.toHtml({
      blocks: [
        {
          type: "quote",
          styles: {},
          content: [
            {
              type: "paragraph",
              content: [
                {
                  type: "text",
                  content: "item A",
                  styles: {},
                },
              ],
              styles: {},
            },
          ],
        },
      ],
    });
    expect(res).toBe("<blockquote><p>item A</p></blockquote>");
  });

  test("code", () => {
    const res = uniAstToHTMLConverter.toHtml({
      blocks: [
        {
          type: "code",
          language: "typescript",
          content: "console.log('hello world')",
        },
      ],
    });
    expect(res).toBe("<pre>console.log('hello world')</pre>");
  });
  test("table", () => {
    const res = uniAstToHTMLConverter.toHtml({
      blocks: [
        {
          type: "table",
          columns: [
            {
              headerCell: {
                content: [
                  {
                    type: "text",
                    content: "header 1",
                    styles: {},
                  },
                ],
                styles: {},
              },
            },
          ],
          rows: [
            [
              {
                content: [
                  {
                    type: "text",
                    content: "cell 1",
                    styles: {},
                  },
                ],
                styles: {},
              },
            ],
          ],
          styles: {},
        },
      ],
    });
    expect(res).toBe(
      "<table><thead><th>header 1</th></thead><tbody><tr><td>cell 1</td></tr></tbody></table>",
    );
  });

  test("external image", () => {
    const res = uniAstToHTMLConverter.toHtml({
      blocks: [
        {
          type: "image",
          target: { type: "external", url: "https://my.site/image.png" },
          styles: {},
        },
      ],
    });
    expect(res).toBe('<img src="https://my.site/image.png" alt="">');
  });

  test("external image with alt", () => {
    const res = uniAstToHTMLConverter.toHtml({
      blocks: [
        {
          type: "image",
          target: { type: "external", url: "https://my.site/image.png" },
          // caption?
          alt: "image alt",
          //     widthPx?
          //       heightPx?
          styles: {},
        },
      ],
    });
    expect(res).toBe('<img src="https://my.site/image.png" alt="image alt">');
  });

  test("internal image", () => {
    const res = uniAstToHTMLConverter.toHtml({
      blocks: [
        {
          type: "image",
          target: {
            type: "internal",
            rawReference: "A.B@image.png",
            parsedReference: null,
          },
          // caption?
          alt: "image alt",
          //     widthPx?
          //       heightPx?
          styles: {},
        },
      ],
    });
    expect(res).toBe(
      '<img src="https://my.site/A/B/image.png" alt="image alt">',
    );
  });
});

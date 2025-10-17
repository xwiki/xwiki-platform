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

import { DefaultMarkdownToUniAstConverter } from "../markdown/default-markdown-to-uni-ast-converter";
import { DefaultUniAstToMarkdownConverter } from "../markdown/default-uni-ast-to-markdown-converter";
import { EntityType } from "@xwiki/cristal-model-api";
import { Container } from "inversify";
import { describe, expect, test } from "vitest";
import { matches, mock } from "vitest-mock-extended";
import type { MarkdownParserConfiguration } from "../markdown/internal-links/parser/markdown-parser-configuration";
import type { ParserConfigurationResolver } from "../markdown/internal-links/parser/parser-configuration-resolver";
import type { InternalLinksSerializer } from "../markdown/internal-links/serializer/internal-links-serializer";
import type { InternalLinksSerializerResolver } from "../markdown/internal-links/serializer/internal-links-serializer-resolver";
import type {
  ModelReferenceHandlerProvider,
  ModelReferenceParser,
  ModelReferenceParserOptions,
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/cristal-model-reference-api";
import type {
  RemoteURLParserProvider,
  RemoteURLSerializerProvider,
} from "@xwiki/cristal-model-remote-url-api";
import type { UniAst } from "@xwiki/cristal-uniast-api";

// eslint-disable-next-line max-statements
function init() {
  const modelReferenceParserProvider = mock<ModelReferenceParserProvider>();

  const modelReferenceParser = mock<ModelReferenceParser>();
  modelReferenceParser.parseAsync
    .calledWith(
      "http://somewhere.somewhere",
      matches((value: ModelReferenceParserOptions) => {
        return value.type === EntityType.ATTACHMENT;
      }),
    )
    .mockImplementation(() => {
      throw new Error("Not internal");
    });

  modelReferenceParser.parse
    .calledWith(
      "documentReference",
      matches((value: ModelReferenceParserOptions) => {
        return value.type === EntityType.DOCUMENT;
      }),
    )
    .mockImplementation(() => {
      throw new Error("Not internal");
    });

  modelReferenceParser.parse
    .calledWith(
      "imageReference",
      matches((value: ModelReferenceParserOptions) => {
        return value.type === EntityType.ATTACHMENT;
      }),
    )
    .mockImplementation(() => {
      throw new Error("Not internal");
    });

  modelReferenceParserProvider.get.mockReturnValue(modelReferenceParser);

  const modelReferenceSerializerProvider =
    mock<ModelReferenceSerializerProvider>();

  const remoteURLParserProvider = mock<RemoteURLParserProvider>();

  const remoteURLSerializerProvider = mock<RemoteURLSerializerProvider>();

  const modelReferenceHandlerProvider = mock<ModelReferenceHandlerProvider>();

  const containerMock = mock<Container>();

  const parserConfigurationResolver = mock<ParserConfigurationResolver>();
  const markdownParserConfiguration = mock<MarkdownParserConfiguration>();
  markdownParserConfiguration.supportFlexmarkInternalLinks.mockReturnValue(
    false,
  );
  parserConfigurationResolver.get.mockReturnValue(markdownParserConfiguration);

  const internalLinksSerializerResolver =
    mock<InternalLinksSerializerResolver>();

  const internalLinksSerializer = mock<InternalLinksSerializer>();

  internalLinksSerializer.serialize.mockImplementation(
    async (content, target, uniAstToMarkdownConverter) => {
      return `[[${await uniAstToMarkdownConverter.convertInlineContents(content)}|${target.rawReference}]]`;
    },
  );

  internalLinksSerializer.serializeImage.mockImplementation(
    async (target, alt) => {
      return `![[${alt}|${target.rawReference}]]`;
    },
  );

  internalLinksSerializerResolver.get.mockReturnValue(
    Promise.resolve(internalLinksSerializer),
  );

  containerMock.get
    .calledWith("ModelReferenceParserProvider")
    .mockReturnValue(modelReferenceParserProvider);

  containerMock.get
    .calledWith("ModelReferenceSerializerProvider")
    .mockReturnValue(modelReferenceSerializerProvider);

  containerMock.get
    .calledWith("RemoteURLParserProvider")
    .mockReturnValue(remoteURLParserProvider);

  containerMock.get
    .calledWith("RemoteURLSerializerProvider")
    .mockReturnValue(remoteURLSerializerProvider);

  containerMock.get
    .calledWith("ModelReferenceHandlerProvider")
    .mockReturnValue(modelReferenceHandlerProvider);

  return {
    modelReferenceParserProvider,
    modelReferenceHandlerProvider,
    remoteURLSerializerProvider,
    parserConfigurationResolver,
    internalLinksSerializerResolver,
  };
}

describe("MarkdownToUniAstConverter", () => {
  const {
    modelReferenceParserProvider,
    modelReferenceHandlerProvider,
    parserConfigurationResolver,
    internalLinksSerializerResolver,
  } = init();
  const mdToUniAst = new DefaultMarkdownToUniAstConverter(
    modelReferenceParserProvider,
    modelReferenceHandlerProvider,
    parserConfigurationResolver,
  );
  const uniAstToMd = new DefaultUniAstToMarkdownConverter(
    internalLinksSerializerResolver,
  );

  async function testTwoWayConversion(expected: {
    startingFrom: string;
    convertsBackTo: string;
    withUniAst: UniAst;
  }) {
    const uniAst = await mdToUniAst.parseMarkdown(expected.startingFrom);

    expect(uniAst).toStrictEqual(expected.withUniAst);

    if (uniAst instanceof Error) {
      throw new Error("Unreachable");
    }

    expect(await uniAstToMd.toMarkdown(uniAst)).toEqual(
      expected.convertsBackTo,
    );
  }

  // TODO: test inline images + links
  test("parse some text styling", async () => {
    await testTwoWayConversion({
      startingFrom:
        "Normal **Bold** *Italic1* _Italic2_ ~~Strikethrough~~ __Underline__ `Code` **_~~wow!~~_**",
      convertsBackTo:
        "Normal **Bold** _Italic1_ _Italic2_ ~~Strikethrough~~ **Underline** Code ~~_**wow!**_~~",
      withUniAst: {
        blocks: [
          {
            content: [
              {
                content: "Normal ",
                styles: {},
                type: "text",
              },
              {
                content: "Bold",
                styles: {
                  bold: true,
                },
                type: "text",
              },
              {
                content: " ",
                styles: {},
                type: "text",
              },
              {
                content: "Italic1",
                styles: {
                  italic: true,
                },
                type: "text",
              },
              {
                content: " ",
                styles: {},
                type: "text",
              },
              {
                content: "Italic2",
                styles: {
                  italic: true,
                },
                type: "text",
              },
              {
                content: " ",
                styles: {},
                type: "text",
              },
              {
                content: "Strikethrough",
                styles: {
                  strikethrough: true,
                },
                type: "text",
              },
              {
                content: " ",
                styles: {},
                type: "text",
              },
              {
                content: "Underline",
                styles: {
                  bold: true,
                },
                type: "text",
              },
              {
                content: " ",
                styles: {},
                type: "text",
              },
              {
                content: "Code",
                styles: {},
                type: "text",
              },
              {
                content: " ",
                styles: {},
                type: "text",
              },
              {
                content: "wow!",
                styles: {
                  bold: true,
                  italic: true,
                  strikethrough: true,
                },
                type: "text",
              },
            ],
            styles: {},
            type: "paragraph",
          },
        ],
      },
    });
  });

  test("parse tables", async () => {
    await testTwoWayConversion({
      startingFrom: `| Heading 1| Heading 2| Heading **3**|
| ---------|--------- |-------------|
| Row 1 cell 1 | Row 1 cell 2 | Row 1 cell **3** 
| Row 2 cell 1 | Row 2 cell 2 | Row 2 cell **3** |
|Row 3 cell 1 | Row 3 cell 2| Row 3 cell __3__|`,
      convertsBackTo: `| Heading 1 | Heading 2 | Heading **3** |
|  -  |  -  |  -  |
| Row 1 cell 1 | Row 1 cell 2 | Row 1 cell **3** |
| Row 2 cell 1 | Row 2 cell 2 | Row 2 cell **3** |
| Row 3 cell 1 | Row 3 cell 2 | Row 3 cell **3** |`,
      withUniAst: {
        blocks: [
          {
            columns: [
              {
                headerCell: {
                  content: [
                    {
                      content: "Heading 1",
                      styles: {},
                      type: "text",
                    },
                  ],
                  styles: {},
                },
              },
              {
                headerCell: {
                  content: [
                    {
                      content: "Heading 2",
                      styles: {},
                      type: "text",
                    },
                  ],
                  styles: {},
                },
              },
              {
                headerCell: {
                  content: [
                    {
                      content: "Heading ",
                      styles: {},
                      type: "text",
                    },
                    {
                      content: "3",
                      styles: {
                        bold: true,
                      },
                      type: "text",
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
                      content: "Row 1 cell 1",
                      styles: {},
                      type: "text",
                    },
                  ],
                  styles: {},
                },
                {
                  content: [
                    {
                      content: "Row 1 cell 2",
                      styles: {},
                      type: "text",
                    },
                  ],
                  styles: {},
                },
                {
                  content: [
                    {
                      content: "Row 1 cell ",
                      styles: {},
                      type: "text",
                    },
                    {
                      content: "3",
                      styles: {
                        bold: true,
                      },
                      type: "text",
                    },
                  ],
                  styles: {},
                },
              ],
              [
                {
                  content: [
                    {
                      content: "Row 2 cell 1",
                      styles: {},
                      type: "text",
                    },
                  ],
                  styles: {},
                },
                {
                  content: [
                    {
                      content: "Row 2 cell 2",
                      styles: {},
                      type: "text",
                    },
                  ],
                  styles: {},
                },
                {
                  content: [
                    {
                      content: "Row 2 cell ",
                      styles: {},
                      type: "text",
                    },
                    {
                      content: "3",
                      styles: {
                        bold: true,
                      },
                      type: "text",
                    },
                  ],
                  styles: {},
                },
              ],
              [
                {
                  content: [
                    {
                      content: "Row 3 cell 1",
                      styles: {},
                      type: "text",
                    },
                  ],
                  styles: {},
                },
                {
                  content: [
                    {
                      content: "Row 3 cell 2",
                      styles: {},
                      type: "text",
                    },
                  ],
                  styles: {},
                },
                {
                  content: [
                    {
                      content: "Row 3 cell ",
                      styles: {},
                      type: "text",
                    },
                    {
                      content: "3",
                      styles: {
                        bold: true,
                      },
                      type: "text",
                    },
                  ],
                  styles: {},
                },
              ],
            ],
            styles: {},
            type: "table",
          },
        ],
      },
    });
  });

  test("parse some simple blocks", async () => {
    await testTwoWayConversion({
      startingFrom: [
        "Paragraph line 1",
        "Paragraph line 2",
        "",
        "# Heading level 1",
        "## Heading level 2",
        "### Heading level 3",
        "#### Heading level 4",
        "##### Heading level 5",
        "###### Heading level 6",
        "> Blockquote line 1",
        ">   Blockquote line 2",
        "",
        "> Some separate blockquote",
        "",
        "* Bullet item 1",
        "* Bullet item 2",
        "- Bullet item 3",
        "* Bullet item 4 line 1",
        "  Bullet item 4 line 2",
        "  Bullet item 4 line **3**",
        "",
        "1. Numbered item 1",
        "2. Numbered item 2",
        "3. Numbered item 3",
        "6. Numbered item 6",
        "",
        "2. Another list starting at 2",
        "",
        "- [ ] Unchecked task 1",
        "- [x] Checked task 2",
        "- [X] Checked task 3",
        "",
        "```",
        "Code block 1",
        "```",
        "",
        "```javascript",
        "Code block 2 (js)",
        "```",
        "",
        "![Image alt](http://somewhere.somewhere)",
        "",
        "---",
        "***",
      ].join("\n"),
      convertsBackTo: [
        "Paragraph line 1",
        "Paragraph line 2",
        "",
        "# Heading level 1",
        "",
        "## Heading level 2",
        "",
        "### Heading level 3",
        "",
        "#### Heading level 4",
        "",
        "##### Heading level 5",
        "",
        "###### Heading level 6",
        "",
        "> Blockquote line 1",
        "> Blockquote line 2",
        "",
        "> Some separate blockquote",
        "",
        "* Bullet item 1",
        "* Bullet item 2",
        "",
        // This item ends up isolated as it used another bullet style ('-' instead of '*' in the source)
        "* Bullet item 3",
        "",
        "* Bullet item 4 line 1",
        "  Bullet item 4 line 2",
        "  Bullet item 4 line **3**",
        "",
        "1. Numbered item 1",
        "2. Numbered item 2",
        "3. Numbered item 3",
        "4. Numbered item 6",
        // This item ends up combined to the list above as using a number, even after two line breaks, will make it part of the same list
        "5. Another list starting at 2",
        "",
        "* [ ] Unchecked task 1",
        "* [x] Checked task 2",
        "* [x] Checked task 3",
        "",
        "```",
        "Code block 1",
        "```",
        "",
        "```javascript",
        "Code block 2 (js)",
        "```",
        "",
        "![Image alt](http://somewhere.somewhere)",
        "",
        "---",
        "",
        "---",
      ].join("\n"),
      withUniAst: {
        blocks: [
          {
            content: [
              {
                content: "Paragraph line 1\nParagraph line 2",
                styles: {},
                type: "text",
              },
            ],
            styles: {},
            type: "paragraph",
          },
          {
            content: [
              {
                content: "Heading level 1",
                styles: {},
                type: "text",
              },
            ],
            level: 1,
            styles: {},
            type: "heading",
          },
          {
            content: [
              {
                content: "Heading level 2",
                styles: {},
                type: "text",
              },
            ],
            level: 2,
            styles: {},
            type: "heading",
          },
          {
            content: [
              {
                content: "Heading level 3",
                styles: {},
                type: "text",
              },
            ],
            level: 3,
            styles: {},
            type: "heading",
          },
          {
            content: [
              {
                content: "Heading level 4",
                styles: {},
                type: "text",
              },
            ],
            level: 4,
            styles: {},
            type: "heading",
          },
          {
            content: [
              {
                content: "Heading level 5",
                styles: {},
                type: "text",
              },
            ],
            level: 5,
            styles: {},
            type: "heading",
          },
          {
            content: [
              {
                content: "Heading level 6",
                styles: {},
                type: "text",
              },
            ],
            level: 6,
            styles: {},
            type: "heading",
          },
          {
            content: [
              {
                content: [
                  {
                    content: "Blockquote line 1\nBlockquote line 2",
                    styles: {},
                    type: "text",
                  },
                ],
                styles: {},
                type: "paragraph",
              },
            ],
            styles: {},
            type: "quote",
          },
          {
            content: [
              {
                content: [
                  {
                    content: "Some separate blockquote",
                    styles: {},
                    type: "text",
                  },
                ],
                styles: {},
                type: "paragraph",
              },
            ],
            styles: {},
            type: "quote",
          },
          {
            items: [
              {
                checked: undefined,
                content: [
                  {
                    content: [
                      {
                        content: "Bullet item 1",
                        styles: {},
                        type: "text",
                      },
                    ],
                    styles: {},
                    type: "paragraph",
                  },
                ],
                number: undefined,
                styles: {},
              },
              {
                checked: undefined,
                content: [
                  {
                    content: [
                      {
                        content: "Bullet item 2",
                        styles: {},
                        type: "text",
                      },
                    ],
                    styles: {},
                    type: "paragraph",
                  },
                ],
                number: undefined,
                styles: {},
              },
            ],
            styles: {},
            type: "list",
          },
          {
            items: [
              {
                checked: undefined,
                content: [
                  {
                    content: [
                      {
                        content: "Bullet item 3",
                        styles: {},
                        type: "text",
                      },
                    ],
                    styles: {},
                    type: "paragraph",
                  },
                ],
                number: undefined,
                styles: {},
              },
            ],
            styles: {},
            type: "list",
          },
          {
            items: [
              {
                checked: undefined,
                content: [
                  {
                    content: [
                      {
                        content:
                          "Bullet item 4 line 1\nBullet item 4 line 2\nBullet item 4 line ",
                        styles: {},
                        type: "text",
                      },
                      {
                        content: "3",
                        styles: {
                          bold: true,
                        },
                        type: "text",
                      },
                    ],
                    styles: {},
                    type: "paragraph",
                  },
                ],
                number: undefined,
                styles: {},
              },
            ],
            styles: {},
            type: "list",
          },
          {
            items: [
              {
                checked: undefined,
                content: [
                  {
                    content: [
                      {
                        content: "Numbered item 1",
                        styles: {},
                        type: "text",
                      },
                    ],
                    styles: {},
                    type: "paragraph",
                  },
                ],
                number: 1,
                styles: {},
              },
              {
                checked: undefined,
                content: [
                  {
                    content: [
                      {
                        content: "Numbered item 2",
                        styles: {},
                        type: "text",
                      },
                    ],
                    styles: {},
                    type: "paragraph",
                  },
                ],
                number: 2,
                styles: {},
              },
              {
                checked: undefined,
                content: [
                  {
                    content: [
                      {
                        content: "Numbered item 3",
                        styles: {},
                        type: "text",
                      },
                    ],
                    styles: {},
                    type: "paragraph",
                  },
                ],
                number: 3,
                styles: {},
              },
              {
                checked: undefined,
                content: [
                  {
                    content: [
                      {
                        content: "Numbered item 6",
                        styles: {},
                        type: "text",
                      },
                    ],
                    styles: {},
                    type: "paragraph",
                  },
                ],
                number: 4,
                styles: {},
              },
              {
                checked: undefined,
                content: [
                  {
                    content: [
                      {
                        content: "Another list starting at 2",
                        styles: {},
                        type: "text",
                      },
                    ],
                    styles: {},
                    type: "paragraph",
                  },
                ],
                number: 5,
                styles: {},
              },
            ],
            styles: {},
            type: "list",
          },
          {
            items: [
              {
                checked: false,
                content: [
                  {
                    content: [
                      {
                        content: "Unchecked task 1",
                        styles: {},
                        type: "text",
                      },
                    ],
                    styles: {},
                    type: "paragraph",
                  },
                ],
                number: undefined,
                styles: {},
              },
              {
                checked: true,
                content: [
                  {
                    content: [
                      {
                        content: "Checked task 2",
                        styles: {},
                        type: "text",
                      },
                    ],
                    styles: {},
                    type: "paragraph",
                  },
                ],
                number: undefined,
                styles: {},
              },
              {
                checked: true,
                content: [
                  {
                    content: [
                      {
                        content: "Checked task 3",
                        styles: {},
                        type: "text",
                      },
                    ],
                    styles: {},
                    type: "paragraph",
                  },
                ],
                number: undefined,
                styles: {},
              },
            ],
            styles: {},
            type: "list",
          },
          {
            content: "Code block 1",
            language: undefined,
            type: "code",
          },
          {
            content: "Code block 2 (js)",
            language: "javascript",
            type: "code",
          },
          {
            content: [
              {
                alt: "Image alt",
                caption: undefined,
                styles: {},
                target: {
                  type: "external",
                  url: "http://somewhere.somewhere",
                },
                type: "image",
              },
            ],
            styles: {},
            type: "paragraph",
          },
          {
            type: "break",
          },
          {
            type: "break",
          },
        ],
      },
    });
  });

  test("parse XWiki-specific syntax elements", async () => {
    await testTwoWayConversion({
      startingFrom: [
        "A [[title|documentReference]] B",
        "C ![[title|imageReference]] D",
        "E {{someInlineMacro /}} F",
      ].join("\n"),
      convertsBackTo: [
        "A [[title|documentReference]] B",
        "C ![[title|imageReference]] D",
        "E {{someInlineMacro /}} F",
      ].join("\n"),
      withUniAst: {
        blocks: [
          {
            content: [
              {
                content: "A ",
                styles: {},
                type: "text",
              },
              {
                content: [
                  {
                    content: "title",
                    styles: {},
                    type: "text",
                  },
                ],
                target: {
                  type: "internal",
                  parsedReference: null,
                  rawReference: "documentReference",
                },
                type: "link",
              },
              {
                content: " B\nC ",
                styles: {},
                type: "text",
              },
              {
                alt: "title",
                styles: {
                  alignment: "left",
                },
                target: {
                  type: "internal",
                  parsedReference: null,
                  rawReference: "imageReference",
                },
                type: "image",
              },
              {
                content: " D\nE ",
                styles: {},
                type: "text",
              },
              {
                name: "someInlineMacro",
                params: {},
                type: "inlineMacro",
              },
              {
                content: " F",
                styles: {},
                type: "text",
              },
            ],
            styles: {},
            type: "paragraph",
          },
        ],
      },
    });
  });

  test("parse various macros syntaxes", async () => {
    await testTwoWayConversion({
      startingFrom: [
        "{{macro/}}",
        "{{ macro/}}",
        "{{macro /}}",
        "{{  macro  / }}",
        "{{macro param1=1/}}",
        '{{macro param1="1"/}}',
        "{{macro param1=1 /}}",
        '{{macro param1="1" /}}',
        '{{macro param1="1" param2="2" /}}',
        '{{macro param1="param1Value" param2="param2Value" param3="param3Value" /}}',
        '{{macro param1="some \\\\" escaped quote and }} closing braces and \\\\\\ escaped backslashes" /}}',
      ].join("\n\n"),
      convertsBackTo: [
        "{{macro /}}",
        "{{macro /}}",
        "{{macro /}}",
        "{{macro /}}",
        '{{macro param1="1" /}}',
        '{{macro param1="1" /}}',
        '{{macro param1="1" /}}',
        '{{macro param1="1" /}}',
        '{{macro param1="1" param2="2" /}}',
        '{{macro param1="param1Value" param2="param2Value" param3="param3Value" /}}',
        '{{macro param1="some \\\\" escaped quote and }} closing braces and \\\\\\ escaped backslashes" /}}',
      ].join("\n\n"),
      withUniAst: {
        blocks: [
          {
            name: "macro",
            params: {},
            type: "macroBlock",
          },
          {
            name: "macro",
            params: {},
            type: "macroBlock",
          },
          {
            name: "macro",
            params: {},
            type: "macroBlock",
          },
          {
            name: "macro",
            params: {},
            type: "macroBlock",
          },
          {
            name: "macro",
            params: {
              param1: "1",
            },
            type: "macroBlock",
          },
          {
            name: "macro",
            params: {
              param1: "1",
            },
            type: "macroBlock",
          },
          {
            name: "macro",
            params: {
              param1: "1",
            },
            type: "macroBlock",
          },
          {
            name: "macro",
            params: {
              param1: "1",
            },
            type: "macroBlock",
          },
          {
            name: "macro",
            params: {
              param1: "1",
              param2: "2",
            },
            type: "macroBlock",
          },
          {
            name: "macro",
            params: {
              param1: "param1Value",
              param2: "param2Value",
              param3: "param3Value",
            },
            type: "macroBlock",
          },
          {
            name: "macro",
            params: {
              param1:
                'some " escaped quote and }} closing braces and \\ escaped backslashes',
            },
            type: "macroBlock",
          },
        ],
      },
    });
  });
});

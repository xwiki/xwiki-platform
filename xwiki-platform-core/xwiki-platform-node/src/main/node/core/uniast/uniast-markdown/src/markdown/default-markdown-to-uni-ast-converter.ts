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
import { findFirstMatchIn } from "./internal/find-first-match-in";
import { remarkPartialGfm } from "./internal/remark-partial-gfm";
import { ParserConfigurationResolver } from "./internal-links/parser/parser-configuration-resolver";
import {
  CODIFIED_MACRO_PREFIX,
  codifyMacros,
  reparseCodifiedMacro,
} from "./macros";
import {
  assertInArray,
  assertUnreachable,
  findWithIndexTypePredicate,
  tryFalliblePromiseOrError,
} from "@xwiki/platform-fn-utils";
import { macrosServiceName } from "@xwiki/platform-macros-service";
import { EntityType } from "@xwiki/platform-model-api";
import { inject, injectable } from "inversify";
import remarkParse from "remark-parse";
import { unified } from "unified";
import type { MarkdownToUniAstConverter } from "./markdown-to-uni-ast-converter";
import type { MacrosService } from "@xwiki/platform-macros-service";
import type { EntityReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceHandlerProvider,
  ModelReferenceParserProvider,
} from "@xwiki/platform-model-reference-api";
import type {
  Block,
  Image,
  InlineContent,
  LinkTarget,
  TableCell,
  TableColumn,
  TextStyles,
  UniAst,
} from "@xwiki/platform-uniast-api";
import type {
  Html,
  Image as MdImage,
  PhrasingContent,
  RootContent,
} from "mdast";

/**
 * @since 18.0.0RC1
 */
@injectable()
export class DefaultMarkdownToUniAstConverter
  implements MarkdownToUniAstConverter
{
  constructor(
    @inject("ModelReferenceParserProvider")
    private readonly modelReferenceParserProvider: ModelReferenceParserProvider,

    @inject("ModelReferenceHandlerProvider")
    private readonly modelReferenceHandlerProvider: ModelReferenceHandlerProvider,

    @inject("ParserConfigurationResolver")
    private readonly parserConfigurationResolver: ParserConfigurationResolver,

    @inject(macrosServiceName)
    private readonly macrosService: MacrosService,
  ) {}

  async parseMarkdown(markdown: string): Promise<UniAst | Error> {
    // TODO: auto-links (URLs + emails)
    //     > https://jira.xwiki.org/browse/CRISTAL-513

    // First step is encoding macros as inline codes
    const { content, brokeAt } = await codifyMacros(
      markdown,
      this,
      this.macrosService,
    );

    // Ensure the whole input was consumed
    if (brokeAt !== null) {
      throw new Error(
        "Unexpected internal error: macro transform did not parse the full Markdown content",
      );
    }

    // Now we parse the transformed markdown content, with GFM support
    const ast = unified().use(remarkParse).use(remarkPartialGfm).parse(content);

    const blocks = await tryFalliblePromiseOrError(() =>
      Promise.all(ast.children.map((item) => this.convertBlock(item))),
    );

    return blocks instanceof Error ? blocks : { blocks };
  }

  private async convertBlock(block: RootContent): Promise<Block> {
    switch (block.type) {
      case "paragraph": {
        const content = await this.convertInlineContents(block.children, {});

        // Paragraphs only made of a single block macro are actually block macros
        if (
          content.length === 1 &&
          content[0].type === "inlineMacro" &&
          this.macrosService.get(content[0].call.id)?.renderAs === "block"
        ) {
          return {
            type: "macroBlock",
            call: content[0].call,
          };
        }

        return {
          type: "paragraph",
          content,
          styles: {},
        };
      }

      case "heading":
        return {
          type: "heading",
          level: assertInArray(
            block.depth,
            [1, 2, 3, 4, 5, 6] as const,
            "Invalid heading depth in markdown parser",
          ),
          content: await this.convertInlineContents(block.children, {}),
          styles: {},
        };

      case "blockquote":
        return {
          type: "quote",
          content: await Promise.all(
            block.children.map((item) => this.convertBlock(item)),
          ),
          styles: {},
        };

      case "list":
        // TODO: "token.loose" property
        return {
          type: "list",
          items: await Promise.all(
            block.children.map(async (item, i) => ({
              number: block.ordered ? (block.start ?? 1) + i : undefined,
              checked: item.checked ?? undefined,
              content: await Promise.all(
                item.children.map((item) => this.convertBlock(item)),
              ),
              styles: {},
            })),
          ),
          styles: {},
        };

      case "code":
        // TODO: "token.escaped" property
        // TODO: "token.codeBlockStyle" property
        return {
          type: "code",
          content: block.value,
          language: block.lang ?? undefined,
        };

      case "table": {
        const [headers, ...rows] = block.children;
        const columns = await Promise.all(
          headers?.children.map(
            async (cell): Promise<TableColumn> => ({
              headerCell: {
                content: await this.convertInlineContents(cell.children, {}),
                styles: {},
              },
            }),
          ),
        );
        const tableRows = await Promise.all(
          rows.map(async (row) => {
            const map = row.children.map(
              async (cell): Promise<TableCell> => ({
                content: await this.convertInlineContents(cell.children, {}),
                styles: {},
              }),
            );
            return await Promise.all(map);
          }),
        );
        return {
          type: "table",
          columns: columns,
          rows: tableRows,
          styles: {},
        };
      }

      case "image":
        return {
          type: "image",
          ...(await this.convertImage(block)),
        };

      case "break":
      case "thematicBreak":
        return { type: "break" };

      case "imageReference":
      case "linkReference":
      case "definition":
      case "footnoteDefinition":
      case "footnoteReference":
      case "html":
        throw new Error("TODO: handle blocks of type " + block.type);

      // NOTE: These are handled in the `convertInline` function below
      case "text":
      case "delete":
      case "strong":
      case "emphasis":
      case "inlineCode":
      case "link":
      case "tableCell":
      case "tableRow":
      case "yaml":
      case "listItem":
        throw new Error(
          "Unexpected block type in markdown parser: " + block.type,
        );

      default:
        assertUnreachable(block);
    }
  }

  private async convertInlineContents(
    children: PhrasingContent[],
    styles: TextStyles = {},
  ): Promise<InlineContent[]> {
    const withProcessedHtml = await this._processHtmlInInlineContents(
      children,
      styles,
    );

    if (withProcessedHtml !== null) {
      return withProcessedHtml;
    }

    return (
      await Promise.all(
        children.map((item) => this._convertInlineContent(item, styles)),
      )
    ).flat();
  }

  // eslint-disable-next-line max-statements
  private async _processHtmlInInlineContents(
    children: PhrasingContent[],
    styles: TextStyles,
  ): Promise<InlineContent[] | null> {
    const htmlWithIdx = findWithIndexTypePredicate(
      children,
      (child) => child.type === "html",
    );

    if (!htmlWithIdx) {
      return null;
    }

    const [html, openingIdx] = htmlWithIdx;

    const tagMatch = html.value.match(/^<([a-z]+)>$/);

    if (!tagMatch) {
      throw new Error(`Found invalid HTML tag: "${html}"`);
    }

    const tag = tagMatch[1];

    // NOTE: this is not a state machine, so with content like `<element><element></element></element>`
    // it will select the wrong closing tag
    const closing = findWithIndexTypePredicate(
      children.slice(openingIdx + 1),
      (child): child is Html =>
        child.type === "html" && child.value === `</${tag}>`,
    );

    if (!closing) {
      throw new Error(`Missing closing HTML tag for: "${tag}"`);
    }

    const closingIdx = closing[1] + openingIdx + 1;

    return [
      // Process elements before the HTML tag
      ...(await this.convertInlineContents(children.slice(0, openingIdx))),

      // Process the HTML element and its content
      await this._processHtmlElement(
        tag,
        children.slice(openingIdx + 1, closingIdx),
        styles,
      ),

      // Process elements after the HTML tag (which may very well contain another HTML element)
      ...(await this.convertInlineContents(children.slice(closingIdx + 1))),
    ];
  }

  // eslint-disable-next-line max-statements
  private async _processHtmlElement(
    tag: string,
    inlineContents: PhrasingContent[],
    styles: TextStyles,
  ): Promise<InlineContent> {
    switch (tag) {
      case "sub": {
        const content = await this.convertInlineContents(
          inlineContents,
          styles,
        );

        // Tracking issue: https://github.com/TypeCellOS/BlockNote/issues/1540
        if (content.length !== 1) {
          throw new Error(
            "Expected precisely one inline content for <sub> element",
          );
        }

        if (content[0].type !== "text") {
          throw new Error("Only plain text is supported inside <sub> tags");
        }

        return {
          type: "subscript",
          content: content[0].content,
          styles,
        };
      }

      case "sup": {
        const content = await this.convertInlineContents(
          inlineContents,
          styles,
        );

        // Tracking issue: https://github.com/TypeCellOS/BlockNote/issues/1540
        if (content.length !== 1) {
          throw new Error(
            "Expected precisely one inline content for <sup> element",
          );
        }

        if (content[0].type !== "text") {
          throw new Error("Only plain text is supported inside <sup> tags");
        }

        return {
          type: "superscript",
          content: content[0].content,
          styles,
        };
      }

      default:
        throw new Error(`Unrecognized HTML tag: "${tag}"`);
    }
  }

  // NOTE: should not be used directly, use `convertInlineContents` instead
  private async _convertInlineContent(
    inline: PhrasingContent,
    styles: TextStyles,
  ): Promise<InlineContent[]> {
    switch (inline.type) {
      case "image":
        return [
          {
            type: "image",
            ...(await this.convertImage(inline)),
          },
        ];

      case "strong":
        return this.convertInlineContents(inline.children, {
          ...styles,
          bold: true,
        });

      case "emphasis":
        return this.convertInlineContents(inline.children, {
          ...styles,
          italic: true,
        });

      case "delete":
        return this.convertInlineContents(inline.children, {
          ...styles,
          strikethrough: true,
        });

      case "inlineCode": {
        return [
          inline.value.startsWith(CODIFIED_MACRO_PREFIX)
            ? {
                type: "inlineMacro",
                call: reparseCodifiedMacro(inline.value),
              }
            : {
                type: "text",
                content: inline.value,
                styles: {},
              },
        ];
      }

      case "text":
        return this.convertText(inline.value, styles);

      case "footnoteReference":
      case "linkReference":
      case "imageReference":
      case "break":
        throw new Error("TODO: handle inlines of type " + inline.type);

      case "html":
        console.log(inline);
        throw new Error("TODO");

      case "link": {
        return await this.convertLink(inline, styles);
      }

      default:
        assertUnreachable(inline);
    }
  }

  private async convertLink(
    inline: PhrasingContent & { type: "link" },
    styles: TextStyles,
  ): Promise<InlineContent[]> {
    // TODO: change here for Github and nextcloud + resolve the entity reference in case the link can be resolved as
    //  internal.
    // For Nextcloud, resolve based on the note on AnyType (from docid to url)
    // TODO: the same must be done for attachments!
    let target: LinkTarget;
    if (!this.supportFlexmark()) {
      // If flexmark is not supported, we need to parse the url to find out if it's pointing to an internal entity.
      try {
        const parsed = await this.modelReferenceParserProvider
          .get()!
          .parseAsync(inline.url);
        target = {
          type: "internal",
          parsedReference: parsed,
          rawReference: inline.url,
        };
      } catch (e) {
        console.debug("Error parsing reference: ", e);
        target = { type: "external", url: inline.url };
      }
    } else {
      target = { type: "external", url: inline.url };
    }
    const label = await this.convertInlineContents(inline.children, styles);
    return [
      {
        type: "link",
        content: label.map((token) => {
          if (token.type !== "text") {
            throw new Error("Unexpected link inside link in markdown parser");
          }
          return token;
        }),
        target,
      },
    ];
  }

  private async convertImage(image: MdImage): Promise<Image> {
    // TODO: "token.text" property
    let target: LinkTarget;
    const url = image.url;
    try {
      const parsed = await this.modelReferenceParserProvider
        .get()!
        .parseAsync(url, { type: EntityType.ATTACHMENT });
      target = {
        type: "internal",
        parsedReference: parsed,
        rawReference: url,
      };
    } catch {
      target = { type: "external", url: url };
    }

    return {
      target,
      caption: undefined,
      alt: image.alt ?? undefined,
      styles: {},
    };
  }

  // eslint-disable-next-line max-statements
  private convertText(text: string, styles: TextStyles): InlineContent[] {
    const out: InlineContent[] = [];

    let treated = 0;

    while (true) {
      const firstItem = findFirstMatchIn(
        text.substring(treated),
        // Try to find the first XWiki-specific-element syntax in the text (precedence order)
        // Do not use the expressions for internal images and links when not supported.
        this.supportFlexmark()
          ? [
              { name: "image", match: "![[" },
              { name: "link", match: "[[" },
            ]
          : [],
      );

      // If none is found, exit immediately
      // This also means texts that don't contain any specific syntax will have a very low conversion cost
      if (!firstItem) {
        break;
      }

      const match = treated + firstItem.offset;

      // Ensure the current element is not being escaped with backslashes
      const precedingBackslashes = text.substring(0, match).match(/\\+/);

      // Backslashes are counted as pairs, as two consecutive backslashes are not escaping the next character
      if (precedingBackslashes && precedingBackslashes[0].length % 2 !== 0) {
        continue;
      }

      // Push the text between the last match and the current one as plain text
      if (text.substring(treated, match).length > 0) {
        out.push({
          type: "text",
          content: text.substring(treated, match),
          styles,
        });
      }

      switch (firstItem.name) {
        case "image":
        case "link": {
          treated = this.handleLinkOrImage(
            firstItem,
            match,
            text,
            treated,
            styles,
            out,
          );
          break;
        }

        default:
          assertUnreachable(firstItem.name);
      }
    }

    // Push the leftover text after the last XWiki-specific element as plain text
    if (text.substring(treated).length > 0) {
      out.push({
        type: "text",
        content: text.substring(treated),
        styles,
      });
    }

    return out;
  }

  // eslint-disable-next-line max-statements
  private handleLinkOrImage(
    firstItem: { name: string; match: string },
    match: number,
    text: string,
    treated: number,
    styles: TextStyles,
    out: InlineContent[],
  ): number {
    const isImage = firstItem.name === "image";

    let i;

    // Is the next character being escaped?
    let escaping = false;
    // Is the link or image being closed?
    let closing = false;
    // Has the link or image been closed properly?
    let closed = false;

    for (i = match + firstItem.match.length; i < text.length; i++) {
      if (escaping) {
        escaping = false;

        continue;
      }

      const char = text.charAt(i);

      if (char === "\\") {
        escaping = true;
        continue;
      }

      if (char === "]") {
        if (closing) {
          closed = true;
          break;
        }

        closing = true;
      }
    }

    if (!closed) {
      return treated;
    }

    treated = i + 1;

    const substr = text.substring(match + firstItem.match.length, i - 1);

    let title: string | null;
    let targetStr: string;

    const pipeCharPos = substr.indexOf("|");

    if (pipeCharPos !== -1) {
      title = substr.substring(0, pipeCharPos);
      targetStr = substr.substring(pipeCharPos + 1);
    } else {
      title = null;
      targetStr = substr;
    }

    let reference: EntityReference | null;

    try {
      reference = this.modelReferenceParserProvider.get()!.parse(targetStr, {
        type: isImage ? EntityType.ATTACHMENT : EntityType.DOCUMENT,
      });
    } catch {
      reference = null;
    }

    const target: LinkTarget = {
      type: "internal",
      rawReference: targetStr,
      parsedReference: reference,
    };

    title ??= reference
      ? this.modelReferenceHandlerProvider.get()!.getTitle(reference)
      : "<invalid reference>";

    const items: InlineContent = isImage
      ? {
          type: "image",
          target,
          styles: { alignment: "left" },
          alt: title,
        }
      : {
          type: "link",
          target,
          content: [{ type: "text", content: title, styles }],
        };

    out.push(items);

    return treated;
  }

  private supportFlexmark(): boolean {
    return this.parserConfigurationResolver.get().supportFlexmarkInternalLinks;
  }
}

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

import {
  assertInArray,
  assertUnreachable,
  tryFallibleOrError,
} from "@xwiki/cristal-fn-utils";
import { EntityType } from "@xwiki/cristal-model-api";
import { gfmStrikethroughFromMarkdown } from "mdast-util-gfm-strikethrough";
import { gfmTableFromMarkdown } from "mdast-util-gfm-table";
import { gfmTaskListItemFromMarkdown } from "mdast-util-gfm-task-list-item";
import { gfmStrikethrough } from "micromark-extension-gfm-strikethrough";
import { gfmTable } from "micromark-extension-gfm-table";
import { gfmTaskListItem } from "micromark-extension-gfm-task-list-item";
import remarkParse from "remark-parse";
import { unified } from "unified";
import type {
  Block,
  Image,
  InlineContent,
  LinkTarget,
  ListItem,
  TableCell,
  TableColumn,
  TextStyles,
  UniAst,
} from "../ast";
import type { ConverterContext } from "../interface";
import type { Image as MdImage, PhrasingContent, RootContent } from "mdast";
import type { Processor } from "unified";

/**
 * Convert Markdown string to a Universal AST.
 *
 * @since 0.16
 */
export class MarkdownToUniAstConverter {
  constructor(public context: ConverterContext) {}

  /**
   * Parse a markdown document to a universal AST
   *
   * @since 0.16
   *
   * @param markdown - The markdown content to parse
   *
   * @returns The Universal Ast
   */
  parseMarkdown(markdown: string): UniAst | Error {
    // TODO: auto-links (URLs + emails)
    //     > https://jira.xwiki.org/browse/CRISTAL-513

    const ast = unified()
      .use(remarkParse)
      .use(remarkPartialGfm)
      .parse(markdown);

    const blocks = tryFallibleOrError(() =>
      ast.children.map((item) => this.convertBlock(item)),
    );

    return blocks instanceof Error ? blocks : { blocks };
  }

  private convertBlock(block: RootContent): Block {
    switch (block.type) {
      case "paragraph": {
        const content = block.children.flatMap((item) =>
          this.convertInline(item, {}),
        );

        // Paragraphs only made of a single inline macro are actually block macros
        if (content.length === 1 && content[0].type === "inlineMacro") {
          return {
            type: "macroBlock",
            name: content[0].name,
            params: content[0].params,
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
          content: block.children.flatMap((item) =>
            this.convertInline(item, {}),
          ),
          styles: {},
        };

      case "blockquote":
        return {
          type: "quote",
          content: block.children.map((item) => this.convertBlock(item)),
          styles: {},
        };

      case "list":
        // TODO: "token.loose" property
        return {
          type: "list",
          items: block.children.map(
            (item, i): ListItem => ({
              number: block.ordered ? (block.start ?? 1) + i : undefined,
              checked: item.checked ?? undefined,
              content: item.children.map((item) => this.convertBlock(item)),
              styles: {},
            }),
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

      case "table":
        return {
          type: "table",
          columns: block.children[0]?.children.map(
            (cell): TableColumn => ({
              headerCell: {
                content: cell.children.flatMap((item) =>
                  this.convertInline(item, {}),
                ),
                styles: {},
              },
            }),
          ),
          rows: block.children.map((row) =>
            row.children.map(
              (cell): TableCell => ({
                content: cell.children.flatMap((item) =>
                  this.convertInline(item, {}),
                ),
                styles: {},
              }),
            ),
          ),
          styles: {},
        };

      case "image":
        return {
          type: "image",
          ...this.convertImage(block),
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

  private convertInline(
    inline: PhrasingContent,
    styles: TextStyles,
  ): InlineContent[] {
    switch (inline.type) {
      case "image":
        return [
          {
            type: "image",
            ...this.convertImage(inline),
          },
        ];

      case "strong":
        return inline.children.flatMap((item) =>
          this.convertInline(item, { ...styles, bold: true }),
        );

      case "emphasis":
        return inline.children.flatMap((item) =>
          this.convertInline(item, { ...styles, italic: true }),
        );

      case "delete":
        return inline.children.flatMap((item) =>
          this.convertInline(item, {
            ...styles,
            strikethrough: true,
          }),
        );

      case "inlineCode":
        return [
          {
            type: "text",
            content: inline.value,
            styles: {},
          },
        ];

      case "text":
        return this.convertText(inline.value, styles);

      case "html":
      case "footnoteReference":
      case "linkReference":
      case "imageReference":
      case "break":
        throw new Error("TODO: handle inlines of type " + inline.type);

      case "link": {
        return [
          {
            type: "link",
            content: inline.children
              .flatMap((item) => this.convertInline(item, styles))
              .map((token) => {
                if (token.type !== "text") {
                  throw new Error(
                    "Unexpected link inside link in markdown parser",
                  );
                }

                return token;
              }),
            target: { type: "external", url: inline.url },
          },
        ];
      }

      default:
        assertUnreachable(inline);
    }
  }

  private convertImage(image: MdImage): Image {
    // TODO: "token.text" property
    return {
      target: { type: "external", url: image.url },
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
      // Try to find the first XWiki-specific-element syntax in the text (precedence order)
      const firstItem = findFirstMatchIn(text.substring(treated), [
        { name: "image", match: "![[" },
        { name: "link", match: "[[" },
        { name: "macro", match: "{{" },
      ]);

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
              continue;
            }
          }

          if (!closed) {
            break;
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

          const reference = this.context.parseReference(
            targetStr,
            isImage ? EntityType.ATTACHMENT : EntityType.DOCUMENT,
          );

          const target: LinkTarget = {
            type: "internal",
            rawReference: targetStr,
            parsedReference: reference,
          };

          title ??= reference
            ? this.context.getDisplayName(reference)
            : "<invalid reference>";

          out.push(
            isImage
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
                },
          );

          break;
        }

        case "macro": {
          // Find the macro's name
          const macroNameMatch = text
            .substring(match + firstItem.match.length)
            .match(
              // This weird group matches valid accentuated Unicode letters
              /\s*([A-Za-zÀ-ÖØ-öø-ÿ\d]+)(\s+(?=[A-Za-zÀ-ÖØ-öø-ÿ\d/])|(?=\/))/,
            );

          if (!macroNameMatch) {
            treated = match + firstItem.match.length;
            out.push({ type: "text", content: firstItem.match, styles: {} });
            break;
          }

          const macroName = macroNameMatch[1];

          let i;

          // Is the next character being escaped?
          let escaping = false;

          // Parameters are built character by character
          // First the name is parsed from the source, then the value
          let buildingParameter: { name: string; value: string | null } | null =
            null;

          // The list of parsed parameters
          const parameters: Record<string, string> = {};

          // Is the macro being closed?
          let closingMacro = false;

          for (
            i = match + firstItem.match.length + macroNameMatch[0].length;
            i < text.length;
            i++
          ) {
            // Escaping is possible only inside parameter values
            if (escaping) {
              if (!buildingParameter || buildingParameter.value === null) {
                throw new Error("Unexpected");
              }

              escaping = false;
              buildingParameter.value += text[i];

              continue;
            }

            // If we're not building a parameter, we are expecting one thing between...
            if (!buildingParameter) {
              // ...a space (no particular meaning)
              if (text[i] === " ") {
                continue;
              }

              // ...a valid identifier character which will begin the parameter's name
              if (text[i].match(/[A-Za-zÀ-ÖØ-öø-ÿ_\d]/)) {
                buildingParameter = { name: text[i], value: null };
                continue;
              }

              // ...or a closing slash which indicates the macro has no more parameter
              if (text[i] === "/") {
                closingMacro = true;
                break;
              }

              // Invalid character, stop building macro here
              break;
            }

            // If we're building a parameter's name, we are expecting one thing between...
            if (buildingParameter.value === null) {
              // ...a valid identifier character which will continue the parameter's name
              if (text[i].match(/[A-Za-zÀ-ÖØ-öø-ÿ_\d]/)) {
                buildingParameter.name += text[i];
                continue;
              }

              // ...or an '=' operator sign which indicates we are going to assign a value to the parameter
              if (text[i] === "=") {
                // Usually parameters start with a double quote to indicate they have a string-like value
                if (text[i + 1] === '"') {
                  i += 1;
                  buildingParameter.value = "";
                  continue;
                }

                // But unquoted integers are also accepted
                const number = text
                  .substring(i + 1)
                  .match(/\d+(?=[^A-Za-zÀ-ÖØ-öø-ÿ\d])/);

                if (!number) {
                  // Invalid character, stop building macro here
                  break;
                }

                parameters[buildingParameter.name] = number[0];
                buildingParameter = null;

                i += number[0].length;
                continue;
              }

              // Invalid character, stop building macro here
              break;
            }

            // If we reach this point, we are building the parameter's value.
            // Which means we are expecting either:
            // ...an escaping character
            if (text[i] === "\\") {
              escaping = true;
            }
            // ...a closing double quote to indicate the parameter's value's end
            else if (text[i] === '"') {
              parameters[buildingParameter.name] = buildingParameter.value;
              buildingParameter = null;
            }
            // ...or any other character that will continue the parameter's value
            else {
              buildingParameter.value += text[i];
            }
          }

          // When the macro closes, we expect double braces afterwards
          const closingBraces = text.substring(i).match(/\s*}}/);

          // If the macro has not been closed properly (with a '/') or doesn't have the closing braces, it's invalid
          if (!closingMacro || !closingBraces) {
            treated = match + firstItem.match.length;
            out.push({ type: "text", content: firstItem.match, styles: {} });
          }
          // Otherwise, we can properly build the macro
          else {
            treated = i + 1 + closingBraces[0].length;

            // NOTE: If a paragraph only contains an inline macro, it will be converted to a macro block instead
            //       by the calling function
            out.push({
              type: "inlineMacro",
              name: macroName,
              params: parameters,
            });
          }

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
}

function findFirstMatchIn<K extends string>(
  subject: string,
  candidates: Array<{ name: K; match: string }>,
): { name: K; match: string; offset: number } | null {
  let first: { name: K; match: string; offset: number } | null = null;

  for (const { name, match } of candidates) {
    const offset = subject.indexOf(match);

    if (offset !== -1 && (first === null || first.offset > offset)) {
      first = { name, match, offset };
    }
  }

  return first;
}

/**
 * Extension to *partially* support Github's Front Matter (Markdown) syntax flavor
 *
 * Does **NOT** include some of GFM features like autolinks or footnotes, which are implemented differently in another part of the code
 */
function remarkPartialGfm(this: Processor) {
  const data = this.data();

  data.micromarkExtensions ??= [];
  data.fromMarkdownExtensions ??= [];

  data.micromarkExtensions.push(
    gfmStrikethrough(),
    gfmTable(),
    gfmTaskListItem(),
  );

  data.fromMarkdownExtensions.push(
    gfmStrikethroughFromMarkdown(),
    gfmTableFromMarkdown(),
    gfmTaskListItemFromMarkdown(),
  );
}

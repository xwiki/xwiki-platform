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

import { MACRO_NAME_PREFIX } from "@xwiki/cristal-editors-blocknote-react";
import {
  assertUnreachable,
  provideTypeInference,
  tryFallibleOrError,
} from "@xwiki/cristal-fn-utils";
import type { Link, TableCell as BlockNoteTableCell } from "@blocknote/core";
import type {
  BlockType,
  EditorInlineContentSchema,
  EditorLink,
  EditorStyleSchema,
  EditorStyledText,
} from "@xwiki/cristal-editors-blocknote-react";
import type {
  Block,
  BlockStyles,
  ConverterContext,
  InlineContent,
  LinkTarget,
  ListItem,
  TableCell,
  UniAst,
} from "@xwiki/cristal-uniast-api";

// TODO: escape characters that need it (e.g. '`', '\', '*', '_', etc.)

/**
 * Convert the internal format of Blocknote to the Universal AST.
 * @since 0.16
 */
export class BlockNoteToUniAstConverter {
  constructor(public context: ConverterContext) {}

  blocksToUniAst(blocks: BlockType[]): UniAst | Error {
    const uniAstBlocks = tryFallibleOrError(() => this.convertBlocks(blocks));

    return uniAstBlocks instanceof Error
      ? uniAstBlocks
      : {
          blocks: uniAstBlocks,
        };
  }

  // eslint-disable-next-line max-statements
  private convertBlocks(blocks: BlockType[]): Block[] {
    const out: Block[] = [];

    for (const block of blocks) {
      if (
        block.type !== "bulletListItem" &&
        block.type !== "numberedListItem" &&
        block.type !== "checkListItem"
      ) {
        const converted = this.convertBlock(block);

        if (converted !== null) {
          out.push(...(Array.isArray(converted) ? converted : [converted]));
        }

        continue;
      }

      const lastBlock = out.at(-1);
      const currentList = lastBlock?.type === "list" ? lastBlock : null;

      const listItem = this.convertListItem(block, currentList);

      if (currentList) {
        currentList.items.push(listItem);
      } else {
        out.push({
          type: "list",
          items: [listItem],
          styles: {},
        });
      }
    }

    return out;
  }

  private convertBlock(
    block: Exclude<
      BlockType,
      {
        // List items are to be handled through the `convertListItem` method
        type: "bulletListItem" | "numberedListItem" | "checkListItem";
      }
    >,
  ): Block | Block[] | null {
    const dontExpectChildren = () => {
      if (block.children.length > 0) {
        console.error({ unexpextedChildrenInBlock: block });
        throw new Error("Unexpected children in block type: " + block.type);
      }
    };

    // Convert macros
    if (block.type.startsWith(MACRO_NAME_PREFIX)) {
      return {
        type: "macroBlock",
        name: block.type.substring(MACRO_NAME_PREFIX.length),
        // Conversion is required as the AST is dynamically typed
        params: block.props as Record<string, boolean | number | string>,
      };
    }

    switch (block.type) {
      case "paragraph":
        dontExpectChildren();

        return {
          type: "paragraph",
          content: block.content.map((item) => this.convertInlineContent(item)),
          styles: this.convertBlockStyles(block.props),
        };

      case "heading":
        return [
          provideTypeInference<Block>({
            type: "heading",
            level: block.props.level,
            content: block.content.map((item) =>
              this.convertInlineContent(item),
            ),
            styles: this.convertBlockStyles(block.props),
          }),
        ].concat(this.convertBlocks(block.children));

      case "Heading4":
        dontExpectChildren();

        return {
          type: "heading",
          level: 4,
          content: block.content.map((item) => this.convertInlineContent(item)),
          styles: {}, // TODO
        };

      case "Heading5":
        dontExpectChildren();

        return {
          type: "heading",
          level: 4,
          content: block.content.map((item) => this.convertInlineContent(item)),
          styles: {}, // TODO
        };

      case "Heading6":
        dontExpectChildren();

        return {
          type: "heading",
          level: 4,
          content: block.content.map((item) => this.convertInlineContent(item)),
          styles: {}, // TODO
        };

      case "codeBlock":
        dontExpectChildren();

        return {
          type: "code",
          content: block.content
            .map((inline) => {
              if (inline.type !== "text") {
                throw new Error(
                  "Unexpected inline element type in code block: " +
                    inline.type,
                );
              }

              return inline.text;
            })
            .join(""),
          language: block.props.language,
        };

      case "quote":
        dontExpectChildren();

        return {
          type: "quote",
          content: [
            {
              type: "paragraph",
              content: block.content.map((item) =>
                this.convertInlineContent(item),
              ),
              styles: {},
            },
          ],
          styles: this.convertBlockStyles(block.props),
        };

      case "image":
        dontExpectChildren();

        if (!block.props.url) {
          return null;
        }

        return {
          type: "image",
          target: this.parseTarget(block.props.url),
          caption: block.props.caption,
          widthPx: block.props.previewWidth,
          styles: { alignment: block.props.textAlignment },
        };

      case "table": {
        dontExpectChildren();

        const [header, ...rows] = block.content.rows;

        return {
          type: "table",
          columns: header.cells.map((cell, i) => ({
            headerCell: this.convertTableCell(cell),
            widthPx: block.content.columnWidths[i],
          })),
          rows: rows.map((row) =>
            row.cells.map((item) => this.convertTableCell(item)),
          ),
          styles: this.convertBlockStyles(block.props),
        };
      }

      default:
        assertUnreachable(block);
    }
  }

  private convertListItem(
    block: Extract<
      BlockType,
      {
        type: "bulletListItem" | "numberedListItem" | "checkListItem";
      }
    >,
    currentList: Extract<Block, { type: "list" }> | null,
  ): ListItem {
    switch (block.type) {
      case "bulletListItem":
        return {
          content: [
            // TODO: change when nested blocks are supported in blocknote
            {
              type: "paragraph",
              content: block.content.map((item) =>
                this.convertInlineContent(item),
              ),
              styles: {},
            },
            ...this.convertBlocks(block.children),
          ],
          styles: this.convertBlockStyles(block.props),
        };

      case "numberedListItem": {
        const prevNumber = currentList?.items.at(-1)?.number;

        const number = (prevNumber ?? 0) + 1;

        return {
          number,
          content: [
            // TODO: change when nested blocks are supported in blocknote
            {
              type: "paragraph",
              content: block.content.map((item) =>
                this.convertInlineContent(item),
              ),
              styles: {},
            },
            ...this.convertBlocks(block.children),
          ],
          styles: this.convertBlockStyles(block.props),
        };
      }

      case "checkListItem":
        return {
          checked: block.props.checked,
          content: [
            // TODO: change when nested blocks are supported in blocknote
            {
              type: "paragraph",
              content: block.content.map((item) =>
                this.convertInlineContent(item),
              ),
              styles: {},
            },
            ...this.convertBlocks(block.children),
          ],
          styles: this.convertBlockStyles(block.props),
        };

      default:
        assertUnreachable(block);
    }
  }

  private convertBlockStyles(styles: BlockStyles): BlockStyles {
    // Remove unneeded properties
    const { textAlignment, backgroundColor, textColor } = styles;
    return { textAlignment, backgroundColor, textColor };
  }

  private convertTableCell(
    cell:
      | Array<EditorStyledText | EditorLink>
      | BlockNoteTableCell<EditorInlineContentSchema, EditorStyleSchema>,
  ): TableCell {
    return Array.isArray(cell)
      ? {
          content: cell.map((item) => this.convertInlineContent(item)),
          styles: {},
        }
      : {
          content: cell.content.map((item) => this.convertInlineContent(item)),
          styles: this.convertBlockStyles(cell.props),
          colSpan: cell.props.colspan,
          rowSpan: cell.props.rowspan,
        };
  }

  private convertInlineContent(
    inlineContent: EditorStyledText | Link<EditorStyleSchema>,
  ): InlineContent {
    // Handle macros
    if (inlineContent.type.startsWith(MACRO_NAME_PREFIX)) {
      return {
        type: "inlineMacro",
        name: inlineContent.type.substring(MACRO_NAME_PREFIX.length),
        // Conversion is required because the AST is dynamically typed
        params: (
          inlineContent as unknown as {
            props: Record<string, boolean | number | string>;
          }
        ).props,
      };
    }

    switch (inlineContent.type) {
      case "text": {
        const {
          bold,
          italic,
          underline,
          strike,
          code,
          backgroundColor,
          textColor,
        } = inlineContent.styles;

        return {
          type: "text",
          content: inlineContent.text,
          styles: {
            bold: bold ?? false,
            italic: italic ?? false,
            underline: underline ?? false,
            strikethrough: strike ?? false,
            code: code ?? false,
            backgroundColor,
            textColor,
          },
        };
      }

      case "link":
        return {
          type: "link",
          content: inlineContent.content.map((item) => {
            const converted = this.convertInlineContent(item);

            if (converted.type === "link") {
              throw new Error(
                "Nested links are not supported inside BlockNote",
              );
            }

            return converted;
          }),
          target: this.parseTarget(inlineContent.href),
        };
    }
  }

  private parseTarget(url: string): LinkTarget {
    const reference = this.context.parseReferenceFromUrl(url);

    return reference
      ? {
          type: "internal",
          parsedReference: reference,
          // TODO: preserve the raw reference from the original UniAst
          //
          // Waiting for solved issue in BlockNote:
          // > https://github.com/TypeCellOS/BlockNote/issues/1840
          //
          rawReference: this.context.serializeReference(reference),
        }
      : { type: "external", url };
  }
}

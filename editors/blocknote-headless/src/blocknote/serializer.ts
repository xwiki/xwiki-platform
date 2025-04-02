/*
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
  BlockType,
  EditorInlineContentSchema,
  EditorLink,
  EditorStyleSchema,
  EditorStyledText,
} from ".";
import { Link, TableCell as BlockNoteTableCell } from "@blocknote/core";
import {
  Block,
  BlockStyles,
  ConverterContext,
  InlineContent,
  TableCell,
  Text,
  UniAst,
} from "@xwiki/cristal-uniast";

// TODO: escape characters that need it (e.g. '`', '\', '*', '_', etc.)

/**
 * Convert the internal format of Blocknote to the Universal AST.
 * @since 0.16
 */
export class BlockNoteToUniAstConverter {
  constructor(public context: ConverterContext) {}

  blocksToUniAst(blocks: BlockType[]): UniAst {
    return {
      blocks: this.convertBlocks(blocks),
    };
  }

  private convertBlocks(blocks: BlockType[]): Block[] {
    return blocks.map((block, i) =>
      this.convertBlock(block, blocks.slice(0, i)),
    );
  }

  // TODO: explain that previous blocks are required to compute number for contiguous numbered list items
  private convertBlock(block: BlockType, previousBlocks: BlockType[]): Block {
    const dontExpectChildren = () => {
      if (block.children.length > 0) {
        console.error({ unexpextedChildrenInBlock: block });
        throw new Error("Unexpected children in block");
      }
    };

    switch (block.type) {
      case "paragraph":
        dontExpectChildren();

        return {
          type: "paragraph",
          content: block.content.map((item) => this.convertInlineContent(item)),
          styles: this.convertBlockStyles(block.props),
        };

      case "heading":
        dontExpectChildren();

        return {
          type: "heading",
          level: block.props.level,
          content: block.content.map((item) => this.convertInlineContent(item)),
          styles: this.convertBlockStyles(block.props),
        };

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

      case "bulletListItem":
        return {
          type: "bulletListItem",
          content: block.content.map((item) => this.convertInlineContent(item)),
          subItems: this.convertBlocks(block.children).map((block) => {
            if (
              block.type !== "bulletListItem" &&
              block.type !== "numberedListItem"
            ) {
              throw new Error(
                "Unexpected child type in list item: " + block.type,
              );
            }

            return block;
          }),
          styles: this.convertBlockStyles(block.props),
        };

      case "numberedListItem":
        return {
          type: "numberedListItem",
          number: this.computeNumberedListItemNum(block, previousBlocks),
          content: block.content.map((item) => this.convertInlineContent(item)),
          subItems: this.convertBlocks(block.children).map((block) => {
            if (
              block.type !== "bulletListItem" &&
              block.type !== "numberedListItem"
            ) {
              throw new Error(
                "Unexpected child type in list item: " + block.type,
              );
            }

            return block;
          }),
          styles: this.convertBlockStyles(block.props),
        };

      case "checkListItem":
        return {
          type: "checkedListItem",
          checked: block.props.checked,
          content: block.content.map((item) => this.convertInlineContent(item)),
          subItems: this.convertBlocks(block.children).map((block) => {
            if (
              block.type !== "bulletListItem" &&
              block.type !== "numberedListItem"
            ) {
              throw new Error(
                "Unexpected child type in list item: " + block.type,
              );
            }

            return block;
          }),
          styles: this.convertBlockStyles(block.props),
        };

      case "codeBlock":
        dontExpectChildren();

        return {
          type: "codeBlock",
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

      case "BlockQuote":
        dontExpectChildren();

        return {
          type: "blockQuote",
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

        return {
          type: "image",
          target: {
            // TODO: support internal
            type: "external",
            url: block.props.url,
          },
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

      case "column":
        // TODO
        return {
          type: "paragraph",
          content: [],
          styles: {},
        };

      case "columnList":
        return {
          type: "paragraph",
          content: [],
          styles: {},
        };
    }
  }

  // eslint-disable-next-line max-statements
  private computeNumberedListItemNum(
    numberedListItem: Extract<BlockType, { type: "numberedListItem" }>,
    previousBlocks: BlockType[],
  ): number {
    if (numberedListItem.props.start !== undefined) {
      return numberedListItem.props.start;
    }

    let number = 1;

    for (const block of previousBlocks.toReversed()) {
      if (block.type !== "numberedListItem") {
        break;
      }

      if (block.props.start !== undefined) {
        number += block.props.start;
        break;
      }

      number += 1;
    }

    return number;
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
    switch (inlineContent.type) {
      case "text":
        return {
          type: "text",
          props: this.convertText(inlineContent),
        };

      case "link":
        return {
          type: "link",
          content: inlineContent.content.map((item) => this.convertText(item)),
          target: {
            // TODO: internal links
            type: "external",
            url: inlineContent.href,
          },
        };
    }
  }

  private convertText(text: EditorStyledText): Text {
    const {
      bold,
      italic,
      underline,
      strike,
      code,
      backgroundColor,
      textColor,
    } = text.styles;

    return {
      content: text.text,
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
}

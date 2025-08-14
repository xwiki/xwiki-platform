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

import { TableCell } from "@blocknote/core";
import {
  BlockType,
  EditorInlineContentSchema,
  EditorLink,
  EditorStyleSchema,
  EditorStyledText,
  MACRO_NAME_PREFIX,
} from "@xwiki/cristal-editors-blocknote-react";
import { assertUnreachable, tryFallibleOrError } from "@xwiki/cristal-fn-utils";
import {
  Block,
  BlockStyles,
  ConverterContext,
  Image,
  InlineContent,
  UniAst,
} from "@xwiki/cristal-uniast";

/**
 * Converts the Universal AS to the internal format of Blocknote.
 *
 * @since 0.16
 */
export class UniAstToBlockNoteConverter {
  constructor(public context: ConverterContext) {}

  uniAstToBlockNote(uniAst: UniAst): BlockType[] | Error {
    return tryFallibleOrError(() =>
      uniAst.blocks.flatMap((item) => this.convertBlock(item)),
    );
  }

  private convertBlock(block: Block): BlockType | BlockType[] {
    switch (block.type) {
      case "paragraph":
        if (block.content.length === 1 && block.content[0].type === "image") {
          return this.convertImage(block.content[0]);
        }

        return {
          type: "paragraph",
          id: genId(),
          children: [],
          content: block.content.map((item) => this.convertInlineContent(item)),
          props: this.convertBlockStyles(block.styles),
        };

      case "heading":
        switch (block.level) {
          case 1:
          case 2:
          case 3:
            return {
              type: "heading",
              id: genId(),
              children: [],
              content: block.content.map((item) =>
                this.convertInlineContent(item),
              ),
              props: {
                ...this.convertBlockStyles(block.styles),
                level: block.level,
                isToggleable: false,
              },
            };

          case 4:
            return {
              type: "Heading4",
              id: genId(),
              children: [],
              content: block.content.map((item) =>
                this.convertInlineContent(item),
              ),
              props: this.convertBlockStyles(block.styles),
            };

          case 5:
            return {
              type: "Heading5",
              id: genId(),
              children: [],
              content: block.content.map((item) =>
                this.convertInlineContent(item),
              ),
              props: this.convertBlockStyles(block.styles),
            };

          case 6:
            return {
              type: "Heading6",
              id: genId(),
              children: [],
              content: block.content.map((item) =>
                this.convertInlineContent(item),
              ),
              props: this.convertBlockStyles(block.styles),
            };
        }

        break;

      case "quote":
        return {
          type: "quote",
          id: genId(),
          children: [],
          content: this.convertCustomBlockContent(block.content),
          props: this.convertBlockStyles(block.styles),
        };

      case "code":
        return {
          type: "codeBlock",
          id: genId(),
          children: [],
          content: [
            {
              type: "text",
              text: block.content,
              styles: {},
            },
          ],
          props: {
            language: block.language ?? "",
          },
        };

      case "list":
        return this.convertList(block);

      case "table":
        return {
          type: "table",
          id: genId(),
          content: {
            type: "tableContent",
            columnWidths: block.columns.map((col) => col.widthPx),
            rows: block.rows.map((cells) => ({
              cells: cells.map(
                (
                  cell,
                ): TableCell<EditorInlineContentSchema, EditorStyleSchema> => ({
                  type: "tableCell",
                  content: cell.content.map((item) =>
                    this.convertInlineContent(item),
                  ),
                  props: {
                    ...this.convertBlockStyles(cell.styles),
                    colspan: cell.colSpan,
                    rowspan: cell.rowSpan,
                  },
                }),
              ),
            })),
          },
          children: [],
          props: this.convertBlockStyles(block.styles),
        };

      case "image":
        return this.convertImage(block);

      case "break":
        throw new Error("TODO: handle block of type " + block.type);

      case "macroBlock":
        return {
          // @ts-expect-error: macros are dynamically added to the AST
          type: `${MACRO_NAME_PREFIX}${block.name}`,
          id: genId(),
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          props: block.params as any,
        };

      default:
        assertUnreachable(block);
    }
  }

  private convertCustomBlockContent(
    content: Block[],
  ): Array<EditorStyledText | EditorLink> {
    if (content.length > 1 || content[0].type !== "paragraph") {
      throw new Error("Expected a single paragraph inside custom block");
    }

    return content[0].content.map((item) => this.convertInlineContent(item));
  }

  private convertBlockStyles(styles: BlockStyles) {
    return {
      backgroundColor: styles.backgroundColor ?? "default",
      textColor: styles.textColor ?? "default",
      textAlignment: styles.textAlignment ?? "left",
    };
  }

  private convertList(
    list: Extract<Block, { type: "list" }>,
  ): Extract<
    BlockType,
    { type: "bulletListItem" | "checkListItem" | "numberedListItem" }
  >[] {
    // eslint-disable-next-line max-statements
    return list.items.map((listItem) => {
      const contentParagraph = listItem.content.at(0);

      if (contentParagraph && contentParagraph.type !== "paragraph") {
        throw new Error(
          "List items should start with a paragraph in BlockNote",
        );
      }

      const content =
        contentParagraph?.content.map((item) =>
          this.convertInlineContent(item),
        ) ?? [];

      const styles = {
        ...this.convertBlockStyles(list.styles),
        ...this.convertBlockStyles(listItem.styles),
      };

      const subList = listItem.content.at(1);

      if (subList && subList.type !== "list") {
        throw new Error(
          "Only sub-lists are alllowed inside list items in BlockNote",
        );
      }

      const children = subList ? this.convertList(subList) : [];

      if (listItem.checked !== undefined) {
        return {
          id: genId(),
          type: "checkListItem",
          content,
          children,
          props: { ...styles, checked: listItem.checked },
        };
      }

      if (listItem.number !== undefined) {
        return {
          id: genId(),
          type: "numberedListItem",
          content,
          children,
          props: { ...styles, start: listItem.number },
        };
      }

      return {
        id: genId(),
        type: "bulletListItem",
        content,
        children,
        props: styles,
      };
    });
  }

  private convertImage(image: Image): BlockType {
    const url =
      image.target.type === "external"
        ? image.target.url
        : image.target.parsedReference
          ? this.context.getUrlFromReference(image.target.parsedReference)
          : // TODO: think about what to do in case of invalid reference - let it as it is, show an error, replace by a fallback, ...?
            image.target.rawReference;

    return {
      type: "image",
      id: genId(),
      children: [],
      content: undefined,
      props: {
        url,
        caption: image.caption ?? "",
        showPreview: true,
        // TODO: BlockNote specifies an invalid type for previewWidth property on image blocks, which forces us to
        // perform a cast. Remove the cast after https://github.com/TypeCellOS/BlockNote/issues/1765 is fixed.
        previewWidth: image.widthPx ?? (undefined as unknown as number),
        backgroundColor: "default",
        textAlignment: image.styles.alignment ?? "left",
        name: image.alt ?? "",
      },
    };
  }

  private convertInlineContent(
    inlineContent: InlineContent,
  ): EditorStyledText | EditorLink {
    switch (inlineContent.type) {
      case "text": {
        const {
          bold,
          italic,
          underline,
          strikethrough,
          code,
          backgroundColor,
          textColor,
        } = inlineContent.styles;

        return {
          type: "text",
          text: inlineContent.content,
          styles: {
            ...(bold && { bold }),
            ...(italic && { italic }),
            ...(underline && { underline }),
            ...(strikethrough && { strike: true }),
            ...(code && { code }),
            ...(backgroundColor && { backgroundColor }),
            ...(textColor && { textColor }),
          },
        };
      }

      case "link": {
        const href =
          inlineContent.target.type === "external"
            ? inlineContent.target.url
            : inlineContent.target.parsedReference
              ? this.context.getUrlFromReference(
                  inlineContent.target.parsedReference,
                )
              : // TODO: think about what to do in case of invalid reference - let it as it is, show an error, replace by a fallback, ...?
                inlineContent.target.rawReference;

        return {
          type: "link",
          content: inlineContent.content.map((item) => {
            const converted = this.convertInlineContent(item);

            if (converted.type !== "text") {
              throw new Error(
                "Only inline texts are supported inside links in BlockNote",
              );
            }

            return converted;
          }),
          href,
        };
      }

      case "image":
        throw new Error("Inline images are currently unsupported in blocknote");

      case "inlineMacro":
        return {
          // @ts-expect-error: macros are dynamically added to the AST
          type: `${MACRO_NAME_PREFIX}${inlineContent.name}`,
          props: inlineContent.params,
        };
    }
  }
}

function genId(): string {
  return Math.random().toString();
}

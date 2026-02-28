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

import {
  MACRO_NAME_PREFIX,
  buildMacroRawContent,
} from "@xwiki/platform-editors-blocknote-react";
import {
  assertUnreachable,
  tryFallibleOrError,
} from "@xwiki/platform-fn-utils";
import type { Styles, TableCell } from "@blocknote/core";
import type {
  BlockType,
  EditorInlineContentSchema,
  EditorStyleSchema,
  InlineContentType,
} from "@xwiki/platform-editors-blocknote-react";
import type { RemoteURLSerializer } from "@xwiki/platform-model-remote-url-api";
import type {
  Block,
  BlockStyles,
  Image,
  InlineContent,
  TableCell as TableCellUniast,
  TextStyles,
  UniAst,
} from "@xwiki/platform-uniast-api";

/**
 * Converts the Universal AS to the internal format of Blocknote.
 *
 * @since 18.0.0RC1
 * @beta
 */
// TODO: convert to an actual inversify component
export class UniAstToBlockNoteConverter {
  constructor(private readonly remoteURLSerializer: RemoteURLSerializer) {}

  uniAstToBlockNote(uniAst: UniAst): BlockType[] | Error {
    return tryFallibleOrError(() =>
      uniAst.blocks.flatMap((item) => this.convertBlock(item)),
    );
  }

  // eslint-disable-next-line max-statements
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
        return {
          type: "heading",
          id: genId(),
          children: [],
          content: block.content.map((item) => this.convertInlineContent(item)),
          props: {
            ...this.convertBlockStyles(block.styles),
            level: block.level,
            isToggleable: false,
          },
        };

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

      case "table": {
        const headerRow: {
          cells: TableCell<EditorInlineContentSchema, EditorStyleSchema>[];
        }[] = block.columns.some((c) => c.headerCell === undefined)
          ? []
          : [
              {
                cells: block.columns.map((c) =>
                  this.convertCell(c.headerCell!),
                ),
              },
            ];
        const contentRows: {
          cells: TableCell<EditorInlineContentSchema, EditorStyleSchema>[];
        }[] = block.rows.map((cells) => ({
          cells: cells.map((c) => this.convertCell(c)),
        }));
        return {
          type: "table",
          id: genId(),
          content: {
            type: "tableContent",
            headerRows: 1,
            columnWidths: block.columns.map((col) => col.widthPx),
            rows: [...headerRow, ...contentRows],
          },
          children: [],
          props: this.convertBlockStyles(block.styles),
        };
      }

      case "image":
        return this.convertImage(block);

      case "break":
        throw new Error("TODO: handle block of type " + block.type);

      case "macroBlock": {
        let content: InlineContentType[] | null = null;

        const { body } = block.call;

        switch (body.type) {
          case "none":
            content = null;
            break;

          case "raw":
            content = [buildMacroRawContent(body.content)];
            break;

          case "inlineContent":
            throw new Error(
              "Unexpectedly found inlineContent as body for block macro (expected a list of inline contents)",
            );

          case "inlineContents":
            content = body.inlineContents.map((inline) =>
              this.convertInlineContent(inline),
            );
            break;
        }

        const out: BlockType = {
          // @ts-expect-error: AST is dynamically typed
          type: `${MACRO_NAME_PREFIX}${block.call.id}`,
          id: genId(),
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          props: block.call.params as any,
        };

        if (content) {
          out.content = content;
        }

        return out;
      }

      default:
        assertUnreachable(block);
    }
  }

  private convertCell(
    cell: TableCellUniast,
  ): TableCell<EditorInlineContentSchema, EditorStyleSchema> {
    return {
      type: "tableCell",
      content: cell.content.map((item) => this.convertInlineContent(item)),
      props: {
        ...this.convertBlockStyles(cell.styles),
        colspan: cell.colSpan,
        rowspan: cell.rowSpan,
      },
    };
  }

  private convertCustomBlockContent(
    content: Block[],
  ): Array<InlineContentType> {
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
          ? this.remoteURLSerializer.serialize(image.target.parsedReference)!
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

  // eslint-disable-next-line max-statements
  private convertInlineContent(
    inlineContent: InlineContent,
  ): InlineContentType {
    switch (inlineContent.type) {
      case "text": {
        return {
          type: "text",
          text: inlineContent.content,
          styles: this.convertTextStyles(inlineContent.styles),
        };
      }

      case "link": {
        const href =
          inlineContent.target.type === "external"
            ? inlineContent.target.url
            : inlineContent.target.parsedReference
              ? this.remoteURLSerializer.serialize(
                  inlineContent.target.parsedReference,
                )!
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

      case "inlineMacro": {
        let content: InlineContent | null = null;

        const { body } = inlineContent.call;

        switch (body.type) {
          case "none":
            content = null;
            break;

          case "raw":
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            content = buildMacroRawContent(body.content) as any;
            break;

          case "inlineContent":
            content = body.inlineContent;
            break;

          case "inlineContents":
            throw new Error(
              "Unexpectedly found inlineContents as body for inline macro (expected one single inline content at most)",
            );
        }

        const out: InlineContentType = {
          // @ts-expect-error: macros are dynamically added to the AST
          type: `${MACRO_NAME_PREFIX}${inlineContent.call.id}`,
          props: inlineContent.call.params,
        };

        if (content) {
          // @ts-expect-error: AST is dynamically typed
          out.content = content;
        }

        return out;
      }

      case "subscript":
        return {
          type: "subscript",
          content: [
            {
              type: "text",
              text: inlineContent.content,
              styles: inlineContent.styles,
            },
          ],
          props: {},
        };

      case "superscript":
        return {
          type: "superscript",
          content: [
            {
              type: "text",
              text: inlineContent.content,
              styles: inlineContent.styles,
            },
          ],
          props: {},
        };
    }
  }

  private convertTextStyles(styles: TextStyles): Styles<EditorStyleSchema> {
    const {
      bold,
      italic,
      underline,
      strikethrough,
      code,
      backgroundColor,
      textColor,
    } = styles;

    return {
      ...(bold && { bold }),
      ...(italic && { italic }),
      ...(underline && { underline }),
      ...(strikethrough && { strike: true }),
      ...(code && { code }),
      ...(backgroundColor && { backgroundColor }),
      ...(textColor && { textColor }),
    };
  }
}

function genId(): string {
  return Math.random().toString();
}

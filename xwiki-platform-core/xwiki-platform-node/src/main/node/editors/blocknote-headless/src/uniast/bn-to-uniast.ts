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
  extractMacroRawContent,
} from "@xwiki/platform-editors-blocknote-react";
import {
  assertUnreachable,
  provideTypeInference,
  tryFallible,
  tryFallibleOrError,
} from "@xwiki/platform-fn-utils";
import type { Styles, TableCell as BlockNoteTableCell } from "@blocknote/core";
import type {
  BlockType,
  EditorInlineContentSchema,
  EditorStyleSchema,
  InlineContentType,
} from "@xwiki/platform-editors-blocknote-react";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";
import type { ModelReferenceSerializer } from "@xwiki/platform-model-reference-api";
import type { RemoteURLParser } from "@xwiki/platform-model-remote-url-api";
import type {
  Block,
  BlockStyles,
  InlineContent,
  LinkTarget,
  ListItem,
  TableCell,
  TextStyles,
  UniAst,
} from "@xwiki/platform-uniast-api";

/**
 * Convert the internal format of Blocknote to the Universal AST.
 * @since 18.0.0RC1
 * @beta
 */
// TODO: convert to an actual inversify component
export class BlockNoteToUniAstConverter {
  private readonly macros: Record<string, MacroWithUnknownParamsType>;

  constructor(
    private readonly remoteURLParser: RemoteURLParser,
    private readonly modelReferenceSerializer: ModelReferenceSerializer,
    macros: MacroWithUnknownParamsType[],
  ) {
    this.macros = Object.fromEntries(
      macros.map((macro) => [macro.infos.id, macro]),
    );
  }

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

  // eslint-disable-next-line max-statements
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
        console.error({ unexpectedChildrenInBlock: block });
        throw new Error("Unexpected children in block type: " + block.type);
      }
    };

    // Convert macros
    if (block.type.startsWith(MACRO_NAME_PREFIX)) {
      const id = block.type.substring(MACRO_NAME_PREFIX.length);

      if (!Object.hasOwn(this.macros, id)) {
        throw new Error(`Found unregistered macro: "${id}"`);
      }

      const {
        infos: { bodyType },
      } = this.macros[id];

      if (block.content && !Array.isArray(block.content)) {
        throw new Error(
          "Macro block should have a list of contents, found: " +
            block.content.type,
        );
      }

      const content: InlineContentType[] = block.content ?? [];

      return {
        type: "macroBlock",
        call: {
          id,
          // Conversion is required as the AST is dynamically typed
          params: block.props as Record<string, boolean | number | string>,
          body:
            bodyType === "none"
              ? { type: "none" }
              : bodyType === "raw"
                ? {
                    type: "raw",
                    content: extractMacroRawContent(content),
                  }
                : {
                    type: "inlineContents",
                    inlineContents: content.map((inline) =>
                      this.convertInlineContent(inline),
                    ),
                  },
        },
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

      case "heading": {
        const { level } = block.props;

        if (
          level !== 1 &&
          level !== 2 &&
          level !== 3 &&
          level !== 4 &&
          level !== 5 &&
          level !== 6
        ) {
          throw new Error(
            "Unreachable error: heading level should be between 1 and 6",
          );
        }

        return [
          provideTypeInference<Block>({
            type: "heading",
            level,
            content: block.content.map((item) =>
              this.convertInlineContent(item),
            ),
            styles: this.convertBlockStyles(block.props),
          }),
        ].concat(this.convertBlocks(block.children));
      }

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

      case "divider":
        // TODO: support dividers
        // Tracking issue: https://jira.xwiki.org/browse/CRISTAL-756
        throw new Error("TODO: add support for BlockNote dividers to UniAst");

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
      | Array<InlineContentType>
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

  // eslint-disable-next-line max-statements
  private convertInlineContent(
    inlineContent: InlineContentType,
  ): InlineContent {
    // Handle macros
    if (inlineContent.type.startsWith(MACRO_NAME_PREFIX)) {
      const id = inlineContent.type.substring(MACRO_NAME_PREFIX.length);

      if (!Object.hasOwn(this.macros, id)) {
        throw new Error(`Found unregistered macro: "${id}"`);
      }

      const {
        infos: { bodyType },
      } = this.macros[id];

      return {
        type: "inlineMacro",
        call: {
          id,
          // Conversion is required because the AST is dynamically typed
          params: (
            inlineContent as unknown as {
              props: Record<string, boolean | number | string>;
            }
          ).props,
          body:
            bodyType === "none"
              ? { type: "none" }
              : bodyType === "raw"
                ? {
                    type: "raw",
                    content: extractMacroRawContent([inlineContent]),
                  }
                : {
                    type: "inlineContents",
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    inlineContents: [inlineContent as any],
                  },
        },
      };
    }

    switch (inlineContent.type) {
      case "text": {
        return {
          type: "text",
          content: inlineContent.text,
          styles: this.convertTextStyles(inlineContent.styles),
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

      case "subscript": {
        if (inlineContent.content.length === 0) {
          return { type: "subscript", content: "", styles: {} };
        } else if (inlineContent.content.length !== 1) {
          throw new Error(
            "Expected at most one inline content in BlockNote subscript",
          );
        }

        return {
          type: "subscript",
          content: inlineContent.content[0].text,
          styles: this.convertTextStyles(inlineContent.content[0].styles),
        };
      }

      case "superscript": {
        if (inlineContent.content.length === 0) {
          return { type: "subscript", content: "", styles: {} };
        } else if (inlineContent.content.length > 1) {
          throw new Error(
            "Expected at most one inline content in BlockNote superscript",
          );
        }

        return {
          type: "superscript",
          content: inlineContent.content[0].text,
          styles: this.convertTextStyles(inlineContent.content[0].styles),
        };
      }
    }
  }

  private convertTextStyles(styles: Styles<EditorStyleSchema>): TextStyles {
    const {
      bold,
      italic,
      underline,
      strike,
      code,
      backgroundColor,
      textColor,
    } = styles;

    return {
      bold: bold ?? false,
      italic: italic ?? false,
      underline: underline ?? false,
      strikethrough: strike ?? false,
      code: code ?? false,
      backgroundColor,
      textColor,
    };
  }

  private parseTarget(url: string): LinkTarget {
    const reference = tryFallible(() => this.remoteURLParser.parse(url));

    return reference
      ? {
          type: "internal",
          parsedReference: reference,
          // TODO: preserve the raw reference from the original UniAst
          //
          // Waiting for solved issue in BlockNote:
          // > https://github.com/TypeCellOS/BlockNote/issues/1840
          //
          rawReference: this.modelReferenceSerializer.serialize(reference)!,
        }
      : { type: "external", url };
  }
}

import {
  BlockType,
  EditorInlineContentSchema,
  EditorLink,
  EditorStyleSchema,
  EditorStyledText,
} from ".";
import { TableCell } from "@blocknote/core";
import {
  Block,
  BlockStyles,
  ConverterContext,
  InlineContent,
  Text,
  UniAst,
} from "@xwiki/cristal-uniast";

/**
 * Converts the Universal AS to the internal format of Blocknote.
 *
 * @since 0.16
 */
export class UniAstToBlockNoteConverter {
  constructor(public context: ConverterContext) {}

  uniAstToBlockNote(uniAst: UniAst): BlockType[] {
    return uniAst.blocks.map((item) => this.convertBlock(item));
  }

  private convertBlock(block: Block): BlockType {
    switch (block.type) {
      case "paragraph":
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

      case "blockQuote":
        return {
          type: "BlockQuote",
          id: genId(),
          children: [],
          content: this.convertCustomBlockContent(block.content),
          props: this.convertBlockStyles(block.styles),
        };

      case "codeBlock":
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

      case "bulletListItem":
        return {
          type: "bulletListItem",
          id: genId(),
          children: block.subItems.map((item) => this.convertBlock(item)),
          content: block.content.map((item) => this.convertInlineContent(item)),
          props: this.convertBlockStyles(block.styles),
        };

      case "numberedListItem":
        return {
          type: "numberedListItem",
          id: genId(),
          children: block.subItems.map((item) => this.convertBlock(item)),
          content: block.content.map((item) => this.convertInlineContent(item)),
          props: {
            ...this.convertBlockStyles(block.styles),
            start: block.number,
          },
        };

      case "checkedListItem":
        return {
          type: "checkListItem",
          id: genId(),
          children: block.subItems.map((item) => this.convertBlock(item)),
          content: block.content.map((item) => this.convertInlineContent(item)),
          props: {
            ...this.convertBlockStyles(block.styles),
            checked: block.checked,
          },
        };

      case "table":
        return {
          type: "table",
          id: genId(),
          content: {
            type: "tableContent",
            columnWidths: block.columns.map((col) => col.widthPx),
            rows: block.rows.map((cells) => ({
              cells: cells.map(
                (cell) =>
                  ({
                    type: "tableCell",
                    content: cell.content.map((item) =>
                      this.convertInlineContent(item),
                    ),
                    props: {
                      ...this.convertBlockStyles(cell.styles),
                      colspan: cell.colSpan,
                      rowspan: cell.rowSpan,
                    },
                  }) satisfies TableCell<
                    EditorInlineContentSchema,
                    EditorStyleSchema
                  >,
              ),
            })),
          },
          children: [],
          props: this.convertBlockStyles(block.styles),
        };

      case "image":
        return {
          type: "image",
          id: genId(),
          children: [],
          content: undefined,
          props: {
            url:
              block.target.type === "external"
                ? block.target.url
                : (this.context.serializeReferenceToUrl(
                    block.target.reference,
                  ) ??
                  // TODO: proper error handling
                  "about:blank"),
            caption: block.caption ?? "",
            showPreview: true,
            previewWidth: block.widthPx ?? 0,
            backgroundColor: "default",
            textAlignment: block.styles.alignment ?? "left",
            // TODO (?)
            name: "",
          },
        };

      case "macro":
        throw new Error("TODO: macro");
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

  private convertInlineContent(
    inlineContent: InlineContent,
  ): EditorStyledText | EditorLink {
    switch (inlineContent.type) {
      case "text":
        return this.convertText(inlineContent.props);

      case "link":
        return {
          type: "link",
          content: inlineContent.content.map((item) => this.convertText(item)),
          href:
            inlineContent.target.type === "external"
              ? inlineContent.target.url
              : (this.context.serializeReferenceToUrl(
                  inlineContent.target.reference,
                ) ??
                // TODO: proper error handling
                "about:blank"),
        };
    }
  }

  private convertText(text: Text): EditorStyledText {
    return {
      type: "text",
      text: text.content,
      styles: text.styles,
    };
  }
}

function genId(): string {
  return Math.random().toString();
}

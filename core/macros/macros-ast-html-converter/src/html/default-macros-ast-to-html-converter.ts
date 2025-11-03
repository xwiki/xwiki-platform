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
import { assertUnreachable, tryFallibleOrError } from "@xwiki/cristal-fn-utils";
import { inject, injectable } from "inversify";
import type { MacrosAstToHtmlConverter } from "./macros-ast-to-html-converter";
import type {
  MacroBlock,
  MacroBlockStyles,
  MacroInlineContent,
  MacroLinkTarget,
} from "@xwiki/cristal-macros-api";
import type { ModelReferenceParserProvider } from "@xwiki/cristal-model-reference-api";
import type { RemoteURLSerializerProvider } from "@xwiki/cristal-model-remote-url-api";

/**
 * Converter that transforms a macro's returned AST to HTML
 *
 * @since 0.24
 * @beta
 */
@injectable()
export class DefaultMacrosAstToHtmlConverter
  implements MacrosAstToHtmlConverter
{
  /**
   * XML serializer for fast HTML escaping
   */
  private readonly xmlSerializer = new XMLSerializer();

  constructor(
    @inject("ModelReferenceParserProvider")
    private readonly modelReferenceParserProvider: ModelReferenceParserProvider,

    @inject("RemoteURLSerializerProvider")
    private readonly remoteURLSerializerProvider: RemoteURLSerializerProvider,
  ) {}

  /**
   * Render a macro's AST blocks to an HTML string
   *
   * @param blocks - The blocks to render
   *
   * @returns The HTML render
   */
  blocksToHTML(blocks: MacroBlock[]): string | Error {
    return tryFallibleOrError(() => this.convertBlocks(blocks));
  }

  /**
   * Render a macro's AST inline contents to an HTML stirng
   *
   * @param inlineContents - The inline contents to render
   *
   * @returns The HTML render
   */
  inlineContentsToHTML(inlineContents: MacroInlineContent[]): string | Error {
    return tryFallibleOrError(() => this.convertInlineContents(inlineContents));
  }

  private convertBlocks(blocks: MacroBlock[]): string {
    return blocks.map((block) => this.convertBlock(block)).join("");
  }

  private convertInlineContents(inlineContents: MacroInlineContent[]): string {
    return inlineContents
      .map((inlineContent) => this.convertInlineContent(inlineContent))
      .join("");
  }

  private convertBlock(block: MacroBlock): string {
    switch (block.type) {
      case "paragraph":
        return this.produceBlockHtml(
          "p",
          block.styles,
          this.convertInlineContents(block.content),
        );

      case "heading":
        return this.produceBlockHtml(
          `h${block.level}`,
          block.styles,
          this.convertInlineContents(block.content),
        );

      case "list":
        return this.produceBlockHtml(
          block.numbered ? "ol" : "ul",
          block.styles,
          block.items
            .map((item) =>
              this.produceHtmlEl(
                "li",
                {},
                `${item.checked !== undefined ? this.produceHtmlEl("input", { type: "checkbox", checked: item.checked.toString(), readonly: "true" }, false) : ""}${this.convertInlineContents(item.content)}`,
              ),
            )
            .join(""),
        );

      case "quote":
        return this.produceBlockHtml(
          "blockquote",
          block.styles,
          this.convertBlocks(block.content),
        );

      case "code":
        // TODO: syntax highlighting?
        return this.produceBlockHtml("pre", {}, this.escapeHtml(block.content));

      case "table": {
        const colgroup = this.produceHtmlEl(
          "colgroup",
          {},
          block.columns
            .map((col) =>
              this.produceHtmlEl(
                "col",
                {
                  width: col.widthPx ? `${col.widthPx}px` : "",
                },
                false,
              ),
            )
            .join(""),
        );

        const thead = block.columns.find((col) => col.headerCell)
          ? this.produceHtmlEl(
              "thead",
              {},
              block.columns
                .map((col) =>
                  col.headerCell
                    ? this.produceBlockHtml(
                        "th",
                        col.headerCell.styles,
                        this.convertInlineContents(col.headerCell.content),
                      )
                    : "",
                )
                .join(""),
            )
          : "";

        const tbody = block.rows.map((row) =>
          this.produceHtmlEl(
            "tr",
            {},

            row
              .map((cell) =>
                this.produceBlockHtml(
                  "td",
                  cell.styles,
                  this.convertInlineContents(cell.content),
                  {
                    colspan: cell.colSpan?.toString(),
                    rowspan: cell.rowSpan?.toString(),
                  },
                ),
              )
              .join(""),
          ),
        );

        return this.produceBlockHtml(
          "table",
          block.styles,
          [colgroup, thead, tbody].join(""),
        );
      }

      case "image":
        return this.produceHtmlEl(
          "img",
          {
            src: this.getTargetUrl(block.target),
            alt: block.alt,
            width: block.widthPx ? `${block.widthPx}px` : undefined,
            height: block.heightPx ? `${block.heightPx}px` : undefined,
          },
          false,
        );

      case "rawHtml":
        // NOTE: This HTML will not be sanitized ; it is up to the macro itself to ensure the HTML is safe for the end user
        return block.html;

      case "macroBlock":
        throw new Error("Nested macros are not supported yet");

      case "macroBlockEditableArea":
        return "<!-- Macro block editable aera -->";

      default:
        assertUnreachable(block);
    }
  }

  // eslint-disable-next-line max-statements
  private convertInlineContent(inlineContent: MacroInlineContent): string {
    switch (inlineContent.type) {
      case "text": {
        const { content, styles } = inlineContent;

        const {
          bold,
          italic,
          strikethrough,
          underline,
          code,
          textColor,
          backgroundColor,
        } = styles;

        let html = this.escapeHtml(content);

        if (bold) {
          html = this.produceHtmlEl(
            "strong",
            { style: "font-weight: bold;" },
            html,
          );
        }

        if (italic) {
          html = this.produceHtmlEl(
            "em",
            { style: "font-style: italic;" },
            html,
          );
        }

        if (strikethrough) {
          html = this.produceHtmlEl(
            "s",
            { style: "text-decoration: italic;" },
            html,
          );
        }

        if (underline) {
          html = this.produceHtmlEl(
            "u",
            { style: "text-decoration: underline;" },
            html,
          );
        }

        if (textColor) {
          html = this.produceHtmlEl(
            "span",
            { style: `color: ${textColor};` },
            html,
          );
        }

        if (backgroundColor) {
          html = this.produceHtmlEl(
            "span",
            { style: `background-color: ${backgroundColor};` },
            html,
          );
        }

        // Code must be last as it's going to be the most outer surrounding
        // Otherwise other surroundings would be "trapped" inside the inline code content
        if (code) {
          html = this.produceHtmlEl("pre", {}, html);
        }

        return html;
      }

      case "link":
        return this.produceHtmlEl(
          "a",
          { href: this.getTargetUrl(inlineContent.target) },
          this.convertInlineContents(inlineContent.content),
        );

      case "rawHtml":
        return inlineContent.html;

      case "inlineMacro":
        throw new Error("Nested macros are not supported yet");

      case "inlineMacroEditableArea":
        return "<!-- Macro inline editable aera -->";

      default:
        assertUnreachable(inlineContent);
    }
  }

  private getTargetUrl(target: MacroLinkTarget): string {
    if (target.type === "external") {
      return target.url;
    }

    const { rawReference } = target;

    const parsedRef = tryFallibleOrError(() =>
      this.modelReferenceParserProvider.get()!.parse(rawReference),
    );

    if (parsedRef instanceof Error) {
      throw new Error(
        `Failed to parse reference "${rawReference}": ${parsedRef.message}`,
      );
    }

    const url = this.remoteURLSerializerProvider.get()!.serialize(parsedRef);

    // TODO: when could this even happen?
    if (!url) {
      throw new Error(`Failed to serialize reference "${rawReference}"`);
    }

    return url;
  }

  private produceBlockHtml(
    tagName: string,
    styles: MacroBlockStyles,
    innerHTML: string,
    attrs?: Record<string, string | undefined>,
  ): string {
    let cssRules = "";

    if (styles.backgroundColor) {
      cssRules += `background-color: ${styles.backgroundColor};`;
    }

    if (styles.textColor) {
      cssRules += `color: ${styles.textColor};`;
    }

    if (styles.textAlignment) {
      cssRules += `text-align: ${styles.textAlignment};`;
    }

    return this.produceHtmlEl(
      tagName,
      {
        ...attrs,
        style: cssRules !== "" ? cssRules.trim() : undefined,
        class: styles.cssClasses?.length
          ? styles.cssClasses.join(" ")
          : undefined,
      },
      innerHTML,
    );
  }

  private produceHtmlEl(
    tagName: string,
    attrs: Record<string, string | undefined>,
    innerHTML: string | false,
  ): string {
    const el = document.createElement(tagName);

    for (const [name, value] of Object.entries(attrs)) {
      if (value !== undefined) {
        el.setAttribute(name, value);
      }
    }

    if (innerHTML !== false) {
      el.innerHTML = innerHTML;
    }

    return el.outerHTML;
  }

  private escapeHtml(content: string) {
    return this.xmlSerializer.serializeToString(
      document.createTextNode(content),
    );
  }
}

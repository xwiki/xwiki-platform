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

import { tryFallibleOrError } from "@xwiki/cristal-fn-utils";
import type {
  Block,
  Image,
  InlineContent,
  ListItem,
  TableCell,
  Text,
  UniAst,
} from "@xwiki/cristal-uniast-api";

/**
 * Converts Universal AST trees to markdown.
 *
 * @since 0.16
 * @beta
 */
export class UniAstToMarkdownConverter {
  toMarkdown(uniAst: UniAst): string | Error {
    const { blocks } = uniAst;

    const out: string[] = [];

    for (let i = 0; i < blocks.length; i++) {
      const md = tryFallibleOrError(() => this.blockToMarkdown(blocks[i]));

      if (md instanceof Error) {
        return md;
      }

      out.push(md);
    }

    return out.join("\n\n");
  }

  private blockToMarkdown(block: Block): string {
    switch (block.type) {
      case "paragraph":
        return this.convertInlineContents(block.content);

      case "heading":
        return `${"#".repeat(block.level)} ${this.convertInlineContents(block.content)}`;

      case "list": {
        return block.items.map((item) => this.convertListItem(item)).join("\n");
      }

      case "quote":
        return block.content
          .map((item) => this.blockToMarkdown(item))
          .flatMap((item) => item.split("\n"))
          .map((line) => `> ${line}`)
          .join("\n");

      case "code":
        return `\`\`\`${block.language ?? ""}\n${block.content}\n\`\`\``;

      case "table":
        return this.convertTable(block);

      case "image":
        return this.convertImage(block);

      case "break":
        return "---";

      case "macroBlock":
        return this.convertMacro(block.name, block.params);
    }
  }

  private convertListItem(listItem: ListItem): string {
    let prefix = listItem.number !== undefined ? `${listItem.number}. ` : "* ";

    if (listItem.checked !== undefined) {
      prefix += `[${listItem.checked ? "x" : " "}] `;
    }

    const content = listItem.content
      .flatMap((item) => this.blockToMarkdown(item).split("\n"))
      .map((line, i) => (i > 0 ? " ".repeat(prefix.length) : "") + line)
      .join("\n");

    return `${prefix}${content}`;
  }

  private convertImage(image: Image): string {
    // TODO: alt text
    return image.target.type === "external"
      ? `![${image.alt}](${image.target.url})`
      : `![[${image.alt}|${image.target.rawReference}]]`;
  }

  private convertTable(table: Extract<Block, { type: "table" }>): string {
    const { columns, rows } = table;

    const out = [
      columns
        .map((column) =>
          column.headerCell ? this.convertTableCell(column.headerCell) : "",
        )
        .join(" | "),
      columns.map(() => " - ").join(" | "),
    ];

    for (const cell of rows) {
      out.push(cell.map((item) => this.convertTableCell(item)).join(" | "));
    }

    return out.map((line) => `| ${line} |`).join("\n");
  }

  private convertTableCell(cell: TableCell): string {
    return this.convertInlineContents(cell.content);
  }

  private convertInlineContents(inlineContents: InlineContent[]): string {
    return inlineContents
      .map((item) => this.convertInlineContent(item))
      .join("");
  }

  private convertInlineContent(inlineContent: InlineContent): string {
    switch (inlineContent.type) {
      case "text":
        return this.convertText(inlineContent);

      case "image":
        return this.convertImage(inlineContent);

      case "link":
        switch (inlineContent.target.type) {
          case "external":
            return `[${this.convertInlineContents(inlineContent.content)}](${inlineContent.target.url})`;

          case "internal":
            return `[[${this.convertInlineContents(inlineContent.content)}|${inlineContent.target.rawReference}]]`;
        }

        break;

      case "inlineMacro":
        return this.convertMacro(inlineContent.name, inlineContent.params);
    }
  }

  private convertMacro(
    name: string,
    parameters: Record<string, boolean | number | string>,
  ): string {
    return `{{${name}${Object.entries(parameters)
      .map(
        ([name, value]) =>
          ` ${name}="${value.toString().replace(/\\/g, "\\\\\\").replace(/"/g, '\\\\"')}"`,
      )
      .join("")} /}}`;
  }

  // eslint-disable-next-line max-statements
  private convertText(text: Text): string {
    const { content, styles } = text;

    const { bold, italic, strikethrough, code } = styles;

    const surroundings = [];

    // Code must be first as it's going to be the most outer surrounding
    // Otherwise other surroundings would be "trapped" inside the inline code content
    if (code) {
      surroundings.push("`");
    }

    if (strikethrough) {
      surroundings.push("~~");
    }

    if (italic) {
      surroundings.push("_");
    }

    if (bold) {
      surroundings.push("**");
    }

    return `${surroundings.join("")}${content}${surroundings.reverse().join("")}`;
  }
}

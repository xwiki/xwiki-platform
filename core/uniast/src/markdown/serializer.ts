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
  Block,
  InlineContent,
  ListItem,
  TableCell,
  Text,
  UniAst,
} from "../ast";

/**
 * Converts Universal AST trees to markdown.
 *
 * @since 0.16
 */
export class UniAstToMarkdownConverter {
  toMarkdown(uniAst: UniAst): string {
    const { blocks } = uniAst;

    const out: string[] = [];

    for (let i = 0; i < blocks.length; i++) {
      out.push(this.blockToMarkdown(blocks[i]));
    }

    return out.join("\n\n");
  }

  private blockToMarkdown(block: Block): string {
    switch (block.type) {
      case "paragraph":
        return this.inlineContentsToMarkdown(block.content);

      case "heading":
        return `${"#".repeat(block.level)} ${this.inlineContentsToMarkdown(block.content)}`;

      case "bulletListItem":
        return `* ${this.listItemContentToMarkdown(block)}`;

      case "numberedListItem": {
        return `${block.number}. ${this.listItemContentToMarkdown(block)}`;
      }

      case "checkedListItem":
        return `* [${block.checked ? "x" : " "}] ${this.listItemContentToMarkdown(block)}`;

      case "blockQuote":
        return block.content
          .map((item) => this.blockToMarkdown(item))
          .flatMap((item) => item.split("\n"))
          .map((line) => `> ${line}`)
          .join("\n");

      case "codeBlock":
        return `\`\`\`${block.language ?? ""}\n${block.content}\n\`\`\``;

      case "table":
        return this.tableToMarkdown(block);

      case "image":
        return block.target.type === "external"
          ? `![${block.caption}](${block.target.url})`
          : `![[${block.caption}|${block.target.reference}]]`;

      case "macro":
        throw new Error("TODO: macro");
    }
  }

  private listItemContentToMarkdown(item: ListItem): string {
    return (
      this.inlineContentsToMarkdown(item.content) +
      item.subItems
        .map((item) => this.blockToMarkdown(item))
        .flatMap((item) => item.split("\n"))
        .map((line) => "\n\t" + line)
        .join("")
    );
  }

  private tableToMarkdown(table: Extract<Block, { type: "table" }>): string {
    const { columns, rows } = table;

    const out = [
      columns
        .map((column) =>
          column.headerCell ? this.tableCellToMarkdown(column.headerCell) : "",
        )
        .join(" | "),
      columns.map(() => " - ").join(" | "),
    ];

    for (const cell of rows) {
      out.push(cell.map((item) => this.tableCellToMarkdown(item)).join(" | "));
    }

    return out.map((line) => `| ${line} |`).join("\n");
  }

  private tableCellToMarkdown(cell: TableCell): string {
    return this.inlineContentsToMarkdown(cell.content);
  }

  private inlineContentsToMarkdown(inlineContents: InlineContent[]): string {
    return inlineContents
      .map((item) => this.inlineContentToMarkdown(item))
      .join("");
  }

  private inlineContentToMarkdown(inlineContent: InlineContent): string {
    switch (inlineContent.type) {
      case "text":
        return this.textToMarkdown(inlineContent.props);

      case "link":
        switch (inlineContent.target.type) {
          case "external":
            return `[${inlineContent.content.map((item) => this.textToMarkdown(item)).join("")}](${inlineContent.target.url})`;

          case "internal":
            return `[[${inlineContent.content.map((item) => this.textToMarkdown(item)).join("")}|${inlineContent.target.reference}]]`;
        }
    }
  }

  // eslint-disable-next-line max-statements
  private textToMarkdown(text: Text): string {
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

    return `${surroundings.join("")}${content}${surroundings.toReversed().join("")}`;
  }
}

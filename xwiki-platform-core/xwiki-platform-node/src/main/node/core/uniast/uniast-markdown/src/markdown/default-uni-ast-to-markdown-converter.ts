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
import { InternalLinksSerializerResolver } from "./internal-links/serializer/internal-links-serializer-resolver";
import { tryFallibleOrError } from "@manuelleducorg/fn-utils";
import { inject, injectable } from "inversify";
import type { UniAstToMarkdownConverter } from "./uni-ast-to-markdown-converter";
import type {
  Block,
  Image,
  InlineContent,
  Link,
  ListItem,
  TableCell,
  Text,
  UniAst,
} from "@manuelleducorg/uniast-api";

/**
 * @since 0.22
 */
@injectable()
export class DefaultUniAstToMarkdownConverter
  implements UniAstToMarkdownConverter
{
  constructor(
    @inject("InternalLinksSerializerResolver")
    private readonly internalLinksSerializerResolver: InternalLinksSerializerResolver,
  ) {}

  /**
   * Converts the provided AST to Markdown.
   *
   * @param uniAst - the AST to convert to markdown
   *
   * understand the impacts
   */
  async toMarkdown(uniAst: UniAst): Promise<string | Error> {
    const { blocks } = uniAst;

    const out: Promise<string>[] = [];

    for (const element of blocks) {
      // TODO: fix this (can't error as it's a promise)
      const md = tryFallibleOrError(() => this.blockToMarkdown(element));

      if (md instanceof Error) {
        return md;
      }

      out.push(md);
    }

    // TODO: try fallible here as this could throw
    return (await Promise.all(out)).join("\n\n");
  }

  private async blockToMarkdown(block: Block): Promise<string> {
    switch (block.type) {
      case "paragraph":
        return this.convertInlineContents(block.content);

      case "heading":
        return `${"#".repeat(block.level)} ${await this.convertInlineContents(block.content)}`;

      case "list": {
        return (
          await Promise.all(
            block.items.map((item) => this.convertListItem(item)),
          )
        ).join("\n");
      }

      case "quote": {
        const values = block.content
          .map((item) => this.blockToMarkdown(item))
          .flatMap(async (item) => (await item).split("\n"))
          .flatMap(async (line) => {
            const strings = await line;
            return strings.map((s) => `> ${s}`).join("\n");
          });
        return (await Promise.all(values)).join("\n");
      }

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

  private async convertListItem(listItem: ListItem): Promise<string> {
    let prefix = listItem.number !== undefined ? `${listItem.number}. ` : "* ";

    if (listItem.checked !== undefined) {
      prefix += `[${listItem.checked ? "x" : " "}] `;
    }

    const contents: string[] = [];
    for (const item of listItem.content) {
      const md = await this.blockToMarkdown(item);
      const lines = md.split("\n");
      contents.push(
        lines
          .map((line, i) => (i > 0 ? " ".repeat(prefix.length) : "") + line)
          .join("\n"),
      );
    }
    return `${prefix}${contents.join("\n")}`;
  }

  private async convertImage(image: Image): Promise<string> {
    // TODO: alt text
    return image.target.type === "external"
      ? `![${image.alt ?? ""}](${image.target.url})`
      : await (
          await this.internalLinksSerializerResolver.get()
        ).serializeImage(image.target, image.alt);
  }

  private async convertTable(
    table: Extract<Block, { type: "table" }>,
  ): Promise<string> {
    const { columns, rows } = table;

    const out = [
      (
        await Promise.all(
          columns.map((column) =>
            column.headerCell ? this.convertTableCell(column.headerCell) : "",
          ),
        )
      ).join(" | "),
      columns.map(() => " - ").join(" | "),
    ];

    for (const cell of rows) {
      out.push(
        (
          await Promise.all(cell.map((item) => this.convertTableCell(item)))
        ).join(" | "),
      );
    }

    return out.map((line) => `| ${line} |`).join("\n");
  }

  private convertTableCell(cell: TableCell): Promise<string> {
    return this.convertInlineContents(cell.content);
  }

  async convertInlineContents(
    inlineContents: InlineContent[],
  ): Promise<string> {
    return (
      await Promise.all(
        inlineContents.map((item) => this.convertInlineContent(item)),
      )
    ).join("");
  }

  async convertInlineContent(inlineContent: InlineContent): Promise<string> {
    switch (inlineContent.type) {
      case "text":
        return this.convertText(inlineContent);
      case "image":
        return this.convertImage(inlineContent);
      case "link":
        return this.convertLink(inlineContent);
      case "inlineMacro":
        return this.convertMacro(inlineContent.name, inlineContent.params);
    }
  }

  private async convertLink(inlineContent: Link): Promise<string> {
    switch (inlineContent.target.type) {
      case "external":
        return `[${await this.convertInlineContents(inlineContent.content)}](${inlineContent.target.url})`;

      case "internal": {
        const linksSerializer =
          await this.internalLinksSerializerResolver.get();
        return linksSerializer.serialize(
          inlineContent.content,
          inlineContent.target,
          this,
        );
      }
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

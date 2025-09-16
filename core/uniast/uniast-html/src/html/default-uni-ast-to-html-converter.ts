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
import { EntityType } from "@xwiki/cristal-model-api";
import { inject, injectable } from "inversify";
import { escape } from "lodash-es";
import type { UniAstToHTMLConverter } from "./uni-ast-to-html-converter";
import type { EntityReference } from "@xwiki/cristal-model-api";
import type { ModelReferenceParserProvider } from "@xwiki/cristal-model-reference-api";
import type { RemoteURLSerializerProvider } from "@xwiki/cristal-model-remote-url-api";
import type {
  Block,
  Image,
  InlineContent,
  ListItem,
  TableCell,
  Text,
  UniAst,
} from "@xwiki/cristal-uniast-api";

@injectable()
export class DefaultUniAstToHTMLConverter implements UniAstToHTMLConverter {
  constructor(
    @inject("RemoteURLSerializerProvider")
    private readonly remoteURLSerializerProvider: RemoteURLSerializerProvider,
    @inject("ModelReferenceParserProvider")
    private readonly modelReferenceParserProvider: ModelReferenceParserProvider,
  ) {}

  toHtml(uniAst: UniAst): string | Error {
    const { blocks } = uniAst;

    const out: string[] = [];

    for (const block of blocks) {
      try {
        out.push(this.blockToHTML(block));
      } catch (e) {
        console.error(e);
      }
    }

    return out.join("\n");
  }

  private blockToHTML(block: Block): string {
    switch (block.type) {
      case "paragraph":
        return `<p>${this.convertInlineContents(block.content)}</p>`;

      case "heading":
        return `<h${block.level}>${this.convertInlineContents(block.content)}</h${block.level}>`;

      case "list": {
        const tag = block.items[0]?.number ? "ol" : "ul";
        return `<${tag}>${block.items
          .map((item) => this.convertListItem(item))
          .join("")}</${tag}>`;
      }

      case "quote": {
        const blockquoteContent = block.content
          .map((item) => this.blockToHTML(item))
          ?.join("");
        return `<blockquote>${blockquoteContent}</blockquote>`;
      }

      case "code":
        // TODO: support for syntax highlighting
        return `<pre>${this.escapeHTML(block.content)}</pre>`;

      case "table":
        return this.convertTable(block);

      case "image":
        return this.convertImage(block);

      case "break":
        return "<hr>";

      case "macroBlock":
        // TODO: currently unsupported
        return "";
    }
  }

  private convertListItem(listItem: ListItem): string {
    return `<li>${listItem.content.map((item) => this.blockToHTML(item)).join("")}</li>`;
  }

  private convertImage(image: Image): string {
    const target = image.target;
    let srcValue: string;
    if (target.type === "external") {
      srcValue = escape(target.url);
    } else if (target.parsedReference !== null) {
      srcValue = escape(
        this.remoteURLSerializerProvider
          .get()!
          .serialize(target.parsedReference),
      );
    } else {
      srcValue = escape(
        this.convertReference(target.rawReference, EntityType.ATTACHMENT),
      );
    }
    const altValue: string = escape(image.alt) ?? "";
    return `<img src="${srcValue}" alt="${altValue}">`;
  }

  private convertTable(table: Extract<Block, { type: "table" }>): string {
    const { columns, rows } = table;

    const ths: string = columns
      .map(
        (column) =>
          `<th>${column.headerCell ? this.convertTableCell(column.headerCell) : ""}</th>`,
      )
      .join("");

    const trs: string = rows
      .map((row) =>
        row.map((cell) => `<td>${this.convertTableCell(cell)}</td>`).join(""),
      )
      .map((row) => `<tr>${row}</tr>`)
      .join("");

    return `<table><thead>${ths}</thead><tbody>${trs}</tbody></table>`;
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

      case "link": {
        const linkContent = this.convertInlineContents(inlineContent.content);
        switch (inlineContent.target.type) {
          case "external":
            return `<a href="${escape(inlineContent.target.url)}" class="wikiexternallink">${linkContent}</a>`;

          case "internal": {
            // TODO: convert reference

            const href = inlineContent.target.parsedReference
              ? this.serializeReference(inlineContent.target.parsedReference)
              : this.convertReference(
                  inlineContent.target.rawReference,
                  EntityType.DOCUMENT,
                );
            return `<a href="${escape(href)}">${linkContent}</a>`;
          }
        }
        break;
      }

      case "inlineMacro":
        // TODO: currently unsupported
        return "";
    }
  }

  // eslint-disable-next-line max-statements
  private convertText(text: Text): string {
    const { content, styles } = text;

    const { bold, italic, strikethrough, code } = styles;

    const surroundings = [];

    // Code must be first as it's going to be the most outer surrounding
    // Otherwise other surroundings would be "trapped" inside the inline code content
    if (code) {
      surroundings.push("pre");
    }

    if (strikethrough) {
      surroundings.push("s");
    }

    if (italic) {
      surroundings.push("em");
    }

    if (bold) {
      surroundings.push("strong");
    }

    let output = "";

    for (const surrounding of surroundings) {
      output += `<${surrounding}>`;
    }

    output += this.escapeHTML(content);

    surroundings.reverse();
    for (const surrounding of surroundings) {
      output += `</${surrounding}>`;
    }

    return output;
  }

  private escapeHTML(content: string) {
    const text = document.createTextNode(content);
    const p = document.createElement("p");
    p.appendChild(text);
    return p.innerHTML;
  }

  private convertReference(rawReference: string, type: EntityType) {
    const parseReference = this.modelReferenceParserProvider
      .get()!
      .parse(rawReference, { type });
    return this.serializeReference(parseReference);
  }

  private serializeReference(parseReference: EntityReference) {
    return this.remoteURLSerializerProvider.get()!.serialize(parseReference);
  }
}

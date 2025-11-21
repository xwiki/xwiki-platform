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
import type { InlineContent, UniAst } from "@xwiki/platform-uniast-api";

/**
 * Converts Universal AST trees to markdown.
 *
 * @since 0.16
 * @beta
 */
export interface UniAstToMarkdownConverter {
  /**
   * Converts the provided AST to Markdown.
   *
   * @param uniAst - the AST to convert to markdown
   *
   * understand the impacts
   */
  toMarkdown(uniAst: UniAst): Promise<string | Error>;

  /**
   * @since 0.22
   * @internal
   * @param inlineContents - the inline contents to convert to markdown
   * @returns the markdown representation of the inline content
   */
  convertInlineContents(inlineContents: InlineContent[]): Promise<string>;
}

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
import { uniAstToHTMLConverterName } from "@xwiki/cristal-uniast-html";
import { markdownToUniAstConverterName } from "@xwiki/cristal-uniast-markdown";
import { Container } from "inversify";
import type { UniAstToHTMLConverter } from "@xwiki/cristal-uniast-html";
import type { MarkdownToUniAstConverter } from "@xwiki/cristal-uniast-markdown";

/**
 * Converts a markdown source into html.
 *
 * @param source - the markdown content
 * @param container - the inversify container
 * @since 0.22
 */
export async function renderMarkdown(
  source: string,
  container: Container,
): Promise<string> {
  // Mardown to uniast to html

  const md = container.get<MarkdownToUniAstConverter>(
    markdownToUniAstConverterName,
  );

  const html = container.get<UniAstToHTMLConverter>(uniAstToHTMLConverterName);

  const uniAst = await md.parseMarkdown(source);
  if (uniAst instanceof Error) {
    throw uniAst;
  }
  const toHtml = html.toHtml(uniAst);
  if (toHtml instanceof Error) {
    throw toHtml;
  }
  return toHtml;
}

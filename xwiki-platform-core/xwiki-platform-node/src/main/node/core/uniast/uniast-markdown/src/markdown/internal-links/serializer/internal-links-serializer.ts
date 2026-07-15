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
import type { UniAstToMarkdownConverter } from "../../uni-ast-to-markdown-converter";
import type { Link, LinkTarget } from "@xwiki/platform-uniast-api";

/**
 * Serialize internal links and images for a specific backend.
 *
 * @beta
 * @since 18.2.0RC1
 */
export interface InternalLinksSerializer {
  /**
   * Serialize an internal link to Markdown.
   *
   * @param content - the link content
   * @param target - the internal target of the link
   * @param uniAstToMarkdownConverter - the Markdown converter to convert the link content to markdown
   * @returns the serialized internal link string
   */
  serialize(
    content: Link["content"],
    target: Extract<LinkTarget, { type: "internal" }>,
    uniAstToMarkdownConverter: UniAstToMarkdownConverter,
  ): Promise<string>;

  /**
   * Serialize an internal image to markdown.
   *
   * @param target - the internal target of the image
   * @param alt - the alt text of the image
   * @returns the serialized internal image string
   */
  serializeImage(
    target: Extract<LinkTarget, { type: "internal" }>,
    alt?: string,
  ): Promise<string>;
}

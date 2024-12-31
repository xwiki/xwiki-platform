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

import { parseInternalImages } from "./parseInternalImages";
import { parseInternalLinks } from "./parseInternalLinks";
import { MarkdownRenderer } from "@xwiki/cristal-markdown-api";
import { inject, injectable } from "inversify";
import markdownit from "markdown-it";
import type { ModelReferenceParserProvider } from "@xwiki/cristal-model-reference-api";
import type { RemoteURLSerializerProvider } from "@xwiki/cristal-model-remote-url-api";
import type MarkdownIt from "markdown-it";

/**
 * Default implementation based on markdown-it.
 * @since 0.13
 */
@injectable()
export class DefaultMarkdownRenderer implements MarkdownRenderer {
  private md: MarkdownIt;

  constructor(
    @inject<ModelReferenceParserProvider>("ModelReferenceParserProvider")
    private readonly modelReferenceParserProvider: ModelReferenceParserProvider,
    @inject<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
    private readonly remoteURLSerializerProvider: RemoteURLSerializerProvider,
  ) {
    this.md = markdownit();
    const modelReferenceParser = this.modelReferenceParserProvider.get()!;
    const remoteURLSerializer = this.remoteURLSerializerProvider.get()!;
    this.md.core.ruler.before(
      "inline",
      "markdown-internal-links",
      parseInternalLinks(modelReferenceParser, remoteURLSerializer),
    );
    // This declaration needs to happen after markdown-internal-links, otherwise an error is thrown because
    // markdown-internal-links is not found. But, "markdown-internal-images" is executed before
    // "markdown-internal-links"
    this.md.core.ruler.before(
      "markdown-internal-links",
      "markdown-internal-images",
      parseInternalImages(modelReferenceParser, remoteURLSerializer),
    );
  }

  render(markdown: string): string {
    return this.md.render(markdown);
  }
}

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
  parseInternalImages,
  parseInternalLinks,
} from "@xwiki/cristal-markdown-default";
import { ModelReferenceParser } from "@xwiki/cristal-model-reference-api";
import { RemoteURLSerializer } from "@xwiki/cristal-model-remote-url-api";
import MarkdownIt from "markdown-it";
import { Markdown } from "tiptap-markdown";

export default function (
  modelReferenceParser: ModelReferenceParser,
  remoteURLSerializer: RemoteURLSerializer,
) {
  return Markdown.extend({
    onBeforeCreate() {
      const content = this.editor.options.content;
      this.parent?.();
      this.editor.storage.markdown.parser.md =
        this.editor.storage.markdown.parser.md.use((md: MarkdownIt) => {
          md.core.ruler.before(
            "inline",
            "markdown-internal-links",
            parseInternalLinks(modelReferenceParser, remoteURLSerializer),
          );
          // Is it important for the images to be parsed before the links, otherwise the exclamation mark prefixing the
          // image links is just ignored as the rest of the syntax is the same.
          md.core.ruler.before(
            "markdown-internal-links",
            "markdown-internal-images",
            parseInternalImages(modelReferenceParser, remoteURLSerializer),
          );
        });
      // TODO: this is not optimal as the parent is also parsing the content but without the additional plugins.
      // But his is also avoiding quite a lot of code duplication so keeping it like this for now.
      this.editor.options.content =
        this.editor.storage.markdown.parser.parse(content);
    },
  });
}

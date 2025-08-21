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

import { default as Link } from "@tiptap/extension-link";
import type { Mark } from "@tiptap/pm/model";
import type { EntityReference } from "@xwiki/cristal-model-api";
import type { ModelReferenceSerializer } from "@xwiki/cristal-model-reference-api";
import type { RemoteURLParser } from "@xwiki/cristal-model-remote-url-api";

/**
 * Extends the default tiptap extension link with custom Markdown serialization
 * rules to handle internal links.
 */
export default function initLinkExtension(
  serializer: ModelReferenceSerializer,
  parser: RemoteURLParser,
) {
  function parseLink(mark: Mark): EntityReference | undefined {
    try {
      return parser.parse(mark.attrs.href as string);
    } catch {
      return undefined;
    }
  }

  return Link.extend({
    addAttributes() {
      const parents = this.parent?.();
      return {
        ...parents,
        external: {
          default: null,
          renderHTML(attributes) {
            const classes = [];
            // Add the wikiexternallink class on links that are not marked as internal.
            // We do so by checking if the 'internal-link' class is defined on the element.
            if (
              !attributes.class ||
              !attributes.class.split(" ").includes("internal-link")
            ) {
              classes.push("wikiexternallink");
            }
            return {
              class: classes.join(" "),
            };
          },
        },
      };
    },
    addStorage() {
      return {
        markdown: {
          serialize: {
            open(state: unknown, mark: Mark) {
              const reference = parseLink(mark);
              if (reference) {
                return "[[";
              } else {
                return "[";
              }
            },
            close: function (state: unknown, mark: Mark) {
              if (parseLink(mark)) {
                return `|${serializer.serialize(parser.parse(mark.attrs.href))}]]`;
              } else {
                // TODO: replace with a call to the default spec.
                return `](${mark.attrs.href.replace(/[()"]/g, "\\$&")}${
                  mark.attrs.title
                    ? ` "${mark.attrs.title.replace(/"/g, '\\"')}"`
                    : ""
                })`;
              }
            },
            mixable: true,
          },
        },
      };
    },
  });
}

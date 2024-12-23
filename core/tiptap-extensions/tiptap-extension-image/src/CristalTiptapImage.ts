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
import ImageView from "./vue/ImageView.vue";
import Image from "@tiptap/extension-image";
import { VueNodeViewRenderer } from "@tiptap/vue-3";
import { EntityReference } from "@xwiki/cristal-model-api";
import { ModelReferenceSerializer } from "@xwiki/cristal-model-reference-api";
import { RemoteURLParser } from "@xwiki/cristal-model-remote-url-api";
import type { Node } from "@tiptap/pm/model";
import type { MarkdownSerializerState } from "prosemirror-markdown";

function serializeExternal(node: Node, state: MarkdownSerializerState) {
  state.write("![");
  if (node.attrs.alt) {
    state.write(node.attrs.alt);
  }
  state.write("](");
  state.write(node.attrs.src);
  state.write(")");
  state.closeBlock(node);
}

/**
 * We need to override the default image view to be able to easily add widgets (i.e., visual elements that are not
 * part of the persisted DOM) using Vue.
 */
const initTiptapImage = function (
  serializer: ModelReferenceSerializer,
  parser: RemoteURLParser,
) {
  function parseLink(mark: Node): EntityReference | undefined {
    try {
      return parser.parse(mark.attrs.src as string);
    } catch {
      return undefined;
    }
  }

  function serializeInternal(
    node: Node,
    state: MarkdownSerializerState,
    entityRefence: EntityReference,
  ) {
    state.write("![[");
    if (node.attrs.alt) {
      state.write(node.attrs.alt);
      state.write("|");
    }
    state.write(serializer.serialize(entityRefence)!);
    state.write("]]");
    state.closeBlock(node);
  }

  return Image.extend({
    addAttributes() {
      return {
        ...this.parent?.(),
        width: undefined,
        height: undefined,
      };
    },
    addNodeView() {
      return VueNodeViewRenderer(ImageView);
    },
    addStorage() {
      return {
        markdown: {
          serialize(state: MarkdownSerializerState, node: Node) {
            const entityReference = parseLink(node);
            if (entityReference) {
              serializeInternal(node, state, entityReference);
            } else {
              serializeExternal(node, state);
            }
          },
        },
      };
    },
  });
};

export { initTiptapImage };

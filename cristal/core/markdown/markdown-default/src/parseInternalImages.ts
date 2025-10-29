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

import type { ModelReferenceParser } from "@xwiki/cristal-model-reference-api";
import type { RemoteURLSerializer } from "@xwiki/cristal-model-remote-url-api";
import type { RuleInline } from "markdown-it/lib/parser_inline.mjs";
import type { default as StateInline } from "markdown-it/lib/rules_inline/state_inline.mjs";
import type { default as Token } from "markdown-it/lib/token.mjs";

// eslint-disable-next-line max-statements
function parseInternalImageLabel(state: StateInline, start: number) {
  let level, found, marker;
  const max = state.posMax;
  const oldPos = state.pos;
  state.pos = start + 2;
  level = 1;
  while (state.pos < max) {
    marker = state.src.charCodeAt(state.pos);
    if (
      marker === 93 /* ] */ &&
      state.src.charCodeAt(state.pos + 1) === 93 /* ] */
    ) {
      level--;
      if (level === 0) {
        found = true;
        break;
      }
    }
    state.md.inline.skipToken(state);
  }
  let labelEnd = -1;
  if (found) {
    labelEnd = state.pos;
  }
  // restore old state
  state.pos = oldPos;
  return labelEnd;
}

/**
 * @param modelReferenceParser - the model reference parser
 * @param remoteURLSerializer - the remote URL serializer
 * @since 0.20
 * @beta
 */
export function parseInternalImages(
  modelReferenceParser: ModelReferenceParser,
  remoteURLSerializer: RemoteURLSerializer,
): RuleInline {
  // eslint-disable-next-line max-statements
  return (state: StateInline & { linkLevel?: number }, silent: boolean) => {
    if (state.src.charCodeAt(state.pos) !== 33 /* ! */) {
      return false;
    }
    if (state.src.charCodeAt(state.pos + 1) !== 91 /* [ */) {
      return false;
    }
    if (state.src.charCodeAt(state.pos + 2) !== 91 /* [ */) {
      return false;
    }
    const max = state.posMax;
    const labelStart = state.pos + 2;
    const labelEnd = parseInternalImageLabel(state, state.pos);

    const linkContent = state.src.slice(state.pos + 3, labelEnd);
    let ref: string;
    let content: string | undefined;
    const hasLabel = linkContent.includes("|");
    if (hasLabel) {
      const splits = linkContent.split("|");
      ref = splits[splits.length - 1];
      content = splits.slice(0, splits.length - 1).join("|");
    } else {
      ref = linkContent;
    }

    if (!silent) {
      state.pos = labelStart + 1;
      state.posMax = labelEnd - ref.length - 1;

      const image = state.push("image", "img", 0);
      const parsed = modelReferenceParser.parse(ref);
      image.attrs = [
        ["src", remoteURLSerializer.serialize(parsed)!],
        ["alt", ""],
      ];

      const tokens: Token[] = [];
      if (content) {
        state.md.inline.parse(content, state.md, state.env, tokens);
        image.content = content;
      }
      image.children = tokens;
    }
    state.pos = labelEnd + 2; // position right after the link
    state.posMax = max;

    return true;
  };
}

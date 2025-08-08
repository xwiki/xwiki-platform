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

// eslint-disable-next-line max-statements
function parseInternalLinkLabel(
  state: StateInline,
  start: number,
  disableNested: boolean,
) {
  let level, found, marker, prevPos;
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
    prevPos = state.pos;
    state.md.inline.skipToken(state);
    if (marker === 0x5b /* [ */) {
      if (prevPos === state.pos - 1) {
        // increase level if we find text `[`, which is not a part of any token
        level++;
      } else if (disableNested) {
        state.pos = oldPos;
        return -1;
      }
    }
  }
  let labelEnd = -1;
  if (found) {
    labelEnd = state.pos;
  }
  // restore old state
  state.pos = oldPos;
  return labelEnd;
}

export function parseInternalLinks(
  modelReferenceParser: ModelReferenceParser,
  remoteURLSerializer: RemoteURLSerializer,
): RuleInline {
  // eslint-disable-next-line max-statements
  return (state: StateInline & { linkLevel?: number }, silent: boolean) => {
    if (state.src.charCodeAt(state.pos) !== 91 /* [ */) {
      return false;
    }
    if (state.src.charCodeAt(state.pos + 1) !== 91 /* [ */) {
      return false;
    }
    const max = state.posMax;
    const labelStart = state.pos + 1;
    const labelEnd = parseInternalLinkLabel(state, state.pos, false);

    const linkContent = state.src.slice(state.pos + 2, labelEnd);
    let ref: string;
    const hasLabel = linkContent.includes("|");
    if (hasLabel) {
      const splits = linkContent.split("|");
      ref = splits[splits.length - 1];
    } else {
      ref = linkContent;
    }

    if (!silent) {
      state.pos = labelStart + 1;
      state.posMax = labelEnd - ref.length - 1;

      const tokenO = state.push("link_open", "a", 1);
      const parsed = modelReferenceParser.parse(ref);
      tokenO.attrs = [
        ["href", remoteURLSerializer.serialize(parsed)!],
        ["class", "internal-link"],
      ];
      if (state.linkLevel) {
        state.linkLevel++;
      }
      if (hasLabel) {
        state.md.inline.tokenize(state);
      } else {
        const text = state.push("text", "", 0);
        text.content = ref;
      }
      if (state.linkLevel) {
        state.linkLevel--;
      }

      state.push("link_close", "a", -1);
    }
    state.pos = labelEnd + 2; // position right after the link
    state.posMax = max;

    return true;
  };
}

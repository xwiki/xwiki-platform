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

import type { ModelReferenceParser } from "@xwiki/cristal-model-reference-api";
import type { RemoteURLSerializer } from "@xwiki/cristal-model-remote-url-api";
import type { StateCore } from "markdown-it";

const INTERNAL_LINK_REGEX = /\[\[((?<text>[^\]|]+)\|)?(?<reference>[^\]|]+)]]/g;

type InternalLink = { text?: string; reference: string };

/**
 * Takes the substring of content between start and end (and from start until the end if end is empty).
 * Add the extracted substring to the tokens if the string is not empty.
 * @param content - the content to update
 * @param tokens - the tokens to append
 * @param start - the start index
 * @param end - the end index
 */
function appendIfNotEmpty(
  content: string,
  tokens: (string | InternalLink)[],
  start: number,
  end?: number,
) {
  const preString = content.substring(start, end);
  if (preString.length > 0) {
    tokens.push(preString);
  }
  return preString;
}

/**
 * Returns true if at least an internal link was found, false otherwise.
 * @param internalTokens - an array of tokens
 */
function hasLink(internalTokens: (string | InternalLink)[]) {
  return (
    internalTokens.length > 1 ||
    (internalTokens.length == 1 && typeof internalTokens[0] !== "string")
  );
}

/**
 * Parse a string in look for internal links. Returns a array of string or InternalLink types.
 * @param content - a string to parse
 */
// TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
// eslint-disable-next-line max-statements
function parseStringForInternalLinks(
  content: string,
): (string | InternalLink)[] {
  const tokens = [];

  // The offset of the content before the previously matched internal link (or the beginning of the string).
  let offset = 0;

  const matches = content.matchAll(INTERNAL_LINK_REGEX);
  for (const match of matches) {
    const preString = appendIfNotEmpty(content, tokens, offset, match.index);
    const text = match.groups?.text;
    const reference: string = match.groups?.reference || "";
    tokens.push({ text, reference });
    // Shift the offset to the index right after the end of the current internal link.
    offset += match[0].length + preString.length;
  }

  // Put the end of the text, after the last internal link found.
  appendIfNotEmpty(content, tokens, offset);
  return tokens;
}

export function parseInternalLinks(
  modelReferenceParser: ModelReferenceParser,
  remoteURLSerializer: RemoteURLSerializer,
) {
  return function (state: StateCore): void {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    state.tokens.forEach((blockToken: any) => {
      if (blockToken.type == "inline") {
        const internalTokens = parseStringForInternalLinks(blockToken.content);

        // We replace the content of the current block node only if at least a link has been found.
        if (hasLink(internalTokens)) {
          blockToken.content = "";
          // TODO: reduce the number of statements in the following method and reactivate the disabled eslint
          // rule.
          // eslint-disable-next-line max-statements
          blockToken.children = internalTokens.flatMap((v) => {
            if (typeof v == "string") {
              const token = new state.Token("text", "span", 0);
              token.content = v;
              return [token];
            } else {
              const { text, reference } = v;

              const openToken = new state.Token("link_open", "a", 1);
              openToken.attrSet(
                "href",
                remoteURLSerializer.serialize(
                  modelReferenceParser.parse(reference),
                ) ?? "",
              );
              openToken.attrPush(["class", "internal-link"]);
              const contentToken = new state.Token("text", "", 0);
              contentToken.content = text || reference;
              const closeToken = new state.Token("link_close", "a", -1);
              // This is useful for the serializer, who is going to have access to this value to determine
              // how to serialize the link
              closeToken.attrPush(["class", "internal-link"]);
              return [openToken, contentToken, closeToken];
            }
          });
        }
      }
    });
  };
}

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
import { ResourceType } from "@xwiki/platform-rendering-api";
import type { LinkEditionData } from "../services/linkEditor";

// A URL whose scheme is followed by "//" (e.g. "https://xwiki.org"), or an e-mail address URL: the text that can only be
// a URL, unlike a page name holding a colon (e.g. "Note: draft" or "wiki:Page").
const URL_PATTERN = /^(?:[a-z][a-z0-9+.-]*:\/\/|mailto:)/i;

/**
 * The link target that the link editor is prefilled with when a link is created from some text, e.g. the selected text
 * or the text typed in the link suggestion menu. The link editor can then suggest the pages matching the text right
 * away, and the user can link to a page named after the text (e.g. one that doesn't exist yet) without typing it again.
 *
 * @param text - the text the link is created from
 * @returns the link URL when the text is one (e.g. a link to an external site, written as plain text), the reference of
 *   the page named after the text otherwise, relative to the edited page; nothing when the text is empty or spans
 *   several lines (e.g. a selection of several blocks), which can't name a page
 */
function linkTargetPrefill(
  text: string,
): Pick<Partial<LinkEditionData>, "url" | "reference"> {
  const target = text.trim();
  if (target === "" || /[\r\n]/.test(target)) {
    return {};
  }
  if (URL_PATTERN.test(target)) {
    return { url: target };
  }
  return {
    reference: {
      type: ResourceType.DOCUMENT,
      typed: false,
      reference: target,
      parameters: {},
    },
  };
}

export { linkTargetPrefill };

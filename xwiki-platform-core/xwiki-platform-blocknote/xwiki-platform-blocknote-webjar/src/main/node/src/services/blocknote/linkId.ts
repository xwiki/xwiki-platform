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

/**
 * The query string parameter used to carry the synthetic link id inside the link href.
 *
 * A BlockNote link is inline content and, unlike a block, has no id that survives editing. The href
 * is the only part of a link that BlockNote preserves verbatim, so we smuggle a synthetic id in it
 * as this query parameter in order to bind the link to its XWiki metadata (resource reference,
 * parameters, freestanding flag) that cannot be stored in the BlockNote schema.
 */
const LINK_ID_PARAM = "__xwikiLinkId";

// Sentinel base used to parse relative hrefs with the URL constructor. Stripped back off before the
// result is returned.
const RELATIVE_BASE = "http://relative.base/";

/**
 * Parses the given href with the URL constructor, falling back to a sentinel base for relative
 * hrefs (which the URL constructor rejects without a base).
 */
function parseHref(href: string): { url: URL; relative: boolean } {
  try {
    return { url: new URL(href), relative: false };
  } catch {
    return { url: new URL(href, RELATIVE_BASE), relative: true };
  }
}

/**
 * Serializes the given URL back to a string, stripping the sentinel base for relative hrefs.
 */
function serializeHref(url: URL, relative: boolean): string {
  if (!relative) {
    return url.toString();
  }
  const base = RELATIVE_BASE.replace(/\/$/, "");
  const full = url.toString();
  return full.startsWith(base) ? full.slice(base.length) : full;
}

/**
 * Adds (or replaces) the synthetic link id in the given href.
 *
 * @param href - the link href
 * @param id - the synthetic link id to store in the href
 * @returns the href with the synthetic link id in its query string
 */
function injectLinkId(href: string, id: string): string {
  const { url, relative } = parseHref(href);
  url.searchParams.set(LINK_ID_PARAM, id);
  return serializeHref(url, relative);
}

/**
 * Reads the synthetic link id from the given href.
 *
 * @param href - the link href
 * @returns the synthetic link id stored in the href, or undefined if there is none
 */
function extractLinkId(href: string): string | undefined {
  try {
    const { url } = parseHref(href);
    return url.searchParams.get(LINK_ID_PARAM) ?? undefined;
  } catch {
    return undefined;
  }
}

/**
 * Removes the synthetic link id from the given href.
 *
 * @param href - the link href
 * @returns the href without the synthetic link id
 */
function stripLinkId(href: string): string {
  const { url, relative } = parseHref(href);
  url.searchParams.delete(LINK_ID_PARAM);
  return serializeHref(url, relative);
}

export { LINK_ID_PARAM, extractLinkId, injectLinkId, stripLinkId };

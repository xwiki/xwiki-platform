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
import { filterMap } from "@xwiki/platform-fn-utils";
import type { SearchBoxItem } from "@xwiki/platform-link-modal-ui";
import type {
  Link,
  LinkSuggestService,
  LinkType,
} from "@xwiki/platform-link-suggest-api";

/**
 * Search the links of a given type matching a query, as items of the search box. The links are listed as returned by
 * the link suggestion service, which is in charge of matching them against the query.
 *
 * @param linkSuggestService - the link suggestion service, if any
 * @param query - the text to search for
 * @param type - the type of the links to search for
 * @param toValue - converts a link into the value emitted when it is selected, or returns `null` to leave the link out
 *   (e.g. when its reference can't be parsed)
 * @param toQuery - computes the text written in the search field when a link is selected, from its value (e.g. its
 *   reference relative to the document holding the edited link); the reference of the link by default
 * @returns the search box items, rendered from the links themselves, or `false` when there is no link suggestion
 *   service
 */
async function suggestLinks<T>(
  linkSuggestService: LinkSuggestService | undefined,
  query: string,
  type: LinkType,
  toValue: (link: Link) => T | null,
  toQuery?: (value: T) => string,
): Promise<SearchBoxItem<T, Link>[] | false> {
  if (!linkSuggestService) {
    return false;
  }

  let links: Link[];
  try {
    links = await linkSuggestService.getLinks(query, type);
  } catch (e) {
    console.error("Failed to fetch the link suggestions", e);
    return [];
  }

  return filterMap(links, (link) => {
    const value = link.type === type ? toValue(link) : null;
    return value === null
      ? null
      : {
          key: link.url,
          value,
          renderingData: link,
          equivalentQuery: toQuery?.(value) ?? link.reference,
        };
  });
}

export { suggestLinks };

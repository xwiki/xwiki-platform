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

import { Link, LinkType } from "./index";

/**
 * Provide the operation to get links suggestions.
 * @since 0.8
 */
interface LinkSuggestService {
  /**
   * Returns a list of page links from a text query.
   * @param query - a textual search value (e.g., PageName)
   * @param linkType - when provided, only results matching the provided type are returned
   * @param mimetype - when provided, only results matching the provided mimetype are returned. Only used when
   *     linkType is {@link LinkType.ATTACHMENT} (e.g., "image/*" or "application/pdf")
   */
  getLinks(
    query: string,
    linkType?: LinkType,
    mimetype?: string,
  ): Promise<Link[]>;
}

export { type LinkSuggestService };

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
import type { Filter } from "./filter";
import type { SortEntry } from "./sortEntry";
import type { Source } from "./source";

/**
 * The query used to get the live data.
 * @since 18.2.0RC1
 * @beta
 */
export interface Query {
  /**
   * The list of properties whose values we want to fetch.
   */
  properties: string[];
  /**
   * The list of properties to sort on, along with their corresponding sort direction.
   */
  sort: SortEntry[];
  /**
   * Where to take the data from
   */
  source: Source;
  /**
   * The number of entries to fetch (the page size).
   */
  limit: number;
  /**
   * The index where the current page of entries starts
   */
  offset: number;
  /**
   * The filters to apply on the property values.
   */
  filters: Filter[];
}

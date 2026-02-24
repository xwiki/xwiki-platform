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
import type { Data } from "./data";
import type { Query } from "./query";
import type { Source } from "./source";

/**
 * The component that provides the live data entries and their metadata.
 * @since 18.2.0RC1
 * @beta
 */
interface LiveDataSource {
  /**
   * Fetch the entries from a given live data source based on the provided query. The result is a paginated results
   * plus the total number of entries.
   * @param query - the query of apply when fetching the entries
   * @returns a promise with the fetched data
   */
  getEntries(query: Query): Promise<Data>;

  /**
   * Update a single property of an entry
   * @param source - the source description
   * @param entryId - the entry id
   * @param propertyId - the property id
   * @param value - the new value of the property in the entry
   * @returns a promise that completes when the value is updated
   */
  updateEntryProperty(
    source: Source,
    entryId: string,
    propertyId: string,
    value: unknown,
  ): Promise<void>;

  /**
   * Update all the values of a given entry.
   * @param source - the source description
   * @param entryId - the entry id
   * @param values - the new values of the entry
   * @returns a promise that completes when the values are updated
   */
  updateEntry(source: Source, entryId: string, values: unknown): Promise<void>;
}

export type { LiveDataSource };

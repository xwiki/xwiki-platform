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
import { asTypes, typesUrl } from "./fieldUse";
import type { PropertyType } from "./fieldUse";
import type { JsonFetcher } from "./fields";

/**
 * Fetches and parses a JSON document.
 *
 * @param url - the URL to fetch
 * @returns the parsed body
 */
async function fetchJson(url: string): Promise<unknown> {
  const response = await fetch(url, {
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    throw new Error(`Failed to load [${url}]: ${response.status}`);
  }
  return response.json();
}

/**
 * The property types of the source, loaded once per page since they do not depend on the data type.
 */
let types: Promise<PropertyType[]> | null = null;

/**
 * @param fetchJson - fetches and parses the types resource
 * @returns the property types of the source, which say what a field of each type can be used for
 */
function loadTypes(fetchJson: JsonFetcher): Promise<PropertyType[]> {
  types ??= fetchTypes(fetchJson);
  return types;
}

/**
 * @param fetchJson - fetches and parses the types resource
 * @returns the property types of the source
 */
async function fetchTypes(fetchJson: JsonFetcher): Promise<PropertyType[]> {
  try {
    return asTypes(await fetchJson(typesUrl(XWiki.contextPath)));
  } catch (error) {
    // A failure is reported by the picker that asked, and the next load tries again.
    types = null;
    throw error;
  }
}

export { fetchJson, loadTypes };

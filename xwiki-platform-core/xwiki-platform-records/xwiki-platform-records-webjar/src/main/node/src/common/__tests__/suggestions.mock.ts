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
import { typesUrl } from "../fieldUse";
import { vi } from "vitest";
import type { PropertyType } from "../fieldUse";
import type { JsonFetcher, PropertyDescriptor } from "../fields";
import type { Mock } from "vitest";

/**
 * The defaults of the types the picker suggestions tests use, as the types resource reports them.
 */
const TYPES: PropertyType[] = [
  { id: "String", sortable: true, filterable: true, filter: { id: "text" } },
  { id: "Number", sortable: true, filterable: true, filter: { id: "number" } },
  { id: "Password", sortable: false, filterable: false, filter: {} },
];

/**
 * What the properties resource reports for the data type.
 */
const DESCRIPTORS: PropertyDescriptor[] = [
  { id: "doc.title", name: "Title", type: "String" },
  {
    id: "status",
    name: "Status",
    type: "String",
    filter: { id: "text", searchURL: "/xwiki/values?fp={encodedQuery}" },
  },
  { id: "count", name: "Count", type: "Number" },
  { id: "code", name: "Code", type: "Password" },
];

/**
 * Sets up a dialog whose data type is picked, on a wiki whose resources answer with {@link TYPES},
 * {@link DESCRIPTORS}, and two values for any field that suggests some.
 *
 * The caller unstubs the globals after each test.
 *
 * @returns the picker field, and the fetcher answering each resource in the shape it really uses
 */
function givenPickerDialog(): {
  element: Element;
  fetchJson: Mock<JsonFetcher>;
} {
  vi.stubGlobal("XWiki", { contextPath: "/xwiki" });
  document.body.innerHTML = `
    <form>
      <input name="class" value="Book.Class" />
      <input class="picker" />
    </form>`;
  const fetchJson = vi.fn<JsonFetcher>(async (url) => {
    if (url === typesUrl("/xwiki")) {
      return { types: TYPES };
    }
    if (url.includes("/values")) {
      return { propertyValues: [{ value: "Active" }, { value: "On hold" }] };
    }
    return { links: [], properties: DESCRIPTORS };
  });
  return { element: document.querySelector(".picker")!, fetchJson };
}

/**
 * @param options - options, as a picker's suggestions return them
 * @returns their values
 */
function values(options: { value: string }[]): string[] {
  return options.map((option) => option.value);
}

export { givenPickerDialog, values };

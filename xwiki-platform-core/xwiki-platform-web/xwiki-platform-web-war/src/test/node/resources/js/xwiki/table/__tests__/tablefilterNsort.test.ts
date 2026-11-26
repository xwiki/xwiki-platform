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
import TABLE_FILTER_N_SORT_SOURCE from "@webapp/resources/js/xwiki/table/tablefilterNsort.js?raw";
import { afterEach, beforeAll, describe, expect, it } from "vitest";

// The functions the script declares in the global scope.
declare function ts_resortTable(link: Element): void;

/**
 * Get the sort arrows of the sortable table, checking that the image lightbox is disabled for each of them, as the
 * lightbox decides it: from the closest element carrying the data-xwiki-lightbox attribute.
 *
 * @returns the sort arrows
 */
function getSortArrowsExcludedFromLightbox(): Element[] {
  const arrows = Array.from(
    document.querySelectorAll("#sortableTable .sortarrow img"),
  );
  expect(arrows).toHaveLength(2);
  arrows.forEach((arrow) =>
    expect(
      arrow
        .closest("[data-xwiki-lightbox]")
        ?.getAttribute("data-xwiki-lightbox"),
    ).toBe("false"),
  );
  return arrows;
}

describe("Sortable table", () => {
  beforeAll(() => {
    // Evaluate the script in the global scope, which makes the tables sortable when the window is loaded.
    (0, eval)(TABLE_FILTER_N_SORT_SOURCE);
  });

  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("excludes the sort arrows from the image lightbox, before and after sorting", () => {
    document.body.innerHTML = `
      <table id="sortableTable" class="sortable">
        <tr class="sortHeader"><th>Title 1</th><th>Title 2</th></tr>
        <tr><td>b</td><td>1</td></tr>
        <tr><td>a</td><td>2</td></tr>
      </table>
    `;
    window.dispatchEvent(new Event("load"));
    const arrows = getSortArrowsExcludedFromLightbox();

    // Sorting replaces the arrow images.
    ts_resortTable(document.querySelector("#sortableTable a.sortHeader")!);

    expect(
      document.querySelector("#sortableTable tr:nth-child(2) td")?.textContent,
    ).toBe("a");
    expect(getSortArrowsExcludedFromLightbox()[0]).not.toBe(arrows[0]);
  });
});

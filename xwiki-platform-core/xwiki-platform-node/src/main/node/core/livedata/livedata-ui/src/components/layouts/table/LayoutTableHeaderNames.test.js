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

import LayoutTableHeaderNames from "./LayoutTableHeaderNames.vue";
import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";

/**
 * @param editMode - whether the live data is in edit mode
 * @returns a header of a live data with a mandatory "title" property and an optional "name" one
 */
function initWrapper(editMode) {
  return mount(LayoutTableHeaderNames, {
    global: {
      provide: {
        logic: {
          data: { query: { sort: [] } },
          getPropertyDescriptors: () => [
            { id: "title", name: "Title", mandatory: true },
            { id: "name", name: "Name" },
          ],
          isPropertyVisible: () => true,
          isPropertySortable: () => false,
          isSelectionEnabled: () => false,
          isEditMode: () => editMode,
        },
      },
      mocks: { $t: (key) => key },
    },
  });
}

describe("LayoutTableHeaderNames.vue", () => {
  it("Marks the mandatory properties in edit mode", () => {
    const headers = initWrapper(true).findAll("th.draggable-item");

    expect(headers[0].find(".property-mandatory").text()).toBe(
      "livedata.property.mandatory",
    );
    expect(headers[1].find(".property-mandatory").exists()).toBe(false);
  });

  it("Does not mark the mandatory properties in view mode", () => {
    expect(initWrapper(false).find(".property-mandatory").exists()).toBe(false);
  });
});

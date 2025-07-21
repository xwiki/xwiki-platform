/*
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

import { describe, expect, it, vi } from "vitest";
import { init } from "./liveDataSource.js";
import $ from "jquery";
import sinon from "sinon";

const getJSONStub = sinon.stub($, "getJSON");
getJSONStub.returns(Promise.resolve({ count: 0, entries: [] }));

global.XWiki = { contextPath: "http://localhost/" };

describe("liveDataSource.js", () => {

  vi.mock("@/services/require.js", () => {
    return {
      loadById(id) {
        if (id === "jquery") {
          return $;
        }

      },
    };
  });

  describe("getEntries", () => {
    it("is tested", async () => {
      // spyOn($, 'getJSON').and.callFake((entriesURL, params) => {
      //   expect(entriesURL).toMatch(/\/rest\/liveData\/sources\/test\/entries\?timestamp=\d+&sourceParams\.sourceProperty1=property1$/)
      // expect(params).toBe("properties=1&properties=2&properties=3&offset=4&limit=5") return
      // Promise.resolve({ count: 0, entries: [] }) })

      global.XWiki = {};

      const liveDataSource = init($);

      const res = await liveDataSource.getEntries({
        source: {
          id: "test",
          sourceProperty1: "property1",
        },
        properties: [1, 2, 3],
        offset: 4,
        limit: 5,
        filters: [],
        sort: [],
        descending: [],
      });

      expect(res).toEqual({ count: 0, entries: [] });
    });

  });

});

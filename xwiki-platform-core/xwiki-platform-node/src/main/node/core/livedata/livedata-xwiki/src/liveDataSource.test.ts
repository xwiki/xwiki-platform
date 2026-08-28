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

import { XWikiLiveDataSource } from "./XWikiLiveDataSource";
// eslint-disable-next-line import-x/no-named-as-default
import $ from "jquery";
import { stub } from "sinon";
import { afterEach, describe, expect, it, vi } from "vitest";

const getJSONStub = stub($, "getJSON");
// @ts-expect-error leftover from initial javascript implementation
getJSONStub.returns(Promise.resolve({ count: 0, entries: [] }));

global.XWiki = { contextPath: "http://localhost/" };
global.$ = $;

describe("liveDataSource.js", () => {
  vi.mock("@xwiki/platform-livedata-ui", () => {
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

      const liveDataSource = new XWikiLiveDataSource($);

      const res = await liveDataSource.getEntries({
        source: {
          id: "test",
          sourceProperty1: "property1",
        },
        properties: ["1", "2", "3"],
        offset: 4,
        limit: 5,
        filters: [],
        sort: [],
        descending: [],
      });

      expect(res).toEqual({ count: 0, entries: [] });
    });
  });

  describe("getEntry", () => {
    afterEach(() => {
      // Restore the default behavior of the stub, shared by the whole test suite.
      getJSONStub.resetBehavior();
      // @ts-expect-error leftover from initial javascript implementation
      getJSONStub.returns(Promise.resolve({ count: 0, entries: [] }));
    });

    it("requests the given properties and returns the values of the entry", async () => {
      global.XWiki = { contextPath: "http://localhost", currentWiki: "xwiki" };
      getJSONStub.resetBehavior();
      getJSONStub.returns(
        // @ts-expect-error leftover from initial javascript implementation
        Promise.resolve({ values: { name: "entryName", status: "done" } }),
      );

      const liveDataSource = new XWikiLiveDataSource($);

      const values = await liveDataSource.getEntry(
        { id: "test" },
        "MySpace.MyEntry",
        ["name", "status"],
      );

      expect(values).toEqual({ name: "entryName", status: "done" });
      const entryURL = getJSONStub.lastCall.args[0] as string;
      expect(entryURL).toContain(
        "/rest/liveData/sources/test/entries/MySpace.MyEntry?",
      );
      // The properties are appended to the parameters the entry URL already holds.
      expect(entryURL).toContain("namespace=wiki%3Axwiki");
      expect(entryURL).toContain("properties=name&properties=status");
    });

    it("returns undefined when the source does not return any value", async () => {
      global.XWiki = { contextPath: "http://localhost", currentWiki: "xwiki" };
      getJSONStub.resetBehavior();
      // @ts-expect-error leftover from initial javascript implementation
      getJSONStub.returns(Promise.resolve(undefined));

      const liveDataSource = new XWikiLiveDataSource($);

      expect(
        await liveDataSource.getEntry({ id: "test" }, "MySpace.MyEntry", []),
      ).toBeUndefined();
    });
  });
});

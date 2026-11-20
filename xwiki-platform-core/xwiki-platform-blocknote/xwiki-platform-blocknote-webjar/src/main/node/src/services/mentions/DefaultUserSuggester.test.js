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
import { DefaultUserSuggester } from "./DefaultUserSuggester";
import { beforeEach, describe, expect, it, vi } from "vitest";

function user(name) {
  return { value: `XWiki.${name}`, label: name };
}

/**
 * @param userScope - the user scope of the wiki, left out when the wiki module isn't available
 * @returns the editor instance the suggester reads the user scope from
 */
function instance(userScope) {
  return { options: { features: { mentions: { userScope } } } };
}

/**
 * Serves the given suggestions to the successive requests, so that the local and the global users can differ.
 */
function mockSuggestionsPerCall(...suggestions) {
  mockSuggestions([]);
  suggestions.forEach((result) =>
    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => result,
    }),
  );
}

function fetchedWikis() {
  return fetch.mock.calls.map((call) =>
    new URLSearchParams(String(call[0]).split("?")[1]).get("wiki"),
  );
}

function mockSuggestions(suggestions) {
  vi.stubGlobal("XWiki", {
    currentDocument: {
      getURL: (action, parameters) => `/bin/${action}/Space/Page?${parameters}`,
    },
  });
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({ ok: true, json: async () => suggestions }),
  );
}

describe("DefaultUserSuggester", () => {
  beforeEach(() => {
    vi.unstubAllGlobals();
    // The tests below trigger the failures they assert on, so their logs are expected.
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  it("maps the suggestions of the user suggestion template", async () => {
    mockSuggestions([
      {
        value: "XWiki.Admin",
        label: "Administrator",
        hint: "admin@example.com",
        icon: { url: "/avatar.png" },
        url: "/bin/view/XWiki/Admin",
      },
    ]);

    expect(
      await new DefaultUserSuggester().suggestUsers(instance(), "admin"),
    ).toEqual([
      {
        reference: "XWiki.Admin",
        label: "Administrator",
        hint: "admin@example.com",
        icon: { url: "/avatar.png" },
      },
    ]);
  });

  it("queries the user suggestion template for users only", async () => {
    mockSuggestions([]);

    await new DefaultUserSuggester().suggestUsers(instance(), "admin");

    expect(fetch).toHaveBeenCalledWith(
      "/bin/get/Space/Page?xpage=uorgsuggest&media=json&uorg=user&input=admin&limit=6",
    );
  });

  describe("the user scope of the wiki", () => {
    it("queries only the local users when the wiki has its own users", async () => {
      mockSuggestions([user("John")]);

      await new DefaultUserSuggester().suggestUsers(
        instance("LOCAL_ONLY"),
        "joh",
      );

      expect(fetchedWikis()).toEqual([null]);
    });

    it("queries only the global users when the wiki has no users of its own", async () => {
      mockSuggestions([user("John")]);

      await new DefaultUserSuggester().suggestUsers(
        instance("GLOBAL_ONLY"),
        "joh",
      );

      expect(fetchedWikis()).toEqual(["global"]);
    });

    it("completes the local users with the global ones, sorted by name", async () => {
      mockSuggestionsPerCall([user("Carol"), user("Alice")], [user("Bob")]);

      const users = await new DefaultUserSuggester().suggestUsers(
        instance("LOCAL_AND_GLOBAL"),
        "",
      );

      expect(fetchedWikis()).toEqual([null, "global"]);
      expect(users.map((it) => it.label)).toEqual(["Alice", "Bob", "Carol"]);
    });

    it("suggests a user once when the local and the global wiki are the same", async () => {
      // What the main wiki returns when its scope is LOCAL_AND_GLOBAL: both requests hit the same wiki.
      mockSuggestionsPerCall(
        [user("Alice"), user("Bob")],
        [user("Alice"), user("Bob")],
      );

      const users = await new DefaultUserSuggester().suggestUsers(
        instance("LOCAL_AND_GLOBAL"),
        "",
      );

      expect(users.map((it) => it.label)).toEqual(["Alice", "Bob"]);
    });

    it("doesn't query the global users when the local ones already fill the menu", async () => {
      mockSuggestions(["Al", "Bo", "Cy", "Di", "Ed", "Fay"].map(user));

      await new DefaultUserSuggester().suggestUsers(
        instance("LOCAL_AND_GLOBAL"),
        "",
      );

      expect(fetchedWikis()).toEqual([null]);
    });
  });

  it("keeps the local users when the global ones can't be fetched", async () => {
    mockSuggestionsPerCall([user("Carol")]);
    fetch.mockRejectedValueOnce(new Error("Connection reset"));

    const users = await new DefaultUserSuggester().suggestUsers(
      instance("LOCAL_AND_GLOBAL"),
      "",
    );

    expect(users.map((it) => it.label)).toEqual(["Carol"]);
  });

  it("suggests no user when the request is rejected", async () => {
    mockSuggestions([]);
    fetch.mockResolvedValue({ ok: false, status: 401 });

    expect(
      await new DefaultUserSuggester().suggestUsers(instance(), "admin"),
    ).toEqual([]);
  });

  it("drops the hint of a user that has none", async () => {
    mockSuggestions([{ value: "XWiki.Admin", label: "Administrator" }]);

    expect(
      (await new DefaultUserSuggester().suggestUsers(instance(), "admin"))[0]
        .hint,
    ).toBe(undefined);
  });
});

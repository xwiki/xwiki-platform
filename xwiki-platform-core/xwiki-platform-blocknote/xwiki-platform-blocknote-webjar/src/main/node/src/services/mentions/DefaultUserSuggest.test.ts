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
import { DefaultUserSuggest } from "./DefaultUserSuggest";
import { beforeEach, describe, expect, it, vi } from "vitest";

function user(name: string) {
  return { value: `XWiki.${name}`, label: name };
}

/**
 * Serves the given suggestions to the successive requests, so that the local and the global users can differ.
 */
function mockSuggestionsPerCall(...suggestions: unknown[][]): void {
  mockSuggestions([]);
  suggestions.forEach((result) =>
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      json: async () => result,
    }),
  );
}

function fetchedWikis(): (string | null)[] {
  return (fetch as ReturnType<typeof vi.fn>).mock.calls.map((call) =>
    new URLSearchParams(String(call[0]).split("?")[1]).get("wiki"),
  );
}

function mockSuggestions(suggestions: unknown[]): void {
  vi.stubGlobal("XWiki", {
    currentDocument: {
      getURL: (action: string, parameters: URLSearchParams) =>
        `/bin/${action}/Space/Page?${parameters}`,
    },
  });
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({ json: async () => suggestions }),
  );
}

describe("DefaultUserSuggest", () => {
  beforeEach(() => {
    vi.unstubAllGlobals();
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

    expect(await new DefaultUserSuggest().getUsers("admin")).toEqual([
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

    await new DefaultUserSuggest().getUsers("admin");

    expect(fetch).toHaveBeenCalledWith(
      "/bin/get/Space/Page?xpage=uorgsuggest&media=json&uorg=user&input=admin&limit=6",
    );
  });

  it("queries only the local users when the wiki has its own users", async () => {
    mockSuggestions([user("John")]);

    await new DefaultUserSuggest().getUsers("joh", "LOCAL_ONLY");

    expect(fetchedWikis()).toEqual([null]);
  });

  it("queries only the global users when the wiki has no users of its own", async () => {
    mockSuggestions([user("John")]);

    await new DefaultUserSuggest().getUsers("joh", "GLOBAL_ONLY");

    expect(fetchedWikis()).toEqual(["global"]);
  });

  it("completes the local users with the global ones, sorted by name", async () => {
    mockSuggestionsPerCall([user("Carol"), user("Alice")], [user("Bob")]);

    const users = await new DefaultUserSuggest().getUsers(
      "",
      "LOCAL_AND_GLOBAL",
    );

    expect(fetchedWikis()).toEqual([null, "global"]);
    expect(users.map((it) => it.label)).toEqual(["Alice", "Bob", "Carol"]);
  });

  it("doesn't query the global users when the local ones already fill the menu", async () => {
    mockSuggestions(["Al", "Bo", "Cy", "Di", "Ed", "Fay"].map(user));

    await new DefaultUserSuggest().getUsers("", "LOCAL_AND_GLOBAL");

    expect(fetchedWikis()).toEqual([null]);
  });

  it("drops the hint of a user that has none", async () => {
    mockSuggestions([{ value: "XWiki.Admin", label: "Administrator" }]);

    expect((await new DefaultUserSuggest().getUsers("admin"))[0].hint).toBe(
      undefined,
    );
  });
});

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

import { ComponentInit } from "../componentsInit";
import { userSuggesterName } from "@xwiki/platform-user-suggest-api";
import { Container } from "inversify";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { UserSuggester } from "@xwiki/platform-user-suggest-api";

/**
 * @param authorizationHeader - the credentials of the current user, none when they are a guest
 * @param basePageURL - the URL the wiki serves its pages from, none when it serves them from its base URL
 * @returns the user suggester resolved from a container holding just what it needs
 */
function userSuggester({
  authorizationHeader,
  basePageURL,
}: { authorizationHeader?: string; basePageURL?: string } = {}): UserSuggester {
  const container = new Container();
  container.bind("CristalApp").toConstantValue({
    getWikiConfig: () => ({
      baseURL: "http://localhost/xwiki",
      basePageURL,
    }),
  });
  container.bind("AuthenticationManagerProvider").toConstantValue({
    get: () => ({ getAuthorizationHeader: async () => authorizationHeader }),
  });
  new ComponentInit(container);
  return container.get<UserSuggester>(userSuggesterName, { name: "XWiki" });
}

function mockSuggestions(suggestions: unknown[]): void {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({ ok: true, json: async () => suggestions }),
  );
}

/**
 * @returns the URL the suggestion request was sent to
 */
function requestURL(): URL {
  return new URL(vi.mocked(fetch).mock.calls[0][0] as string);
}

describe("XWikiUserSuggester", () => {
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

    expect(await userSuggester().suggestUsers("admin")).toEqual([
      {
        reference: "XWiki.Admin",
        label: "Administrator",
        hint: "admin@example.com",
        icon: { url: "/avatar.png" },
      },
    ]);
  });

  it("drops the hint of a user that has none", async () => {
    mockSuggestions([{ value: "XWiki.Admin", label: "Administrator" }]);

    expect((await userSuggester().suggestUsers("admin"))[0].hint).toBe(
      undefined,
    );
  });

  it("asks the wiki to apply its own user scope", async () => {
    mockSuggestions([]);

    await userSuggester().suggestUsers("admin");

    const url = requestURL();
    expect(url.pathname).toBe("/xwiki/bin/get/Main/WebHome");
    expect(Object.fromEntries(url.searchParams)).toEqual({
      xpage: "uorgsuggest",
      media: "json",
      uorg: "user",
      wiki: "auto",
      input: "admin",
      limit: "6",
    });
  });

  it("queries the wiki the application talks to", async () => {
    mockSuggestions([]);

    // A subwiki reached through a path serves its pages from its own URL, not from the one of the main wiki.
    await userSuggester({
      basePageURL: "http://localhost/xwiki/wiki/subwiki/",
    }).suggestUsers("admin");

    expect(requestURL().pathname).toBe("/xwiki/wiki/subwiki/get/Main/WebHome");
  });

  it("asks for as many users as the caller wants", async () => {
    mockSuggestions([]);

    await userSuggester().suggestUsers("admin", { limit: 20 });

    expect(requestURL().searchParams.get("limit")).toBe("20");
  });

  it("sends the credentials of the current user", async () => {
    mockSuggestions([]);

    await userSuggester({ authorizationHeader: "Bearer token" }).suggestUsers(
      "admin",
    );

    expect(vi.mocked(fetch).mock.calls[0][1]).toEqual({
      headers: { Authorization: "Bearer token" },
    });
  });

  it("sends no credentials for a guest", async () => {
    mockSuggestions([]);

    await userSuggester().suggestUsers("admin");

    expect(vi.mocked(fetch).mock.calls[0][1]).toEqual({ headers: {} });
  });

  it("fails when the request is rejected", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({ ok: false, status: 401 }),
    );

    await expect(userSuggester().suggestUsers("admin")).rejects.toThrow(
      "Unexpected response status [401].",
    );
  });
});

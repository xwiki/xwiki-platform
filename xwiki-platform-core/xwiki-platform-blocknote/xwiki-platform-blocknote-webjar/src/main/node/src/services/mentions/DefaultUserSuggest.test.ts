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

  it("drops the hint of a user that has none", async () => {
    mockSuggestions([{ value: "XWiki.Admin", label: "Administrator" }]);

    expect((await new DefaultUserSuggest().getUsers("admin"))[0].hint).toBe(
      undefined,
    );
  });
});

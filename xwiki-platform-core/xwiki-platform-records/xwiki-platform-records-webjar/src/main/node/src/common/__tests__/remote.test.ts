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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { JsonFetcher } from "../fields";

/**
 * The module under test, imported again for each test, since it caches the property types per page.
 */
let remote: typeof import("../remote");

beforeEach(async () => {
  vi.stubGlobal("XWiki", { contextPath: "/xwiki" });
  vi.resetModules();
  remote = await import("../remote");
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("fetchJson", () => {
  it("asks for JSON and returns the parsed body", async () => {
    const fetch = vi.fn().mockResolvedValue(Response.json({ ok: 1 }));
    vi.stubGlobal("fetch", fetch);

    await expect(remote.fetchJson("/some/url")).resolves.toEqual({ ok: 1 });
    expect(fetch).toHaveBeenCalledWith("/some/url", {
      headers: { Accept: "application/json" },
    });
  });

  it("fails with the URL and the status when the response is an error", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response(null, { status: 404 })),
    );

    await expect(remote.fetchJson("/missing")).rejects.toThrow(
      "Failed to load [/missing]: 404",
    );
  });
});

describe("loadTypes", () => {
  const TYPES = [{ id: "Number", sortable: true, filterable: true }];

  it("loads the property types once per page", async () => {
    const fetchJson = vi.fn<JsonFetcher>().mockResolvedValue({ types: TYPES });

    await expect(remote.loadTypes(fetchJson)).resolves.toEqual(TYPES);
    await expect(remote.loadTypes(fetchJson)).resolves.toEqual(TYPES);
    expect(fetchJson).toHaveBeenCalledTimes(1);
    expect(fetchJson).toHaveBeenCalledWith(typesUrl("/xwiki"));
  });

  it("tries again on the next load after a failure", async () => {
    const fetchJson = vi
      .fn<JsonFetcher>()
      .mockRejectedValueOnce(new Error("HTTP 500"))
      .mockResolvedValue({ types: TYPES });

    await expect(remote.loadTypes(fetchJson)).rejects.toThrow("HTTP 500");
    await expect(remote.loadTypes(fetchJson)).resolves.toEqual(TYPES);
    expect(fetchJson).toHaveBeenCalledTimes(2);
  });
});

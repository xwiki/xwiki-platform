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
import { translatorFactory } from "../index";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const TARGET = "https://example.com/translations";

function mockFetch(translations: { key: string; rawSource: string }[]) {
  return vi.fn().mockResolvedValue({
    ok: true,
    json: () => ({ translations }),
  });
}

beforeEach(() => {
  vi.stubGlobal("fetch", mockFetch([]));
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("translatorFactory", () => {
  it("fetches and returns translations for array query", async () => {
    vi.stubGlobal("fetch", mockFetch([{ key: "a.key", rawSource: "Hello" }]));

    const translator = translatorFactory(TARGET);
    const result = await translator.resolve(["a.key"]);

    expect(result).toMatchObject({ "a.key": "Hello" });
    expect(fetch).toHaveBeenCalledWith(
      `${TARGET}?key=a.key&locale=en`,
      expect.objectContaining({ method: "GET" }),
    );
  });

  it("fetches with prefix for object query", async () => {
    vi.stubGlobal("fetch", mockFetch([{ key: "ns.hello", rawSource: "Hi" }]));

    const translator = translatorFactory(TARGET);
    await translator.resolve({ prefix: "ns.", keys: ["hello"] });

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/translations?prefix=ns.&key=hello&locale=en",
      expect.anything(),
    );
  });

  it("caches resolved keys and skips fetch on second call", async () => {
    vi.stubGlobal("fetch", mockFetch([{ key: "a.key", rawSource: "Hello" }]));

    const translator = translatorFactory(TARGET);
    await translator.resolve(["a.key"]);
    await translator.resolve(["a.key"]); // should hit cache

    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it("skips already-cached keys in a mixed query", async () => {
    const fetchMock = mockFetch([{ key: "a.key", rawSource: "Hello" }]);
    vi.stubGlobal("fetch", fetchMock);

    const translator = translatorFactory(TARGET);
    await translator.resolve(["a.key"]);

    // Second call: a.key cached, b.key is new
    fetchMock.mockResolvedValue({
      ok: true,
      json: () => ({
        translations: [{ key: "b.key", rawSource: "World" }],
      }),
    });

    const result = await translator.resolve(["a.key", "b.key"]);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/translations?key=a.key&locale=en",
      expect.anything(),
    );

    expect(result).toMatchObject({ "a.key": "Hello", "b.key": "World" });
  });

  it("deduplicates inflight requests for the same query", async () => {
    vi.stubGlobal("fetch", mockFetch([{ key: "a.key", rawSource: "Hello" }]));

    const translator = translatorFactory(TARGET);
    const [r1, r2] = await Promise.all([
      translator.resolve(["a.key"]),
      translator.resolve(["a.key"]),
    ]);

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(r1).toBe(r2); // same promise reference
  });

  it("skips fetch entirely when all keys are cached (no 'key' param)", async () => {
    vi.stubGlobal("fetch", mockFetch([{ key: "a.key", rawSource: "Hello" }]));

    const translator = translatorFactory(TARGET);
    await translator.resolve(["a.key"]);

    expect(fetch).toHaveBeenCalledTimes(1);

    await translator.resolve(["a.key"]);

    // Still one time after the second call since already resolved.
    expect(fetch).toHaveBeenCalledTimes(1);
  });
});

describe("translatorFactory, on failure", () => {
  it("rejects and logs on fetch failure", async () => {
    const error = new Error("Network error");
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(error));
    const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    const translator = translatorFactory(TARGET);

    await expect(translator.resolve(["a.key"])).rejects.toThrow(
      "Network error",
    );
    expect(consoleSpy).toHaveBeenCalledWith(
      'Failed to retrieve the translations for query [["a.key"]]',
      error,
    );

    consoleSpy.mockRestore();
  });

  it("rejects when the response is not ok", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 403,
        json: () => ({}),
      }),
    );
    const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    const translator = translatorFactory(TARGET);

    await expect(translator.resolve(["a.key"])).rejects.toThrow(
      `Unexpected response status [403] from [${TARGET}]`,
    );

    consoleSpy.mockRestore();
  });

  it("retries an identical query after a failure", async () => {
    const fetchMock = vi.fn().mockRejectedValue(new Error("Network error"));
    vi.stubGlobal("fetch", fetchMock);
    const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    const translator = translatorFactory(TARGET);
    await expect(translator.resolve(["a.key"])).rejects.toThrow();

    fetchMock.mockResolvedValue({
      ok: true,
      json: () => ({ translations: [{ key: "a.key", rawSource: "Hello" }] }),
    });

    await expect(translator.resolve(["a.key"])).resolves.toMatchObject({
      "a.key": "Hello",
    });
    expect(fetchMock).toHaveBeenCalledTimes(2);

    consoleSpy.mockRestore();
  });
});

describe("translatorFactory, object queries", () => {
  it("handles object query without prefix", async () => {
    vi.stubGlobal("fetch", mockFetch([{ key: "hello", rawSource: "Hi" }]));

    const translator = translatorFactory(TARGET);
    const result = await translator.resolve({ keys: ["hello"] });

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/translations?key=hello&locale=en",
      expect.anything(),
    );
    expect(result).toMatchObject({ hello: "Hi" });
  });

  it("handles object query with a locale", async () => {
    vi.stubGlobal("fetch", mockFetch([{ key: "hello", rawSource: "Hi" }]));

    const translator = translatorFactory(TARGET);
    const result = await translator.resolve({
      keys: ["world"],
      prefix: "hello.",
      locale: "fr",
    });

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/translations?prefix=hello.&key=world&locale=fr",
      expect.anything(),
    );
    expect(result).toMatchObject({ hello: "Hi" });
  });
});

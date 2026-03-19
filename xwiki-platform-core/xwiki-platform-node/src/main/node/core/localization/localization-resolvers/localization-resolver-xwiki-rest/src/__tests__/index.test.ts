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
    json: vi.fn().mockResolvedValue({ translations }),
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
      `${TARGET}?key=a.key`,
      expect.objectContaining({ method: "GET" }),
    );
  });

  it("fetches with prefix for object query", async () => {
    vi.stubGlobal("fetch", mockFetch([{ key: "ns.hello", rawSource: "Hi" }]));

    const translator = translatorFactory(TARGET);
    await translator.resolve({ prefix: "ns.", keys: ["hello"] });

    const calledUrl = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][0];
    expect(calledUrl).toContain("prefix=ns.");
    expect(calledUrl).toContain("key=hello");
  });

  it("caches resolved keys and skips fetch on second call", async () => {
    const fetchMock = mockFetch([{ key: "a.key", rawSource: "Hello" }]);
    vi.stubGlobal("fetch", fetchMock);

    const translator = translatorFactory(TARGET);
    await translator.resolve(["a.key"]);
    await translator.resolve(["a.key"]); // should hit cache

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("skips already-cached keys in a mixed query", async () => {
    const fetchMock = mockFetch([{ key: "a.key", rawSource: "Hello" }]);
    vi.stubGlobal("fetch", fetchMock);

    const translator = translatorFactory(TARGET);
    await translator.resolve(["a.key"]);

    // Second call: a.key cached, b.key is new
    fetchMock.mockResolvedValue({
      json: vi.fn().mockResolvedValue({
        translations: [{ key: "b.key", rawSource: "World" }],
      }),
    });

    const result = await translator.resolve(["a.key", "b.key"]);

    const calledUrl = fetchMock.mock.calls[1][0] as string;
    expect(calledUrl).not.toContain("a.key");
    expect(calledUrl).toContain("b.key");
    expect(result).toMatchObject({ "a.key": "Hello", "b.key": "World" });
  });

  it("deduplicates inflight requests for the same query", async () => {
    const fetchMock = mockFetch([{ key: "a.key", rawSource: "Hello" }]);
    vi.stubGlobal("fetch", fetchMock);

    const translator = translatorFactory(TARGET);
    const [r1, r2] = await Promise.all([
      translator.resolve(["a.key"]),
      translator.resolve(["a.key"]),
    ]);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(r1).toBe(r2); // same promise reference
  });

  it("returns cache (empty) and logs on fetch failure", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(new Error("Network error")),
    );
    const consoleSpy = vi.spyOn(console, "log").mockImplementation(() => {});

    const translator = translatorFactory(TARGET);
    const result = await translator.resolve(["a.key"]);

    expect(result).toEqual({});
    expect(consoleSpy).toHaveBeenCalled();

    consoleSpy.mockRestore();
  });

  it("skips fetch entirely when all keys are cached (no 'key' param)", async () => {
    const fetchMock = mockFetch([{ key: "a.key", rawSource: "Hello" }]);
    vi.stubGlobal("fetch", fetchMock);

    const translator = translatorFactory(TARGET);
    await translator.resolve(["a.key"]);
    fetchMock.mockClear();

    await translator.resolve(["a.key"]);

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("handles object query without prefix", async () => {
    const fetchMock = mockFetch([{ key: "hello", rawSource: "Hi" }]);
    vi.stubGlobal("fetch", fetchMock);

    const translator = translatorFactory(TARGET);
    const result = await translator.resolve({ keys: ["hello"] });

    const calledUrl = fetchMock.mock.calls[0][0] as string;
    expect(calledUrl).not.toContain("prefix");
    expect(result).toMatchObject({ hello: "Hi" });
  });
});

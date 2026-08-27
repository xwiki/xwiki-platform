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
import { initialize } from "../index";
import { describe, expect, it, vi } from "vitest";
import type { Query, Translator } from "@xwiki/platform-localization-api";

function makeTranslator(resolved: Record<string, string>): Translator {
  return {
    resolve: vi.fn(async (query: Query) => {
      const keys = Array.isArray(query)
        ? query
        : query.keys.map((k) => (query.prefix ?? "") + k);
      return Object.fromEntries(
        keys.filter((k) => k in resolved).map((k) => [k, resolved[k]]),
      );
    }),
  };
}

describe("initialize", () => {
  it("resolves keys from a single translator", async () => {
    const translator = makeTranslator({ "a.key": "Hello" });
    const resolver = initialize(translator);

    const result = await resolver.resolve(["a.key"]);

    expect(result.translations).toEqual({ "a.key": "Hello" });
    expect(result.missed).toEqual([]);
  });

  it("reports missed keys when no translator resolves them", async () => {
    const translator = makeTranslator({});
    const resolver = initialize(translator);

    const result = await resolver.resolve(["missing.key"]);

    expect(result.translations).toEqual({});
    expect(result.missed).toEqual(["missing.key"]);
  });

  it("chains translators and stops early when all keys resolved", async () => {
    const t1 = makeTranslator({ "a.key": "From T1" });
    const t2 = makeTranslator({ "b.key": "From T2" });
    const resolver = initialize(t1, t2);

    const result = await resolver.resolve(["a.key", "b.key"]);

    expect(result.translations).toEqual({
      "a.key": "From T1",
      "b.key": "From T2",
    });
    expect(result.missed).toEqual([]);
  });

  it("does not call subsequent translators once all keys are resolved", async () => {
    const t1 = makeTranslator({ "a.key": "Hello", "b.key": "World" });
    const t2 = makeTranslator({ "b.key": "Never" });
    const resolver = initialize(t1, t2);

    await resolver.resolve(["a.key", "b.key"]);

    expect(t2.resolve).not.toHaveBeenCalled();
  });

  it("first translator wins for duplicate keys", async () => {
    const t1 = makeTranslator({ "a.key": "From T1" });
    const t2 = makeTranslator({ "a.key": "From T2" });
    const resolver = initialize(t1, t2);

    const result = await resolver.resolve(["a.key"]);

    expect(result.translations["a.key"]).toBe("From T1");
  });

  it("handles object query with prefix", async () => {
    const translator = makeTranslator({ "ns.hello": "Hi", "ns.bye": "Bye" });
    const resolver = initialize(translator);

    const result = await resolver.resolve({
      prefix: "ns.",
      keys: ["hello", "bye"],
    });

    expect(result.translations).toEqual({ "ns.hello": "Hi", "ns.bye": "Bye" });
    expect(result.missed).toEqual([]);
  });

  it("handles object query without prefix", async () => {
    const translator = makeTranslator({ hello: "Hi" });
    const resolver = initialize(translator);

    const result = await resolver.resolve({ keys: ["hello"] });

    expect(result.translations).toEqual({ hello: "Hi" });
    expect(result.missed).toEqual([]);
  });

  it("resolves with no translators, all keys missed", async () => {
    const resolver = initialize();

    const result = await resolver.resolve(["a.key"]);

    expect(result.translations).toEqual({});
    expect(result.missed).toEqual(["a.key"]);
  });
});

describe("initialize, locales", () => {
  it("forwards the locale of the query to the translators", async () => {
    const translator = makeTranslator({ "ns.hello": "Salut" });
    const resolver = initialize(translator);

    await resolver.resolve({
      prefix: "ns.",
      keys: ["hello"],
      locale: "fr",
    });

    expect(translator.resolve).toHaveBeenCalledWith({
      keys: ["ns.hello"],
      locale: "fr",
    });
  });

  it("forwards an undefined locale for an array query", async () => {
    const translator = makeTranslator({ "a.key": "Hello" });
    const resolver = initialize(translator);

    await resolver.resolve(["a.key"]);

    expect(translator.resolve).toHaveBeenCalledWith({
      keys: ["a.key"],
      locale: undefined,
    });
  });

  it("keeps forwarding the locale to the next translator of the chain", async () => {
    const t1 = makeTranslator({ "a.key": "From T1" });
    const t2 = makeTranslator({ "b.key": "From T2" });
    const resolver = initialize(t1, t2);

    await resolver.resolve({ keys: ["a.key", "b.key"], locale: "fr" });

    expect(t2.resolve).toHaveBeenCalledWith({
      keys: ["b.key"],
      locale: "fr",
    });
  });
});

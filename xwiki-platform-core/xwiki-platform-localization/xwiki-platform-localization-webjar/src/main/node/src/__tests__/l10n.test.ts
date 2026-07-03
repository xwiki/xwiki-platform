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
import { transformTranslation } from "../internal/transformTranslation";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { DefineWithLoad } from "../l10n";
import type { Query } from "@xwiki/platform-localization-api";

let capturedLoader: ReturnType<DefineWithLoad>;

vi.stubGlobal("define", (_name: string, factory: DefineWithLoad) => {
  capturedLoader = factory();
});

const mockResolve = vi.fn();

vi.mock("../index", () => ({
  resolver: { resolve: mockResolve },
}));

await import("../l10n");

/**
 * Mock the query and the translations when requesting the l10n requirejs module.
 * @param query - the query
 * @param translations - the returned translations
 */
function mockModuleLoad(
  query: Query,
  translations: Record<string, string>,
): Promise<Record<string, string> & { get: typeof transformTranslation }> {
  return new Promise((resolve) => {
    mockResolve.mockResolvedValueOnce({ translations });
    capturedLoader.load(
      "xwiki-l10n!somemodule",
      (_names, cb) => cb(query),
      resolve as () => Record<string, string>,
    );
  });
}

describe("l10n module", () => {
  beforeEach(() => mockResolve.mockReset());

  it("return keys", async () => {
    const mod = await mockModuleLoad(["a.b", "a.c"], {
      "a.b": "AB",
      "a.c": "AC",
    });

    expect(mod["a.b"]).toBe("AB");
    expect(mod["a.c"]).toBe("AC");
  });

  it("don't include keys not part of the query", async () => {
    const mod = await mockModuleLoad(["a.b"], {
      "a.b": "AB",
      "a.c": "AC",
    });

    expect(mod["a.c"]).toBeUndefined();
  });

  it("remove prefix from returned keys", async () => {
    const mod = await mockModuleLoad(
      { prefix: "ns.", keys: ["foo", "bar"] },
      { "ns.foo": "Foo", "ns.bar": "Bar" },
    );

    expect(mod["foo"]).toBe("Foo");
    expect(mod["bar"]).toBe("Bar");
  });

  it("undefined prefix is the empty string", async () => {
    const mod = await mockModuleLoad({ keys: ["x"] }, { x: "X" });
    expect(mod["x"]).toBe("X");
  });

  it("logs an error when resolver rejects", async () => {
    const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    mockResolve.mockRejectedValueOnce(new Error("network failure"));

    await new Promise<void>((resolve) => {
      capturedLoader.load(
        "ignored",
        (_names, cb) => cb(["k"]),
        () => {},
      );
      setTimeout(resolve, 50);
    });

    expect(consoleSpy).toHaveBeenCalledWith(
      "An issue occurred during the resolution of localization query k",
      expect.any(Error),
    );
    consoleSpy.mockRestore();
  });
});

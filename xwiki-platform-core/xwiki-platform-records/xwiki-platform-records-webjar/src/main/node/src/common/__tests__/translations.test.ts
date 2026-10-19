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
import { METADATA_COLUMNS } from "../fields";
import {
  TRANSLATION_KEYS_MODULE,
  defineTranslationKeys,
  metadataLabels,
  setMessages,
  translate,
  withProblem,
} from "../translations";
import { afterEach, describe, expect, it, vi } from "vitest";

/**
 * Hands the module messages that echo the key and its arguments, so that a test can tell what was asked for.
 */
function givenEchoMessages(): void {
  setMessages({
    get: (key, ...args) => [`[${key}]`, ...args].join(" "),
  });
}

afterEach(() => {
  setMessages({ get: () => null });
  vi.unstubAllGlobals();
});

describe("translate", () => {
  it("returns the translated message, with its parameters", () => {
    givenEchoMessages();
    expect(translate("picker.loadFailed", "Book")).toBe(
      "[picker.loadFailed] Book",
    );
  });

  it("returns the key when the bundle does not have it, so a missing translation still shows up", () => {
    expect(translate("picker.retry")).toBe("picker.retry");
  });
});

describe("metadataLabels", () => {
  it("translates the label of every page metadata column", () => {
    givenEchoMessages();
    const labels = metadataLabels();
    expect(Object.keys(labels)).toEqual(METADATA_COLUMNS);
    expect(labels["doc.title"]).toBe("[picker.metadata.doc.title]");
  });
});

describe("withProblem", () => {
  const option = { value: "code", label: "Code", optgroup: "fields" };

  it("leaves an option without a problem as it is", () => {
    expect(withProblem(option, null)).toBe(option);
  });

  it("attaches the translated problem to a copy of the option", () => {
    givenEchoMessages();
    expect(
      withProblem(option, {
        key: "picker.problem.notSortable",
        args: ["Code"],
      }),
    ).toEqual({ ...option, problem: "[picker.problem.notSortable] Code" });
  });
});

describe("defineTranslationKeys", () => {
  it("declares the keys module the l10n loader resolves, with the bundle prefix", () => {
    const define = vi.fn();
    vi.stubGlobal("define", define);

    defineTranslationKeys();

    expect(define).toHaveBeenCalledWith(
      TRANSLATION_KEYS_MODULE,
      [],
      expect.any(Function),
    );
    const { prefix, keys } = define.mock.calls[0][2]();
    expect(prefix).toBe("rendering.macro.records.");
    expect(keys).toContain("picker.loadFailed");
    expect(keys).toContain("picker.metadata.doc.title");
  });
});

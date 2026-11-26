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

import SELECTIZE_SOURCE from "@webapp/resources/uicomponents/suggest/xwiki.selectize.js?raw";
import { mockRequireJS } from "@xwiki/platform-test-requirejs";
import jquery from "jquery";
import TomSelect from "tom-select";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";

type Option = { value: string; label: string };
type Settings = {
  options?: Option[];
  maxItems?: number;
  create?: boolean;
  loadSelected?: (text: string, callback: (options: Option[]) => void) => void;
};
type EnhancedInput = HTMLInputElement & { selectize: TomSelect };

let requirejs: RequireJSMock;

function enhance(value: string, settings: Settings): TomSelect {
  document.body.innerHTML = `<input type="text" value="${value}" />`;
  const input = document.querySelector("input") as EnhancedInput;
  (
    jquery(input) as unknown as {
      xwikiSelectize: (settings: Settings) => void;
    }
  ).xwikiSelectize(settings);
  return input.selectize;
}

// Wait for the labels of the selected values to be loaded.
async function waitForSelectedValues(): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve));
}

describe("xwiki-selectize", () => {
  beforeEach(() => {
    requirejs = mockRequireJS({
      module: { config: () => ({}) },
      jquery,
      "tom-select": TomSelect,
      "xwiki-l10n!xwiki-selectize-messages": { get: (key: string) => key },
      "xwiki-events-bridge": {},
    });
    // Evaluate the script in the global scope, where the RequireJS mock is installed.
    (0, eval)(SELECTIZE_SOURCE);
  });

  afterEach(async () => {
    // Let the widget initialization that the script schedules for when the page is ready run before cleaning up.
    await new Promise((resolve) => setTimeout(resolve));
    requirejs.restore();
    const fn = jquery.fn as unknown as Record<string, unknown>;
    delete fn.xwikiSelectize;
    delete fn.selectize;
    document.body.innerHTML = "";
  });

  describe("text input", () => {
    it("selects the initial value when the options are empty", async () => {
      const tomSelect = enhance("DISCLOSED", {
        options: [],
        loadSelected: (text, callback) =>
          callback([{ value: text, label: text.toLowerCase() }]),
      });
      await waitForSelectedValues();

      expect(tomSelect.items).toEqual(["DISCLOSED"]);
      expect(tomSelect.getItem("DISCLOSED")?.textContent).toBe("disclosed");
    });

    it("selects the initial value when it is not among the options", () => {
      const tomSelect = enhance("DISCLOSED", {
        options: [{ value: "DRAFT", label: "draft" }],
      });

      expect(tomSelect.items).toEqual(["DISCLOSED"]);
      expect(Object.keys(tomSelect.options).sort()).toEqual([
        "DISCLOSED",
        "DRAFT",
      ]);
    });

    it("keeps the label of the initial values that are among the options", () => {
      const tomSelect = enhance("DRAFT,DISCLOSED", {
        options: [{ value: "DRAFT", label: "draft" }],
      });

      expect(tomSelect.items).toEqual(["DRAFT", "DISCLOSED"]);
      expect(tomSelect.getItem("DRAFT")?.textContent).toBe("draft");
    });

    it("keeps the order of the options", () => {
      const tomSelect = enhance("DISCLOSED", {
        options: [
          { value: "DRAFT", label: "draft" },
          { value: "DISCLOSED", label: "disclosed" },
        ],
        maxItems: 1,
      });
      tomSelect.refreshOptions();

      const options = tomSelect.dropdown_content.querySelectorAll(".option");
      expect([...options].map((option) => option.textContent)).toEqual([
        "draft",
        "disclosed",
      ]);
    });

    it("doesn't split the initial value when multiple selection is disabled", () => {
      const tomSelect = enhance("Smith, John", { options: [], maxItems: 1 });

      expect(tomSelect.items).toEqual(["Smith, John"]);
      expect(tomSelect.getValue()).toBe("Smith, John");
    });

    it("selects the empty value when the initial value is only the delimiter", () => {
      const tomSelect = enhance(",", { options: [], maxItems: 1 });

      expect(tomSelect.items).toEqual([""]);
    });

    it("selects an initial value missing from the options when creating options is disabled", () => {
      const tomSelect = enhance("DISCLOSED", {
        options: [{ value: "DRAFT", label: "draft" }],
        create: false,
      });

      expect(tomSelect.items).toEqual(["DISCLOSED"]);
    });

    it("selects a value set from outside when creating options is disabled", () => {
      const tomSelect = enhance("", { options: [], create: false });
      jquery("input").val("DISCLOSED").trigger("change");

      expect(tomSelect.items).toEqual(["DISCLOSED"]);
    });
  });

  describe("select", () => {
    it("keeps all the selected values of a multiple select", () => {
      document.body.innerHTML = `<select multiple>
        <option value="DRAFT" selected>draft</option>
        <option value="DISCLOSED" selected>disclosed</option>
      </select>`;
      const select = document.querySelector("select") as HTMLSelectElement & {
        selectize: TomSelect;
      };
      (
        jquery(select) as unknown as {
          xwikiSelectize: (settings: Settings) => void;
        }
      ).xwikiSelectize({});

      expect(select.selectize.items).toEqual(["DRAFT", "DISCLOSED"]);
    });
  });
});

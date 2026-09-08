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

import {
  DERIVED_PARAMETERS,
  findDataTypeInput,
  findScope,
  hasValue,
  resetDerivedParameters,
  setTabsVisible,
} from "../dialog";
import { describe, expect, it } from "vitest";

/**
 * Builds the shape the macro editor produces: the mandatory field, then the tab strip and its panes holding the
 * parameters derived from it.
 *
 * @param dataType - the value of the data type input
 * @param derived - the values of the derived parameters
 * @returns the dialog's form
 */
function givenDialog(
  dataType: string,
  derived: Partial<Record<string, string>> = {},
): HTMLFormElement {
  document.body.innerHTML = `
    <form>
      <div class="macro-parameter-field">
        <select name="class"><option value="">none</option>
          <option value="${dataType}" selected>${dataType}</option></select>
      </div>
      <ul class="nav nav-tabs macro-tabs"><li>Columns</li></ul>
      <div class="tab-content">
        <input name="properties" value="${derived.properties ?? ""}" />
        <input name="filters" value="${derived.filters ?? ""}" />
        <input name="sort" value="${derived.sort ?? ""}" />
      </div>
    </form>`;
  return document.querySelector("form")!;
}

describe("findScope", () => {
  it("scopes lookups to the enclosing form, so two dialogs cannot read each other", () => {
    document.body.innerHTML = `
      <form id="a"><input name="class" value="A.Class" /><span class="p"></span></form>
      <form id="b"><input name="class" value="B.Class" /></form>`;
    const marker = document.querySelector("#a .p")!;
    const scope = findScope(marker);
    expect(findDataTypeInput(scope)!.value).toBe("A.Class");
  });

  it("falls back to the document when the field is not in a form", () => {
    document.body.innerHTML = `<input name="class" value="Loose" /><span class="p"></span>`;
    const scope = findScope(document.querySelector(".p")!);
    expect(findDataTypeInput(scope)!.value).toBe("Loose");
  });
});

describe("findDataTypeInput", () => {
  it("returns null when the dialog has no data type field", () => {
    document.body.innerHTML = `<form><input name="properties" /></form>`;
    expect(findDataTypeInput(document.querySelector("form")!)).toBeNull();
  });
});

describe("setTabsVisible", () => {
  it("hides the tab strip and its panes together", () => {
    const form = givenDialog("Some.Class");
    setTabsVisible(form, false);
    expect(form.querySelector<HTMLElement>(".macro-tabs")!.hidden).toBe(true);
    expect(form.querySelector<HTMLElement>(".tab-content")!.hidden).toBe(true);
  });

  it("shows them again", () => {
    const form = givenDialog("Some.Class");
    setTabsVisible(form, false);
    setTabsVisible(form, true);
    expect(form.querySelector<HTMLElement>(".macro-tabs")!.hidden).toBe(false);
    expect(form.querySelector<HTMLElement>(".tab-content")!.hidden).toBe(false);
  });

  it("does not touch another dialog's tabs", () => {
    document.body.innerHTML = `
      <form id="a"><ul class="macro-tabs"></ul></form>
      <form id="b"><ul class="macro-tabs"></ul></form>`;
    setTabsVisible(document.querySelector("#a")!, false);
    expect(document.querySelector<HTMLElement>("#a .macro-tabs")!.hidden).toBe(
      true,
    );
    expect(document.querySelector<HTMLElement>("#b .macro-tabs")!.hidden).toBe(
      false,
    );
  });
});

describe("resetDerivedParameters", () => {
  it("clears every parameter derived from the data type", () => {
    const form = givenDialog("Some.Class", {
      properties: "doc.title,status",
      filters: "status=Active",
      sort: "doc.title:asc",
    });
    resetDerivedParameters(form);
    DERIVED_PARAMETERS.forEach((name) => {
      expect(
        form.querySelector<HTMLInputElement>(`[name="${name}"]`)!.value,
      ).toBe("");
    });
  });

  it("reports which parameters actually held a value", () => {
    const form = givenDialog("Some.Class", {
      properties: "doc.title",
      sort: "doc.title:asc",
    });
    expect(resetDerivedParameters(form).sort()).toEqual(["properties", "sort"]);
  });

  it("reports nothing when there was nothing to lose", () => {
    expect(resetDerivedParameters(givenDialog("Some.Class"))).toEqual([]);
  });

  it("leaves the data type itself alone", () => {
    const form = givenDialog("Some.Class", { properties: "doc.title" });
    resetDerivedParameters(form);
    expect(findDataTypeInput(form)!.value).toBe("Some.Class");
  });

  it("clears an enhanced widget through its own API rather than the element", () => {
    const form = givenDialog("Some.Class");
    const element = form.querySelector('[name="properties"]')!;
    const calls: string[] = [];
    Object.assign(element, {
      selectize: {
        clear: () => calls.push("clear"),
        clearOptions: () => calls.push("clearOptions"),
      },
    });
    resetDerivedParameters(form);
    // Both matter: clear drops the selection, clearOptions drops the previous type's candidates.
    expect(calls).toEqual(["clear", "clearOptions"]);
  });
});

describe("hasValue", () => {
  it("ignores a placeholder option, which is not a value", () => {
    document.body.innerHTML = `<select name="properties"><option value="" selected>Every field</option></select>`;
    expect(hasValue(document.querySelector("select")!)).toBe(false);
  });

  it("sees a real selection", () => {
    document.body.innerHTML = `
      <select name="properties" multiple>
        <option value="" >Every field</option><option value="doc.title" selected>Title</option>
      </select>`;
    expect(hasValue(document.querySelector("select")!)).toBe(true);
  });
});

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

import { setMessages } from "../../common/translations";
import { updateCreationFields, wireCreationFields } from "../creationFields";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type TomSelect from "tom-select";

/**
 * Builds the creation fields as the macro editor and the displayer templates render them.
 *
 * @param options - the state of the dialog
 * @returns the dialog's form
 */
function givenDialog(
  options: {
    editMode?: boolean;
    columns?: string;
    strategy?: string;
  } = {},
): HTMLFormElement {
  const selected = (value: string) =>
    (options.strategy ?? "") === value ? " selected" : "";
  document.body.innerHTML = `
    <form>
      <input name="properties" value="${options.columns ?? ""}" />
      <input type="checkbox" name="allowEditMode" value="true"${options.editMode ? " checked" : ""} />
      <input type="hidden" name="allowEditMode" value="false" />
      <select name="creationStrategy">
        <option value=""${selected("")}>Creation disabled</option>
        <option value="title" data-required-columns="doc.title"${selected("title")}>Title</option>
        <option value="uuid" data-required-columns=""${selected("uuid")}>UUID</option>
      </select>
      <div class="location-picker-compact">
        <input name="newEntriesLocation" value="" />
        <button type="button">Browse</button>
      </div>
    </form>`;
  return document.querySelector("form")!;
}

function strategySelect(): HTMLSelectElement {
  return document.querySelector<HTMLSelectElement>(
    'select[name="creationStrategy"]',
  )!;
}

function option(value: string): HTMLOptionElement {
  return strategySelect().querySelector<HTMLOptionElement>(
    `option[value="${value}"]`,
  )!;
}

function locationInput(): HTMLInputElement {
  return document.querySelector<HTMLInputElement>(
    'input[name="newEntriesLocation"]',
  )!;
}

function locationButton(): HTMLButtonElement {
  return document.querySelector<HTMLButtonElement>(
    ".location-picker-compact button",
  )!;
}

beforeEach(() => {
  setMessages({
    get: (key, ...args) => [`[${key}]`, ...args].join(" "),
  });
});

afterEach(() => {
  setMessages({ get: () => null });
});

/**
 * Changes a field the way the author does, which the wired dialog listens to.
 */
function change(field: HTMLInputElement | HTMLSelectElement): void {
  field.dispatchEvent(new Event("change"));
}

function givenWiredDialog(
  options: Parameters<typeof givenDialog>[0] = {},
): HTMLFormElement {
  const scope = givenDialog(options);
  wireCreationFields(scope, (field, handler) =>
    field.addEventListener("change", handler),
  );
  return scope;
}

function allowEditMode(allowed = true): void {
  const editMode = document.querySelector<HTMLInputElement>(
    'input[type="checkbox"][name="allowEditMode"]',
  )!;
  editMode.checked = allowed;
  change(editMode);
}

describe("updateCreationFields, following the edit mode", () => {
  it("enables the strategy, and the location once a strategy is picked, when the edit mode is allowed", () => {
    const scope = givenDialog({ editMode: true });

    updateCreationFields(scope, false);
    expect(strategySelect().disabled).toBe(false);
    expect(locationInput().disabled).toBe(true);

    strategySelect().value = "uuid";
    updateCreationFields(scope, false);
    expect(locationInput().disabled).toBe(false);
    expect(locationButton().disabled).toBe(false);
  });

  it("enables and disables the enhanced location picker through its own API", () => {
    const scope = givenDialog({ editMode: true, strategy: "uuid" });
    const picker = { enable: vi.fn(), disable: vi.fn() };
    locationInput().selectize = picker as unknown as TomSelect;

    updateCreationFields(scope, false);
    expect(picker.enable).toHaveBeenCalled();

    strategySelect().value = "";
    updateCreationFields(scope, false);
    expect(picker.disable).toHaveBeenCalled();
  });

  it("does nothing in a dialog without the creation strategy", () => {
    document.body.innerHTML = `<form><input name="properties" value="status" /></form>`;

    expect(() =>
      updateCreationFields(document.querySelector("form")!, true),
    ).not.toThrow();
  });
});

describe("updateCreationFields, following the columns", () => {
  it("offers the strategies needing the title or a field when no column is picked", () => {
    const scope = givenDialog({ editMode: true });
    option("uuid").dataset.requiredColumns = "status";

    updateCreationFields(scope, false);

    expect(option("title").disabled).toBe(false);
    expect(option("uuid").disabled).toBe(false);
  });

  it("disables a strategy needing other page metadata when no column is picked", () => {
    const scope = givenDialog({ editMode: true });
    option("uuid").dataset.requiredColumns = "doc.name";

    updateCreationFields(scope, false);

    expect(option("uuid").disabled).toBe(true);
  });

  it("disables a strategy needing a column the table does not display, and says why below the dropdown", () => {
    const scope = givenDialog({ editMode: true, columns: "status,doc.name" });

    updateCreationFields(scope, false);

    expect(option("title").disabled).toBe(true);
    expect(option("uuid").disabled).toBe(false);
    expect(strategySelect().nextElementSibling?.textContent).toBe(
      "[picker.creation.columnsMissing] Title [picker.metadata.doc.title]",
    );
  });

  it("enables the strategy again once the column is displayed", () => {
    const scope = givenDialog({ editMode: true, columns: "status" });
    updateCreationFields(scope, false);

    document.querySelector<HTMLInputElement>('[name="properties"]')!.value =
      "doc.title, status";
    updateCreationFields(scope, true);

    expect(option("title").disabled).toBe(false);
  });

  it("reads the columns of a select, as the enhanced picker keeps them", () => {
    const scope = givenDialog({ editMode: true });
    document.querySelector('[name="properties"]')!.outerHTML = `
      <select name="properties" multiple>
        <option value="doc.title">Title</option>
        <option value="status" selected>Status</option>
      </select>`;

    updateCreationFields(scope, false);

    expect(option("title").disabled).toBe(true);
  });

  it("keeps the saved strategy when the dialog opens, so that reopening it changes nothing", () => {
    const scope = givenDialog({
      editMode: true,
      columns: "status",
      strategy: "title",
    });

    updateCreationFields(scope, false);

    // The macro editor does not save a disabled option.
    expect(strategySelect().value).toBe("title");
    expect(option("title").disabled).toBe(false);
    expect(strategySelect().nextElementSibling?.textContent).toBe(
      "[picker.creation.columnsMissing] Title [picker.metadata.doc.title]",
    );
  });

  it("keeps the saved strategy and location when the dialog opens without the edit mode", () => {
    const scope = givenDialog({ strategy: "uuid" });

    updateCreationFields(scope, false);

    expect(strategySelect().disabled).toBe(false);
    expect(locationInput().disabled).toBe(false);
  });

  it("turns creation off when the columns no longer allow the picked strategy", () => {
    const scope = givenDialog({
      editMode: true,
      columns: "status",
      strategy: "title",
    });

    updateCreationFields(scope, true);

    expect(strategySelect().value).toBe("");
    expect(locationInput().disabled).toBe(true);
  });
});

describe("updateCreationFields, explaining the disabled strategies", () => {
  it("explains each missing column on its own line", () => {
    const scope = givenDialog({ editMode: true, columns: "doc.name" });
    option("title").dataset.requiredColumns = "doc.title,status";

    updateCreationFields(scope, true);

    const lines = [
      "[picker.creation.columnsMissing] Title [picker.metadata.doc.title]",
      "[picker.creation.columnsMissing] Title status",
    ];
    expect(strategySelect().nextElementSibling?.textContent).toBe(
      lines.join(""),
    );
  });

  it("removes the warning once the column is displayed, and shows none without the edit mode", () => {
    const scope = givenDialog({ editMode: true, columns: "status" });
    updateCreationFields(scope, true);
    document.querySelector<HTMLInputElement>('[name="properties"]')!.value =
      "status,doc.title";

    updateCreationFields(scope, true);

    expect(strategySelect().nextElementSibling?.matches(".box")).toBe(false);

    const withoutEditMode = givenDialog({ columns: "status" });
    updateCreationFields(withoutEditMode, true);

    expect(strategySelect().nextElementSibling?.matches(".box")).toBe(false);
  });
});

describe("wireCreationFields", () => {
  it("enables the strategy once the edit mode is allowed", () => {
    givenWiredDialog();
    expect(strategySelect().disabled).toBe(true);

    allowEditMode();

    expect(strategySelect().disabled).toBe(false);
  });

  it("disables the strategy and the location once the edit mode is turned off", () => {
    givenWiredDialog({ editMode: true, strategy: "uuid" });

    allowEditMode(false);

    expect(strategySelect().disabled).toBe(true);
    expect(locationInput().disabled).toBe(true);
    expect(locationButton().disabled).toBe(true);
  });

  it("keeps a strategy picked without the edit mode in a dialog opened with a saved one", () => {
    givenWiredDialog({ strategy: "uuid" });

    strategySelect().value = "title";
    change(strategySelect());

    expect(strategySelect().disabled).toBe(false);
    expect(strategySelect().value).toBe("title");
  });

  it("turns creation off when a column the picked strategy needs is removed", () => {
    givenWiredDialog();
    allowEditMode();
    strategySelect().value = "title";
    change(strategySelect());

    const columns = document.querySelector<HTMLInputElement>(
      '[name="properties"]',
    )!;
    columns.value = "status";
    change(columns);

    expect(strategySelect().value).toBe("");
    expect(locationInput().disabled).toBe(true);
  });
});

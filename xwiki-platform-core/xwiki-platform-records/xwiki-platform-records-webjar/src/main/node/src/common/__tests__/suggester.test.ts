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
import { showMessage } from "../dialog";
import {
  createSettings,
  reload,
  resolveAgain,
  watchProblems,
} from "../suggester";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { FieldOption, JsonFetcher } from "../fields";
import type { Suggester, Suggestions } from "../suggester";

const STATUS: FieldOption = {
  value: "status",
  label: "Status",
  optgroup: "fields",
};

const CODE: FieldOption = {
  value: "code",
  label: "Code",
  optgroup: "fields",
  problem: "Cannot be sorted on",
};

/**
 * The picker field, inside a dialog whose data type is picked.
 */
let element: Element;

/**
 * The widget enhancing the picker field, with the methods the module calls replaced by mocks.
 */
let suggester: Suggester;

/**
 * What the picker suggests, answering with {@link STATUS} unless a test says otherwise.
 */
let suggestions: {
  load: ReturnType<typeof vi.fn>;
  resolve: ReturnType<typeof vi.fn>;
};

const fetchJson: JsonFetcher = vi.fn();

beforeEach(() => {
  document.body.innerHTML = `
    <form>
      <select name="class"><option value="Book.Class" selected>Book</option></select>
      <input class="picker" />
      <div class="ts-wrapper"><div class="ts-control"></div></div>
    </form>`;
  element = document.querySelector(".picker")!;
  suggestions = {
    load: vi.fn().mockResolvedValue([STATUS]),
    resolve: vi.fn().mockResolvedValue([STATUS]),
  };
  suggester = {
    items: ["status"],
    options: {},
    settings: createSettings(element, fetchJson, suggestions as Suggestions),
    wrapper: document.querySelector<HTMLElement>(".ts-wrapper")!,
    addOption: vi.fn(),
    updateOption: vi.fn(),
    load: vi.fn(),
    inputValue: () => "sta",
    removeItem: vi.fn(),
    setTextboxValue: vi.fn(),
    refreshOptions: vi.fn(),
    clearOptions: vi.fn(),
  } as unknown as Suggester;
});

/**
 * Asks the widget's `load` setting for a query, the way the widget does.
 *
 * @param query - the typed text
 * @returns the options the setting handed back
 */
async function load(query: string): Promise<FieldOption[]> {
  const callback = vi.fn();
  suggester.settings.load.call(suggester, query, callback);
  await vi.waitFor(() => expect(callback).toHaveBeenCalled());
  return callback.mock.calls[0][0];
}

/**
 * @param values - the values of the items to show in the widget
 */
function showItems(...values: string[]): void {
  suggester.wrapper.querySelector(".ts-control")!.innerHTML = values
    .map(
      (value) =>
        `<div class="item" data-value="${value}"><span class="xwiki-selectize-option-label">${value}</span></div>`,
    )
    .join("");
}

describe("createSettings", () => {
  it("asks the suggestions for the query, telling it what is already selected", async () => {
    await expect(load("sta")).resolves.toEqual([STATUS]);
    expect(suggestions.load).toHaveBeenCalledWith(element, "sta", fetchJson, [
      "status",
    ]);
  });

  it("resolves a stored value through the suggestions", async () => {
    const callback = vi.fn();
    suggester.settings.loadSelected.call(suggester, "status", callback);
    await vi.waitFor(() => expect(callback).toHaveBeenCalledWith([STATUS], []));
    expect(suggestions.resolve).toHaveBeenCalledWith(
      element,
      "status",
      fetchJson,
    );
  });

  it("clears an earlier failure once a load succeeds", async () => {
    showMessage(suggester.wrapper, "error", "Failed");
    await load("");
    expect(document.querySelector(".errormessage")).toBeNull();
  });

  it("points at the selected items the table ignores once a load succeeds", async () => {
    suggester.options = { code: CODE };
    showItems("code");
    await load("");
    expect(document.querySelector(".warningmessage")?.textContent).toBe(
      "Cannot be sorted on",
    );
  });

  it("offers nothing on a failure, and says why under the widget with a way to try again", async () => {
    suggestions.load.mockRejectedValue(new Error("HTTP 500"));

    await expect(load("sta")).resolves.toEqual([]);

    const message = suggester.wrapper.nextElementSibling!;
    expect(message.classList).toContain("errormessage");
    expect(message.textContent).toContain("picker.loadFailed");
    expect(message.querySelector("button")?.textContent).toBe("picker.retry");
  });

  it("tries again from the message: forgets what was loaded, resolves the selection again and loads", async () => {
    suggestions.load.mockRejectedValue(new Error("HTTP 500"));
    await load("sta");

    suggester.wrapper.nextElementSibling!.querySelector("button")!.click();

    expect(suggester.clearOptions).toHaveBeenCalled();
    expect(suggestions.resolve).toHaveBeenCalledWith(
      element,
      "status",
      fetchJson,
    );
    expect(suggester.load).toHaveBeenCalledWith("sta");
  });

  it("reports a failure after the field itself, without a retry, before the widget exists", async () => {
    suggestions.load.mockRejectedValue(new Error("HTTP 500"));
    const callback = vi.fn();

    suggester.settings.load.call(undefined, "", callback);
    await vi.waitFor(() => expect(callback).toHaveBeenCalledWith([], []));

    const message = element.nextElementSibling!;
    expect(message.classList).toContain("errormessage");
    expect(message.querySelector("button")).toBeNull();
  });

  it("loads what is typed when the widget gets the focus", () => {
    suggester.settings.onFocus!.call(suggester);
    expect(suggester.load).toHaveBeenCalledWith("sta");
  });

  it("lets the suggestions override the shared settings", () => {
    const settings = createSettings(element, fetchJson, {
      ...(suggestions as Suggestions),
      settings: { plugins: ["remove_button"] },
    });
    expect(settings.plugins).toEqual(["remove_button"]);
    expect(settings.persist).toBe(false);
  });
});

describe("reload", () => {
  it("drops the loaded options and loads what is typed again", () => {
    reload(suggester);
    expect(suggester.clearOptions).toHaveBeenCalled();
    expect(suggester.load).toHaveBeenCalledWith("sta");
  });
});

describe("resolveAgain", () => {
  it("replaces the option of a value the widget already knows", async () => {
    suggester.options = { status: { ...STATUS, label: "status" } };
    resolveAgain(suggester, "status");
    await vi.waitFor(() =>
      expect(suggester.updateOption).toHaveBeenCalledWith("status", STATUS),
    );
    expect(suggester.addOption).not.toHaveBeenCalled();
  });

  it("adds the option of a value the widget does not know yet", async () => {
    resolveAgain(suggester, "status");
    await vi.waitFor(() =>
      expect(suggester.addOption).toHaveBeenCalledWith(STATUS),
    );
  });
});

describe("watchProblems", () => {
  it("points at an ignored item as soon as it is shown", async () => {
    suggester.options = { code: CODE, status: STATUS };
    watchProblems(suggester);

    showItems("status", "code");

    await vi.waitFor(() =>
      expect(
        document.querySelector('[data-value="code"]')!.getAttribute("title"),
      ).toBe("Cannot be sorted on"),
    );
    expect(
      document.querySelector('[data-value="status"]')!.hasAttribute("title"),
    ).toBe(false);
  });
});

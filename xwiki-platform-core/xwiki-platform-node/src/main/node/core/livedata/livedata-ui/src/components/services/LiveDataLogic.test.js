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
import { LiveDataLogic } from "./LiveDataLogic";
import flushPromises from "flush-promises";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { computed } from "vue";

vi.mock("vue-i18n", () => ({
  useI18n: () => ({ t: (key) => key }),
}));

vi.mock("@xwiki/platform-livedata-componentstore", () => ({
  componentStore: { load: vi.fn().mockResolvedValue({}) },
}));

const SOURCE = { id: "liveTable" };

/**
 * @param entries - the entries initially displayed
 * @param options - the actions available on the live data, and the parameters to set on the source
 *   of the query
 * @returns the serialized live data configuration
 */
function initData(entries, { actions = [], sourceParameters = {} } = {}) {
  return JSON.stringify({
    query: {
      properties: ["name", "status"],
      source: { ...SOURCE, ...sourceParameters },
      sort: [],
      filters: [],
      offset: 0,
      limit: 10,
    },
    data: { count: entries.length, entries },
    meta: {
      defaultLayout: "table",
      layouts: [{ id: "table" }],
      propertyDescriptors: [
        { id: "name", visible: true },
        { id: "status", visible: true },
      ],
      propertyTypes: [],
      displayers: [{ id: "text" }],
      defaultDisplayer: "text",
      filters: [],
      defaultFilter: "text",
      entryDescriptor: { idProperty: "id" },
      actions,
    },
  });
}

/**
 * @param entries - the entries initially displayed
 * @param options - the actions available on the live data, and the parameters to set on the source
 *   of the query
 * @returns the live data logic and the mocked live data source
 */
function initLogic(entries, options) {
  const liveDataSource = {
    getEntries: vi.fn(),
    getEntry: vi.fn(),
    addEntry: vi.fn(),
    removeEntry: vi.fn(),
    updateEntry: vi.fn(),
    updateEntryProperty: vi.fn(),
  };
  const logic = new LiveDataLogic(
    liveDataSource,
    initData(entries, options),
    true,
    () => Promise.resolve({}),
  );
  return { logic, liveDataSource };
}

/**
 * @param logic - the live data logic
 * @returns the ids of the currently displayed entries
 */
function displayedIds(logic) {
  return logic.data.data.entries.map((entry) => entry.id);
}

describe("LiveDataLogic", () => {
  let logic;
  let liveDataSource;

  beforeEach(() => {
    ({ logic, liveDataSource } = initLogic([
      { id: "1", name: "one" },
      { id: "2", name: "two" },
      { id: "3", name: "three" },
    ]));
  });

  it("does not freeze the view outside of edit mode", async () => {
    liveDataSource.getEntries.mockResolvedValue({
      count: 3,
      entries: [{ id: "3" }, { id: "1" }, { id: "2" }],
    });

    await logic.updateEntries();

    expect(logic.isViewFrozen()).toBe(false);
    expect(displayedIds(logic)).toStrictEqual(["3", "1", "2"]);
  });

  it("freezes the view when the edit mode is enabled, and unfreezes it when it is disabled", () => {
    expect(logic.isEditMode()).toBe(false);
    expect(logic.isViewFrozen()).toBe(false);

    logic.enableEditMode();
    expect(logic.isEditMode()).toBe(true);
    expect(logic.isViewFrozen()).toBe(true);

    logic.disableEditMode();
    expect(logic.isEditMode()).toBe(false);
    expect(logic.isViewFrozen()).toBe(false);
  });

  it("keeps the entries in their frozen position when they are reordered", async () => {
    logic.enableEditMode();
    liveDataSource.getEntries.mockResolvedValue({
      count: 3,
      entries: [
        { id: "3", name: "three" },
        { id: "1", name: "one" },
        { id: "2", name: "two" },
      ],
    });

    await logic.updateEntries();

    expect(displayedIds(logic)).toStrictEqual(["1", "2", "3"]);
  });

  describe("with frozen entries that are not returned by the query anymore", () => {
    it("fetches the frozen entries that are not returned by the query anymore", async () => {
      logic.enableEditMode();
      // The second entry does not match the query anymore, it is not part of the returned entries.
      liveDataSource.getEntries.mockResolvedValue({
        count: 2,
        entries: [
          { id: "1", name: "one" },
          { id: "3", name: "three" },
        ],
      });
      liveDataSource.getEntry.mockResolvedValue({ name: "two (updated)" });

      await logic.updateEntries();

      expect(liveDataSource.getEntry).toHaveBeenCalledWith(SOURCE, "2", [
        "name",
        "status",
      ]);
      expect(displayedIds(logic)).toStrictEqual(["1", "2", "3"]);
      expect(logic.data.data.entries[1]).toStrictEqual({
        id: "2",
        name: "two (updated)",
      });
    });

    it("drops the frozen entries that do not exist anymore", async () => {
      logic.enableEditMode();
      liveDataSource.getEntries.mockResolvedValue({
        count: 2,
        entries: [
          { id: "1", name: "one" },
          { id: "3", name: "three" },
        ],
      });
      // The entry has been deleted in the meantime.
      liveDataSource.getEntry.mockResolvedValue(undefined);

      await logic.updateEntries();

      expect(displayedIds(logic)).toStrictEqual(["1", "3"]);
    });

    it("keeps the displayed values of the frozen entries that fail to be fetched", async () => {
      const consoleError = vi
        .spyOn(console, "error")
        .mockImplementation(() => {});
      logic.enableEditMode();
      liveDataSource.getEntries.mockResolvedValue({
        count: 2,
        entries: [
          { id: "1", name: "one" },
          { id: "3", name: "three" },
        ],
      });
      const error = new Error("Server error");
      liveDataSource.getEntry.mockRejectedValue(error);

      await logic.updateEntries();

      expect(displayedIds(logic)).toStrictEqual(["1", "2", "3"]);
      expect(logic.data.data.entries[1]).toStrictEqual({
        id: "2",
        name: "two",
      });
      expect(consoleError).toHaveBeenCalledWith(
        "Failed to fetch the entry [2]",
        error,
      );
      consoleError.mockRestore();
    });

    it("drops the update when fetching a frozen entry is aborted", async () => {
      logic.enableEditMode();
      liveDataSource.getEntries.mockResolvedValue({
        count: 2,
        entries: [
          { id: "1", name: "one (updated)" },
          { id: "3", name: "three" },
        ],
      });
      // A newer update aborted the request.
      liveDataSource.getEntry.mockRejectedValue({ statusText: "abort" });

      await logic.updateEntries();

      expect(logic.data.data.entries).toStrictEqual([
        { id: "1", name: "one" },
        { id: "2", name: "two" },
        { id: "3", name: "three" },
      ]);
    });
  });

  it("unfreezes the view when the query changes", async () => {
    logic.enableEditMode();
    // Sorting on a property is an explicit user action, the entries are expected to move.
    logic.data.query.sort = [{ property: "name", descending: false }];
    liveDataSource.getEntries.mockResolvedValue({
      count: 3,
      entries: [
        { id: "3", name: "three" },
        { id: "1", name: "one" },
        { id: "2", name: "two" },
      ],
    });

    await logic.updateEntries();

    expect(displayedIds(logic)).toStrictEqual(["3", "1", "2"]);
    expect(liveDataSource.getEntry).not.toHaveBeenCalled();
    // The new order is frozen in turn.
    expect(logic.isViewFrozen()).toBe(true);
  });

  it("keeps the drafts of the new entries when the view is frozen", async () => {
    logic.enableEditMode();
    logic.data.data.entries.push({ _new: true });
    liveDataSource.getEntries.mockResolvedValue({
      count: 3,
      entries: [
        { id: "1", name: "one" },
        { id: "2", name: "two" },
        { id: "3", name: "three" },
      ],
    });

    await logic.updateEntries();

    expect(displayedIds(logic)).toStrictEqual(["1", "2", "3", undefined]);
    expect(logic.data.data.entries[3]._new).toBe(true);
  });
});

describe("LiveDataLogic maximized state", () => {
  let logic;

  beforeEach(() => {
    ({ logic } = initLogic([]));
  });

  it("is not maximized by default", () => {
    expect(logic.isMaximized()).toBe(false);
  });

  it("switches back and forth between maximized and normal", () => {
    logic.toggleMaximized();
    expect(logic.isMaximized()).toBe(true);

    logic.toggleMaximized();
    expect(logic.isMaximized()).toBe(false);
  });

  it("exposes the maximized state reactively", () => {
    const maximized = computed(() => logic.isMaximized());

    expect(maximized.value).toBe(false);

    logic.toggleMaximized();

    expect(maximized.value).toBe(true);
  });
});

describe("LiveDataLogic edit mode", () => {
  it("has no edit mode when the source does not declare one", () => {
    expect(initLogic([]).logic.hasEditMode()).toBe(false);
    expect(
      initLogic([], {
        sourceParameters: { hasEditMode: "false" },
      }).logic.hasEditMode(),
    ).toBe(false);
  });

  it("has an edit mode when the source declares one", () => {
    expect(
      initLogic([], {
        sourceParameters: { hasEditMode: "true" },
      }).logic.hasEditMode(),
    ).toBe(true);
  });

  it("exposes the edit mode state reactively", () => {
    const { logic } = initLogic([], {
      sourceParameters: { hasEditMode: "true" },
    });
    const editMode = computed(() => logic.isEditMode());

    expect(editMode.value).toBe(false);

    logic.enableEditMode();

    expect(editMode.value).toBe(true);
  });
});

describe("Creating the entry of a new row", () => {
  /**
   * @param actions - the actions available on the live data
   * @returns a logic holding a single placeholder row, as after a click on "add entry"
   */
  async function initLogicWithNewRow(actions) {
    const created = initLogic([], { actions });
    // The row is added the way the "add entry" action does, so that it carries everything a row
    // without an entry holds.
    await created.logic.addEntry();
    created.logic.updateEntries = vi.fn().mockResolvedValue(undefined);
    return created;
  }

  it("creates the entry when a value is set", async () => {
    const { logic, liveDataSource } = await initLogicWithNewRow();
    liveDataSource.addEntry.mockResolvedValue({ id: "4", name: "Esther" });

    await logic.setValues({ entryId: undefined, values: { name: "Esther" } });

    expect(liveDataSource.addEntry).toHaveBeenCalledOnce();
    expect(liveDataSource.addEntry.mock.calls[0][1]).toEqual({
      name: "Esther",
    });
  });

  it("keeps the same row key when the entry of the row is created", async () => {
    const { logic, liveDataSource } = await initLogicWithNewRow();
    liveDataSource.addEntry.mockResolvedValue({ id: "4", name: "Esther" });
    const row = logic.data.data.entries.find((entry) => entry._new);
    const entryKeyBefore = logic.getEntryKey(row);

    await logic.setValues({ entryId: undefined, values: { name: "Esther" } });

    // The row must not be rebuilt when the identifier of its entry appears, otherwise the cell being
    // edited is destroyed along with it.
    expect(logic.getEntryKey(row)).toBe(entryKeyBefore);
    // The key follows the entry once the row is replaced by the refreshed one.
    expect(logic.getEntryKey({ id: "4", name: "Esther" })).toBe(entryKeyBefore);
  });

  it("keeps the created entry in the frozen view", async () => {
    const { logic, liveDataSource } = await initLogicWithNewRow();
    logic.enableEditMode();
    liveDataSource.addEntry.mockResolvedValue({ id: "4", name: "Esther" });

    await logic.setValues({ entryId: undefined, values: { name: "Esther" } });

    // Otherwise the created entry jumps to wherever the sort puts it on the next refresh.
    expect(logic.isViewFrozen()).toBe(true);
    expect(logic.frozenView.entryIds).toContain("4");
  });

  // eslint-disable-next-line max-statements
  it("saves the next cell opened while the entry was being created", async () => {
    const { logic, liveDataSource } = await initLogicWithNewRow();
    liveDataSource.addEntry.mockResolvedValue({ id: "4", name: "Esther" });
    liveDataSource.updateEntry.mockResolvedValue(undefined);
    const editBus = logic.getEditBus();
    const row = logic.data.data.entries.find((entry) => entry._new);

    editBus.start(row, "name");
    editBus.save(row, "name", { name: "Esther" });
    // The next cell is opened while the entry is still being created, so its edition is registered
    // on a row that has no identifier yet.
    editBus.start(row, "status");
    await editBus.whenSaved();
    editBus.save(row, "status", { status: "done" });
    await editBus.whenSaved();

    // Saving it must reach the entry that the row has become, with the value of that cell.
    expect(liveDataSource.updateEntry).toHaveBeenCalledOnce();
    expect(liveDataSource.updateEntry).toHaveBeenCalledWith(
      expect.anything(),
      "4",
      { status: "done" },
    );
  });

  // eslint-disable-next-line max-statements
  it("creates the entry once when the next cell is saved while it is being created", async () => {
    const { logic, liveDataSource } = await initLogicWithNewRow();
    let resolveCreation;
    liveDataSource.addEntry.mockReturnValue(
      new Promise((resolve) => {
        resolveCreation = resolve;
      }),
    );
    liveDataSource.updateEntry.mockResolvedValue(undefined);
    const editBus = logic.getEditBus();
    const row = logic.data.data.entries.find((entry) => entry._new);

    editBus.start(row, "name");
    const creation = editBus.save(row, "name", { name: "Esther" });
    // The next cell is saved before the entry is created.
    editBus.start(row, "status");
    const update = editBus.save(row, "status", { status: "done" });
    resolveCreation({ id: "4", name: "Esther" });
    await creation;
    await update;
    await flushPromises();

    // A single entry is created, and the value of the next cell reaches it.
    expect(liveDataSource.addEntry).toHaveBeenCalledOnce();
    expect(liveDataSource.updateEntry).toHaveBeenCalledOnce();
    expect(liveDataSource.updateEntry).toHaveBeenCalledWith(
      expect.anything(),
      "4",
      { status: "done" },
    );
  });

  it("does not create the entry when the value is left empty", async () => {
    const { logic, liveDataSource } = await initLogicWithNewRow();

    await logic.setValues({ entryId: undefined, values: { name: "" } });

    // Leaving a cell without typing anything must not create a page.
    expect(liveDataSource.addEntry).not.toHaveBeenCalled();
    expect(logic.data.data.entries).toHaveLength(1);
  });

  it("does not create the entry when only the encoding fields are submitted", async () => {
    const { logic, liveDataSource } = await initLogicWithNewRow();

    // An editor submits the syntax of an empty text area along with its empty value.
    await logic.setValues({
      entryId: undefined,
      values: {
        description: "   ",
        description_syntax: "xwiki/2.1",
        RequiresHTMLConversion: "description",
      },
    });

    expect(liveDataSource.addEntry).not.toHaveBeenCalled();
  });

  it("does not create the entry when a list value is empty", async () => {
    const { logic, liveDataSource } = await initLogicWithNewRow();

    await logic.setValues({ entryId: undefined, values: { tags: ["", ""] } });

    expect(liveDataSource.addEntry).not.toHaveBeenCalled();
  });
});

describe("Adding a row", () => {
  it("displays the row where the entry will be created", async () => {
    const { logic } = initLogic([], {
      actions: [{ id: "addEntry", location: "NewRows" }],
    });

    await logic.addEntry();

    // The cells of a row that has no entry yet are displayed in the location the entry ends up in,
    // rather than in the context of the page holding the Live Data.
    const row = logic.data.data.entries.find((entry) => entry._new);
    expect(row._displayReference).toMatch(/^NewRows\./);
  });

  it("still adds the row when the display reference cannot be computed", async () => {
    const { logic } = initLogic([], {
      actions: [{ id: "addEntry", location: "NewRows" }],
    });
    // Whatever goes wrong while looking for where the row is displayed, for instance a browser API
    // that a wiki served over plain HTTP does not provide.
    logic.getActionDescriptor = () => {
      throw new TypeError("unavailable");
    };

    await logic.addEntry();

    // The row must still be usable, falling back on the page holding the Live Data to display it.
    const row = logic.data.data.entries.find((entry) => entry._new);
    expect(row).toBeDefined();
    expect(row._displayReference).toBeUndefined();
  });

  it("leaves the display reference unset when the location is unknown", async () => {
    const { logic } = initLogic([], { actions: [{ id: "addEntry" }] });

    await logic.addEntry();

    // Displaying the cells then falls back on the page holding the Live Data.
    const row = logic.data.data.entries.find((entry) => entry._new);
    expect(row._displayReference).toBeUndefined();
  });
});

describe("Deleting an entry", () => {
  it("drops a row that has no entry without asking", async () => {
    const { logic, liveDataSource } = initLogic([]);
    logic.data.data.entries.push({ _new: "true" });

    await logic.deleteEntry(logic.data.data.entries[0]);

    expect(liveDataSource.removeEntry).not.toHaveBeenCalled();
    expect(logic.data.data.entries).toHaveLength(0);
  });

  it("deletes an existing entry once confirmed", async () => {
    const entry = { id: "1", name: "one" };
    const { logic, liveDataSource } = initLogic([entry]);
    logic.updateEntries = vi.fn().mockResolvedValue(undefined);
    liveDataSource.removeEntry.mockResolvedValue(undefined);
    globalThis.XWiki = {
      widgets: {
        ConfirmationBox: function (behavior) {
          behavior.onYes();
        },
      },
    };

    await logic.deleteEntry(logic.data.data.entries[0]);

    expect(liveDataSource.removeEntry).toHaveBeenCalledOnce();
    expect(liveDataSource.removeEntry.mock.calls[0][1]).toBe("1");
  });

  it("keeps an existing entry when the deletion is refused", async () => {
    const { logic, liveDataSource } = initLogic([{ id: "1", name: "one" }]);
    globalThis.XWiki = {
      widgets: {
        ConfirmationBox: function (behavior) {
          behavior.onNo();
        },
      },
    };

    await logic.deleteEntry(logic.data.data.entries[0]);

    expect(liveDataSource.removeEntry).not.toHaveBeenCalled();
    expect(logic.data.data.entries).toHaveLength(1);
  });
});

describe("Entry keys", () => {
  let logic;

  beforeEach(() => {
    ({ logic } = initLogic([]));
  });

  it("gives distinct keys to entries sharing an id", () => {
    const keys = logic.getEntryKeys([{ id: "1" }, { id: "1" }, { id: "2" }]);

    expect(new Set(keys).size).toBe(3);
    // The first occurrence keeps the key of the entry.
    expect(keys[0]).toBe(logic.getEntryKey({ id: "1" }));
  });

  it("gives distinct keys to entries without an id", () => {
    const keys = logic.getEntryKeys([{ name: "one" }, { name: "two" }]);

    expect(new Set(keys).size).toBe(2);
  });

  it("keeps the key of an entry when the entries are reordered", () => {
    const [firstKey, secondKey] = logic.getEntryKeys([
      { id: "1" },
      { id: "2" },
    ]);

    expect(logic.getEntryKeys([{ id: "2" }, { id: "1" }])).toStrictEqual([
      secondKey,
      firstKey,
    ]);
  });
});

describe("Saving the cells of an entry", () => {
  // eslint-disable-next-line max-statements
  it("saves the next cell saved while the previous one is still being saved", async () => {
    const { logic, liveDataSource } = initLogic([
      { id: "1", name: "one", status: "todo" },
    ]);
    logic.updateEntries = vi.fn().mockResolvedValue(undefined);
    let resolveFirstSave;
    liveDataSource.updateEntry
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveFirstSave = resolve;
        }),
      )
      .mockResolvedValue(undefined);
    const editBus = logic.getEditBus();
    const entry = logic.data.data.entries[0];

    editBus.start(entry, "name");
    const firstSave = editBus.save(entry, "name", { name: "two" });
    // The next cell is saved before the previous one is.
    editBus.start(entry, "status");
    const secondSave = editBus.save(entry, "status", { status: "done" });
    resolveFirstSave();
    await firstSave;
    await secondSave;
    await flushPromises();

    // Each cell is saved once, with its own value.
    expect(liveDataSource.updateEntry).toHaveBeenCalledTimes(2);
    expect(liveDataSource.updateEntry).toHaveBeenNthCalledWith(
      1,
      expect.anything(),
      "1",
      { name: "two" },
    );
    expect(liveDataSource.updateEntry).toHaveBeenNthCalledWith(
      2,
      expect.anything(),
      "1",
      { status: "done" },
    );
  });
});

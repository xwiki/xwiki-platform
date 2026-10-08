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
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("vue-i18n", () => ({
  useI18n: () => ({ t: (key) => key }),
}));

vi.mock("@xwiki/platform-livedata-componentstore", () => ({
  componentStore: { load: vi.fn().mockResolvedValue({}) },
}));

const SOURCE = { id: "liveTable" };

/**
 * @param entries - the entries initially displayed
 * @returns the serialized live data configuration
 */
function initData(entries) {
  return JSON.stringify({
    query: {
      properties: ["name", "status"],
      source: SOURCE,
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
    },
  });
}

/**
 * @param entries - the entries initially displayed
 * @returns the live data logic and the mocked live data source
 */
function initLogic(entries) {
  const liveDataSource = {
    getEntries: vi.fn(),
    getEntry: vi.fn(),
    addEntry: vi.fn(),
    updateEntry: vi.fn(),
    updateEntryProperty: vi.fn(),
  };
  const logic = new LiveDataLogic(liveDataSource, initData(entries), true, () =>
    Promise.resolve({}),
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
    expect(logic.isViewFrozen()).toBe(false);

    logic.enableEditMode();
    expect(logic.isViewFrozen()).toBe(true);

    logic.disableEditMode();
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

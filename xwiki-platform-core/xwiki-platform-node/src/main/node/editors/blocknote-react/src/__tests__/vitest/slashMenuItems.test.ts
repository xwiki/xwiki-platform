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
// @vitest-environment jsdom

import {
  createBlockNoteSchema,
  querySuggestionsMenuItems,
} from "../../blocknote";
import { FULL_SYNTAX } from "../playwright-ct/syntax.mock";
import { BlockNoteEditor } from "@blocknote/core";
import { beforeAll, describe, expect, test, vi } from "vitest";
import type { SlashMenuItem } from "../../features/editorFeature";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type AnyEditor = BlockNoteEditor<any, any, any>;

describe("querySuggestionsMenuItems (contributed items)", () => {
  let editor: AnyEditor;

  beforeAll(() => {
    editor = BlockNoteEditor.create({
      schema: createBlockNoteSchema(),
    }) as AnyEditor;
  });

  function titles(
    featureItems: SlashMenuItem[],
    syntax: SyntaxConfig = FULL_SYNTAX,
  ): string[] {
    return querySuggestionsMenuItems({
      editor,
      query: "",
      syntax,
      featureItems,
    }).map((item) => item.title);
  }

  function item(title: string, key?: string): SlashMenuItem {
    return { key, title, group: "Media", onItemClick: vi.fn() };
  }

  test("an item with the key of a default item takes its place", () => {
    const defaultTitles = titles([]);

    const replaced = titles([item("My image", "image")]);

    expect(replaced).not.toContain("Image");
    expect(replaced.indexOf("My image")).toBe(defaultTitles.indexOf("Image"));
    expect(replaced).toHaveLength(defaultTitles.length);
  });

  test("an item without the key of a default item is added to its group", () => {
    const added = titles([item("Gallery"), item("Diagram", "diagram")]);

    expect(added).toContain("Image");
    expect(added.indexOf("Gallery")).toBe(added.indexOf("File") + 1);
    expect(added.indexOf("Diagram")).toBe(added.indexOf("Gallery") + 1);
  });

  test("the default items of the blocks the syntax can't save are removed", () => {
    const syntax = structuredClone(FULL_SYNTAX);
    syntax.features.blocks.images.basicImages = false;
    syntax.features.blocks.tables.basicTables = false;

    const items = titles([], syntax);

    expect(items).not.toContain("Image");
    expect(items).not.toContain("Table");
    expect(items).toContain("Quote");
  });
});

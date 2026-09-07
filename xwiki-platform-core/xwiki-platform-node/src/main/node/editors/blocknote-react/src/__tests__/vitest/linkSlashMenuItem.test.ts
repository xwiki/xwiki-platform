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
import type { LinkEditionHandler } from "../../components/links/linkEdition";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type AnyEditor = BlockNoteEditor<any, any, any>;

// Identity stub: real translations aren't under test here, so the returned "translation" is just the key,
// which keeps the assertions below tied directly to the keys added in `translation-en.json`.
const t = (key: string) => key;

describe("querySuggestionsMenuItems (Link quick action)", () => {
  let editor: AnyEditor;

  beforeAll(() => {
    editor = BlockNoteEditor.create({
      schema: createBlockNoteSchema([]),
    }) as AnyEditor;
  });

  function findLinkItem(query = "") {
    const items = querySuggestionsMenuItems({
      editor,
      query,
      macros: [],
      syntax: FULL_SYNTAX,
      lang: "en",
      t,
      linkEditionHandler: vi.fn(),
      openMacroInsertionEditor: undefined,
    });

    return items.find(
      (item) => item.title === "blocknote.slashMenu.link.title",
    );
  }

  test("adds a generic Link entry to the slash menu", () => {
    const linkItem = findLinkItem();

    expect(linkItem).toBeDefined();
    expect(linkItem!.subtext).toBe("blocknote.slashMenu.link.subtext");
    expect(linkItem!.aliases).toEqual(["link"]);
    expect(linkItem!.group).toBe("Links");
  });

  test("the Link entry survives a query matching its alias", () => {
    expect(findLinkItem("link")).toBeDefined();
  });

  test("the Link entry is filtered out by a query matching neither its title nor its alias", () => {
    expect(findLinkItem("table")).toBeUndefined();
  });

  // eslint-disable-next-line max-statements
  test("clicking the Link entry opens the link editor, and submitting it creates the link and refocuses the editor", () => {
    const linkEditionHandler: LinkEditionHandler = vi.fn();
    const createLink = vi
      .spyOn(editor, "createLink")
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      .mockImplementation((() => {}) as any);
    const focus = vi.spyOn(editor, "focus").mockImplementation(() => {});

    try {
      const items = querySuggestionsMenuItems({
        editor,
        query: "",
        macros: [],
        syntax: FULL_SYNTAX,
        lang: "en",
        t,
        linkEditionHandler,
        openMacroInsertionEditor: undefined,
      });
      const linkItem = items.find(
        (item) => item.title === "blocknote.slashMenu.link.title",
      )!;

      linkItem.onItemClick?.();

      expect(linkEditionHandler).toHaveBeenCalledTimes(1);
      const handlerProps = vi.mocked(linkEditionHandler).mock.calls[0][0];
      expect(handlerProps.mode).toBe("createNew");
      expect(handlerProps.current).toEqual({ title: "", url: "" });

      // Submitting the link edition modal must insert the link and give focus back to the editor.
      handlerProps.onSubmit({ title: "XWiki", url: "https://xwiki.org" });

      expect(createLink).toHaveBeenCalledWith("https://xwiki.org", "XWiki");
      expect(focus).toHaveBeenCalledTimes(1);
    } finally {
      createLink.mockRestore();
      focus.mockRestore();
    }
  });
});

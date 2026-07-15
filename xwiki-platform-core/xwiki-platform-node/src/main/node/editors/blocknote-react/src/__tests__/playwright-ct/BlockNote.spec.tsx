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
import { BlockNoteForTest } from "./BlockNote.story";
import { expect, test } from "@playwright/experimental-ct-react";
import type { BlockOfType, BlockType } from "../../blocknote";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";

test("BlockNote shows with empty content", async ({ mount }) => {
  const component = await mount(
    <BlockNoteForTest content={[]} macros={false} syntax={FULL_SYNTAX} />,
  );

  await expect(component).toBeVisible();
  await expect(component).toHaveText("");
});

test("BlockNote shows with initial content", async ({ mount }) => {
  const component = await mount(
    <BlockNoteForTest
      content={buildParagraphs(["Hello,", "world!"])}
      macros={false}
      syntax={FULL_SYNTAX}
    />,
  );

  await expect(component).toBeVisible();
  await expect(component).toHaveText("Hello,world!");
});

test("BlockNote's content can be modified", async ({ mount }) => {
  const component = await mount(
    <BlockNoteForTest content={[]} macros={false} syntax={FULL_SYNTAX} />,
  );

  const editorEl = component.locator(".bn-editor");

  await editorEl.fill("Some basic content!!!");
  await expect(editorEl).toHaveText("Some basic content!!!");

  await editorEl.press("Backspace");
  await editorEl.press("Backspace");
  await editorEl.press("Backspace");
  await expect(editorEl).toHaveText("Some basic content");
});

// eslint-disable-next-line max-statements
test("Image insertion UI can be overriden", async ({ mount, page }) => {
  let overrideFnCalledWithUrl: string | null = null;

  const component = await mount(
    <BlockNoteForTest
      content={[buildImage(SMALL_IMG_DATA_URL)]}
      macros={false}
      overrides={{
        // Unfortunately we can't call the "update" image handler here as functions don't cross Playwright's headless browser's boundaries
        imageEdition: (image) => {
          overrideFnCalledWithUrl = image.url;
        },
      }}
      syntax={FULL_SYNTAX}
    />,
  );

  const editorEl = component.locator(".bn-editor");

  const imgEl = editorEl.locator("img.bn-visual-media");
  await imgEl.waitFor({ state: "attached" });

  // Trigger the toolbar by selecting the image.
  await imgEl.click();

  // The toolbar is rendered via FloatingPortal into document.body (outside the component root),
  // so we must use page.locator instead of component.locator
  const toolbarEl = page.locator(".bn-toolbar.bn-formatting-toolbar");
  await toolbarEl.waitFor({ state: "attached" });

  // Trigger the image edition UI
  const imgEditBtnEl = page.locator(
    'button[data-test="blocknote.imageToolbar.buttons.edit"]',
  );
  await imgEditBtnEl.waitFor({ state: "attached" });
  await imgEditBtnEl.click();

  expect(overrideFnCalledWithUrl).toBe(SMALL_IMG_DATA_URL);
});

test("Allowed syntax features should be available", async ({ mount, page }) => {
  const component = await mount(
    <BlockNoteForTest macros={false} content={[]} syntax={FULL_SYNTAX} />,
  );

  const editorEl = component.locator(".bn-editor");

  await editorEl.press("/");

  const slashMenuEl = page.locator(
    "[data-floating-ui-portal] .bn-suggestion-menu",
  );

  await slashMenuEl.waitFor({ state: "attached" });

  const menuItems = await slashMenuEl
    .locator(".bn-suggestion-menu-item p:first-child")
    .all();

  const menuItemsText = await Promise.all(
    menuItems.map((item) => item.textContent()),
  );

  expect(menuItemsText).toContain("Table");
  expect(menuItemsText).toContain("Quote");
});

// eslint-disable-next-line max-statements
test("Disallowed syntax features should be unavailable", async ({
  mount,
  page,
}) => {
  const syntax = structuredClone(FULL_SYNTAX);
  syntax.features.blocks.tables.basicTables = false;
  syntax.features.blocks.quotes = false;

  const component = await mount(
    <BlockNoteForTest macros={false} content={[]} syntax={syntax} />,
  );

  const editorEl = component.locator(".bn-editor");

  await editorEl.press("/");

  const slashMenuEl = page.locator(
    "[data-floating-ui-portal] .bn-suggestion-menu",
  );

  await slashMenuEl.waitFor({ state: "attached" });

  const menuItems = await slashMenuEl
    .locator(".bn-suggestion-menu-item p:first-child")
    .all();

  const menuItemsText = await Promise.all(
    menuItems.map((item) => item.textContent()),
  );

  expect(menuItemsText).not.toContain("Table");
  expect(menuItemsText).not.toContain("Quote");
});

// eslint-disable-next-line max-statements
test("Macros can be inserted", async ({ mount, page }) => {
  let macroInsertionModalTriggered = false;

  const component = await mount(
    <BlockNoteForTest
      content={[
        {
          id: Math.random().toString(),
          type: "paragraph",
          children: [],
          content: [{ type: "text", text: "Yeah", styles: {} }],
          props: {
            backgroundColor: "default",
            textAlignment: "left",
            textColor: "default",
          },
        },
      ]}
      macros={{
        ctx: {
          openParamsEditor() {
            throw new Error("Unreachable");
          },

          openInsertionEditor() {
            macroInsertionModalTriggered = true;
          },
        },
        list: macros,
      }}
      syntax={FULL_SYNTAX}
    />,
  );

  const editorEl = component.locator(".bn-editor");

  const paragraph = editorEl.locator(
    // 'div.bn-block-content[data-content-type="paragraph"]',
    "p.bn-inline-content",
  );

  await paragraph.waitFor({ state: "attached" });

  // Trigger the formatting toolbar
  await paragraph.dblclick();
  const macroInsertBtnEl = page.locator('button[data-test="insertMacro"]');
  await macroInsertBtnEl.waitFor({ state: "attached" });

  expect(macroInsertionModalTriggered).toBe(false);
  await macroInsertBtnEl.click();
  expect(macroInsertionModalTriggered).toBe(true);
});

function buildParagraphs(blocks: string[]): BlockType[] {
  return blocks.map((blockText) => ({
    id: Math.random().toString(),
    type: "paragraph",
    props: {
      backgroundColor: "default",
      textAlignment: "left",
      textColor: "default",
    },
    content: [
      {
        type: "text",
        text: blockText,
        styles: {},
      },
    ],
    children: [],
  }));
}

function buildImage(url: string): BlockOfType<"image"> {
  return {
    type: "image",
    props: {
      url,
      backgroundColor: "default",
      caption: "",
      name: "",
      previewWidth: 100,
      showPreview: true,
      textAlignment: "left",
    },
    children: [],
    content: undefined,
    id: Math.random().toString(),
  };
}

const SMALL_IMG_DATA_URL =
  "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

const FULL_SYNTAX: SyntaxConfig = {
  id: "default-syntax",
  features: {
    blocks: {
      headings: {
        levels1To3: true,
        levels4To6: true,
      },
      images: {
        basicImages: true,
        altText: true,
        caption: true,
        customBorder: true,
        customDimensions: true,
        insideLinks: true,
      },
      lists: {
        bulletLists: true,
        blockInListItems: true,
        checkableLists: true,
        contiguousNumberedLists: true,
        contiguousNumberedListsAnyStartIndex: true,
        mixableCheckableListItems: true,
        multipleBlocksInListItems: true,
        unorderedNumberedLists: true,
        listsNesting: true,
      },
      quotes: true,
      code: {
        basicCodeBlocks: true,
        language: true,
      },
      dividers: true,
      macros: true,
      nesting: true,
      styling: {
        justifyAlignment: true,
        lcrAlignment: true,
      },
      tables: {
        basicTables: true,
        blockInTableCells: true,
        colRows: true,
        colSpan: true,
        headerColumns: true,
        multipleBlocksInTableCells: true,
        multipleFooterRows: true,
        multipleHeaderRows: true,
        noHeaderRowTable: true,
        singleFooterRow: true,
        singleHeaderRow: true,
      },
    },
    inlineContents: {
      images: true,
      links: {
        basicLinks: true,
        customText: true,
        customTextStyling: true,
        descriptiveTooltip: true,
        metadata: true,
      },
      code: {
        basicInlineCode: true,
        language: true,
      },
      macros: true,
      rawHtml: true,
      textStyles: {
        bold: true,
        italic: true,
        strikethrough: true,
        underline: true,
        nesting: true,
        fontFamily: true,
        fontSize: true,
        subscript: true,
        superscript: true,
      },
    },
  },
};
const macros: MacroWithUnknownParamsType[] = [
  {
    renderAs: "block",
    infos: {
      id: "sample-macro",
      name: "Sample Macro",
      description: "A sample macro",
      params: {},
      bodyType: "none",
      defaultParameters: {},
      paramsDescription: {},
    },
    render: () => [],
  },
];

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
import { mountBlockNoteHeadless } from "./BlockNote.story";
import { FULL_SYNTAX } from "./syntax.mock";
import { buildParagraphs, fillDisplayText, pressKeySettled } from "./utils";
import { expect, test } from "@playwright/experimental-ct-vue";
import type { BlockType } from "@xwiki/platform-editors-blocknote-react";

// The Playwright page type, taken from the test fixtures since the component testing package doesn't export it.
type Page = Parameters<Parameters<typeof test>[2]>[0]["page"];

/**
 * @param runs - the text runs of the paragraph, with the bold ones marked
 * @returns a single paragraph holding the given text runs
 */
function buildFormattedParagraph(
  runs: { text: string; bold?: boolean }[],
): BlockType[] {
  return [
    {
      id: "formatted",
      type: "paragraph",
      props: {
        backgroundColor: "default",
        textAlignment: "left",
        textColor: "default",
      },
      content: runs.map(({ text, bold }) => ({
        type: "text",
        text,
        styles: bold ? { bold: true } : {},
      })),
      children: [],
    },
  ];
}

/**
 * Mounts the editor with the given content, waiting for its (first) paragraph.
 *
 * @returns the editor and its first paragraph
 */
async function mountParagraph(
  mount: Parameters<typeof mountBlockNoteHeadless>[0],
  editorContent: BlockType[],
): Promise<{
  editorEl: ReturnType<Page["locator"]>;
  paragraph: ReturnType<Page["locator"]>;
}> {
  const component = await mountBlockNoteHeadless(mount, {
    editorContent,
    editorProps: { syntax: FULL_SYNTAX },
    macros: false,
  });
  const editorEl = component.locator(".bn-editor");
  const paragraph = editorEl.locator("p.bn-inline-content");
  await paragraph.waitFor({ state: "attached" });
  return { editorEl, paragraph };
}

/**
 * Selects, with the keyboard, the given number of characters starting at the given offset of the paragraph, then opens
 * the link modal from the formatting toolbar and sets the target to the given URL.
 */
async function openLinkModalOnSelection(
  page: Page,
  paragraph: ReturnType<Page["locator"]>,
  offset: number,
  length: number,
  url: string,
): Promise<void> {
  // Each key press is awaited until it is painted (see pressKeySettled) so that sending them in quick succession
  // doesn't outrun the editor's selection update. The paragraph is clicked at its start, where Home moves the caret:
  // the editor can apply the caret position of the click after a key press that follows it closely (e.g. on a loaded
  // machine), which would undo a Home pressed after a click elsewhere in the paragraph.
  await paragraph.click({ position: { x: 1, y: 1 } });
  await pressKeySettled(page, "Home");
  for (let i = 0; i < offset; i++) {
    await pressKeySettled(page, "ArrowRight");
  }
  for (let i = 0; i < length; i++) {
    await pressKeySettled(page, "Shift+ArrowRight");
  }

  // The formatting toolbar is rendered via FloatingPortal into document.body.
  await page.locator('button[data-test="createLink"]').click();

  // The link modal defaults to the "Page" target type, switch it to "URL".
  await page.locator('[data-test="linkTargetType"]').selectOption("URL");
  await page.locator('[data-test="linkUrl"]').fill(url);
}

test("Creating a link on a word in the middle of a line keeps the text intact", async ({
  mount,
  page,
}) => {
  const component = await mountBlockNoteHeadless(mount, {
    editorContent: buildParagraphs(["First second third fourth"]),
    editorProps: {
      syntax: FULL_SYNTAX,
    },
    macros: false,
  });

  const editorEl = component.locator(".bn-editor");
  const paragraph = editorEl.locator("p.bn-inline-content");
  await paragraph.waitFor({ state: "attached" });

  // Select the word "second" (offsets 6 to 12) with the keyboard and link it to a URL.
  await openLinkModalOnSelection(page, paragraph, 6, 6, "https://xwiki.org");

  const submitButtonEl = page.locator('[data-test="linkSubmit"]');
  await submitButtonEl.click();

  // The link must be inserted around the selected word...
  const linkEl = editorEl.locator('a[href="https://xwiki.org"]');
  await expect(linkEl).toHaveText("second");

  // ...and the rest of the line must be intact.
  await expect(editorEl).toHaveText("First second third fourth");
});

test("Creating a link on a selection keeps its inline formatting when the display text is left unchanged", async ({
  mount,
  page,
}) => {
  const { editorEl, paragraph } = await mountParagraph(
    mount,
    buildFormattedParagraph([
      { text: "First se" },
      { text: "con", bold: true },
      { text: "d third" },
    ]),
  );

  // Select "second", whose middle is bold: the display text is pre-filled with the selected text.
  await openLinkModalOnSelection(page, paragraph, 6, 6, "https://xwiki.org");
  await expect(page.locator("[data-test='linkDisplayText']")).toHaveValue(
    "second",
  );
  await page.locator('[data-test="linkSubmit"]').click();

  // The selection is linked in place, keeping its bold part (the link is rendered as one element per formatting run).
  const linkEl = editorEl.locator('a[href="https://xwiki.org"]');
  await expect(linkEl).toHaveText(["se", "con", "d"]);
  await expect(editorEl.locator("strong")).toHaveText("con");
  await expect(editorEl).toHaveText("First second third");
});

test("Creating a link on a selection uses the display text typed in the link modal", async ({
  mount,
  page,
}) => {
  const { editorEl, paragraph } = await mountParagraph(
    mount,
    buildFormattedParagraph([
      { text: "First " },
      { text: "second", bold: true },
      { text: " third" },
    ]),
  );

  // Select the bold word "second" and change the display text.
  await openLinkModalOnSelection(page, paragraph, 6, 6, "https://xwiki.org");
  await fillDisplayText(page, "2nd");
  await page.locator('[data-test="linkSubmit"]').click();

  // The selection is replaced with the display text, which keeps the formatting of the whole selection.
  const linkEl = editorEl.locator('a[href="https://xwiki.org"]');
  await expect(linkEl).toHaveText("2nd");
  await expect(editorEl.locator("strong")).toHaveText("2nd");
  await expect(editorEl).toHaveText("First 2nd third");
});

test("Creating a link on a selection with an empty display text replaces the selection with the generated label", async ({
  mount,
  page,
}) => {
  const { editorEl, paragraph } = await mountParagraph(
    mount,
    buildParagraphs(["First second third"]),
  );

  // Select "second", empty the display text and link it to a URL.
  await openLinkModalOnSelection(page, paragraph, 6, 6, "https://xwiki.org");
  await fillDisplayText(page, "");
  await page.locator('[data-test="linkSubmit"]').click();

  // The selection is replaced with the label generated from the link target.
  const linkEl = editorEl.locator('a[href="https://xwiki.org"]');
  await expect(linkEl).toHaveText("https://xwiki.org");
  await expect(editorEl).toHaveText("First https://xwiki.org third");
  await expect(
    page.locator('[data-test="submittedLinkGeneratedTitle"]'),
  ).toHaveText("true");
});

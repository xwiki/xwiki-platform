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
import { buildParagraphs, pressKeySettled } from "./utils";
import { expect, test } from "@playwright/experimental-ct-vue";

test("Creating a link on a word in the middle of a line keeps the text intact", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
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

  // Select the word "second" (offsets 6 to 12) with the keyboard. Each key press is awaited until it is painted
  // (see pressKeySettled) so that sending them in quick succession doesn't outrun the editor's selection update.
  await paragraph.click();
  await pressKeySettled(page, "Home");
  for (let i = 0; i < 6; i++) {
    await pressKeySettled(page, "ArrowRight");
  }
  for (let i = 0; i < 6; i++) {
    await pressKeySettled(page, "Shift+ArrowRight");
  }

  // The formatting toolbar is rendered via FloatingPortal into document.body.
  const linkButtonEl = page.locator('button[data-test="createLink"]');
  await linkButtonEl.waitFor({ state: "attached" });
  await linkButtonEl.click();

  // The link modal defaults to the "Page" target type, switch it to "URL".
  const targetTypeSelectEl = page.locator('[data-test="linkTargetType"]');
  await targetTypeSelectEl.waitFor({ state: "attached" });
  await targetTypeSelectEl.selectOption("URL");

  const urlInputEl = page.locator('[data-test="linkUrl"]');
  await urlInputEl.waitFor({ state: "attached" });
  await urlInputEl.fill("https://xwiki.org");

  const submitButtonEl = page.locator('[data-test="linkSubmit"]');
  await submitButtonEl.click();

  // The link must be inserted around the selected word...
  const linkEl = editorEl.locator('a[href="https://xwiki.org"]');
  await expect(linkEl).toHaveText("second");

  // ...and the rest of the line must be intact.
  await expect(editorEl).toHaveText("First second third fourth");
});

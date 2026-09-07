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
import { pressKeySettled } from "./utils";
import { expect, test } from "@playwright/experimental-ct-vue";

test("Inserting a link from the slash menu focuses the display text field", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const component = await mountBlockNoteHeadless(mount, {
    editorContent: [],
    editorProps: {
      syntax: FULL_SYNTAX,
    },
    macros: false,
  });

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();

  await page.keyboard.type("/link");

  const slashMenuEl = page.locator(
    "[data-floating-ui-portal] .bn-suggestion-menu",
  );
  await slashMenuEl.waitFor({ state: "attached" });
  await expect(
    slashMenuEl.locator(".bn-suggestion-menu-item p:first-child"),
  ).toHaveText("Link");

  // Select the (only remaining) "Link" item, opening the link modal. The rich text editor still holds focus right
  // after this (it just handled the Enter keypress), which is exactly the race LinkConfig.vue's onMounted guards
  // against: the display text field must reclaim focus once settled.
  await pressKeySettled(page, "Enter");

  const titleInputEl = page.locator('[data-test="linkDisplayText"]');
  await titleInputEl.waitFor({ state: "attached" });
  await expect(titleInputEl).toBeFocused();

  await titleInputEl.fill("XWiki");

  const targetTypeSelectEl = page.locator('[data-test="linkTargetType"]');
  await targetTypeSelectEl.waitFor({ state: "attached" });
  await targetTypeSelectEl.selectOption("URL");

  const urlInputEl = page.locator('[data-test="linkUrl"]');
  await urlInputEl.waitFor({ state: "attached" });
  await urlInputEl.fill("https://xwiki.org");

  const submitButtonEl = page.locator('[data-test="linkSubmit"]');
  await submitButtonEl.click();

  // The "/link" trigger text is replaced by the newly created link.
  const linkEl = editorEl.locator('a[href="https://xwiki.org"]');
  await expect(linkEl).toHaveText("XWiki");
  await expect(editorEl).toHaveText("XWiki");
});

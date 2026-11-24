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
import { MentionSuggestionForTest } from "./MentionSuggestion.story";
import { expect, test } from "@playwright/experimental-ct-react";
import type { Locator, Page } from "@playwright/test";

const USERS = [
  { reference: "XWiki.Admin", label: "Administrator", hint: "admin" },
  { reference: "XWiki.Alice", label: "Alice", hint: "alice@example.com" },
];

/**
 * Types the mention trigger, then the given query. The trigger is pressed on its own because it opens the menu, which
 * the query then filters.
 */
async function startMention(page: Page, editorEl: Locator, query = "") {
  await editorEl.click();
  await page.keyboard.press("@");
  if (query) {
    await page.keyboard.type(query);
  }
}

/**
 * @returns the content of the editor, as the test story renders it
 */
async function getContent(page: Page) {
  return JSON.parse((await page.getByTestId("content").textContent()) || "[]");
}

/**
 * @returns the inline contents of the first block, a macro call being reduced to the call it holds and a text to its
 *   text, so that the assertions read as the content does
 */
async function getInlineContents(page: Page) {
  const [paragraph] = await getContent(page);
  return paragraph.content.map(
    (content: { type: string; text?: string; props?: { call: string } }) =>
      content.type === "xwikiInlineMacro"
        ? JSON.parse(content.props!.call)
        : content.text,
  );
}

test("The mention menu suggests the users of the host application", async ({
  mount,
  page,
}) => {
  const component = await mount(<MentionSuggestionForTest users={USERS} />);

  await startMention(page, component.locator(".bn-editor"));

  const items = page.locator(".bn-suggestion-menu-item");
  await expect(items).toHaveCount(2);
  await expect(items.nth(0)).toContainText("Administrator");
  await expect(
    items.nth(0).locator(".bn-mt-suggestion-menu-item-subtitle"),
  ).toHaveText("admin");
  await expect(items.nth(1)).toContainText("Alice");
  await expect(page.locator(".bn-suggestion-menu-label")).toHaveText(
    "blocknote.mentionMenu.group",
  );
});

test("Picking a user inserts the mention rendered for it", async ({
  mount,
  page,
}) => {
  const component = await mount(<MentionSuggestionForTest users={USERS} />);

  await startMention(page, component.locator(".bn-editor"), "ali");
  await expect(
    page.locator('.bn-suggestion-menu-item[aria-selected="true"]'),
  ).toContainText("Alice");
  await page.keyboard.press("Enter");

  // The query typed after the trigger is replaced by the macro call the renderer of the host application returned,
  // displayed as a placeholder until the server renders it.
  await expect(component.locator(".bn-editor")).toContainText("macro:mention");
  expect(await getInlineContents(page)).toEqual([
    { name: "mention", parameters: { reference: "XWiki.Alice" } },
    " ",
  ]);
});

test("The Mention item of the slash menu opens the mention menu", async ({
  mount,
  page,
}) => {
  const component = await mount(<MentionSuggestionForTest users={USERS} />);
  const editorEl = component.locator(".bn-editor");

  await editorEl.click();
  await page.keyboard.press("/");
  await page.keyboard.type("mention");
  await page
    .locator(".bn-suggestion-menu-item", {
      hasText: "blocknote.slashMenu.mention.title",
    })
    .click();

  // The menu opens on an empty query, so it shows every user rather than waiting for one to be typed.
  await expect(page.locator(".bn-suggestion-menu-item")).toHaveCount(2);
  await page.keyboard.press("Enter");

  // Neither the slash menu query nor the trigger that opened the mention menu is left in the content.
  expect(await getInlineContents(page)).toEqual([
    { name: "mention", parameters: { reference: "XWiki.Admin" } },
    " ",
  ]);
});

test("The mention menu doesn't open when the trigger doesn't start a word", async ({
  mount,
  page,
}) => {
  const component = await mount(<MentionSuggestionForTest users={USERS} />);

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type("john.doe");
  await page.keyboard.press("@");
  await page.keyboard.type("example.com");

  await expect(page.locator(".bn-suggestion-menu")).toHaveCount(0);
});

test("The mention menu stays empty when the user suggestions fail", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <MentionSuggestionForTest users={USERS} failingSuggestions />,
  );

  await startMention(page, component.locator(".bn-editor"), "ali");

  // The menu shows BlockNote's "no items" placeholder, which carries no item title.
  await expect(page.locator(".bn-suggestion-menu")).toBeVisible();
  await expect(page.locator(".bn-mt-suggestion-menu-item-title")).toHaveCount(
    0,
  );
});

test("There is no mention menu without a user suggester", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <MentionSuggestionForTest noUserSuggester users={USERS} />,
  );

  await startMention(page, component.locator(".bn-editor"), "ali");

  await expect(page.locator(".bn-suggestion-menu")).toHaveCount(0);
});

test("There is no mention menu without a mention renderer", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <MentionSuggestionForTest noMentionRenderer users={USERS} />,
  );

  await startMention(page, component.locator(".bn-editor"), "ali");

  await expect(page.locator(".bn-suggestion-menu")).toHaveCount(0);
});

test("There is no mention menu when the syntax can't save a mention", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <MentionSuggestionForTest syntaxWithoutMentions users={USERS} />,
  );

  await startMention(page, component.locator(".bn-editor"), "ali");

  await expect(page.locator(".bn-suggestion-menu")).toHaveCount(0);
});

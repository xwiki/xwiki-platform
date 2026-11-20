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

test("Picking a user hands it to the mention inserter", async ({
  mount,
  page,
}) => {
  const component = await mount(<MentionSuggestionForTest users={USERS} />);

  await startMention(page, component.locator(".bn-editor"), "ali");
  await expect(
    page.locator('.bn-suggestion-menu-item[aria-selected="true"]'),
  ).toContainText("Alice");
  await page.keyboard.press("Enter");

  // The query typed after the trigger is replaced by what the inserter of the host application writes.
  await expect(component.locator(".bn-editor")).toContainText("@XWiki.Alice");
  const [paragraph] = await getContent(page);
  expect(paragraph.content.map(({ text }: { text: string }) => text)).toEqual([
    "@XWiki.Alice",
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

test("There is no mention menu without a mention inserter", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <MentionSuggestionForTest noMentionInserter users={USERS} />,
  );

  await startMention(page, component.locator(".bn-editor"), "ali");

  await expect(page.locator(".bn-suggestion-menu")).toHaveCount(0);
});

test("There is no mention menu when the edited content can't hold a mention", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <MentionSuggestionForTest cannotInsert users={USERS} />,
  );

  await startMention(page, component.locator(".bn-editor"), "ali");

  await expect(page.locator(".bn-suggestion-menu")).toHaveCount(0);
});

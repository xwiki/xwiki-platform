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
import { BlockNoteWithFeature } from "./EditorFeature.story";
import { clickSettled, pressKeySettled } from "./utils";
import { expect, test } from "@playwright/experimental-ct-react";
import type { BlockType } from "../../blocknote";
import type { Locator, Page } from "@playwright/test";

function block(type: "paragraph" | "quote", text: string): BlockType {
  return {
    id: type,
    type,
    props: {
      backgroundColor: "default",
      textAlignment: "left",
      textColor: "default",
    },
    content: [{ type: "text", text, styles: {} }],
    children: [],
  } as BlockType;
}

const CONTENT = [block("paragraph", "Some text"), block("quote", "A quote")];

/**
 * Selects the text of the paragraph, which shows the formatting toolbar.
 */
async function selectParagraph(page: Page, editorEl: Locator): Promise<void> {
  await clickSettled(page, editorEl.locator("p.bn-inline-content"));
  await pressKeySettled(page, "Home");
  await pressKeySettled(page, "Shift+End");
}

/**
 * Selects the text of the quote, which follows the paragraph, with the keyboard, since the formatting toolbar of the
 * paragraph can cover it.
 */
async function selectQuote(page: Page, editorEl: Locator): Promise<void> {
  await clickSettled(page, editorEl.locator("p.bn-inline-content"));
  await pressKeySettled(page, "ArrowDown");
  await pressKeySettled(page, "Home");
  await pressKeySettled(page, "Shift+End");
}

/**
 * Opens the slash menu in a new paragraph after the quote, filtered with the given query.
 */
async function openSlashMenu(
  page: Page,
  editorEl: Locator,
  query: string,
): Promise<void> {
  await clickSettled(page, editorEl.locator("p.bn-inline-content"));
  await pressKeySettled(page, "ArrowDown");
  await pressKeySettled(page, "End");
  await pressKeySettled(page, "Enter");
  await page.keyboard.type(`/${query}`);
  await expect(page.locator(".bn-suggestion-menu")).toBeVisible();
}

test("The editor renders the components and the formatting toolbar items of a feature bound in the service container", async ({
  mount,
  page,
}) => {
  const component = await mount(<BlockNoteWithFeature content={CONTENT} />);

  await expect(page.locator('[data-test="testFeatureComponent"]')).toHaveText(
    "Rendered",
  );
  await selectParagraph(page, component.locator(".bn-editor"));
  await expect(page.locator('[data-test="testFeatureButton"]')).toBeVisible();
});

test("A feature can replace the formatting toolbar for a block type", async ({
  mount,
  page,
}) => {
  const component = await mount(<BlockNoteWithFeature content={CONTENT} />);

  await selectQuote(page, component.locator(".bn-editor"));
  await expect(page.locator('[data-test="quoteToolbar"]')).toBeVisible();
  await expect(
    page.locator('[data-test="testFeatureButton"]'),
  ).not.toBeAttached();
});

test("The slash menu offers the items of a feature", async ({
  mount,
  page,
}) => {
  const component = await mount(<BlockNoteWithFeature content={CONTENT} />);
  const editorEl = component.locator(".bn-editor");

  await openSlashMenu(page, editorEl, "greeting");
  await page.getByText("Insert greeting").click();
  await expect(editorEl).toContainText("Hello");
});

test("A disabled feature contributes nothing", async ({ mount, page }) => {
  const component = await mount(
    <BlockNoteWithFeature content={CONTENT} disabled />,
  );
  const editorEl = component.locator(".bn-editor");

  await selectParagraph(page, editorEl);
  await expect(page.locator('[data-test="subscript"]')).toBeVisible();
  await expect(
    page.locator('[data-test="testFeatureButton"]'),
  ).not.toBeAttached();
  await expect(
    page.locator('[data-test="testFeatureComponent"]'),
  ).not.toBeAttached();

  await selectQuote(page, editorEl);
  await expect(page.locator('[data-test="subscript"]')).toBeVisible();
  await expect(page.locator('[data-test="quoteToolbar"]')).not.toBeAttached();
});

test("The slash menu doesn't offer the items of a disabled feature", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <BlockNoteWithFeature content={CONTENT} disabled />,
  );

  await openSlashMenu(page, component.locator(".bn-editor"), "greeting");
  await expect(page.getByText("Insert greeting")).not.toBeAttached();
});

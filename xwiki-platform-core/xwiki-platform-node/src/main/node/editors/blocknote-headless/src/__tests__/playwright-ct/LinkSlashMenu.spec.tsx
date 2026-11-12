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
import { EntityType } from "@xwiki/platform-model-api";
import type { LinkContext } from "@xwiki/platform-link-type-api";
import type { DocumentReference } from "@xwiki/platform-model-api";

// The Playwright page and locator types, taken from the test fixtures since the component testing package doesn't
// export them.
type Page = Parameters<Parameters<typeof test>[2]>[0]["page"];
type Locator = ReturnType<Page["locator"]>;

/**
 * Open the link modal from the slash menu of an empty editor.
 *
 * @returns the editor
 */
async function openLinkModalFromSlashMenu(
  mount: Parameters<typeof mountBlockNoteHeadless>[0],
  page: Page,
  linkContext?: LinkContext,
): Promise<Locator> {
  const component = await mountBlockNoteHeadless(mount, {
    editorContent: [],
    editorProps: {
      syntax: FULL_SYNTAX,
    },
    macros: false,
    linkContext,
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

  // Select the (only remaining) "Link" item, opening the link modal.
  await pressKeySettled(page, "Enter");

  return editorEl;
}

test("Inserting a link from the slash menu focuses the link target field, and labels the link with its URL by default", async ({
  mount,
  page,
}) => {
  const editorEl = await openLinkModalFromSlashMenu(mount, page);

  // The rich text editor still holds focus right after the modal opens (it just handled the Enter keypress), which is
  // exactly the race LinkForm.vue's onMounted guards against: the target field must reclaim focus once settled.
  const targetInputEl = page.locator(".link-target input");
  await targetInputEl.waitFor({ state: "attached" });
  await expect(targetInputEl).toBeFocused();

  await page.locator('[data-test="linkTargetType"]').selectOption("URL");
  await page.locator('[data-test="linkUrl"]').fill("https://xwiki.org");
  // The display text is left empty.
  await page.locator('[data-test="linkSubmit"]').click();

  // The "/link" trigger text is replaced by the newly created link.
  const linkEl = editorEl.locator('a[href="https://xwiki.org"]');
  await expect(linkEl).toHaveText("https://xwiki.org");
  await expect(editorEl).toHaveText("https://xwiki.org");
});

test("Inserting a URL link without a scheme completes the URL when leaving the URL field", async ({
  mount,
  page,
}) => {
  const editorEl = await openLinkModalFromSlashMenu(mount, page);

  await page.locator('[data-test="linkTargetType"]').selectOption("URL");
  const urlInputEl = page.locator('[data-test="linkUrl"]');
  await urlInputEl.fill("www.xwiki.org/download");
  await urlInputEl.press("Tab");
  await expect(urlInputEl).toHaveValue("https://www.xwiki.org/download");

  await page.locator('[data-test="linkSubmit"]').click();

  await expect(
    editorEl.locator('a[href="https://www.xwiki.org/download"]'),
  ).toHaveText("https://www.xwiki.org/download");
  await expect(page.locator('[data-test="submittedLinkReference"]')).toHaveText(
    JSON.stringify({
      type: "url",
      typed: false,
      reference: "https://www.xwiki.org/download",
      parameters: {},
    }),
  );
});

test("Inserting a relative URL link keeps the URL as typed, with a typed path reference", async ({
  mount,
  page,
}) => {
  const editorEl = await openLinkModalFromSlashMenu(mount, page);

  await page.locator('[data-test="linkTargetType"]').selectOption("URL");
  const urlInputEl = page.locator('[data-test="linkUrl"]');
  await urlInputEl.fill("/xwiki/bin/view/Main/");
  await urlInputEl.press("Tab");
  await expect(urlInputEl).toHaveValue("/xwiki/bin/view/Main/");

  await page.locator('[data-test="linkSubmit"]').click();

  await expect(editorEl.locator('a[href="/xwiki/bin/view/Main/"]')).toHaveText(
    "/xwiki/bin/view/Main/",
  );
  // XWiki Rendering parses a URL reference without a scheme back as a document reference, even when typed.
  await expect(page.locator('[data-test="submittedLinkReference"]')).toHaveText(
    JSON.stringify({
      type: "path",
      typed: true,
      reference: "/xwiki/bin/view/Main/",
      parameters: {},
    }),
  );
});

test("Inserting a page link from the slash menu labels the link with the title of the picked page by default", async ({
  mount,
  page,
}) => {
  const editorEl = await openLinkModalFromSlashMenu(mount, page);

  // The link modal defaults to the "Page" target type, whose target field is a search box.
  await page.locator(".link-target input").fill("some");
  await page
    .locator(".link-target li.suggestion", { hasText: "some page label" })
    .click();
  // The display text is left empty.
  await page.locator('[data-test="linkSubmit"]').click();

  const linkEl = editorEl.locator('a[href="https://example.org/some/page"]');
  await expect(linkEl).toHaveText("some page label");
  // The label is reported as generated, so that the integration doesn't save it.
  await expect(
    page.locator('[data-test="submittedLinkGeneratedTitle"]'),
  ).toHaveText("true");
});

test("Inserting an attachment link from the slash menu requires an attachment", async ({
  mount,
  page,
}) => {
  await openLinkModalFromSlashMenu(mount, page);

  await page.locator('[data-test="linkTargetType"]').selectOption("Attachment");
  const targetInputEl = page.locator(".link-target input");
  await expect(targetInputEl).toHaveAttribute("required");

  // The browser doesn't submit the form while the attachment reference is empty: the modal stays open.
  await page.locator('[data-test="linkSubmit"]').click();
  await expect(targetInputEl).toBeVisible();
  expect(
    await targetInputEl.evaluate(
      (input: HTMLInputElement) => input.validity.valueMissing,
    ),
  ).toBe(true);
});

test("Inserting a link to a section of the current page, leaving the page reference empty", async ({
  mount,
  page,
}) => {
  // Plain data, since the props are serialized to reach the browser.
  const editorEl = await openLinkModalFromSlashMenu(mount, page, {
    sourceDocument: {
      type: EntityType.DOCUMENT,
      name: "Current",
    } as unknown as DocumentReference,
  });

  // The page reference is left empty, as the hint of the field suggests, and only the anchor is set.
  await expect(page.locator(".link-target")).toContainText(
    "Leave empty to link to the current page.",
  );
  await page.locator("[data-test='linkOptions'] > summary").click();
  await page.locator('[data-test="linkPageAnchor"]').fill("HSection");
  await page.locator('[data-test="linkSubmit"]').click();

  // The link points to the section of the current page and is labelled with the name of the current page.
  await expect(
    editorEl.locator('a[href="https://example.org/some/page#HSection"]'),
  ).toHaveText("Current");
  // An empty untyped reference designates the current page.
  await expect(page.locator('[data-test="submittedLinkReference"]')).toHaveText(
    JSON.stringify({
      type: "doc",
      typed: false,
      reference: "",
      parameters: { anchor: "HSection" },
    }),
  );
});

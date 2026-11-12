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
import { LinkSuggestionForTest } from "./LinkSuggestion.story";
import { FULL_SYNTAX } from "./syntax.mock";
import { expect, test } from "@playwright/experimental-ct-react";
import type { Locator, Page } from "@playwright/test";

const LINK_TO = "blocknote.linkSuggestion.linkTo";
const UPLOAD = "blocknote.linkSuggestion.upload";

/**
 * Opens the link suggestion menu with the link quick action, i.e. the "Link" item of the slash menu.
 */
async function openLinkQuickAction(page: Page, editorEl: Locator) {
  await editorEl.click();
  await page.keyboard.type("/link");
  await expect(
    page.locator('.bn-suggestion-menu-item[aria-selected="true"]'),
  ).toContainText("blocknote.slashMenu.link.title");
  await page.keyboard.press("Enter");

  // The slash menu is replaced by the link suggestion menu (the slash menu stays attached for a
  // moment while it closes).
  await page
    .locator(".bn-suggestion-menu-item", { hasText: LINK_TO })
    .waitFor();
  await expect(page.locator(".bn-suggestion-menu")).toHaveCount(1);
}

/**
 * @returns the links received by the link edition hooks before being inserted
 */
async function getInsertedLinks(page: Page) {
  return JSON.parse(
    (await page.getByTestId("insertedLinks").textContent()) ?? "[]",
  );
}

/**
 * @param type - the resource type
 * @param reference - the serialized entity reference, relative to the edited page ("Some.Page")
 * @returns the expected resource reference of a link: typed, since a link designates a page by default
 */
function linkReference(type: string, reference: string) {
  return { type, typed: true, reference, parameters: {} };
}

test("The link quick action shows the link editor and upload actions, then the matching pages and attachments", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const component = await mount(
    <LinkSuggestionForTest
      pages={[
        { label: "Cats", url: "/cats", hint: "Animals" },
        { label: "Dogs", url: "/dogs" },
      ]}
      attachments={[
        { label: "cats.pdf", url: "/cats.pdf", hint: "Animals / Cats" },
      ]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await openLinkQuickAction(page, editorEl);

  const menuEl = page.locator(".bn-suggestion-menu");
  const items = menuEl.locator(".bn-suggestion-menu-item");
  await expect(items).toHaveCount(5);

  // The first item opens the link editor, initially selected so it stays keyboard-navigable, and the second one uploads
  // a file to link to.
  await expect(items.nth(0)).toHaveAttribute("aria-selected", "true");
  await expect(items.nth(0)).toContainText(LINK_TO);
  await expect(items.nth(1)).toContainText(UPLOAD);

  // The pages, then the attachments, each in their own group, with their location under them.
  await expect(menuEl.locator(".bn-suggestion-menu-label")).toHaveText([
    "blocknote.linkSuggestion.pagesGroup",
    "blocknote.linkSuggestion.attachmentsGroup",
  ]);
  await expect(items.nth(2)).toContainText("Cats");
  await expect(
    items.nth(2).locator(".bn-mt-suggestion-menu-item-subtitle"),
  ).toHaveText("Animals");
  await expect(items.nth(3)).toContainText("Dogs");
  await expect(
    items.nth(3).locator(".bn-mt-suggestion-menu-item-subtitle"),
  ).toBeEmpty();
  await expect(items.nth(4)).toContainText("cats.pdf");
  await expect(
    items.nth(4).locator(".bn-mt-suggestion-menu-item-subtitle"),
  ).toHaveText("Animals / Cats");
});

/**
 * @returns the given number of suggested pages or attachments
 */
function suggestedLinks(count: number, kind: "page" | "file") {
  return Array.from({ length: count }, (_, i) => ({
    label: `${kind}${i}`,
    url: `/${kind}${i}`,
  }));
}

/**
 * Opens the link suggestion menu and expects it to show the two actions, followed by the given number of pages and
 * attachments, the best matches of each kind.
 */
async function expectSuggestedCounts(
  page: Page,
  editorEl: Locator,
  pageCount: number,
  attachmentCount: number,
) {
  await openLinkQuickAction(page, editorEl);
  const items = page.locator(".bn-suggestion-menu .bn-suggestion-menu-item");
  await expect(items).toHaveCount(2 + pageCount + attachmentCount);
  await expect(items.nth(2 + pageCount - 1)).toContainText(
    `page${pageCount - 1}`,
  );
  await expect(items.nth(2 + pageCount)).toContainText("file0");
  await expect(items.last()).toContainText(`file${attachmentCount - 1}`);
}

test("5 pages and 3 attachments are suggested when there are enough matches of both kinds", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <LinkSuggestionForTest
      pages={suggestedLinks(8, "page")}
      attachments={suggestedLinks(8, "file")}
    />,
  );

  await expectSuggestedCounts(page, component.locator(".bn-editor"), 5, 3);
});

test("More pages are suggested when there are too few matching attachments", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <LinkSuggestionForTest
      pages={suggestedLinks(8, "page")}
      attachments={suggestedLinks(2, "file")}
    />,
  );

  await expectSuggestedCounts(page, component.locator(".bn-editor"), 6, 2);
});

test("More attachments are suggested when there are too few matching pages", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <LinkSuggestionForTest
      pages={suggestedLinks(3, "page")}
      attachments={suggestedLinks(6, "file")}
    />,
  );

  await expectSuggestedCounts(page, component.locator(".bn-editor"), 3, 5);
});

test("Selecting a page suggestion inserts a link to it, with a label generated from the page", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const component = await mount(
    <LinkSuggestionForTest
      pages={[{ label: "Cats", url: "/cats", reference: "Animals.Cats" }]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await openLinkQuickAction(page, editorEl);

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item", { hasText: "Cats" }).click();

  // The link is inserted as rewritten by the link edition hooks.
  const linkEl = editorEl.locator("a");
  await expect(linkEl).toHaveText("Cats");
  await expect(linkEl).toHaveAttribute("href", "/cats#inserted");
  // The hooks are told about the reference of the page, and that the label is generated from it, so that the link can
  // be saved without a label.
  expect(await getInsertedLinks(page)).toEqual([
    {
      title: "Cats",
      generatedTitle: true,
      url: "/cats",
      reference: linkReference("doc", "Animals.Cats"),
    },
  ]);

  // The menu is closed, no text is left behind (neither the slash menu's query nor the link suggestion menu's
  // trigger), and the user can keep typing after the link.
  await expect(menuEl).not.toBeAttached();
  await expect(editorEl).not.toContainText("[[");
  await expect(editorEl).not.toContainText("link");
  await page.keyboard.type(" and dogs");
  await expect(editorEl.locator(".bn-inline-content")).toHaveText(
    "Cats and dogs",
  );
});

test("Selecting an attachment suggestion with the keyboard inserts a link to it, with a label generated from its name", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <LinkSuggestionForTest
      pages={[{ label: "Cats", url: "/cats" }]}
      attachments={[
        {
          label: "cats.pdf",
          url: "/cats.pdf",
          reference: "Animals.Cats@cats.pdf",
        },
      ]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await openLinkQuickAction(page, editorEl);

  // Past the two actions and the page, to the attachment.
  for (let i = 0; i < 3; i++) {
    await editorEl.press("ArrowDown");
  }
  await editorEl.press("Enter");

  await expect(editorEl.locator("a")).toHaveText("cats.pdf");
  expect(await getInsertedLinks(page)).toEqual([
    {
      title: "cats.pdf",
      generatedTitle: true,
      url: "/cats.pdf",
      reference: linkReference("attach", "Animals.Cats@cats.pdf"),
    },
  ]);
});

test("An attachment of the edited page is linked by its name only", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <LinkSuggestionForTest
      attachments={[
        {
          label: "local.pdf",
          url: "/local.pdf",
          reference: "Some.Page@local.pdf",
        },
      ]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await openLinkQuickAction(page, editorEl);
  await page
    .locator(".bn-suggestion-menu-item", { hasText: "local.pdf" })
    .click();

  await expect(editorEl.locator("a")).toHaveText("local.pdf");
  expect(await getInsertedLinks(page)).toEqual([
    {
      title: "local.pdf",
      generatedTitle: true,
      url: "/local.pdf",
      reference: linkReference("attach", "local.pdf"),
    },
  ]);
});

test("The link editor action opens the link editor, and inserts the link it submits", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <LinkSuggestionForTest
      linkEditorSubmit={{ title: "XWiki", url: "https://xwiki.org" }}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await openLinkQuickAction(page, editorEl);

  await page.locator(".bn-suggestion-menu-item", { hasText: LINK_TO }).click();

  // The link editor is opened to create a new link, from scratch.
  expect(
    JSON.parse((await page.getByTestId("linkEditorInput").textContent()) ?? ""),
  ).toEqual({ mode: "createNew", current: { title: "", url: "" } });

  const linkEl = editorEl.locator("a");
  await expect(linkEl).toHaveText("XWiki");
  await expect(linkEl).toHaveAttribute("href", "https://xwiki.org#inserted");
  expect(await getInsertedLinks(page)).toEqual([
    { title: "XWiki", url: "https://xwiki.org" },
  ]);
});

test("The upload action uploads a file of any type and inserts a link to it, with a label generated from its name", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <LinkSuggestionForTest
      uploadedFileURL="/uploaded.pdf"
      uploadedFileName="uploaded.pdf"
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await openLinkQuickAction(page, editorEl);

  const fileChooserPromise = page.waitForEvent("filechooser");
  await page.locator(".bn-suggestion-menu-item", { hasText: UPLOAD }).click();
  const fileChooser = await fileChooserPromise;
  expect(await fileChooser.element().getAttribute("accept")).toBeNull();
  await fileChooser.setFiles({
    name: "document.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("some content"),
  });

  // The label is the name of the attachment, which can differ from the name of the uploaded file.
  await expect(editorEl.locator("a")).toHaveText("uploaded.pdf");
  expect(await getInsertedLinks(page)).toEqual([
    {
      title: "uploaded.pdf",
      generatedTitle: true,
      url: "/uploaded.pdf",
      // The attachment of the edited page is designated by its name only.
      reference: linkReference("attach", "uploaded.pdf"),
    },
  ]);
});

test("A failed upload is logged and leaves the editor unchanged", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  // No uploaded file URL: the upload fails.
  const component = await mount(<LinkSuggestionForTest />);

  const editorEl = component.locator(".bn-editor");
  await openLinkQuickAction(page, editorEl);

  const fileChooserPromise = page.waitForEvent("filechooser");
  await page.locator(".bn-suggestion-menu-item", { hasText: UPLOAD }).click();
  const fileChooser = await fileChooserPromise;

  const errorLogPromise = page.waitForEvent("console", (message) =>
    message.text().includes("Failed to upload the selected file."),
  );
  await fileChooser.setFiles({
    name: "document.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("some content"),
  });
  await errorLogPromise;

  await expect(editorEl.locator("a")).toHaveCount(0);
  expect(await getInsertedLinks(page)).toEqual([]);
});

test("A failed search for one kind of links still shows the other kind, and the actions", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <LinkSuggestionForTest
      pages={[{ label: "Cats", url: "/cats" }]}
      attachments={[{ label: "cats.pdf", url: "/cats.pdf" }]}
      failingSearch="pages"
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await openLinkQuickAction(page, editorEl);

  const menuEl = page.locator(".bn-suggestion-menu");
  const items = menuEl.locator(".bn-suggestion-menu-item");
  await expect(items).toHaveCount(3);
  await expect(items.nth(2)).toContainText("cats.pdf");
  await expect(menuEl.locator(".bn-suggestion-menu-label")).toHaveText([
    "blocknote.linkSuggestion.attachmentsGroup",
  ]);
});

test("Only shows the actions when the backend doesn't support search", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <LinkSuggestionForTest linkSuggestServiceUnavailable={true} />,
  );

  const editorEl = component.locator(".bn-editor");
  await openLinkQuickAction(page, editorEl);

  const menuEl = page.locator(".bn-suggestion-menu");
  await expect(menuEl.locator(".bn-suggestion-menu-loader")).not.toBeAttached();
  await expect(menuEl.locator(".bn-suggestion-menu-item")).toHaveCount(2);
  await expect(menuEl.locator(".bn-suggestion-menu-label")).toHaveCount(0);
});

test("Typing the trigger shows the link suggestions only once followed by a query", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const component = await mount(
    <LinkSuggestionForTest pages={[{ label: "Cats", url: "/cats" }]} />,
  );

  const editorEl = component.locator(".bn-editor");
  const menuEl = page.locator(".bn-suggestion-menu");
  await editorEl.click();

  // Typing the trigger alone doesn't get in the way: Enter breaks the line, instead of selecting
  // an action of a hidden menu.
  await page.keyboard.type("[[");
  await page.keyboard.press("Enter");
  await expect(editorEl.locator(".bn-block-content")).toHaveCount(2);
  await expect(menuEl).not.toBeAttached();

  await page.keyboard.type("[[cat");
  await menuEl.locator(".bn-suggestion-menu-item", { hasText: "Cats" }).click();

  // The typed trigger and query are replaced by the link.
  await expect(editorEl.locator(".bn-block-content").nth(1)).toHaveText("Cats");
});

test("The link suggestions aren't available when the syntax doesn't support links", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <LinkSuggestionForTest
      pages={[{ label: "Cats", url: "/cats" }]}
      syntax={{
        ...FULL_SYNTAX,
        features: {
          ...FULL_SYNTAX.features,
          inlineContents: {
            ...FULL_SYNTAX.features.inlineContents,
            links: {
              ...FULL_SYNTAX.features.inlineContents.links,
              basicLinks: false,
            },
          },
        },
      }}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type("[[cat");

  // The text stays plain text: the suggestion menu doesn't open.
  await expect(editorEl).toContainText("[[cat");
  await expect(page.locator(".bn-suggestion-menu")).not.toBeAttached();
});

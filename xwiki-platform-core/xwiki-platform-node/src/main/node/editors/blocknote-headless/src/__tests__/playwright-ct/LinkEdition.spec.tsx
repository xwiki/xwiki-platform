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
import { fillDisplayText, pressKeySettled } from "./utils";
import { expect, test } from "@playwright/experimental-ct-vue";
import type { BlockType } from "@xwiki/platform-editors-blocknote-react";
import type { DocumentReference } from "@xwiki/platform-model-api";

// The Playwright page and locator types, taken from the test fixtures since the component testing package doesn't
// export them.
type Page = Parameters<Parameters<typeof test>[2]>[0]["page"];
type Locator = ReturnType<Page["locator"]>;

test("Editing the title of a link keeps the rest of the line intact", async ({
  mount,
  page,
}) => {
  const component = await mountBlockNoteHeadless(mount, {
    editorContent: buildParagraphWithLink("https://xwiki.org"),
    editorProps: {
      syntax: FULL_SYNTAX,
    },
    macros: false,
  });

  const editorEl = component.locator(".bn-editor");
  const linkEl = editorEl.locator('a[href="https://xwiki.org"]');
  await linkEl.waitFor({ state: "attached" });

  await openLinkEditor(page, editorEl);

  await fillDisplayText(page, "2nd");

  const submitButtonEl = page.locator('[data-test="linkSubmit"]');
  await submitButtonEl.click();

  // The link title must be updated...
  await expect(linkEl).toHaveText("2nd");

  // ...and the rest of the line must be intact.
  await expect(editorEl).toHaveText("First 2nd third fourth");
});

for (const close of ["submit", "cancel"] as const) {
  test(`The edited content is focused back when the link modal is closed (${close})`, async ({
    mount,
    page,
  }) => {
    const component = await mountBlockNoteHeadless(mount, {
      editorContent: buildParagraphWithLink("https://xwiki.org"),
      editorProps: {
        syntax: FULL_SYNTAX,
      },
      macros: false,
    });

    const editorEl = component.locator(".bn-editor");
    await editorEl.locator('a[href="https://xwiki.org"]').waitFor({
      state: "attached",
    });

    await openLinkEditor(page, editorEl);
    await expect(editorEl).not.toBeFocused();

    await page
      .locator(
        close === "submit"
          ? '[data-test="linkSubmit"]'
          : '[data-test="linkCancel"]',
      )
      .click();

    // Keyboard users can keep typing right away.
    await expect(editorEl).toBeFocused();
  });
}

test("Editing the title of a link to an unsupported resource keeps its reference", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const url = "https://en.wikipedia.org/wiki/XWiki";
  // No link target type supports interwiki references.
  const reference = {
    type: "interwiki",
    typed: true,
    reference: "wikipedia:XWiki",
    parameters: { anchor: "History" },
  };
  const component = await mountBlockNoteHeadless(mount, {
    editorContent: buildParagraphWithLink(url),
    editorProps: {
      syntax: FULL_SYNTAX,
    },
    macros: false,
    linkReferences: { [url]: reference },
  });

  const editorEl = component.locator(".bn-editor");
  const linkEl = editorEl.locator(`a[href="${url}"]`);
  await linkEl.waitFor({ state: "attached" });

  await openLinkEditor(page, editorEl);

  // The link type selector displays the type of the linked resource as is, and its reference is editable as text.
  await expect(
    page.locator('[data-test="linkTargetType"] option:checked'),
  ).toHaveText("interwiki");
  await expect(page.locator('[data-test="linkReference"]')).toHaveValue(
    "wikipedia:XWiki",
  );

  await fillDisplayText(page, "2nd");
  await page.locator('[data-test="linkSubmit"]').click();

  await expect(linkEl).toHaveText("2nd");
  // The link still points to the same resource, with the same reference.
  await expect(page.locator('[data-test="submittedLinkReference"]')).toHaveText(
    JSON.stringify(reference),
  );
});

test("Editing a page link resolves and saves its reference relative to the document holding the link", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const url = "https://example.org/some/page";
  // Only resolved against the document holding the link, see the mocked model reference parser.
  const reference = {
    type: "doc",
    typed: true,
    reference: "some sibling page",
    parameters: {},
  };
  const component = await mountBlockNoteHeadless(mount, {
    editorContent: buildParagraphWithLink(url),
    editorProps: {
      syntax: FULL_SYNTAX,
    },
    macros: false,
    linkReferences: { [url]: reference },
    // Plain data, since the props are serialized to reach the browser: the mocked model reference services only check
    // whether the source document is there.
    linkContext: {
      sourceDocument: {
        name: "source",
      } as unknown as DocumentReference,
    },
  });

  const editorEl = component.locator(".bn-editor");
  const linkEl = editorEl.locator(`a[href="${url}"]`);
  await linkEl.waitFor({ state: "attached" });

  await openLinkEditor(page, editorEl);

  await expect(
    page.locator('[data-test="linkTargetType"] option:checked'),
  ).toHaveText("Page");
  await expect(
    page.locator('[data-test="linkPageReference"] input'),
  ).toHaveValue("some sibling page");

  // The page search box is focused when the link modal opens, which displays its suggestions over the options.
  await page.keyboard.press("Escape");
  await fillDisplayText(page, "2nd");
  await page.locator('[data-test="linkSubmit"]').click();

  await expect(page.locator('[data-test="submittedLinkReference"]')).toHaveText(
    JSON.stringify(reference),
  );
});

test("Editing the title of a path link keeps its reference, even when path links are not offered", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const url = "/xwiki/bin/view/Main/";
  const reference = {
    type: "path",
    typed: true,
    reference: "/xwiki/bin/view/Main/",
    parameters: {},
  };
  const component = await mountBlockNoteHeadless(mount, {
    editorContent: buildParagraphWithLink(url),
    editorProps: {
      syntax: FULL_SYNTAX,
    },
    macros: false,
    linkReferences: { [url]: reference },
    // Like the XWiki integration.
    excludedLinkTargetTypes: ["path", "unc"],
  });

  const editorEl = component.locator(".bn-editor");
  const linkEl = editorEl.locator(`a[href="${url}"]`);
  await linkEl.waitFor({ state: "attached" });

  await openLinkEditor(page, editorEl);

  // The path link is edited with its own link target type, even if it's excluded, unlike the UNC path one.
  await expect(
    page.locator('[data-test="linkTargetType"] option:checked'),
  ).toHaveText("Path");
  await expect(
    page.locator('[data-test="linkTargetType"] option[value="unc"]'),
  ).toHaveCount(0);
  // Path links are edited with the plain text form of the link modal, since no form is registered for them.
  await expect(page.locator('[data-test="linkLocation"]')).toHaveValue(
    "/xwiki/bin/view/Main/",
  );

  await fillDisplayText(page, "2nd");
  await page.locator('[data-test="linkSubmit"]').click();

  await expect(linkEl).toHaveText("2nd");
  // The link is still a typed path reference: untyped, it would be parsed back as a document reference.
  await expect(page.locator('[data-test="submittedLinkReference"]')).toHaveText(
    JSON.stringify(reference),
  );
});

/**
 * Open the link modal on the link of the paragraph built by {@link buildParagraphWithLink}.
 */
async function openLinkEditor(page: Page, editorEl: Locator): Promise<void> {
  // Move the caret inside the link, with the keyboard, to trigger the link toolbar. The toolbar also opens when the
  // link is hovered, but not with a single synthetic mouse move: the editor only records the hovered link when the
  // pointer enters it, and the toolbar starts listening for pointer events on the link on the render that follows,
  // so it never sees that move. Click on "First", away from the link, because clicking the link itself would make
  // the browser follow it. Each key press is awaited until it is painted (see pressKeySettled) so that sending them
  // in quick succession doesn't outrun the editor's selection update.
  await editorEl
    .locator("p.bn-inline-content")
    .click({ position: { x: 5, y: 5 } });
  await pressKeySettled(page, "Home");
  for (let i = 0; i < 8; i++) {
    await pressKeySettled(page, "ArrowRight");
  }

  const editLinkButtonEl = page.locator('button[data-test="editLink"]');
  await editLinkButtonEl.waitFor({ state: "attached" });
  await editLinkButtonEl.click();
}

function buildParagraphWithLink(url: string): BlockType[] {
  return [
    {
      id: Math.random().toString(),
      type: "paragraph",
      props: {
        backgroundColor: "default",
        textAlignment: "left",
        textColor: "default",
      },
      content: [
        { type: "text", text: "First ", styles: {} },
        {
          type: "link",
          href: url,
          content: [{ type: "text", text: "second", styles: {} }],
        },
        { type: "text", text: " third fourth", styles: {} },
      ],
      children: [],
    },
  ];
}

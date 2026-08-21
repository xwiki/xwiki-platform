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
import { BlockNoteWithLinkEditionHooks } from "./LinkEdition.story";
import { expect, test } from "@playwright/experimental-ct-react";
import type { BlockType } from "../../blocknote";
import type { Locator, Page } from "@playwright/test";

// The url of the edited link carries a synthetic id, like the XWiki integration does to bind a link
// to the metadata it can't store in the BlockNote schema.
const LINK_URL = "https://xwiki.org/?id=42";

test("beforeEdit transforms the link data used to pre-fill the link editor", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <BlockNoteWithLinkEditionHooks
      content={buildParagraphWithLink()}
      beforeEditTitle="prefilled title"
    />,
  );

  await editLink(component.locator(".bn-editor"), page);

  // The link editor is opened with the link data returned by beforeEdit, not with the original one.
  await expect(page.locator('[data-test="linkEditorInput"]')).toHaveText(
    JSON.stringify({ url: LINK_URL, title: "prefilled title" }),
  );
});

test("beforeUpdate receives the submitted link data and the previous one, as stored in the content", async ({
  mount,
  page,
}) => {
  const submit = {
    title: "2nd",
    url: "https://example.org/",
    reference: {
      type: "url",
      typed: false,
      reference: "https://example.org/",
      parameters: {},
    },
  };

  const component = await mount(
    <BlockNoteWithLinkEditionHooks
      content={buildParagraphWithLink()}
      beforeEditTitle="prefilled title"
      submit={submit}
    />,
  );

  await editLink(component.locator(".bn-editor"), page);

  // beforeUpdate gets the link data submitted by the link editor, together with the link data as it
  // is stored in the content: not the one transformed by beforeEdit, so that the integration can
  // recover from it the metadata (e.g. a synthetic id) it hides from the link editor.
  await expect(page.locator('[data-test="beforeUpdateInput"]')).toHaveText(
    JSON.stringify({
      linkData: submit,
      previous: { url: LINK_URL, title: "second" },
    }),
  );
});

test("beforeUpdate can rewrite the url written into the content", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <BlockNoteWithLinkEditionHooks
      content={buildParagraphWithLink()}
      submit={{ title: "second", url: "https://example.org/" }}
      beforeUpdateUrl="https://rewritten.example/"
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await editLink(editorEl, page);

  // The url written into the content is the one returned by beforeUpdate.
  await expect(
    editorEl.locator('a[href="https://rewritten.example/"]'),
  ).toHaveText("second");
});

/**
 * Hovers the link to trigger the link toolbar, then opens the link editor.
 */
async function editLink(editorEl: Locator, page: Page): Promise<void> {
  const linkEl = editorEl.locator(`a[href="${LINK_URL}"]`);
  await linkEl.waitFor({ state: "attached" });
  await linkEl.hover();

  const editLinkButtonEl = page.locator('button[data-test="editLink"]');
  await editLinkButtonEl.waitFor({ state: "attached" });
  await editLinkButtonEl.click();
}

function buildParagraphWithLink(): BlockType[] {
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
          href: LINK_URL,
          content: [{ type: "text", text: "second", styles: {} }],
        },
        { type: "text", text: " third fourth", styles: {} },
      ],
      children: [],
    },
  ];
}

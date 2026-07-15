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
import { BlockNoteForTest } from "./BlockNote.story";
import { BlockNoteWithLinkEditionHooks } from "./LinkEdition.story";
import { FULL_SYNTAX } from "./syntax.mock";
import { expect, test } from "@playwright/experimental-ct-react";
import type { BlockType } from "../../blocknote";

// eslint-disable-next-line max-statements
test("Editing the title of a link keeps the rest of the line intact", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <BlockNoteForTest
      content={buildParagraphWithLink()}
      macros={false}
      syntax={FULL_SYNTAX}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  const linkEl = editorEl.locator('a[href="https://xwiki.org"]');
  await linkEl.waitFor({ state: "attached" });

  // Hover the link to trigger the link toolbar.
  await linkEl.hover();

  const editLinkButtonEl = page.locator('button[data-test="editLink"]');
  await editLinkButtonEl.waitFor({ state: "attached" });
  await editLinkButtonEl.click();

  const titleInputEl = page.locator('input[data-test="linkTitle"]');
  await titleInputEl.waitFor({ state: "attached" });
  await titleInputEl.fill("2nd");
  await titleInputEl.press("Enter");

  // The link title must be updated...
  await expect(linkEl).toHaveText("2nd");

  // ...and the rest of the line must be intact.
  await expect(editorEl).toHaveText("First 2nd third fourth");
});

test("beforeEdit can transform the link data used to pre-fill the edit popover", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <BlockNoteWithLinkEditionHooks
      content={buildParagraphWithLink()}
      beforeEditTitle="prefilled title"
    />,
  );

  const editorEl = component.locator(".bn-editor");
  const linkEl = editorEl.locator('a[href="https://xwiki.org"]');
  await linkEl.waitFor({ state: "attached" });

  // Hover the link to trigger the link toolbar, then open the edit popover.
  await linkEl.hover();
  const editLinkButtonEl = page.locator('button[data-test="editLink"]');
  await editLinkButtonEl.waitFor({ state: "attached" });
  await editLinkButtonEl.click();

  // The popover is pre-filled with the title returned by beforeEdit, not the original one.
  const titleInputEl = page.locator('input[data-test="linkTitle"]');
  await expect(titleInputEl).toHaveValue("prefilled title");
});

// eslint-disable-next-line max-statements
test("beforeUpdate can rewrite the URL written into the content", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <BlockNoteWithLinkEditionHooks
      content={buildParagraphWithLink()}
      beforeUpdateUrl="https://rewritten.example/"
    />,
  );

  const editorEl = component.locator(".bn-editor");
  const linkEl = editorEl.locator('a[href="https://xwiki.org"]');
  await linkEl.waitFor({ state: "attached" });

  // Hover the link to trigger the link toolbar, then open the edit popover.
  await linkEl.hover();
  const editLinkButtonEl = page.locator('button[data-test="editLink"]');
  await editLinkButtonEl.waitFor({ state: "attached" });
  await editLinkButtonEl.click();

  const titleInputEl = page.locator('input[data-test="linkTitle"]');
  await titleInputEl.waitFor({ state: "attached" });
  await titleInputEl.fill("2nd");
  await titleInputEl.press("Enter");

  // The URL written to the content is the one returned by beforeUpdate.
  await expect(
    editorEl.locator('a[href="https://rewritten.example/"]'),
  ).toHaveText("2nd");
});

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
          href: "https://xwiki.org",
          content: [{ type: "text", text: "second", styles: {} }],
        },
        { type: "text", text: " third fourth", styles: {} },
      ],
      children: [],
    },
  ];
}

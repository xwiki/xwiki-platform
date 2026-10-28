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
import type { BlockType } from "@xwiki/platform-editors-blocknote-react";

test("Editing the title of a link keeps the rest of the line intact", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
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

  const titleInputEl = page.locator('[data-test="linkDisplayText"]');
  await titleInputEl.waitFor({ state: "attached" });
  await titleInputEl.fill("2nd");

  const submitButtonEl = page.locator('[data-test="linkSubmit"]');
  await submitButtonEl.click();

  // The link title must be updated...
  await expect(linkEl).toHaveText("2nd");

  // ...and the rest of the line must be intact.
  await expect(editorEl).toHaveText("First 2nd third fourth");
});

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

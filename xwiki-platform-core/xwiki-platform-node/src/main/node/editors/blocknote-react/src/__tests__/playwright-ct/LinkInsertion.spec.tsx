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
import { FULL_SYNTAX } from "./syntax.mock";
import { expect, test } from "@playwright/experimental-ct-react";
import type { BlockType } from "../../blocknote";

// eslint-disable-next-line max-statements
test("Creating a link on a word in the middle of a line keeps the text intact", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <BlockNoteForTest
      content={buildParagraph("First second third fourth")}
      macros={false}
      syntax={FULL_SYNTAX}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  const paragraph = editorEl.locator("p.bn-inline-content");
  await paragraph.waitFor({ state: "attached" });

  // Select the word "second" (offsets 6 to 12) with the keyboard.
  await paragraph.click();
  await page.keyboard.press("Home");
  for (let i = 0; i < 6; i++) {
    await page.keyboard.press("ArrowRight");
  }
  for (let i = 0; i < 6; i++) {
    await page.keyboard.press("Shift+ArrowRight");
  }

  // The formatting toolbar is rendered via FloatingPortal into document.body.
  const linkButtonEl = page.locator('button[data-test="createLink"]');
  await linkButtonEl.waitFor({ state: "attached" });
  await linkButtonEl.click();

  const urlInputEl = page.locator(".bn-form-popover input");
  await urlInputEl.waitFor({ state: "attached" });
  await urlInputEl.fill("https://xwiki.org");
  await urlInputEl.press("Enter");

  // The link must be inserted around the selected word...
  const linkEl = editorEl.locator('a[href="https://xwiki.org"]');
  await expect(linkEl).toHaveText("second");

  // ...and the rest of the line must be intact.
  await expect(editorEl).toHaveText("First second third fourth");
});

function buildParagraph(text: string): BlockType[] {
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
        {
          type: "text",
          text,
          styles: {},
        },
      ],
      children: [],
    },
  ];
}

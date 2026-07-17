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
import { expect, test } from "@playwright/experimental-ct-vue";
import type { UniAst } from "@xwiki/platform-uniast-api";

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

  // Hover the link to trigger the link toolbar.
  await linkEl.hover();

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

function buildParagraphWithLink(url: string): UniAst {
  return {
    blocks: [
      {
        type: "paragraph",
        content: [
          { type: "text", content: "First ", styles: {} },
          {
            type: "link",
            target: { type: "external", url },
            content: [{ type: "text", content: "second", styles: {} }],
          },
          { type: "text", content: " third fourth", styles: {} },
        ],
        styles: {},
      },
    ],
  };
}

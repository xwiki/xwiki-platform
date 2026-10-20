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
import type { Page } from "@playwright/test";

// A 1x1 transparent PNG, small enough to inline.
const PNG_BASE64 =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

const UPLOADED_URL = "/xwiki/bin/download/Space/Page/uploaded.png";

/**
 * Pastes or drops a file over the editor, the way a user would, so that BlockNote runs its file insertion handler.
 * This is also how the functional tests drive the editor, so it covers that mechanism too.
 *
 * @param page - the page holding the mounted editor
 * @param eventType - whether the file is pasted or dropped
 * @param name - the name of the inserted file
 * @param type - the media type of the inserted file
 * @param base64 - the content of the inserted file
 */
async function insertFile(
  page: Page,
  eventType: "paste" | "drop",
  name: string,
  type: string,
  base64: string,
): Promise<void> {
  await page.evaluate(
    ({ eventType, name, type, base64 }) => {
      const editor = document.querySelector(".bn-editor")!;
      const bytes = Uint8Array.from(atob(base64), (char) => char.charCodeAt(0));
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(new File([bytes], name, { type }));
      const { left, top, width, height } = editor.getBoundingClientRect();
      const eventInit = {
        bubbles: true,
        cancelable: true,
        clientX: left + width / 2,
        clientY: top + height / 2,
      };
      // Firefox ignores the clipboardData passed to the ClipboardEvent constructor (the event ends up with no types),
      // so we define it as an own property, which shadows the prototype getter in both browsers.
      const paste = new ClipboardEvent("paste", eventInit);
      Object.defineProperty(paste, "clipboardData", {
        value: dataTransfer,
        configurable: true,
      });
      editor.dispatchEvent(
        eventType === "paste"
          ? paste
          : new DragEvent("drop", { ...eventInit, dataTransfer }),
      );
    },
    { eventType, name, type, base64 },
  );
}

/**
 * Drops a file over the editor, the way a user would.
 *
 * @param page - the page holding the mounted editor
 * @param name - the name of the dropped file
 * @param type - the media type of the dropped file
 * @param base64 - the content of the dropped file
 */
function dropFile(
  page: Page,
  name: string,
  type: string,
  base64: string,
): Promise<void> {
  return insertFile(page, "drop", name, type, base64);
}

test("A dropped image is uploaded and shown", async ({ mount, page }) => {
  const component = await mount(
    <BlockNoteForTest
      blockNoteOptions={{ uploadFile: async () => UPLOADED_URL }}
      content={[]}
      macros={false}
      syntax={FULL_SYNTAX}
    />,
  );

  await dropFile(page, "photo.png", "image/png", PNG_BASE64);

  await expect(component.locator(`img[src="${UPLOADED_URL}"]`)).toBeVisible();
});

test("A pasted image is uploaded and shown", async ({ mount, page }) => {
  const component = await mount(
    <BlockNoteForTest
      blockNoteOptions={{ uploadFile: async () => UPLOADED_URL }}
      content={[]}
      macros={false}
      syntax={FULL_SYNTAX}
    />,
  );

  await insertFile(page, "paste", "photo.png", "image/png", PNG_BASE64);

  await expect(component.locator(`img[src="${UPLOADED_URL}"]`)).toBeVisible();
});

test("A failed upload leaves no block behind", async ({ mount, page }) => {
  const component = await mount(
    <BlockNoteForTest
      blockNoteOptions={{
        // What the XWiki integration returns when the upload fails: the inserted block collapses into an empty
        // paragraph instead of the promise being rejected, which BlockNote doesn't handle.
        uploadFile: async () => ({ type: "paragraph", content: [] }),
      }}
      content={[]}
      macros={false}
      syntax={FULL_SYNTAX}
    />,
  );

  await dropFile(page, "photo.png", "image/png", PNG_BASE64);

  await expect(component.locator(".bn-file-block-content-wrapper")).toHaveCount(
    0,
  );
  await expect(component.locator("img")).toHaveCount(0);
});

test("A dropped non-image file can be replaced by a link", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <BlockNoteForTest
      blockNoteOptions={{
        // What the XWiki integration returns for a file that has no XWiki counterpart: a free-standing attachment
        // link, like the CKEditor integration produces.
        uploadFile: async () => ({
          type: "paragraph",
          content: [
            {
              type: "link",
              href: "/download/report.pdf",
              content: "report.pdf",
            },
          ],
        }),
      }}
      content={[]}
      macros={false}
      syntax={FULL_SYNTAX}
    />,
  );

  await dropFile(page, "report.pdf", "application/pdf", PNG_BASE64);

  const link = component.locator('a[href="/download/report.pdf"]');
  await expect(link).toBeVisible();
  await expect(link).toHaveText("report.pdf");
  await expect(component.locator(".bn-file-block-content-wrapper")).toHaveCount(
    0,
  );
});

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
import { BlockNoteWithFileUploader } from "./FileUpload.story";
import { clickSettled, pressKeySettled } from "./utils";
import { expect, test } from "@playwright/experimental-ct-react";
import type { BlockType } from "../../blocknote";
import type { Locator, Page } from "@playwright/test";

// A 1x1 transparent PNG, small enough to inline.
const PNG_BASE64 =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

// The name the browser gives a pasted file is replaced with a generated one: <timestamp>-<random 100..999>.
const GENERATED_NAME = /^\d+-[1-9]\d\d\.png$/;

// Synthetic events, so BlockNote runs its real file insertion handler. The functional tests drive it the same way.
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
      // Firefox ignores clipboardData passed to the constructor, so define it as an own property instead.
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

function dropFile(
  page: Page,
  name: string,
  type: string,
  base64: string,
): Promise<void> {
  return insertFile(page, "drop", name, type, base64);
}

function pasteFile(
  page: Page,
  name: string,
  type: string,
  base64: string,
): Promise<void> {
  return insertFile(page, "paste", name, type, base64);
}

function paragraph(text: string): BlockType {
  return {
    id: "paragraph",
    type: "paragraph",
    props: {
      backgroundColor: "default",
      textAlignment: "left",
      textColor: "default",
    },
    content: [{ type: "text", text, styles: {} }],
    children: [],
  };
}

async function getUploadedFiles(page: Page): Promise<string[]> {
  return JSON.parse(
    (await page.locator('[data-test="uploadedFiles"]').textContent()) ?? "[]",
  );
}

/**
 * Puts the caret at the end of the first paragraph, which the pasted files are inserted at.
 */
async function focusEnd(component: Locator, page: Page): Promise<void> {
  await clickSettled(page, component.locator("p.bn-inline-content").first());
  // Awaited until painted (see pressKeySettled), so that the paste doesn't outrun the selection update.
  await pressKeySettled(page, "End");
}

test("A dropped image is uploaded under its name and shown", async ({
  mount,
  page,
}) => {
  const component = await mount(<BlockNoteWithFileUploader content={[]} />);

  await dropFile(page, "photo.png", "image/png", PNG_BASE64);

  await expect(
    component.locator('img[src="/download/photo.png"]'),
  ).toBeVisible();
  expect(await getUploadedFiles(page)).toEqual(["photo.png"]);
  // The file upload listeners are told.
  await expect(page.locator('[data-test="uploadEvents"]')).toHaveText(
    JSON.stringify(["uploaded /download/photo.png"]),
  );
});

test("A pasted image is uploaded under a generated name and shown", async ({
  mount,
  page,
}) => {
  const component = await mount(<BlockNoteWithFileUploader content={[]} />);

  await pasteFile(page, "image.png", "image/png", PNG_BASE64);

  await expect(component.locator('img[src^="/download/"]')).toBeVisible();
  const uploadedFiles = await getUploadedFiles(page);
  expect(uploadedFiles).toHaveLength(1);
  expect(uploadedFiles[0]).toMatch(GENERATED_NAME);
});

test("A failed upload leaves no block behind", async ({ mount, page }) => {
  const component = await mount(
    <BlockNoteWithFileUploader content={[]} fail />,
  );

  await dropFile(page, "photo.png", "image/png", PNG_BASE64);

  await expect(page.locator('[data-test="uploadedFiles"]')).toHaveText(
    JSON.stringify(["photo.png"]),
  );
  await expect(component.locator(".bn-file-block-content-wrapper")).toHaveCount(
    0,
  );
  await expect(component.locator("img")).toHaveCount(0);
  await expect(page.locator('[data-test="uploadEvents"]')).toHaveText(
    JSON.stringify(["failed photo.png"]),
  );
});

test("A pasted file that isn't an image is linked where the caret is", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <BlockNoteWithFileUploader content={[paragraph("Some text")]} />,
  );
  await focusEnd(component, page);

  await pasteFile(page, "report.pdf", "application/pdf", PNG_BASE64);

  // The link is inserted in the paragraph holding the caret, rather than in a block of its own.
  const link = component.locator('p.bn-inline-content a[href^="/download/"]');
  await expect(link).toBeVisible();
  await expect(component.locator("p.bn-inline-content").first()).toContainText(
    "Some text",
  );
  await expect(component.locator(".bn-file-block-content-wrapper")).toHaveCount(
    0,
  );
});

test("A dropped file that isn't an image is uploaded under its name and linked", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <BlockNoteWithFileUploader content={[paragraph("Some text")]} />,
  );

  await dropFile(page, "report.pdf", "application/pdf", PNG_BASE64);

  const link = component.locator('a[href="/download/report.pdf"]');
  await expect(link).toBeVisible();
  await expect(link).toHaveText("report.pdf");
  await expect(component.locator(".bn-file-block-content-wrapper")).toHaveCount(
    0,
  );
});

test("The pasted and dropped files are ignored when uploading is disabled", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <BlockNoteWithFileUploader
      content={[paragraph("Some text")]}
      upload={false}
    />,
  );
  await focusEnd(component, page);

  await pasteFile(page, "image.png", "image/png", PNG_BASE64);
  await dropFile(page, "report.pdf", "application/pdf", PNG_BASE64);

  await expect(component.locator("img")).toHaveCount(0);
  await expect(component.locator("a")).toHaveCount(0);
  expect(await getUploadedFiles(page)).toEqual([]);
});

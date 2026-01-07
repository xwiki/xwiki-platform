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
import { expect, test } from "@playwright/experimental-ct-react";
import type { BlockOfType, BlockType } from "../blocknote";

test("BlockNote shows with empty content", async ({ mount }) => {
  const component = await mount(
    <BlockNoteForTest content={[]} macros={false} />,
  );

  await expect(component).toBeVisible();
  await expect(component).toHaveText("");
});

test("BlockNote shows with initial content", async ({ mount }) => {
  const component = await mount(
    <BlockNoteForTest
      content={buildParagraphs(["Hello,", "world!"])}
      macros={false}
    />,
  );

  await expect(component).toBeVisible();
  await expect(component).toHaveText("Hello,world!");
});

test("BlockNote's content can be modified", async ({ mount }) => {
  const component = await mount(
    <BlockNoteForTest content={[]} macros={false} />,
  );

  const editorEl = component.locator(".bn-editor");

  await editorEl.fill("Some basic content!!!");
  await expect(editorEl).toHaveText("Some basic content!!!");

  await editorEl.press("Backspace");
  await editorEl.press("Backspace");
  await editorEl.press("Backspace");
  await expect(editorEl).toHaveText("Some basic content");
});

// eslint-disable-next-line max-statements
test("Image insertion UI can be overriden", async ({ mount }) => {
  let overrideFnCalledWithUrl: string | null = null;

  const component = await mount(
    <BlockNoteForTest
      content={[buildImage(SMALL_IMG_DATA_URL)]}
      macros={false}
      overrides={{
        // Unfortunately we can't call the "update" image handler here as functions don't cross Playwright's headless browser's boundaries
        imageEdition: (image) => {
          overrideFnCalledWithUrl = image.props.url;
        },
      }}
    />,
  );

  const editorEl = component.locator(".bn-editor");

  const imgEl = editorEl.locator("img.bn-visual-media");
  await imgEl.waitFor({ state: "attached" });

  // Trigger the toolbar by going to the end of the document and then selecting the image
  await editorEl.press("ArrowDown");
  await editorEl.press("ArrowUp");

  const toolbarEl = component.locator(".bn-toolbar.bn-formatting-toolbar");
  await toolbarEl.waitFor({ state: "attached" });

  // Trigger the image edition UI
  //   > NOTE: this will need to be updated if the button's label changes, or if a translation is used
  //   > There is no other real identifying DOM attribute for these buttons
  const imgEditBtnEl = toolbarEl.locator(
    'button[aria-label="blocknote.imageToolbar.buttons.edit"]',
  );
  await imgEditBtnEl.waitFor({ state: "attached" });
  await imgEditBtnEl.click();

  expect(overrideFnCalledWithUrl).toBe(SMALL_IMG_DATA_URL);
});

function buildParagraphs(blocks: string[]): BlockType[] {
  return blocks.map((blockText) => ({
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
        text: blockText,
        styles: {},
      },
    ],
    children: [],
  }));
}

function buildImage(url: string): BlockOfType<"image"> {
  return {
    type: "image",
    props: {
      url,
      backgroundColor: "default",
      caption: "",
      name: "",
      previewWidth: 100,
      showPreview: true,
      textAlignment: "left",
    },
    children: [],
    content: undefined,
    id: Math.random().toString(),
  };
}

const SMALL_IMG_DATA_URL =
  "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

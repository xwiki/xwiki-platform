/**
 * See the LICENSE file distributed with this work for additional
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
import { BlockType } from "../blocknote";
import { expect, test } from "@playwright/experimental-ct-react";

test("BlockNote shows with empty content", async ({ mount }) => {
  const component = await mount(<BlockNoteForTest content={[]} />);

  await expect(component).toBeVisible();
  await expect(component).toHaveText("");
});

test("BlockNote shows with initial content", async ({ mount }) => {
  const component = await mount(
    <BlockNoteForTest content={buildParagraphs(["Hello,", "world!"])} />,
  );

  await expect(component).toBeVisible();
  await expect(component).toHaveText("Hello,world!");
});

test("BlockNote's content can be modified", async ({ mount }) => {
  const component = await mount(<BlockNoteForTest content={[]} />);

  await component.locator(".bn-editor").fill("Some basic content!!!");
  await expect(component).toHaveText("Some basic content!!!");

  await component.locator(".bn-editor").press("Backspace");
  await component.locator(".bn-editor").press("Backspace");
  await component.locator(".bn-editor").press("Backspace");
  await expect(component).toHaveText("Some basic content");
});

function buildParagraphs(blocks: string[]): BlockType[] {
  return blocks.map((blockText, i) => ({
    id: i.toString(),
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

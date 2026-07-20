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
import { mountBlockNoteHeadless } from "./BlockNote.story.jsx";
import { FULL_SYNTAX } from "./syntax.mock.js";
import { buildParagraphs } from "./utils.js";
import { expect, test } from "@playwright/experimental-ct-vue";

test("BlockNote Headless mounts properly", async ({ mount }) => {
  const component = await mountBlockNoteHeadless(mount, {
    editorContent: { blocks: [] },
    editorProps: {
      syntax: FULL_SYNTAX,
    },
    macros: false,
  });

  await expect(component).toBeVisible();
  await expect(component).toHaveText("");
});

test("BlockNote Headless shows with initial content", async ({ mount }) => {
  const component = await mountBlockNoteHeadless(mount, {
    editorContent: buildParagraphs(["Hello,", "world!"]),
    editorProps: {
      syntax: FULL_SYNTAX,
    },
    macros: false,
  });

  await expect(component).toBeVisible();
  await expect(component).toHaveText("Hello,world!");
});

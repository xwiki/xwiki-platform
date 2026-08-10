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

import type { BlockType } from "@xwiki/platform-editors-blocknote-react";

interface PageWithKeyboard {
  keyboard: { press(key: string): Promise<void> };
  evaluate<T>(pageFunction: () => T | Promise<T>): Promise<T>;
}

/**
 * Presses a key and waits for the resulting DOM change to be painted before returning. Sending keys back-to-back
 * with `page.keyboard.press` can outrun the rich text editor's selection/state update (e.g. ProseMirror applies a
 * transaction asynchronously with respect to the native key event), which can make some key presses effectively
 * lost. Waiting for two animation frames (the first to let the browser schedule the pending update, the second to
 * confirm it has been painted) ensures each key press is fully applied before the next one is sent.
 *
 * @param page - the page to send the key to
 * @param key - the key (or key combination) to press, using Playwright's key syntax
 */
async function pressKeySettled(
  page: PageWithKeyboard,
  key: string,
): Promise<void> {
  await page.keyboard.press(key);
  await page.evaluate(
    () =>
      new Promise<void>((resolve) => {
        requestAnimationFrame(() => requestAnimationFrame(() => resolve()));
      }),
  );
}

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

export { buildParagraphs, pressKeySettled };

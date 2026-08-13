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
import { ImageSuggestionForTest } from "./ImageSuggestion.story";
import { expect, test } from "@playwright/experimental-ct-react";

test("Triggering the image suggestions shows the upload button, then the matching images", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const component = await mount(
    <ImageSuggestionForTest
      images={[
        { label: "Cat picture", url: "https://picsum.photos/150" },
        { label: "Dog picture", url: "https://picsum.photos/300" },
      ]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type(":");

  const menuEl = page.locator(".slash-menu");
  await menuEl.waitFor({ state: "attached" });

  const items = menuEl.locator(".slash-menu-item");
  await expect(items).toHaveCount(3);

  // The first item is the upload button, initially selected so it stays keyboard-navigable.
  await expect(items.nth(0)).toHaveClass(/selected/);
  await expect(items.nth(0).locator("button")).toBeVisible();

  // The following items are the matching image suggestions, in order, each with a preview.
  await expect(items.nth(1).locator("img")).toHaveAttribute(
    "src",
    "https://picsum.photos/150",
  );
  await expect(items.nth(1)).toContainText("Cat picture");

  await expect(items.nth(2).locator("img")).toHaveAttribute(
    "src",
    "https://picsum.photos/300",
  );
  await expect(items.nth(2)).toContainText("Dog picture");
});

test("Only the first 5 image suggestions are shown, in addition to the upload button", async ({
  mount,
  page,
}) => {
  const images = Array.from({ length: 8 }, (_, i) => ({
    label: `Image ${i}`,
    url: `https://picsum.photos/${100 + i}`,
  }));

  const component = await mount(<ImageSuggestionForTest images={images} />);

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type(":");

  const menuEl = page.locator(".slash-menu");
  await menuEl.waitFor({ state: "attached" });

  // 1 upload button + 5 suggestions max, even though 8 were available.
  await expect(menuEl.locator(".slash-menu-item")).toHaveCount(6);
});

test("Clicking an image suggestion inserts an image block with its URL", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <ImageSuggestionForTest
      images={[{ label: "Cat picture", url: "https://picsum.photos/150" }]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type(":");

  const menuEl = page.locator(".slash-menu");
  await menuEl.waitFor({ state: "attached" });

  await menuEl.locator(".slash-menu-item").nth(1).click();

  await expect(editorEl.locator("img.bn-visual-media")).toHaveAttribute(
    "src",
    "https://picsum.photos/150",
  );
});

test("Keyboard navigation can select an image suggestion", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <ImageSuggestionForTest
      images={[
        { label: "Cat picture", url: "https://picsum.photos/150" },
        { label: "Dog picture", url: "https://picsum.photos/300" },
      ]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type(":");

  const menuEl = page.locator(".slash-menu");
  await menuEl.waitFor({ state: "attached" });

  // Moves the selection from the upload button (index 0) to the first image suggestion (index 1).
  await editorEl.press("ArrowDown");
  await expect(menuEl.locator(".slash-menu-item").nth(1)).toHaveClass(
    /selected/,
  );

  await editorEl.press("Enter");

  await expect(editorEl.locator("img.bn-visual-media")).toHaveAttribute(
    "src",
    "https://picsum.photos/150",
  );
});

test("Falls back to an explanatory message when the backend doesn't support search", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <ImageSuggestionForTest linkSuggestServiceUnavailable={true} />,
  );

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type(":");

  const menuEl = page.locator(".slash-menu");
  await menuEl.waitFor({ state: "attached" });

  // i18next isn't initialized in this component test harness (as with the rest of this suite), so
  // the untranslated key is rendered as-is instead of its "Search is not supported for the current
  // backend" translation.
  await expect(menuEl).toContainText(
    "blocknote.combobox.backendSearchUnsupported",
  );

  // It shouldn't be rendered as an image suggestion (no preview, not clickable to insert a block).
  await expect(
    menuEl.locator(".slash-menu-item").nth(1).locator("img"),
  ).toHaveCount(0);
});

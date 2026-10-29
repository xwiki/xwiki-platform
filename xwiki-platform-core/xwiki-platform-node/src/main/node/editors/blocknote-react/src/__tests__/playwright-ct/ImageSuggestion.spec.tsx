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

test("Triggering the image suggestions shows the browse and upload actions, then the matching images", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const component = await mount(
    <ImageSuggestionForTest
      images={[
        {
          label: "Cat picture",
          url: "https://picsum.photos/150",
          reference: "some nested attachment reference",
        },
        {
          label: "Dog picture",
          url: "https://picsum.photos/300",
          reference: "some terminal attachment reference",
        },
        {
          label: "Bird picture",
          url: "https://picsum.photos/450",
          reference: "some invalid reference",
        },
      ]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type("img::");

  const menuEl = page.locator(".bn-suggestion-menu");
  // The menu is shown (with a loader) before its items are loaded.
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  const items = menuEl.locator(".bn-suggestion-menu-item");
  await expect(items).toHaveCount(5);

  // The first item is the "browse" action, initially selected so it stays keyboard-navigable.
  await expect(items.nth(0)).toHaveAttribute("aria-selected", "true");
  await expect(items.nth(0)).toContainText("blocknote.imageSuggestion.browse");

  // The second item is the upload action.
  await expect(items.nth(1)).toContainText("blocknote.imageSuggestion.upload");

  // A group label separates the actions from the image suggestions.
  await expect(menuEl.locator(".bn-suggestion-menu-label")).toHaveText(
    "blocknote.imageSuggestion.imagesGroup",
  );

  // The following items are the matching image suggestions, in order, each with a preview that
  // has an empty alternative text, the image's title being right next to it, and the location of
  // the page holding the image under it.
  await expect(items.nth(2).locator("img")).toHaveAttribute(
    "src",
    "https://picsum.photos/150",
  );
  await expect(items.nth(2).locator("img")).toHaveAttribute("alt", "");
  await expect(items.nth(2)).toContainText("Cat picture");
  await expect(
    items.nth(2).locator(".bn-mt-suggestion-menu-item-subtitle"),
  ).toHaveText("Animals / Cats");

  await expect(items.nth(3).locator("img")).toHaveAttribute(
    "src",
    "https://picsum.photos/300",
  );
  await expect(items.nth(3)).toContainText("Dog picture");
  await expect(
    items.nth(3).locator(".bn-mt-suggestion-menu-item-subtitle"),
  ).toHaveText("Animals / Cat");

  // An image whose reference can't be parsed is still suggested, only without its location.
  await expect(items.nth(4)).toContainText("Bird picture");
  await expect(
    items.nth(4).locator(".bn-mt-suggestion-menu-item-subtitle"),
  ).toBeEmpty();
});

test("Only the first 5 image suggestions are shown, in addition to the browse and upload actions", async ({
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
  await page.keyboard.type("img::");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  // 1 browse action + 1 upload action + 5 suggestions max, even though 8 were available.
  await expect(menuEl.locator(".bn-suggestion-menu-item")).toHaveCount(7);
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
  await page.keyboard.type("img::");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  await menuEl.locator(".bn-suggestion-menu-item").nth(2).click();

  await expect(editorEl.locator("img.bn-visual-media")).toHaveAttribute(
    "src",
    "https://picsum.photos/150",
  );

  // The menu is closed and the trigger text removed, as when selecting with the keyboard.
  await expect(menuEl).not.toBeAttached();
  await expect(editorEl).not.toContainText("img::");
});

test("Keyboard navigation can select an image suggestion", async ({
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
  await page.keyboard.type("img::");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  // Moves the selection from the browse action (index 0), past the upload action (index 1), to
  // the first image suggestion (index 2).
  await editorEl.press("ArrowDown");
  await editorEl.press("ArrowDown");
  await expect(
    menuEl.locator(".bn-suggestion-menu-item").nth(2),
  ).toHaveAttribute("aria-selected", "true");

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
  await page.keyboard.type("img::");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  // i18next isn't initialized in this component test harness (as with the rest of this suite), so
  // the untranslated key is rendered as-is instead of its "Search is not supported for the current
  // backend" translation.
  await expect(menuEl).toContainText(
    "blocknote.combobox.backendSearchUnsupported",
  );

  // It shouldn't be rendered as an image suggestion (no preview, not clickable to insert a block).
  await expect(
    menuEl.locator(".bn-suggestion-menu-item").nth(2).locator("img"),
  ).toHaveCount(0);
});

test("Still shows the browse and upload actions when searching for images fails", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <ImageSuggestionForTest linkSuggestServiceFailing={true} />,
  );

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type("img::");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  // The menu doesn't keep loading forever: it shows the actions, without any image suggestion.
  await expect(menuEl.locator(".bn-suggestion-menu-loader")).not.toBeAttached();
  await expect(menuEl.locator(".bn-suggestion-menu-item")).toHaveCount(2);
});

test("Clicking the upload action opens a file picker offering only images", async ({
  mount,
  page,
}) => {
  const component = await mount(<ImageSuggestionForTest />);

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type("img::");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  const fileChooserPromise = page.waitForEvent("filechooser");
  await menuEl.locator(".bn-suggestion-menu-item").nth(1).click();
  const fileChooser = await fileChooserPromise;

  expect(await fileChooser.element().getAttribute("accept")).toBe("image/*");
});

test("Clicking the browse action opens the full image selector where the suggestion menu was", async ({
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
  await page.keyboard.type("img::");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  await menuEl.locator(".bn-suggestion-menu-item").nth(0).click();

  // The suggestion menu is gone, replaced by the full selector's own upload button and search box.
  await expect(menuEl).not.toBeAttached();
  await expect(
    page.getByText("blocknote.imageSelector.uploadButton"),
  ).toBeVisible();
  await expect(page.locator('[data-test="searchBoxInput"]')).toBeVisible();
});

test("The upload button of the opened image selector opens a file picker offering only images", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const component = await mount(<ImageSuggestionForTest />);

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type("img::");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();
  await menuEl.locator(".bn-suggestion-menu-item").nth(0).click();

  const fileChooserPromise = page.waitForEvent("filechooser");
  await page.getByText("blocknote.imageSelector.uploadButton").click();
  const fileChooser = await fileChooserPromise;

  expect(await fileChooser.element().getAttribute("accept")).toBe("image/*");
});

test("Selecting the browse action with the keyboard also opens the full image selector", async ({
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
  await page.keyboard.type("img::");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  // The browse action is selected by default (index 0), so <Enter> selects it directly.
  await editorEl.press("Enter");

  await expect(menuEl).not.toBeAttached();
  await expect(page.locator('[data-test="searchBoxInput"]')).toBeVisible();
});

test("Selecting a search result in the opened image selector inserts the image block", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const component = await mount(
    <ImageSuggestionForTest
      images={[{ label: "Cat picture", url: "https://picsum.photos/150" }]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type("img::");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  await menuEl.locator(".bn-suggestion-menu-item").nth(0).click();

  const searchInput = page.locator('[data-test="searchBoxInput"]');
  await searchInput.click();
  await searchInput.fill("Cat");

  await page.getByText("Cat picture").click();

  await expect(editorEl.locator("img.bn-visual-media")).toHaveAttribute(
    "src",
    "https://picsum.photos/150",
  );
});

test("Keyboard navigation can select a search result in the opened image selector", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const component = await mount(
    <ImageSuggestionForTest
      images={[{ label: "Cat picture", url: "https://picsum.photos/150" }]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type("img::");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  // Selects the browse action with the keyboard, same as the dedicated test above.
  await editorEl.press("Enter");

  const searchInput = page.locator('[data-test="searchBoxInput"]');
  await expect(searchInput).toBeVisible();

  // Navigates to, and selects, the only search result without ever using the mouse.
  await searchInput.press("ArrowDown");
  await searchInput.press("Enter");

  await expect(editorEl.locator("img.bn-visual-media")).toHaveAttribute(
    "src",
    "https://picsum.photos/150",
  );
});

test("Pressing Escape closes the opened image selector without inserting anything", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const component = await mount(
    <ImageSuggestionForTest
      images={[{ label: "Cat picture", url: "https://picsum.photos/150" }]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type("img::");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  await menuEl.locator(".bn-suggestion-menu-item").nth(0).click();

  const searchInput = page.locator('[data-test="searchBoxInput"]');
  await expect(searchInput).toBeVisible();

  await page.keyboard.press("Escape");

  await expect(searchInput).not.toBeAttached();
  await expect(editorEl.locator("img.bn-visual-media")).toHaveCount(0);
});

test("Opening the full image selector clears the typed trigger text from the editor", async ({
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
  await page.keyboard.type("img::some query");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  await menuEl.locator(".bn-suggestion-menu-item").nth(0).click();

  await expect(page.locator('[data-test="searchBoxInput"]')).toBeVisible();

  // The trigger and any typed query are removed from the document once the full selector opens,
  // instead of being left behind as stray text.
  await expect(editorEl).not.toContainText("img::");
  await expect(editorEl).not.toContainText("some query");
});

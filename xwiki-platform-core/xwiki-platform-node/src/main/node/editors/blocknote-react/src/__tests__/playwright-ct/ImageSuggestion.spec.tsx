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
import { FULL_SYNTAX } from "./syntax.mock";
import { expect, test } from "@playwright/experimental-ct-react";
import type { Locator, Page } from "@playwright/test";

/**
 * Opens the image suggestion menu with the image quick action, i.e. the "Image" item of the slash menu.
 */
async function openImageQuickAction(page: Page, editorEl: Locator) {
  await editorEl.click();
  await page.keyboard.type("/image");
  await expect(
    page.locator('.bn-suggestion-menu-item[aria-selected="true"]'),
  ).toContainText("Image");
  await page.keyboard.press("Enter");

  // The slash menu is replaced by the image suggestion menu (the slash menu stays attached for a
  // moment while it closes).
  await page
    .locator(".bn-suggestion-menu-item", {
      hasText: "blocknote.imageSuggestion.browse",
    })
    .waitFor();
  await expect(page.locator(".bn-suggestion-menu")).toHaveCount(1);
}

test("The image quick action shows the browse and upload actions, then the matching images", async ({
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
          hint: "Animals / Cats",
        },
        {
          label: "Dog picture",
          url: "https://picsum.photos/300",
          hint: "Animals / Cat",
        },
        {
          label: "Bird picture",
          url: "https://picsum.photos/450",
        },
      ]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await openImageQuickAction(page, editorEl);

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

  // An image without a location (empty hint) is shown without subtitle.
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
  await openImageQuickAction(page, editorEl);

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
  await openImageQuickAction(page, editorEl);

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  await menuEl.locator(".bn-suggestion-menu-item").nth(2).click();

  await expect(editorEl.locator("img.bn-visual-media")).toHaveAttribute(
    "src",
    "https://picsum.photos/150",
  );

  // The menu is closed and no text is left behind (neither the slash menu's query nor the image
  // suggestion menu's trigger).
  await expect(menuEl).not.toBeAttached();
  await expect(editorEl).not.toContainText("image");
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
  await openImageQuickAction(page, editorEl);

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

test("Only shows the browse and upload actions when the backend doesn't support search", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <ImageSuggestionForTest linkSuggestServiceUnavailable={true} />,
  );

  const editorEl = component.locator(".bn-editor");
  await openImageQuickAction(page, editorEl);

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  // No message pretending to be a menu item (it would be selectable and clickable): the image
  // selector opened by the browse action is the one telling that search isn't supported.
  await expect(menuEl.locator(".bn-suggestion-menu-loader")).not.toBeAttached();
  await expect(menuEl.locator(".bn-suggestion-menu-item")).toHaveCount(2);
  await expect(menuEl.locator(".bn-suggestion-menu-label")).toHaveCount(0);
});

test("Still shows the browse and upload actions when searching for images fails", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <ImageSuggestionForTest linkSuggestServiceFailing={true} />,
  );

  const editorEl = component.locator(".bn-editor");
  await openImageQuickAction(page, editorEl);

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
  await openImageQuickAction(page, editorEl);

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  const fileChooserPromise = page.waitForEvent("filechooser");
  await menuEl.locator(".bn-suggestion-menu-item").nth(1).click();
  const fileChooser = await fileChooserPromise;

  expect(await fileChooser.element().getAttribute("accept")).toBe("image/*");
});

test("A failed upload from the upload action is logged and leaves the editor unchanged", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const pageErrors: Error[] = [];
  page.on("pageerror", (error) => pageErrors.push(error));

  // No uploaded image URL: the upload fails.
  const component = await mount(<ImageSuggestionForTest />);

  const editorEl = component.locator(".bn-editor");
  await openImageQuickAction(page, editorEl);

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  const fileChooserPromise = page.waitForEvent("filechooser");
  await menuEl.locator(".bn-suggestion-menu-item").nth(1).click();
  const fileChooser = await fileChooserPromise;

  const errorLogPromise = page.waitForEvent("console", (message) =>
    message.text().includes("Failed to upload the selected image."),
  );
  await fileChooser.setFiles({
    name: "picture.png",
    mimeType: "image/png",
    buffer: Buffer.from("some content"),
  });
  expect((await errorLogPromise).type()).toBe("error");

  await expect(editorEl.locator("img")).toHaveCount(0);
  expect(pageErrors).toEqual([]);
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
  await openImageQuickAction(page, editorEl);

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
}) => {
  const component = await mount(<ImageSuggestionForTest />);

  const editorEl = component.locator(".bn-editor");
  await openImageQuickAction(page, editorEl);

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();
  await menuEl.locator(".bn-suggestion-menu-item").nth(0).click();

  const fileChooserPromise = page.waitForEvent("filechooser");
  await page.getByText("blocknote.imageSelector.uploadButton").click();
  const fileChooser = await fileChooserPromise;

  expect(await fileChooser.element().getAttribute("accept")).toBe("image/*");
});

test("Uploading an image from the opened image selector inserts it and closes the selector", async ({
  mount,
  page,
  // eslint-disable-next-line max-statements
}) => {
  const component = await mount(
    <ImageSuggestionForTest uploadedImageUrl="https://picsum.photos/600" />,
  );

  const editorEl = component.locator(".bn-editor");
  await openImageQuickAction(page, editorEl);

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();
  await menuEl.locator(".bn-suggestion-menu-item").nth(0).click();

  const fileChooserPromise = page.waitForEvent("filechooser");
  await page.getByText("blocknote.imageSelector.uploadButton").click();
  const fileChooser = await fileChooserPromise;
  await fileChooser.setFiles({
    name: "picture.png",
    mimeType: "image/png",
    buffer: Buffer.from("some content"),
  });

  await expect(editorEl.locator("img.bn-visual-media")).toHaveAttribute(
    "src",
    "https://picsum.photos/600",
  );
  await expect(page.locator('[data-test="searchBoxInput"]')).not.toBeAttached();
});

test("Uploading an image from the edit panel of an image replaces that image", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <ImageSuggestionForTest
      uploadedImageUrl="https://picsum.photos/600"
      content={[
        {
          id: "some-image",
          type: "image",
          props: {
            url: "https://picsum.photos/150",
          },
          content: undefined,
          children: [],
        },
      ]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await editorEl.locator("img.bn-visual-media").click();
  await page.getByLabel("blocknote.imageToolbar.buttons.edit").click();

  const fileChooserPromise = page.waitForEvent("filechooser");
  await page.getByText("blocknote.imageSelector.uploadButton").click();
  const fileChooser = await fileChooserPromise;
  await fileChooser.setFiles({
    name: "picture.png",
    mimeType: "image/png",
    buffer: Buffer.from("some content"),
  });

  // The image is replaced, rather than a new one being inserted.
  await expect(editorEl.locator("img.bn-visual-media")).toHaveCount(1);
  await expect(editorEl.locator("img.bn-visual-media")).toHaveAttribute(
    "src",
    "https://picsum.photos/600",
  );
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
  await openImageQuickAction(page, editorEl);

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
  await openImageQuickAction(page, editorEl);

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

test("The opened image selector shows the location of the found images", async ({
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
          hint: "Animals / Cats",
        },
        { label: "Bird picture", url: "https://picsum.photos/450" },
      ]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await openImageQuickAction(page, editorEl);
  await page.locator(".bn-suggestion-menu-item").nth(0).click();

  const searchInput = page.locator('[data-test="searchBoxInput"]');
  await searchInput.click();
  await searchInput.fill("picture");

  const options = page.getByRole("option");
  await expect(options).toHaveCount(2);
  await expect(options.nth(0)).toHaveText("Cat pictureAnimals / Cats");
  // An image without a location is shown with its name only.
  await expect(options.nth(1)).toHaveText("Bird picture");
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
  await openImageQuickAction(page, editorEl);

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
  await openImageQuickAction(page, editorEl);

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
  await page.keyboard.type("image:some query");

  const menuEl = page.locator(".bn-suggestion-menu");
  await menuEl.locator(".bn-suggestion-menu-item").first().waitFor();

  await menuEl.locator(".bn-suggestion-menu-item").nth(0).click();

  await expect(page.locator('[data-test="searchBoxInput"]')).toBeVisible();

  // The trigger and any typed query are removed from the document once the full selector opens,
  // instead of being left behind as stray text.
  await expect(editorEl).not.toContainText("image:");
  await expect(editorEl).not.toContainText("some query");
});

test("Typing the trigger shows the image suggestions only once followed by a query", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <ImageSuggestionForTest
      images={[{ label: "Cat picture", url: "https://picsum.photos/150" }]}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  const menuEl = page.locator(".bn-suggestion-menu");
  await editorEl.click();

  // Writing the trigger in a sentence doesn't get in the way: Enter breaks the line, instead of
  // selecting an action of a hidden menu.
  await page.keyboard.type("The image: a cat");
  await page.keyboard.press("Enter");
  await expect(editorEl.locator(".bn-block-content")).toHaveCount(2);
  await expect(menuEl).not.toBeAttached();

  await page.keyboard.type("image:cat");
  await expect(
    menuEl.locator(".bn-suggestion-menu-item", { hasText: "Cat picture" }),
  ).toBeVisible();
});

test("The image suggestions aren't available when the syntax doesn't support images", async ({
  mount,
  page,
}) => {
  const component = await mount(
    <ImageSuggestionForTest
      images={[{ label: "Cat picture", url: "https://picsum.photos/150" }]}
      syntax={{
        ...FULL_SYNTAX,
        features: {
          ...FULL_SYNTAX.features,
          blocks: {
            ...FULL_SYNTAX.features.blocks,
            images: {
              ...FULL_SYNTAX.features.blocks.images,
              basicImages: false,
            },
          },
        },
      }}
    />,
  );

  const editorEl = component.locator(".bn-editor");
  await editorEl.click();
  await page.keyboard.type("image:cat");

  // The text stays plain text: the suggestion menu doesn't open.
  await expect(editorEl).toContainText("image:cat");
  await expect(page.locator(".bn-suggestion-menu")).not.toBeAttached();
});

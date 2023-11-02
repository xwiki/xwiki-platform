import { expect, test } from "@playwright/test";
import { AxeBuilder } from "@axe-core/playwright";

// See here how to get started:
// https://playwright.dev/docs/intro
test("visits the app root url", async ({ page }) => {
  await page.goto("/");
  await expect(page.locator("div.greetings > h1")).toHaveText("You did it!");
  const axeResults = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa"])
    .analyze();
  expect(axeResults.violations).toBe([])
});

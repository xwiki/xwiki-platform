/*
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

import { expect, test } from "@playwright/test";

test.afterEach(async ({ page }, testInfo) => {
  if (testInfo.status !== testInfo.expectedStatus) {
    // Get a unique place for the screenshot.
    const screenshotPath = testInfo.outputPath(`failure.png`);
    // Add it to the report.
    testInfo.attachments.push({
      name: "screenshot",
      path: screenshotPath,
      contentType: "image/png",
    });
    // Take the screenshot itself.
    await page.screenshot({ path: screenshotPath, timeout: 5000 });
  }
});

// const localDefaultPage = "/Localhost/#/";
const localDefaultPage = "/Localhost/#/Main.WebHome/view";
test("has title", async ({ page }) => {
  await page.goto(localDefaultPage);

  // Expect a title "to contain" a substring.
  await expect(page).toHaveTitle(/Cristal Wiki/);
});

test("has content", async ({ page }) => {
  await page.goto(localDefaultPage);

  const locator = page.locator("#xwikicontent");
  await expect(locator).toContainText("Welcome to Main.WebHome");
});

test("can follow links", async ({ page }) => {
  await page.goto(localDefaultPage);

  await page.locator('a[href="Page2.WebHome"]').click();

  await expect(page.locator("#xwikicontent")).toContainText(
    "Welcome to Page2.WebHome",
  );

  await page.goBack();

  await expect(page.locator("#xwikicontent")).toContainText(
    "Welcome to Main.WebHome",
  );
});

test("has breadcrumb", async ({ page }) => {
  await page.goto(localDefaultPage);

  const breadcrumbItems = page.locator("#breadcrumbRoot li a");

  await expect(breadcrumbItems).toHaveCount(2);
  await expect(breadcrumbItems.first()).toContainText("Home");
  expect(await breadcrumbItems.first().getAttribute("href")).toEqual(
    "http://localhost:15680/xwiki/bin/view/Main/",
  );
  await expect(breadcrumbItems.last()).toContainText("Main");
  expect(await breadcrumbItems.last().getAttribute("href")).toEqual(
    "http://localhost:15680/xwiki/bin/view/Main/",
  );
});

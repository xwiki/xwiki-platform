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
import test, { expect } from "@playwright/test";
import { screenshotIfTestFailed } from "./screenshot-failures";

test.beforeEach(async ({ page }) => {
  await page.goto("/XWikiNoRealtime/#/Main.WebHome/edit");
  await page.waitForLoadState("networkidle");

  await expect(page).toHaveTitle("Cristal Wiki");

  await page.waitForSelector(".bn-container");
  expect(page.locator(".bn-container")).toBeVisible();

  await page.waitForSelector(".bn-editor");
  expect(page.locator(".bn-editor")).toBeVisible();
});

test.afterEach(async ({ page }, testInfo) => {
  await screenshotIfTestFailed(page, testInfo);
});

test("BlockNote shows up", async ({ page }) => {
  await expect(page.locator(".bn-container")).toBeVisible();
  await expect(page.locator(".bn-editor")).toBeVisible();
  await expect(page.locator(".bn-editor")).toContainClass("ProseMirror");
});

test("Content can be input", async ({ page }) => {
  const blocknote = page.locator(".bn-editor");

  await blocknote.fill("Hello world!");
  await expect(blocknote).toHaveText("Hello world!");
});

test("Content can be saved", async ({ page }) => {
  const blocknote = page.locator(".bn-editor");

  const str = `Randomized message ${Math.random()}`;

  await blocknote.clear();
  await blocknote.fill(str);
  await expect(blocknote).toHaveText(str);

  const saveBtn = page.locator(".pagemenu button:last-child");
  await expect(saveBtn).toBeVisible();

  // TODO: storage mocking to enable saving (https://jira.xwiki.org/browse/CRISTAL-579)

  // This code is left here as a reference as it should work perfectly once we have storage mocking

  // await saveBtn.click();

  // await page.waitForURL("/XWikiNoRealtime/#/Main.WebHome/view");
  // await page.waitForLoadState("networkidle");

  // const content = page.locator("#xwikicontent");
  // await expect(content).toBeVisible();
  // await expect(content).toHaveText(str);
});

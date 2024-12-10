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
import { DesignSystem } from "./DesignSystem";
import { BreadcrumbPageObject } from "./pageObjects/Breadcrumb";
import { HistoryExtraTabPageObject } from "./pageObjects/HistoryExtraTab";
import { NavigationTreePageObject } from "./pageObjects/NavigationTree";
import { SidebarPageObject } from "./pageObjects/Sidebar";

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

// A set of variability setting
const configs: {
  localDefaultPage: string;
  offlineDefaultPage?: string;
  name: string;
  designSystem: DesignSystem;
}[] = [
  {
    name: "Vuetify",
    localDefaultPage: "/Localhost/#/Main.WebHome/view",
    offlineDefaultPage: "/LocalhostOffline/#/Main.Offline/view",
    designSystem: DesignSystem.VUETIFY,
  },
  {
    name: "Shoelace",
    localDefaultPage: "/LocalhostSL/#/Main.WebHome/view",
    designSystem: DesignSystem.SHOELACE,
  },
];
configs.forEach(
  ({ name, localDefaultPage, offlineDefaultPage, designSystem }) => {
    test(`[${name}] has title`, async ({ page }) => {
      await page.goto(localDefaultPage);

      // Expect a title "to contain" a substring.
      await expect(page).toHaveTitle(/Cristal Wiki/);
    });

    test(`[${name}] has content`, async ({ page }) => {
      await page.goto(localDefaultPage);

      const locator = page.locator("#xwikicontent");
      await expect(locator).toContainText("Welcome to Main.WebHome");
    });

    test(`[${name}] can follow links`, async ({ page }) => {
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

    test(`[${name}] has breadcrumb`, async ({ page }) => {
      await page.goto(localDefaultPage);

      const breadcrumbItems = await new BreadcrumbPageObject(
        page,
        designSystem,
      ).findItems();

      expect(breadcrumbItems.length).toEqual(2);
      await expect(breadcrumbItems[0].getText()).toContainText("Home");
      expect(await breadcrumbItems[0].getLink()).toEqual(
        "#/Main.WebHome/view",
      );
      await expect(breadcrumbItems[1].getText()).toContainText("Main");
      expect(await breadcrumbItems[1].getLink()).toEqual(
        "#/Main.WebHome/view",
      );
    });

    test(`[${name}] has navigation tree`, async ({ page }) => {
      await page.goto(localDefaultPage);

      await new SidebarPageObject(page).openSidebar();
      const navigationTreeNodes = await new NavigationTreePageObject(
        page,
        designSystem,
      ).findItems();

      expect(navigationTreeNodes.length).toEqual(4);
      await expect(navigationTreeNodes[0].getText()).toContainText("Help");
      expect(await navigationTreeNodes[0].getLink()).toEqual(
        "#/Help.WebHome/view",
      );
      await expect(navigationTreeNodes[1].getText()).toContainText(
        "Terminal Page",
      );
      expect(await navigationTreeNodes[1].getLink()).toEqual("#/Terminal/view");
      await expect(navigationTreeNodes[2].getText()).toContainText(
        "Deep Page Root",
      );
      expect(await navigationTreeNodes[2].getLink()).toEqual(
        "#/Deep1.WebHome/view",
      );
      await expect(navigationTreeNodes[3].getText()).toContainText(
        "Cristal Wiki",
      );
      expect(await navigationTreeNodes[3].getLink()).toEqual(
        "#/Main.WebHome/view",
      );

      await navigationTreeNodes[2].expand();
      const children = await navigationTreeNodes[2].getChildren();
      expect(children.length).toEqual(1);
      await expect(children[0].getText()).toContainText("Deep Page Leaf");
      expect(await children[0].getLink()).toEqual("#/Deep1.Deep2/view");
    });

    test(`[${name}] has working history`, async ({ page }) => {
      await page.goto(localDefaultPage);

      const revisions = await new HistoryExtraTabPageObject(
        page,
        designSystem,
      ).findRevisions();
      expect(revisions.length == 3);
      expect(await revisions[0].getVersion()).toEqual("3.1");
      expect(await revisions[1].getVersion()).toEqual("2.1");
      expect(await revisions[2].getVersion()).toEqual("1.1");
      expect(await revisions[0].getDate()).toEqual("1/1/24, 8:00 PM");
      expect(await revisions[1].getDate()).toEqual("1/1/22, 8:00 PM");
      expect(await revisions[2].getDate()).toEqual("1/1/20, 8:00 PM");
      expect(await revisions[0].getUser()).toEqual("XWiki.User3");
      expect(await revisions[1].getUser()).toEqual("XWiki.User2");
      expect(await revisions[2].getUser()).toEqual("XWiki.User1");
      expect(await revisions[0].getComment()).toEqual("Best version");
      expect(await revisions[1].getComment()).toEqual("");
      expect(await revisions[2].getComment()).toEqual("Initial version");

      // We open a revision and check that the content updated.
      await (await revisions[1].getLink()).click();
      await expect(page.locator("#xwikicontent")).toContainText("Revision 2.1");
    });

    test(`[${name}] has working editor on new page`, async ({ page }) => {
      await page.goto(localDefaultPage);

      await new SidebarPageObject(page).openSidebar();
      await page.locator("#sidebar #new-page-button").nth(0).click();

      const newPageDialogButton = page.getByRole('button', { name: 'Create' }).nth(0);

      // The button can end up unclickable sometimes, due to the speed of the test.
      // This ensures that it still gets clicked no matter what.
      await expect(newPageDialogButton).toBeEnabled();
      await newPageDialogButton.dispatchEvent("click");

      const editorHeader = page.locator(".edit-wrapper .doc-header input").nth(0);
      const editorContent = page.locator(".edit-wrapper .doc-content p").nth(0);
      expect(await editorHeader.getAttribute("placeholder")).toEqual("NewPage");
      expect(editorHeader).toBeEmpty();
      expect(await editorContent.getAttribute("data-placeholder")).toEqual(
        "Type '/' to show the available actions"
      );
      expect(editorContent).toBeEmpty();
    });

    if (offlineDefaultPage) {
      test(`[${name}] offline actually fetch updated versions`, async ({
        page,
      }) => {
        await page.goto(offlineDefaultPage);

        const locator = page.locator("#xwikicontent");
        await expect(locator).toContainText("Welcome to Main.Offline");
        const textBefore = (await page.locator(".offlinecount").textContent())!;
        await page.reload();
        await expect(locator).toContainText("Welcome to Main.Offline");
        // Assert that at some point to page content is reloaded with a more recent
        // version from the server (the counter is incremented by one at each
        // request).
        const textContent = await page.locator(".offlinecount").textContent();
        expect(textContent).toBe(`${parseInt(textBefore) + 1}`);
      });
    }
  },
);

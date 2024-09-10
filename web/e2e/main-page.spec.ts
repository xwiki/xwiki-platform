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
import { NavigationTreePageObject } from "./pageObjects/NavigationTree";

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
        "http://localhost:15680/xwiki/bin/view/Main/",
      );
      await expect(breadcrumbItems[1].getText()).toContainText("Main");
      expect(await breadcrumbItems[1].getLink()).toEqual(
        "http://localhost:15680/xwiki/bin/view/Main/",
      );
    });

    test(`[${name}] has navigation tree`, async ({ page, isMobile }) => {
      await page.goto(localDefaultPage);

      if (isMobile) {
        const openSidebar = page.locator(".open-sidebar");
        await openSidebar.nth(0).click();
      }

      const navigationTreeNodes = await new NavigationTreePageObject(
        page,
        designSystem,
      ).findItems();

      expect(navigationTreeNodes.length).toEqual(3);
      await expect(navigationTreeNodes[0].getText()).toContainText("Help");
      expect(await navigationTreeNodes[0].getLink()).toEqual(
        "#/Help.WebHome/view",
      );
      await expect(navigationTreeNodes[1].getText()).toContainText("Terminal Page");
      expect(await navigationTreeNodes[1].getLink()).toEqual(
        "#/Terminal/view",
      );
      await expect(navigationTreeNodes[2].getText()).toContainText("Deep Page Root");
      expect(await navigationTreeNodes[2].getLink()).toEqual(
        "#/Deep1.WebHome/view",
      );

      await navigationTreeNodes[2].expand();
      const children = await navigationTreeNodes[2].getChildren();
      expect(children.length).toEqual(1);
      await expect(children[0].getText()).toContainText("Deep Page Leaf");
      expect(await children[0].getLink()).toEqual(
        "#/Deep1.Deep2/view",
      )
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
        await expect(page.locator(".offlinecount")).not.toContainText(
          textBefore,
        );
      });
    }
  },
);

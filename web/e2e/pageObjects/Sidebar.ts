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

import { expect } from "@playwright/test";
import type { Locator, Page } from "@playwright/test";

/**
 * Page object to interact with the sidebar.
 * @since 0.13
 */
export class SidebarPageObject {
  private sidebarLocator: Locator;

  constructor(
    private readonly page: Page,
  ) {
    this.sidebarLocator = this.page.locator("#sidebar");
  }

  async openSidebar(): Promise<void> {
    await expect(this.sidebarLocator.locator("..")).toBeVisible();
    if (!await this.sidebarLocator.isVisible()) {
      const openSidebar = this.page.locator(".open-sidebar");
      await openSidebar.nth(0).click();
    }
    await expect(this.sidebarLocator).toBeVisible();
  }

  async hideSidebar(): Promise<void> {
    await expect(this.sidebarLocator.locator("..")).toBeVisible();
    if (!await this.sidebarLocator.isHidden()) {
      const hideSidebar = this.page.locator(".hide-sidebar:visible").or(
        this.page.locator(".close-sidebar:visible")
      );
      await hideSidebar.nth(0).click();
    }
    await expect(this.sidebarLocator).toBeHidden();
  }

  async pinSidebar(): Promise<void> {
    await this.openSidebar();
    const pinSidebar = this.page.locator(".pin-sidebar");
    if (await pinSidebar.isVisible()) {
      await pinSidebar.nth(0).click();
    }
    await expect(pinSidebar).toBeHidden();
  }
}

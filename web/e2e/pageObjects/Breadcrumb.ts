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

import { Locator, Page } from "@playwright/test";
import { DesignSystem } from "../DesignSystem";

/**
 * The page object interface for a segment of a breadcrumb.
 */
export interface BreadcrumbSegmentElement {
  /**
   * @returns the text element of the segment
   */
  getText(): Locator;

  /**
   * @returns the segment link
   */
  getLink(): Locator;

  /**
   * @returns the href value of the segment link
   */
  getLinkTarget(): Promise<string>;
}

/**
 * Page object to interact with a breadcrumb element.
 */
export class BreadcrumbPageObject {
  constructor(
    private readonly page: Page,
    private readonly designSystem: DesignSystem,
  ) {}

  async findItems(): Promise<BreadcrumbSegmentElement[]> {
    // TODO: we need to generalize that to make it easy to extend with more design systems
    switch (this.designSystem) {
      case DesignSystem.VUETIFY:
        return await this.findItemsVuetify();
      case DesignSystem.SHOELACE:
        return await this.findItemsShoelace();
    }
  }

  private async findItemsShoelace(): Promise<BreadcrumbSegmentElement[]> {
    return await this.findItemsInternal(
      ".page-header sl-breadcrumb sl-breadcrumb-item",
      (element) => {
        return {
          getText() {
            return element;
          },
          getLink() {
            return element.locator(".breadcrumb-item__label--link");
          },
          async getLinkTarget() {
            return (await this.getLink().getAttribute("href"))!;
          },
        };
      },
    );
  }

  private async findItemsVuetify(): Promise<BreadcrumbSegmentElement[]> {
    return await this.findItemsInternal(".page-header .v-breadcrumbs li a", (element) => {
      return {
        getText() {
          return element;
        },
        getLink() {
          return element;
        },
        async getLinkTarget() {
          return (await this.getLink().getAttribute("href"))!;
        },
      };
    });
  }

  private async findItemsInternal(
    selector: string,
    wrap: (element: Locator) => BreadcrumbSegmentElement,
  ) {
    const locator = this.page.locator(selector);
    // Waiting for the first breadcrumb segment to be visible before calling .all() (which otherwise returns an empty
    // list if no elements are visible at the time of calling). Assuming the rest of the breadcrumb segment becomes
    // visible at the same time as the first one.
    await locator.nth(0).waitFor({
      state: "visible",
    });
    return (await locator.all()).map(wrap);
  }
}

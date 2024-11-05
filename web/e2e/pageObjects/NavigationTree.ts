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
 * The page object interface for a node of a navigation tree.
 */
export interface NavigationTreeNode {
  /**
   * @returns the text element of the node
   */
  getText(): Locator;

  /**
   * @returns the href value of the node link
   */
  getLink(): Promise<string>;

  /**
   * @returns the children of the node
   */
  getChildren(): Promise<Array<NavigationTreeNode>>;

  /**
   * Expands the node's children.
   */
  expand(): Promise<void>;
}

/**
 * Page object to interact with a navigation tree element.
 */
export class NavigationTreePageObject {
  constructor(
    private readonly page: Page,
    private readonly designSystem: DesignSystem,
  ) {}

  async findItems(): Promise<Array<NavigationTreeNode>> {
    // TODO: we need to generalize that to make it easy to extend with more design systems
    switch (this.designSystem) {
      case DesignSystem.VUETIFY:
        return await this.findItemsVuetify();
      case DesignSystem.SHOELACE:
        return await this.findItemsShoelace();
    }
  }

  private async findItemsShoelace(): Promise<Array<NavigationTreeNode>> {
    return await NavigationTreePageObject.findItemsInternal(
      this.page.locator("#sidebar sl-tree:visible sl-tree-item"),
      NavigationTreePageObject.wrapShoelace,
    );
  }

  private static wrapShoelace(element: Locator): NavigationTreeNode {
    const label = element.locator("a");
    return {
      getText() {
        return label;
      },
      async getLink() {
        return (await label.getAttribute("href"))!;
      },
      async getChildren() {
        return await NavigationTreePageObject.findItemsInternal(
          element.locator("sl-tree-item"),
          NavigationTreePageObject.wrapShoelace,
        );
      },
      async expand() {
        await element.locator("div[part='expand-button']").nth(0).click();
      },
    };
  }

  private static wrapVuetify(element: Locator): NavigationTreeNode {
    const label = element.locator("a");
    return {
      getText() {
        return label;
      },
      async getLink() {
        return (await label.getAttribute("href"))!;
      },
      async getChildren() {
        return await NavigationTreePageObject.findItemsInternal(
          element.locator(".v-list-group__items > div"),
          NavigationTreePageObject.wrapVuetify,
        );
      },
      async expand() {
        await element.locator("button").nth(0).click();
      },
    };
  }

  private async findItemsVuetify(): Promise<Array<NavigationTreeNode>> {
    return await NavigationTreePageObject.findItemsInternal(
      this.page.locator("#sidebar .sidebar-content .v-treeview > div"),
      NavigationTreePageObject.wrapVuetify,
    );
  }

  private static async findItemsInternal(
    locator: Locator,
    wrap: (element: Locator) => NavigationTreeNode,
  ) {
    // Waiting for the first item to be visible before calling .all() (which otherwise returns an empty
    // list if no elements are visible at the time of calling).
    await locator.nth(0).waitFor({
      state: "visible",
    });
    return (await locator.all()).map(wrap);
  }
}

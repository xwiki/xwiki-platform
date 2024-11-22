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

import { DesignSystem } from "../DesignSystem";
import type { Locator, Page } from "@playwright/test";

/**
 * The page object interface for a revision.
 */
export interface RevisionElement {
  /**
   * @returns the version of the revision
   */
  getVersion(): Promise<string>;

  /**
   * @returns the date of the revision
   */
  getDate(): Promise<string>;

  /**
   * @returns the user that made the revision
   */
  getUser(): Promise<string>;

  /**
   * @returns the comment for the revision
   */
  getComment(): Promise<string>;

  /**
   * @returns the link to the revision
   */
  getLink(): Promise<Locator>;
}

/**
 * Page object to interact with the History extra tab.
 */
export class HistoryExtraTabPageObject {
  constructor(
    private readonly page: Page,
    private readonly designSystem: DesignSystem,
  ) {}

  async findRevisions(): Promise<RevisionElement[]> {
    // We first open the tab.
    switch (this.designSystem) {
      case DesignSystem.VUETIFY:
        await this.page
          .locator("#content .doc-info-extra .v-tabs button[value='history']")
          .click();
        break;
      case DesignSystem.SHOELACE:
        await this.page
          .locator(
            "#content .doc-info-extra sl-tab-group sl-tab[panel='history']",
          )
          .click();
        break;
    }

    // Then we wait for the history to be loaded.
    const revisions: Locator = this.page.locator(
      "#content .doc-info-extra table tr",
    );
    await revisions.nth(0).waitFor({
      state: "visible",
    });

    // Finally we return the revisions.
    return (await revisions.all()).map((revision) => {
      return {
        async getVersion() {
          return revision.locator("td").nth(0).textContent();
        },
        async getDate() {
          return revision
            .locator("td")
            .nth(1)
            .locator("a")
            .nth(0)
            .textContent();
        },
        async getUser() {
          const text = await revision.locator("td").nth(1).innerText();
          return text.replace(/^.* by /, "").replace(/\n.*$/, "");
        },
        async getComment() {
          const text = await revision.locator("td").nth(1).innerText();
          return text.replace(/^.*\n/, "");
        },
        async getLink() {
          return revision.locator("td").nth(1).locator("a").nth(0);
        },
      } as RevisionElement;
    });
  }
}

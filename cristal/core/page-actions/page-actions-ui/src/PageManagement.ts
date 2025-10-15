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

import messages from "./translations";
import { AbstractPageActionCategory } from "@xwiki/cristal-page-actions-api";
import { inject, injectable } from "inversify";
import type { PageAction } from "@xwiki/cristal-page-actions-api";
import type { PageRenameManagerProvider } from "@xwiki/cristal-rename-api";
import type { Component } from "vue";

const PAGE_MANAGEMENT_ID: string = "page-management";

@injectable()
class PageManagementActionCategory extends AbstractPageActionCategory {
  title: string;
  constructor() {
    super(messages);
    this.title = this.t("page.action.category.page.management.title");
  }
  id: string = PAGE_MANAGEMENT_ID;
  order = 1000;
}

/**
 * {@link PageAction} to change the parent of a page.
 * @since 0.14
 * @beta
 */
@injectable()
class PageMoveAction implements PageAction {
  constructor(
    @inject("PageRenameManagerProvider")
    private readonly pageRenameManagerProvider: PageRenameManagerProvider,
  ) {}

  id = "page-move";
  categoryId: string = PAGE_MANAGEMENT_ID;
  order = 3000;

  async enabled(): Promise<boolean> {
    return this.pageRenameManagerProvider.has();
  }

  async component(): Promise<Component> {
    return (await import("./vue/MovePage.vue")).default;
  }
}

/**
 * {@link PageAction} to change the name part of a page reference.
 * @since 0.14
 * @beta
 */
@injectable()
class PageRenameAction implements PageAction {
  constructor(
    @inject("PageRenameManagerProvider")
    private readonly pageRenameManagerProvider: PageRenameManagerProvider,
  ) {}

  id = "page-rename";
  categoryId: string = PAGE_MANAGEMENT_ID;
  order = 4000;

  async enabled(): Promise<boolean> {
    return this.pageRenameManagerProvider.has();
  }

  async component(): Promise<Component> {
    return (await import("./vue/RenamePage.vue")).default;
  }
}

@injectable()
class PageDeleteAction implements PageAction {
  id = "page-delete";
  categoryId: string = PAGE_MANAGEMENT_ID;
  order = 5000;

  async enabled(): Promise<boolean> {
    return true;
  }

  async component(): Promise<Component> {
    return (await import("./vue/DeletePage.vue")).default;
  }
}

export {
  PageDeleteAction,
  PageManagementActionCategory,
  PageMoveAction,
  PageRenameAction,
};

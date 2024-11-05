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

import DeletePage from "./vue/DeletePage.vue";
import messages from "./translations";
import { type Component } from "vue";
import { injectable } from "inversify";
import {
  type PageAction,
  AbstractPageActionCategory,
} from "@xwiki/cristal-page-actions-api";

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

@injectable()
class PageDeleteAction implements PageAction {
  id = "page-delete";
  categoryId: string = PAGE_MANAGEMENT_ID;
  order = 5000;

  async component(): Promise<Component> {
    return DeletePage;
  }
}

export { PageManagementActionCategory, PageDeleteAction };

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

import {
  PageDeleteAction,
  PageManagementActionCategory,
  PageMoveAction,
  PageRenameAction,
} from "./PageManagement";
import PageActions from "./vue/PageActions.vue";
import type {
  PageAction,
  PageActionCategory,
} from "@xwiki/cristal-page-actions-api";
import type { Container } from "inversify";

/**
 * @beta
 */
class ComponentInit {
  constructor(container: Container) {
    container
      .bind<PageActionCategory>("PageActionCategory")
      .to(PageManagementActionCategory)
      .whenDefault();
    container.bind<PageAction>("PageAction").to(PageMoveAction).whenDefault();
    container.bind<PageAction>("PageAction").to(PageRenameAction).whenDefault();
    container.bind<PageAction>("PageAction").to(PageDeleteAction).whenDefault();
  }
}

/**
 * @since 0.11
 * @beta
 */
const PageActionsAnnotated = PageActions;

export { ComponentInit, PageActionsAnnotated as PageActions };

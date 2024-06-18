/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/

import { Component } from "vue";

/**
 * Defines the structure of a tab. Including its content.
 *
 * @since 0.9
 */
interface ExtraTab {
  /**
   * The unique id of a tab
   */
  id: string;

  /**
   * And integer value, panels are sorted by ascending numbers.
   */
  order: number;

  /**
   * The title of the tab.
   */
  title: string;

  /**
   * The component to display the matching panel
   * The component is wrapped in a promise so that it's possible to lazy-load
   * it.
   */
  panel(): Promise<Component>;
}

/**
 * Operations to access the available extra tabs.
 *
 * @since 0.9
 */
interface ExtraTabsService {
  /**
   * Returns the list of available extra tabs
   */
  list(): Promise<ExtraTab[]>;
}

export { type ExtraTabsService, type ExtraTab };

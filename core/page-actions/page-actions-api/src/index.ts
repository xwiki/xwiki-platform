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

import { injectable, unmanaged } from "inversify";
import { ComposerTranslation, useI18n } from "vue-i18n";
import type { Component } from "vue";

/**
 * Defines a category for page actions.
 *
 * @since 0.11
 */
interface PageActionCategory {
  /**
   * The unique id of the category.
   */
  id: string;

  /**
   * An integer value, categories are sorted by ascending numbers.
   */
  order: number;

  /**
   * The title of the category.
   */
  title: string;
}

/**
 * Operations to access the available categories.
 *
 * @since 0.11
 */
interface PageActionCategoryService {
  /**
   * Returns the list of available categories.
   */
  list(): PageActionCategory[];
}

/**
 * Defines a page action.
 *
 * @since 0.11
 */
interface PageAction {
  /**
   * The unique id of the action.
   */
  id: string;

  /**
   * The id of the category of the action.
   */
  categoryId: string;

  /**
   * An integer value, actions are sorted by ascending numbers.
   */
  order: number;

  /**
   * Get a Vue component for this action.
   * The component should handle the following props:
   *   - currentPage: PageData corresponding to the displayed page
   *   - currentPageName: name of the displayed page
   */
  component(): Promise<Component>;
}

/**
 * Operations to access the available actions.
 *
 * @since 0.11
 */
interface PageActionService {
  /**
   * Returns the list of available actions for a given category.
   *
   * @param categoryId - the id of the category
   */
  list(categoryId: string): PageAction[];
}

/**
 * Abstract class helping with localization of action categories.
 *
 * @since 0.11
 */
@injectable()
abstract class AbstractPageActionCategory implements PageActionCategory {
  protected t: ComposerTranslation;

  constructor(@unmanaged() messages: Record<string, Record<string, string>>) {
    const { t, mergeLocaleMessage } = useI18n();
    for (const messagesKey in messages) {
      mergeLocaleMessage(messagesKey, messages[messagesKey]);
    }
    this.t = t;
  }

  abstract id: string;
  abstract order: number;
  abstract title: string;
}

export {
  AbstractPageActionCategory,
  type PageAction,
  type PageActionCategory,
  type PageActionCategoryService,
  type PageActionService,
};

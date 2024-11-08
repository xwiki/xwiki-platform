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
import { Component } from "vue";
import { ComposerTranslation, useI18n } from "vue-i18n";

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

/**
 * Abstract class helping with localization of extra tabs.
 *
 * @since 0.9
 */
@injectable()
abstract class AbstractExtraTab implements ExtraTab {
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

  abstract panel(): Promise<Component>;
}

export { AbstractExtraTab, type ExtraTab, type ExtraTabsService };

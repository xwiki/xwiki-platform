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

import messages from "./translations";
import HistoryTab from "./vue/HistoryTab.vue";
import { AbstractExtraTab } from "@xwiki/cristal-extra-tabs-api";
import { inject, injectable } from "inversify";
import { Component } from "vue";
import type { PageRevisionManagerProvider } from "@xwiki/cristal-history-api";

@injectable()
export class HistoryExtraTab extends AbstractExtraTab {
  title: string;

  constructor(
    @inject("PageRevisionManagerProvider")
    private readonly pageRevisionManagerProvider: PageRevisionManagerProvider,
  ) {
    super(messages);
    this.title = this.t("history.extraTabs.title");
  }

  order = 3000;
  id = "history";

  async panel(): Promise<Component> {
    return HistoryTab;
  }

  override async enabled(): Promise<boolean> {
    return this.pageRevisionManagerProvider.has();
  }
}

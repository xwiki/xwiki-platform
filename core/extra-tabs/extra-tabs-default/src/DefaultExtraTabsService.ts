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

import { ExtraTab, ExtraTabsService } from "@xwiki/cristal-extra-tabs-api";

import { injectable, multiInject } from "inversify";
import { sortBy } from "lodash";

/**
 * @since 0.9
 */
@injectable()
class DefaultExtraTabsService implements ExtraTabsService {
  constructor(@multiInject("ExtraTab") private extraTabs: ExtraTab[]) {}

  /**
   * Returns the list of tabs sorted by ascending order
   */
  async list(): Promise<ExtraTab[]> {
    const enabledTabs: boolean[] = await Promise.all(
      this.extraTabs.map(async (tab) => tab.enabled()),
    );
    return sortBy(
      this.extraTabs.filter((_, i) => enabledTabs[i]),
      ["order"],
    );
  }
}

export { DefaultExtraTabsService };

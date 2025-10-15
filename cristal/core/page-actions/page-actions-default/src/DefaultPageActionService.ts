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

import { injectable, multiInject } from "inversify";
import { sortBy } from "lodash-es";
import type {
  PageAction,
  PageActionService,
} from "@xwiki/cristal-page-actions-api";

/**
 * @since 0.11
 * @beta
 */
@injectable()
class DefaultPageActionService implements PageActionService {
  constructor(
    @multiInject("PageAction")
    private actions: PageAction[],
  ) {}

  async list(categoryId: string): Promise<PageAction[]> {
    const enabledActions: boolean[] = await Promise.all(
      this.actions.map(
        async (action) =>
          (await action.enabled()) && action.categoryId == categoryId,
      ),
    );
    return sortBy(
      this.actions.filter((_, i) => enabledActions[i]),
      ["order"],
    );
  }
}

export { DefaultPageActionService };

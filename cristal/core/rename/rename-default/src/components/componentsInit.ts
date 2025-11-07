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

import { name as pageRenameManagerName } from "@xwiki/cristal-rename-api";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";
import type {
  PageRenameManager,
  PageRenameManagerProvider,
} from "@xwiki/cristal-rename-api";

/**
 * Default implementation for {@link PageRenameManagerProvider}.
 *
 * @since 0.14
 * @beta
 **/
@injectable()
class DefaultPageRenameManagerProvider implements PageRenameManagerProvider {
  constructor(@inject("CristalApp") private readonly cristalApp: CristalApp) {}

  has(): boolean {
    const container = this.cristalApp.getContainer();
    const wikiConfigType = this.cristalApp.getWikiConfig().getType();
    return container.isBound(pageRenameManagerName, { name: wikiConfigType });
  }

  get(): PageRenameManager {
    const container = this.cristalApp.getContainer();
    const wikiConfigType = this.cristalApp.getWikiConfig().getType();
    return container.get<PageRenameManager>(pageRenameManagerName, {
      name: wikiConfigType,
    });
  }
}

export { DefaultPageRenameManagerProvider };

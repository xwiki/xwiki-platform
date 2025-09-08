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

import { name } from "@xwiki/cristal-hierarchy-api";
import { getPageHierarchyFromPath } from "@xwiki/cristal-hierarchy-default";
import { Container, inject, injectable } from "inversify";
import type { CristalApp, Logger } from "@xwiki/cristal-api";
import type {
  PageHierarchyItem,
  PageHierarchyResolver,
} from "@xwiki/cristal-hierarchy-api";
import type {
  DocumentReference,
  SpaceReference,
} from "@xwiki/cristal-model-api";

/**
 * Implementation of PageHierarchyResolver for Nextcloud backend.
 *
 * @since 0.10
 * @beta
 **/
@injectable()
class NextcloudPageHierarchyResolver implements PageHierarchyResolver {
  private cristalApp: CristalApp;
  public logger: Logger;

  constructor(
    @inject("Logger") logger: Logger,
    @inject("CristalApp") cristalApp: CristalApp,
  ) {
    this.logger = logger;
    this.logger.setModule("storage.components.NextcloudPageHierarchyResolver");
    this.cristalApp = cristalApp;
  }

  async getPageHierarchy(
    page: DocumentReference | SpaceReference,
    includeHomePage: boolean = true,
  ): Promise<Array<PageHierarchyItem>> {
    let hierarchy: Array<PageHierarchyItem> = includeHomePage
      ? [
          {
            label: "Home",
            pageId: this.cristalApp.getWikiConfig().homePage,
            url: this.cristalApp.getRouter().resolve({
              name: "view",
              params: { page: this.cristalApp.getWikiConfig().homePage },
            }).href,
          },
        ]
      : [];
    if (page != null) {
      hierarchy = hierarchy.concat(
        await getPageHierarchyFromPath(page, this.cristalApp),
      );
    }
    return hierarchy;
  }
}

/**
 * @beta
 */
export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<PageHierarchyResolver>(name)
      .to(NextcloudPageHierarchyResolver)
      .inSingletonScope()
      .whenNamed("Nextcloud");
  }
}

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

import { Container, inject, injectable } from "inversify";
import type { CristalApp, PageData, Logger } from "@xwiki/cristal-api";
import {
  name as PageHierarchyResolverName,
  type PageHierarchyItem,
  type PageHierarchyResolver,
  type PageHierarchyResolverProvider,
} from "@xwiki/cristal-hierarchy-api";

/**
 * Default implementation for PageHierarchyResolver.
 * This should can be used as a fallback, and only returns the homepage and the current page.
 *
 * @since 0.9
 **/
@injectable()
class DefaultPageHierarchyResolver implements PageHierarchyResolver {
  private cristalApp: CristalApp;
  public logger: Logger;

  constructor(
    @inject<Logger>("Logger") logger: Logger,
    @inject<CristalApp>("CristalApp") cristalApp: CristalApp,
  ) {
    this.logger = logger;
    this.logger.setModule("api.components.DefaultPageHierarchyResolver");
    this.cristalApp = cristalApp;
  }

  async getPageHierarchy(
    pageData: PageData,
  ): Promise<Array<PageHierarchyItem>> {
    const hierarchy: Array<PageHierarchyItem> = [
      {
        label: "Home",
        url: this.cristalApp.getRouter().resolve({
          name: "view",
          params: { page: this.cristalApp.getWikiConfig().homePage },
        }).href,
      },
    ];
    if (pageData != null) {
      hierarchy.push({
        label: pageData.name,
        url: window.location.href,
      });
    }
    return hierarchy;
  }
}

/**
 * Default implementation for PageHierarchyResolverProvider.
 *
 * @since 0.9
 **/
@injectable()
class DefaultPageHierarchyResolverProvider
  implements PageHierarchyResolverProvider
{
  private cristalApp: CristalApp;
  public logger: Logger;

  constructor(
    @inject<Logger>("Logger") logger: Logger,
    @inject<CristalApp>("CristalApp") cristalApp: CristalApp,
  ) {
    this.logger = logger;
    this.logger.setModule(
      "core.hierarchy.hierarchy-default.DefaultPageHierarchyResolver",
    );
    this.cristalApp = cristalApp;
  }

  get(): PageHierarchyResolver {
    const container = this.cristalApp.getContainer();
    const wikiConfigType = this.cristalApp.getWikiConfig().getType();
    if (container.isBoundNamed(PageHierarchyResolverName, wikiConfigType)) {
      return container.getNamed<PageHierarchyResolver>(
        PageHierarchyResolverName,
        wikiConfigType,
      );
    } else {
      return container.get<PageHierarchyResolver>(PageHierarchyResolverName);
    }
  }
}

export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<PageHierarchyResolver>(PageHierarchyResolverName)
      .to(DefaultPageHierarchyResolver)
      .inSingletonScope()
      .whenTargetIsDefault();
    container
      .bind<PageHierarchyResolverProvider>(
        `${PageHierarchyResolverName}Provider`,
      )
      .to(DefaultPageHierarchyResolverProvider)
      .inSingletonScope()
      .whenTargetIsDefault();
  }
}

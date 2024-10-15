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

import { Container, inject, injectable } from "inversify";
import type { CristalApp, PageData, Logger } from "@xwiki/cristal-api";
import {
  name,
  type PageHierarchyItem,
  type PageHierarchyResolver,
} from "@xwiki/cristal-hierarchy-api";
import { getRestSpacesApiUrl } from "../utils";
import type { AuthenticationManagerProvider } from "@xwiki/cristal-authentication-api";

/**
 * Implementation of PageHierarchyResolver for the XWiki backend.
 *
 * @since 0.9
 **/
@injectable()
class XWikiPageHierarchyResolver implements PageHierarchyResolver {
  private cristalApp: CristalApp;
  public defaultHierarchyResolver: PageHierarchyResolver;
  public logger: Logger;

  constructor(
    @inject<Logger>("Logger") logger: Logger,
    @inject<CristalApp>("CristalApp") cristalApp: CristalApp,
    @inject("PageHierarchyResolver")
    pageHierarchyResolver: PageHierarchyResolver,
    @inject<AuthenticationManagerProvider>("AuthenticationManagerProvider")
    private authenticationManagerProvider: AuthenticationManagerProvider,
  ) {
    this.logger = logger;
    this.logger.setModule("storage.components.XWikiPageHierarchyResolver");
    this.cristalApp = cristalApp;
    this.defaultHierarchyResolver = pageHierarchyResolver;
  }

  async getPageHierarchy(
    pageData: PageData,
  ): Promise<Array<PageHierarchyItem>> {
    const documentId = pageData.document.getIdentifier();
    if (documentId == null) {
      this.logger.debug(
        `No identifier found for page ${pageData.name}, falling back to default hierarchy resolver.`,
      );
      return this.defaultHierarchyResolver.getPageHierarchy(pageData);
    }
    // TODO: support subwikis.
    const restApiUrl = getRestSpacesApiUrl(
      this.cristalApp.getWikiConfig(),
      documentId,
    );

    try {
      const authorization = await this.authenticationManagerProvider
        .get()
        ?.getAuthorizationHeader();
      const headers: { Accept: string; Authorization?: string } = {
        Accept: "application/json",
      };

      if (authorization) {
        headers.Authorization = authorization;
      }
      const response = await fetch(restApiUrl, { headers });
      const jsonResponse = await response.json();
      const hierarchy: Array<PageHierarchyItem> = [];
      jsonResponse.hierarchy.items.forEach(
        (hierarchyItem: { label: string; url: string }) => {
          hierarchy.push({
            label: hierarchyItem.label,
            url: hierarchyItem.url,
          });
        },
      );
      hierarchy[0].label = "Home";
      // If the last item is not terminal (i.e., WebHome) we exclude it.
      if (hierarchy[hierarchy.length - 1].url.endsWith("/")) {
        hierarchy.pop();
      }
      return hierarchy;
    } catch (error) {
      this.logger.error(error);
      this.logger.debug(
        `Could not load hierarchy for page ${pageData.name}, falling back to default hierarchy resolver.`,
      );
      return this.defaultHierarchyResolver.getPageHierarchy(pageData);
    }
  }
}

export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<PageHierarchyResolver>(name)
      .to(XWikiPageHierarchyResolver)
      .inSingletonScope()
      .whenTargetNamed("XWiki");
  }
}

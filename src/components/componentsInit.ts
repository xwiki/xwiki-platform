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

import { name } from "@xwiki/cristal-hierarchy-api";
import { getRestSpacesApiUrl } from "@xwiki/cristal-xwiki-utils";
import { Container, inject, injectable, named } from "inversify";
import type { CristalApp, Logger, PageData } from "@xwiki/cristal-api";
import type { AuthenticationManagerProvider } from "@xwiki/cristal-authentication-api";
import type { StorageProvider } from "@xwiki/cristal-backend-api";
import type {
  PageHierarchyItem,
  PageHierarchyResolver,
} from "@xwiki/cristal-hierarchy-api";
import type { DocumentReference } from "@xwiki/cristal-model-api";
import type { ModelReferenceSerializer } from "@xwiki/cristal-model-reference-api";
import type { RemoteURLParser } from "@xwiki/cristal-model-remote-url-api";

/**
 * Implementation of PageHierarchyResolver for the XWiki backend.
 *
 * @since 0.9
 **/
@injectable()
class XWikiPageHierarchyResolver implements PageHierarchyResolver {
  constructor(
    @inject<Logger>("Logger") private readonly logger: Logger,
    @inject<CristalApp>("CristalApp") private readonly cristalApp: CristalApp,
    @inject("PageHierarchyResolver")
    private readonly defaultHierarchyResolver: PageHierarchyResolver,
    @inject<AuthenticationManagerProvider>("AuthenticationManagerProvider")
    private readonly authenticationManagerProvider: AuthenticationManagerProvider,
    @inject<StorageProvider>("StorageProvider")
    private readonly storageProvider: StorageProvider,
    @inject<RemoteURLParser>("RemoteURLParser")
    @named("XWiki")
    private readonly urlParser: RemoteURLParser,
    @inject<ModelReferenceSerializer>("ModelReferenceSerializer")
    @named("XWiki")
    private readonly referenceSerializer: ModelReferenceSerializer,
  ) {
    this.logger.setModule("storage.components.XWikiPageHierarchyResolver");
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
        (hierarchyItem: { label: string; url: string; type: string }) => {
          // If a document item is not terminal (i.e., WebHome) we exclude it.
          if (
            hierarchyItem.type != "document" ||
            !hierarchyItem.url.endsWith("/")
          ) {
            hierarchy.push({
              label: hierarchyItem.label,
              pageId: this.storageProvider
                .get()
                .getPageFromViewURL(hierarchyItem.url)!,
              url: this.cristalApp.getRouter().resolve({
                name: "view",
                params: {
                  page: this.referenceSerializer.serialize(
                    this.urlParser.parse(
                      hierarchyItem.url,
                    )! as DocumentReference,
                  ),
                },
              }).href,
            });
          }
        },
      );
      hierarchy[0].label = "Home";
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

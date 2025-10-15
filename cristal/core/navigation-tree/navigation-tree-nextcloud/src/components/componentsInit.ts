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

import { SpaceReference, WikiReference } from "@xwiki/cristal-model-api";
import { name as NavigationTreeSourceName } from "@xwiki/cristal-navigation-tree-api";
import { getParentNodesIdFromPath } from "@xwiki/cristal-navigation-tree-default";
import { Container, inject, injectable } from "inversify";
import type { CristalApp, Logger } from "@xwiki/cristal-api";
import type { AuthenticationManagerProvider } from "@xwiki/cristal-authentication-api";
import type { DocumentReference } from "@xwiki/cristal-model-api";
import type {
  NavigationTreeNode,
  NavigationTreeSource,
} from "@xwiki/cristal-navigation-tree-api";

/**
 * Implementation of NavigationTreeSource for the Nextcloud backend.
 *
 * @since 0.10
 * @beta
 **/
@injectable()
class NextcloudNavigationTreeSource implements NavigationTreeSource {
  private cristalApp: CristalApp;
  public logger: Logger;

  constructor(
    @inject("Logger") logger: Logger,
    @inject("CristalApp") cristalApp: CristalApp,
    @inject("AuthenticationManagerProvider")
    private authenticationManagerProvider: AuthenticationManagerProvider,
  ) {
    this.logger = logger;
    this.logger.setModule(
      "navigation-tree-nextcloud.components.NextcloudNavigationTreeSource",
    );
    this.cristalApp = cristalApp;
  }

  async getChildNodes(id?: string): Promise<Array<NavigationTreeNode>> {
    const currentId = id ? id : "";
    const navigationTree: Array<NavigationTreeNode> = [];
    const currentDepth = currentId ? currentId.split("/").length : 0;

    const subdirectories = await this.getSubDirectories(currentId);
    for (const d of subdirectories) {
      const spaces = d.split("/");
      const currentPageData = await this.cristalApp.getPage(d);
      if (spaces.length == currentDepth + 1) {
        navigationTree.push({
          id: d,
          label:
            currentPageData && currentPageData.name
              ? currentPageData.name
              : spaces[spaces.length - 1],
          location: new SpaceReference(
            new WikiReference(
              (
                await this.authenticationManagerProvider.get()!.getUserDetails()
              ).username!,
            ),
            ...spaces,
          ),
          url: this.cristalApp.getRouter().resolve({
            name: "view",
            params: {
              page: d,
            },
          }).href,
          has_children: subdirectories.some((d2) => d2.startsWith(`${d}/`)),
          is_terminal: false,
        });
      }
    }

    return navigationTree;
  }

  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  private async getSubDirectories(directory: string): Promise<Array<string>> {
    const username = (
      await this.authenticationManagerProvider.get()?.getUserDetails()
    )?.username;
    if (!username) {
      return [];
    }

    const subdirectories: Array<string> = [];
    try {
      const config = this.cristalApp.getWikiConfig();
      const rootUrl = new URL(
        `${config.baseRestURL}${
          config.storageRoot ?? `/files/${username}/.cristal`
        }`.replace("${username}", username),
      );
      const response = await fetch(`${rootUrl}/${directory}`, {
        method: "PROPFIND",
        headers: {
          Authorization: (await this.authenticationManagerProvider
            .get()!
            .getAuthorizationHeader())!,
          Depth: "2",
        },
      });

      const text = await response.text();
      const data = new window.DOMParser().parseFromString(text, "text/xml");
      const responses = data.getElementsByTagName("d:response");

      for (let i = 1; i < responses.length; i++) {
        const response = responses[i];
        if (response.getElementsByTagName("d:collection").length === 0) {
          let urlFragments = response
            .getElementsByTagName("d:href")[0]
            .textContent!.replace(rootUrl.pathname, "")
            .split("/");
          if (urlFragments[0] == "") {
            urlFragments = urlFragments.slice(1);
          }
          urlFragments[urlFragments.length - 1] = this.removeExtension(
            urlFragments[urlFragments.length - 1],
          );

          subdirectories.push(urlFragments.join("/"));
        }
      }
    } catch (error) {
      this.logger.error(error);
      this.logger.debug("Could not load navigation tree.");
    }

    return subdirectories;
  }

  getParentNodesId(
    page: DocumentReference,
    _includeTerminal?: boolean,
    includeRootNode?: boolean,
  ): Array<string> {
    // Nextcloud implementation does not handle terminal pages.
    return getParentNodesIdFromPath(page, includeRootNode);
  }

  private removeExtension(file: string): string {
    if (!file.includes(".")) {
      return file;
    }
    return file.slice(0, file.lastIndexOf("."));
  }
}

/**
 * @beta
 */
export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<NavigationTreeSource>(NavigationTreeSourceName)
      .to(NextcloudNavigationTreeSource)
      .inSingletonScope()
      .whenNamed("Nextcloud");
  }
}

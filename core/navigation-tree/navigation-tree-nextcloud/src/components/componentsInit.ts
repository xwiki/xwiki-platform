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

import { PASSWORD, USERNAME } from "@xwiki/cristal-authentication-nextcloud";
import { SpaceReference } from "@xwiki/cristal-model-api";
import { name as NavigationTreeSourceName } from "@xwiki/cristal-navigation-tree-api";
import { getParentNodesIdFromPath } from "@xwiki/cristal-navigation-tree-default";
import { Container, inject, injectable } from "inversify";
import type { CristalApp, Logger, PageData } from "@xwiki/cristal-api";
import type {
  NavigationTreeNode,
  NavigationTreeSource,
} from "@xwiki/cristal-navigation-tree-api";

/**
 * Implementation of NavigationTreeSource for the Nextcloud backend.
 *
 * @since 0.10
 **/
@injectable()
class NextcloudNavigationTreeSource implements NavigationTreeSource {
  private cristalApp: CristalApp;
  public logger: Logger;

  constructor(
    @inject<Logger>("Logger") logger: Logger,
    @inject<CristalApp>("CristalApp") cristalApp: CristalApp,
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
          location: new SpaceReference(undefined, ...spaces),
          url: this.cristalApp.getRouter().resolve({
            name: "view",
            params: {
              page: d,
            },
          }).href,
          has_children: subdirectories.some((d2) => d2.startsWith(`${d}/`)),
        });
      }
    }

    return navigationTree;
  }

  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  private async getSubDirectories(directory: string): Promise<Array<string>> {
    const subdirectories: Array<string> = [];
    try {
      const response = await fetch(
        `${this.cristalApp.getWikiConfig().baseRestURL}/${USERNAME}/.cristal/${directory}`,
        {
          method: "PROPFIND",
          headers: {
            ...this.getBaseHeaders(),
            Depth: "2",
          },
        },
      );

      const text = await response.text();
      const data = new window.DOMParser().parseFromString(text, "text/xml");
      const responses = data.getElementsByTagName("d:response");

      for (let i = 1; i < responses.length; i++) {
        const response = responses[i];
        if (response.getElementsByTagName("d:collection").length > 0) {
          const urlFragments = response
            .getElementsByTagName("d:href")[0]
            .textContent!.split("/");
          const subdirectory = urlFragments[urlFragments.length - 2];

          // Remove attachments folders
          if (subdirectory !== "attachments") {
            subdirectories.push(urlFragments.slice(6, -1).join("/"));
          }
        }
      }
    } catch (error) {
      this.logger.error(error);
      this.logger.debug("Could not load navigation tree.");
    }

    return subdirectories;
  }

  getParentNodesId(page?: PageData): Array<string> {
    return getParentNodesIdFromPath(page);
  }

  private getBaseHeaders() {
    // TODO: the authentication is currently hardcoded.
    return {
      Authorization: `Basic ${btoa(`${USERNAME}:${PASSWORD}`)}`,
    };
  }
}

export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<NavigationTreeSource>(NavigationTreeSourceName)
      .to(NextcloudNavigationTreeSource)
      .inSingletonScope()
      .whenTargetNamed("Nextcloud");
  }
}

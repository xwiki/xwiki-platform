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
import type { CristalApp, Logger, PageData } from "@xwiki/cristal-api";
import {
  name as NavigationTreeSourceName,
  type NavigationTreeNode,
  type NavigationTreeSource,
} from "@xwiki/cristal-navigation-tree-api";
import { getParentNodesIdFromPath } from "@xwiki/cristal-navigation-tree-default";

/**
 * Implementation of NavigationTreeSource for the GitHub backend.
 *
 * @since 0.10
 **/
@injectable()
class GitHubNavigationTreeSource implements NavigationTreeSource {
  private cristalApp: CristalApp;
  public logger: Logger;

  constructor(
    @inject<Logger>("Logger") logger: Logger,
    @inject<CristalApp>("CristalApp") cristalApp: CristalApp,
  ) {
    this.logger = logger;
    this.logger.setModule(
      "navigation-tree-github.components.GitHubNavigationTreeSource",
    );
    this.cristalApp = cristalApp;
  }

  async getChildNodes(id?: string): Promise<Array<NavigationTreeNode>> {
    const currentId = id ? id : "";
    const navigationTree: Array<NavigationTreeNode> = [];

    const navigationTreeRequestUrl = new URL(
      `${this.cristalApp.getWikiConfig().baseURL}/${currentId}`,
    );
    navigationTreeRequestUrl.search = new URLSearchParams([
      ["noancestors", "1"],
    ]).toString();
    try {
      const response = await fetch(navigationTreeRequestUrl, {
        headers: {
          Accept: "application/json",
        },
      });
      const jsonResponse = await response.json();
      jsonResponse.payload.tree.items.forEach(
        (treeNode: { name: string; path: string; contentType: string }) => {
          navigationTree.push({
            id: treeNode.path,
            label: treeNode.name,
            location: treeNode.path,
            url: this.cristalApp.getRouter().resolve({
              name: "view",
              params: {
                page: treeNode.path,
              },
            }).href,
            has_children: treeNode.contentType == "directory",
          });
        },
      );
    } catch (error) {
      this.logger.error(error);
      this.logger.debug("Could not load navigation tree.");
    }
    return navigationTree;
  }

  getParentNodesId(page?: PageData): Array<string> {
    return getParentNodesIdFromPath(page);
  }
}

export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<NavigationTreeSource>(NavigationTreeSourceName)
      .to(GitHubNavigationTreeSource)
      .inSingletonScope()
      .whenTargetNamed("GitHub");
  }
}

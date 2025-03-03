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

import { SpaceReference } from "@xwiki/cristal-model-api";
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
 * Implementation of NavigationTreeSource for the GitHub backend.
 *
 * @since 0.10
 **/
@injectable()
class GitHubNavigationTreeSource implements NavigationTreeSource {
  constructor(
    @inject<Logger>("Logger") private readonly logger: Logger,
    @inject<CristalApp>("CristalApp") private readonly cristalApp: CristalApp,
    @inject<AuthenticationManagerProvider>("AuthenticationManagerProvider")
    private readonly authenticationManagerProvider: AuthenticationManagerProvider,
  ) {
    this.logger = logger;
    this.logger.setModule(
      "navigation-tree-github.components.GitHubNavigationTreeSource",
    );
    this.cristalApp = cristalApp;
  }

  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  async getChildNodes(id?: string): Promise<Array<NavigationTreeNode>> {
    const currentId = id ? id : "";
    const navigationTree: Array<NavigationTreeNode> = [];

    const authorization = await this.authenticationManagerProvider
      .get()
      ?.getAuthorizationHeader();
    const headers: { Accept: string; Authorization?: string } = {
      Accept: "application/vnd.github.raw+json",
    };
    if (authorization) {
      headers.Authorization = authorization;
    }

    const navigationTreeRequestUrl = new URL(
      this.cristalApp.getWikiConfig().storage.getPageRestURL(currentId, ""),
    );
    try {
      const response = await fetch(navigationTreeRequestUrl, {
        headers: headers,
      });
      const jsonResponse = await response.json();
      navigationTree.push(
        ...(
          await Promise.all(
            jsonResponse.map(
              async (treeNode: {
                name: string;
                path: string;
                type: string;
                git_url: string;
              }) => {
                if (treeNode.type == "dir" && treeNode.name != "attachments") {
                  const currentPageData = await this.cristalApp.getPage(
                    treeNode.path,
                  );
                  const gitResponse = await fetch(treeNode.git_url, {
                    headers: headers,
                  });
                  return {
                    id: treeNode.path,
                    label:
                      currentPageData && currentPageData.name
                        ? currentPageData.name
                        : treeNode.name,
                    location: new SpaceReference(
                      undefined,
                      ...treeNode.path.split("/"),
                    ),
                    url: this.cristalApp.getRouter().resolve({
                      name: "view",
                      params: {
                        page: treeNode.path,
                      },
                    }).href,
                    has_children: (
                      (await gitResponse.json()) as {
                        tree: Array<{ type: string }>;
                      }
                    ).tree.some((n) => n.type == "tree"),
                  };
                }
              },
            ),
          )
        ).filter((n: NavigationTreeNode) => n !== undefined),
      );
    } catch (error) {
      this.logger.error(error);
      this.logger.debug("Could not load navigation tree.");
    }
    return navigationTree;
  }

  getParentNodesId(page?: DocumentReference): Array<string> {
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

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

import { EntityType } from "@xwiki/cristal-model-api";
import { name as NavigationTreeSourceName } from "@xwiki/cristal-navigation-tree-api";
import { getParentNodesIdFromPath } from "@xwiki/cristal-navigation-tree-default";
import { Container, inject, injectable } from "inversify";
import type { CristalApp, Logger } from "@xwiki/cristal-api";
import type { AuthenticationManagerProvider } from "@xwiki/cristal-authentication-api";
import type { DocumentReference } from "@xwiki/cristal-model-api";
import type { ModelReferenceSerializerProvider } from "@xwiki/cristal-model-reference-api";
import type { RemoteURLParserProvider } from "@xwiki/cristal-model-remote-url-api";
import type {
  NavigationTreeNode,
  NavigationTreeSource,
} from "@xwiki/cristal-navigation-tree-api";

/**
 * Implementation of NavigationTreeSource for the GitHub backend.
 *
 * @since 0.10
 * @beta
 **/
@injectable()
class GitHubNavigationTreeSource implements NavigationTreeSource {
  constructor(
    @inject("Logger") private readonly logger: Logger,
    @inject("CristalApp") private readonly cristalApp: CristalApp,
    @inject("AuthenticationManagerProvider")
    private readonly authenticationManagerProvider: AuthenticationManagerProvider,
    @inject("RemoteURLParserProvider")
    private readonly remoteURLParserProvider: RemoteURLParserProvider,
    @inject("ModelReferenceSerializerProvider")
    private readonly modelReferenceSerializerProvider: ModelReferenceSerializerProvider,
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

    try {
      const input = this.cristalApp
        .getWikiConfig()
        .storage.getPageRestURL(currentId, "");
      const response = await fetch(input, {
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
                url: string;
              }) => {
                const parse = this.remoteURLParserProvider
                  .get()!
                  .parse(
                    treeNode.url,
                    treeNode.type === "dir"
                      ? EntityType.SPACE
                      : EntityType.DOCUMENT,
                  );
                if (parse?.type === EntityType.DOCUMENT) {
                  const modelReferenceSerializer =
                    this.modelReferenceSerializerProvider.get()!;
                  const page = modelReferenceSerializer.serialize(parse);
                  if (!page) {
                    throw new Error(
                      `Could not serialize page [${treeNode.path}]`,
                    );
                  }
                  const currentPageData = await this.cristalApp.getPage(page);
                  return {
                    id: page,
                    label: this.computeLabel(currentPageData, parse, treeNode),
                    location: parse,
                    url: this.cristalApp.getRouter().resolve({
                      name: "view",
                      params: {
                        page,
                      },
                    }).href,
                    has_children: jsonResponse.some(
                      (it: { path: string; type: string }) =>
                        it.path === page && it.type == "dir",
                    ),
                    is_terminal: false,
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

  /**
   * @param names - a list of objects with a name property
   * @returns the first element that is not undefined with a name that is not undefined or the empty string
   */
  private computeLabel(
    ...names: (
      | {
          name: string | undefined;
        }
      | undefined
    )[]
  ) {
    return names
      .filter((it) => it?.name !== undefined && it.name !== "")
      .map((it) => it!.name)[0];
  }

  getParentNodesId(
    page: DocumentReference,
    _includeTerminal?: boolean,
    includeRootNode?: boolean,
  ): Array<string> {
    // GitHub implementation does not handle terminal pages.
    return getParentNodesIdFromPath(page, includeRootNode);
  }
}

/**
 * @beta
 */
export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<NavigationTreeSource>(NavigationTreeSourceName)
      .to(GitHubNavigationTreeSource)
      .inSingletonScope()
      .whenNamed("GitHub");
  }
}

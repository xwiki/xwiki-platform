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

import { name as NavigationTreeSourceName } from "@xwiki/cristal-navigation-tree-api";
import { Container, inject, injectable, named } from "inversify";
import type { CristalApp, Logger } from "@xwiki/cristal-api";
import type { AuthenticationManagerProvider } from "@xwiki/cristal-authentication-api";
import type { DocumentReference } from "@xwiki/cristal-model-api";
import type { ModelReferenceSerializer } from "@xwiki/cristal-model-reference-api";
import type { RemoteURLParser } from "@xwiki/cristal-model-remote-url-api";
import type {
  NavigationTreeNode,
  NavigationTreeSource,
} from "@xwiki/cristal-navigation-tree-api";

/**
 * Implementation of NavigationTreeSource for the XWiki backend.
 *
 * @since 0.10
 * @beta
 **/
@injectable()
class XWikiNavigationTreeSource implements NavigationTreeSource {
  constructor(
    @inject("Logger") private readonly logger: Logger,
    @inject("CristalApp") private readonly cristalApp: CristalApp,
    @inject("AuthenticationManagerProvider")
    private authenticationManagerProvider: AuthenticationManagerProvider,
    @inject("RemoteURLParser")
    @named("XWiki")
    private readonly urlParser: RemoteURLParser,
    @inject("ModelReferenceSerializer")
    @named("XWiki")
    private readonly referenceSerializer: ModelReferenceSerializer,
  ) {
    this.logger.setModule(
      "navigation-tree-xwiki.components.XWikiNavigationTreeSource",
    );
  }

  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  async getChildNodes(id?: string): Promise<Array<NavigationTreeNode>> {
    const currentId = id ? id : "#";
    const navigationTree: Array<NavigationTreeNode> = [];

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

      navigationTree.push(...(await this.fetchNodes(currentId, headers, 0)));
    } catch (error) {
      this.logger.error(error);
      this.logger.debug("Could not load navigation tree.");
    }
    return navigationTree;
  }

  getParentNodesId(
    page: DocumentReference,
    includeTerminal: boolean = true,
    includeRootNode?: boolean,
  ): Array<string> {
    const result = includeRootNode ? [""] : [];
    if (page.space) {
      let currentParent = "";
      for (const parent of page.space!.names) {
        currentParent += parent;
        // TODO: Support subwikis.
        result.push(`document:xwiki:${currentParent}.WebHome`);
        currentParent += ".";
      }
      if (page.name != "WebHome" && includeTerminal) {
        result.push(
          `document:xwiki:${this.referenceSerializer.serialize(page)}`,
        );
      }
    }
    return result;
  }

  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  private async fetchNodes(
    currentId: string,
    headers: { Accept: string; Authorization?: string },
    offset: number,
  ): Promise<Array<NavigationTreeNode>> {
    const nodes: Array<NavigationTreeNode> = [];
    const baseXWikiURL = this.cristalApp
      .getWikiConfig()
      .baseURL.replace(/\/[^/]*$/, "");
    // The DocumentTree sheet sets a default limit of 15 items par page.
    // We use a greater one to reduce the number of requests to fetch them all.
    const limit = 100;

    const navigationTreeRequestUrl = new URL(
      `${this.cristalApp.getWikiConfig().baseURL}/bin/get`,
    );
    navigationTreeRequestUrl.search = new URLSearchParams([
      ["id", currentId],
      ["outputSyntax", "plain"],
      ["sheet", "XWiki.DocumentTree"],
      ["data", "children"],
      ["compact", "true"],
      ["offset", offset.toString()],
      ["limit", limit.toString()],
      ["showTranslations", "false"],
      ["showAttachments", "false"],
    ]).toString();

    const response = await fetch(navigationTreeRequestUrl, { headers });
    const jsonResponse: [
      {
        id: string;
        text: string;
        children: boolean;
        data: { type: string; offset: number };
        a_attr?: { href: string };
      },
    ] = await response.json();
    for (const treeNode of jsonResponse) {
      if (treeNode.id == "pagination:wiki:xwiki") {
        nodes.push(
          ...(await this.fetchNodes(currentId, headers, treeNode.data.offset)),
        );
      } else if (
        !["attachments", "translations"].includes(treeNode.data.type) &&
        treeNode.a_attr
      ) {
        const documentReference = this.urlParser.parse(
          `${baseXWikiURL}${treeNode.a_attr.href}`,
        )! as DocumentReference;
        nodes.push({
          id: treeNode.id,
          label: treeNode.text,
          location: documentReference.space!,
          url: this.cristalApp.getRouter().resolve({
            name: "view",
            params: {
              page: this.referenceSerializer.serialize(documentReference),
            },
          }).href,
          has_children: treeNode.children,
          is_terminal: !treeNode.id.endsWith(".WebHome"),
        });
      }
    }

    // Handle paging if necessary.
    if (jsonResponse.length >= limit) {
      nodes.push(
        ...(await this.fetchNodes(currentId, headers, offset + limit)),
      );
    }

    return nodes;
  }
}

/**
 * @beta
 */
export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<NavigationTreeSource>(NavigationTreeSourceName)
      .to(XWikiNavigationTreeSource)
      .inSingletonScope()
      .whenNamed("XWiki");
  }
}

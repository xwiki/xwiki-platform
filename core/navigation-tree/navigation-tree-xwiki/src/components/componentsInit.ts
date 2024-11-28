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

import { name as NavigationTreeSourceName } from "@xwiki/cristal-navigation-tree-api";
import { Container, inject, injectable, named } from "inversify";
import type { CristalApp, Logger, PageData } from "@xwiki/cristal-api";
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
 **/
@injectable()
class XWikiNavigationTreeSource implements NavigationTreeSource {
  constructor(
    @inject<Logger>("Logger") private readonly logger: Logger,
    @inject<CristalApp>("CristalApp") private readonly cristalApp: CristalApp,
    @inject<AuthenticationManagerProvider>("AuthenticationManagerProvider")
    private authenticationManagerProvider: AuthenticationManagerProvider,
    @inject<RemoteURLParser>("RemoteURLParser")
    @named("XWiki")
    private readonly urlParser: RemoteURLParser,
    @inject<ModelReferenceSerializer>("ModelReferenceSerializer")
    @named("XWiki")
    private readonly referenceSerializer: ModelReferenceSerializer,
  ) {
    this.logger.setModule(
      "navigation-tree-xwiki.components.XWikiNavigationTreeSource",
    );
  }

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

  getParentNodesId(page?: PageData): Array<string> {
    const result = [];
    if (page) {
      const documentId = page.document.getIdentifier();
      if (!documentId) {
        this.logger.debug(
          `No identifier found for page ${page.name}, cannot resolve parents.`,
        );
        return [];
      }
      // TODO: Use a page resolver instead when CRISTAL-234 is fixed.
      const parents = documentId
        .replace(/\.WebHome$/, "")
        .split(/(?<![^\\](?:\\\\)*\\)\./);
      let currentParent = "";
      let i;
      for (i = 0; i < parents.length - 1; i++) {
        currentParent += parents[i];
        // TODO: Support subwikis.
        result.push(`document:xwiki:${currentParent}.WebHome`);
        currentParent += ".";
      }
      result.push(`document:xwiki:${documentId}`);
    }
    return result;
  }

  private async fetchNodes(
    currentId: string,
    headers: { Accept: string; Authorization?: string },
    offset: number,
  ): Promise<Array<NavigationTreeNode>> {
    const nodes: Array<NavigationTreeNode> = [];
    const baseXWikiURL = this.cristalApp
      .getWikiConfig()
      .baseURL.replace(/\/[^/]*$/, "");

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
          has_children: treeNode.children, //TODO: ignore translations and attachments
        });
      }
    }

    return nodes;
  }
}

export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<NavigationTreeSource>(NavigationTreeSourceName)
      .to(XWikiNavigationTreeSource)
      .inSingletonScope()
      .whenTargetNamed("XWiki");
  }
}

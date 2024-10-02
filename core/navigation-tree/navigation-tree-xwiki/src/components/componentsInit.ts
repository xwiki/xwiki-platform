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

/**
 * Implementation of NavigationTreeSource for the XWiki backend.
 *
 * @since 0.10
 **/
@injectable()
class XWikiNavigationTreeSource implements NavigationTreeSource {
  private cristalApp: CristalApp;
  public logger: Logger;

  constructor(
    @inject<Logger>("Logger") logger: Logger,
    @inject<CristalApp>("CristalApp") cristalApp: CristalApp,
  ) {
    this.logger = logger;
    this.logger.setModule(
      "navigation-tree-xwiki.components.XWikiNavigationTreeSource",
    );
    this.cristalApp = cristalApp;
  }

  async getChildNodes(id?: string): Promise<Array<NavigationTreeNode>> {
    const currentId = id ? id : "#";
    const navigationTree: Array<NavigationTreeNode> = [];

    const navigationTreeRequestUrl = new URL(
      `${this.cristalApp.getWikiConfig().baseURL}/bin/get`,
    );
    navigationTreeRequestUrl.search = new URLSearchParams([
      ["id", currentId],
      ["outputSyntax", "plain"],
      ["sheet", "XWiki.DocumentTree"],
      ["data", "children"],
      ["compact", "true"],
    ]).toString();
    try {
      const baseXWikiURL = this.cristalApp
        .getWikiConfig()
        .baseURL.replace(/\/[^/]*$/, "");
      const response = await fetch(navigationTreeRequestUrl, {
        headers: { Accept: "application/json" },
      });
      const jsonResponse = await response.json();
      jsonResponse.forEach(
        (treeNode: {
          id: string;
          text: string;
          children: boolean;
          data: { type: string };
          a_attr: { href: string };
        }) => {
          if (!["attachments", "translations"].includes(treeNode.data.type)) {
            const pageId = decodeURIComponent(
              this.cristalApp
                .getWikiConfig()
                .storage.getPageFromViewURL(
                  `${baseXWikiURL}${treeNode.a_attr.href}`,
                )!,
            );
            navigationTree.push({
              id: treeNode.id,
              label: treeNode.text,
              location: pageId.replace(/\.WebHome$/, ""),
              url: this.cristalApp.getRouter().resolve({
                name: "view",
                params: {
                  page: pageId,
                },
              }).href,
              has_children: treeNode.children, //TODO: ignore translations and attachments
            });
          }
        },
      );
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

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
import type { CristalApp, Logger, PageData } from "@xwiki/cristal-api";
import type {
  NavigationTreeNode,
  NavigationTreeSource,
} from "@xwiki/cristal-navigation-tree-api";

/**
 * Implementation of NavigationTreeSource for the FileSystem backend.
 *
 * @since 0.10
 **/
@injectable()
class FileSystemNavigationTreeSource implements NavigationTreeSource {
  private cristalApp: CristalApp;
  public logger: Logger;

  constructor(
    @inject<Logger>("Logger") logger: Logger,
    @inject<CristalApp>("CristalApp") cristalApp: CristalApp,
  ) {
    this.logger = logger;
    this.logger.setModule(
      "navigation-tree-filesystem.components.FileSystemNavigationTreeSource",
    );
    this.cristalApp = cristalApp;
  }

  async getChildNodes(id?: string): Promise<Array<NavigationTreeNode>> {
    const currentId = id ? id : "";
    const navigationTree: Array<NavigationTreeNode> = [];

    // eslint-disable-next-line
    const { fileSystemStorage } = window as any;
    const children = await fileSystemStorage.listChildren(currentId);

    for (const child of children) {
      // Remove attachments folders
      if (child !== "attachments") {
        const id = `${currentId}${currentId ? "/" : ""}${child}`;
        const currentPageData = await this.cristalApp.getPage(id);
        navigationTree.push({
          id: id,
          label:
            currentPageData && currentPageData.name
              ? currentPageData.name
              : child,
          location: new SpaceReference(undefined, ...id.split("/")),
          url: this.cristalApp.getRouter().resolve({
            name: "view",
            params: {
              page: id,
            },
          }).href,
          has_children:
            (await fileSystemStorage.listChildren(id)).filter(
              (c: string) => c != "attachments",
            ).length > 0,
        });
      }
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
      .to(FileSystemNavigationTreeSource)
      .inSingletonScope()
      .whenTargetNamed("FileSystem");
  }
}

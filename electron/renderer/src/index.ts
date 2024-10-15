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

import { CristalAppLoader, loadConfig } from "@xwiki/cristal-lib";
import { ComponentInit as ElectronStorageComponentInit } from "@xwiki/cristal-electron-storage";
import { ComponentInit as BrowserComponentInit } from "@xwiki/cristal-browser-electron";
import { ComponentInit as DefaultPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-default";
import { ComponentInit as FileSystemPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-filesystem";
import { ComponentInit as GitHubPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-github";
import { ComponentInit as NextcloudPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-nextcloud";
import { ComponentInit as XWikiPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-xwiki";
import { ComponentInit as DefaultNavigationTreeComponentInit } from "@xwiki/cristal-navigation-tree-default";
import { ComponentInit as FileSystemNavigationTreeComponentInit } from "@xwiki/cristal-navigation-tree-filesystem";
import { ComponentInit as GitHubNavigationTreeComponentInit } from "@xwiki/cristal-navigation-tree-github";
import { ComponentInit as NextcloudNavigationTreeComponentInit } from "@xwiki/cristal-navigation-tree-nextcloud";
import { ComponentInit as XWikiNavigationTreeComponentInit } from "@xwiki/cristal-navigation-tree-xwiki";
import { ComponentInit as XWikiAuthenticationComponentInit } from "@xwiki/cristal-electron-authentication-xwiki-renderer";
import { Container } from "inversify";

CristalAppLoader.init(
  [
    "skin",
    "dsvuetify",
    "dsfr",
    "dsshoelace",
    "macros",
    "storage",
    "extension-menubuttons",
    "sharedworker",
  ],
  loadConfig("./config.json"),
  true,
  true,
  "FileSystemSL",
  (container: Container) => {
    new ElectronStorageComponentInit(container);
    new BrowserComponentInit(container);
    new DefaultPageHierarchyComponentInit(container);
    new FileSystemPageHierarchyComponentInit(container);
    new GitHubPageHierarchyComponentInit(container);
    new NextcloudPageHierarchyComponentInit(container);
    new XWikiPageHierarchyComponentInit(container);
    new DefaultNavigationTreeComponentInit(container);
    new FileSystemNavigationTreeComponentInit(container);
    new GitHubNavigationTreeComponentInit(container);
    new NextcloudNavigationTreeComponentInit(container);
    new XWikiNavigationTreeComponentInit(container);
    new XWikiAuthenticationComponentInit(container);
  },
);

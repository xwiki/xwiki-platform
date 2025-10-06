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

import "reflect-metadata";
import { ComponentInit as BrowserComponentInit } from "@xwiki/cristal-browser-electron";
import { ComponentInit as GitHubAuthenticationComponentInit } from "@xwiki/cristal-electron-authentication-github-renderer";
import { ComponentInit as NextcloudAuthenticationComponentInit } from "@xwiki/cristal-electron-authentication-nextcloud-renderer";
import { ComponentInit as XWikiAuthenticationComponentInit } from "@xwiki/cristal-electron-authentication-xwiki-renderer";
import { ComponentInit as SettingsComponentInit } from "@xwiki/cristal-electron-settings-renderer";
import { ComponentInit as ElectronStorageComponentInit } from "@xwiki/cristal-electron-storage";
import { ComponentInit as FileSystemPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-filesystem";
import {
  CristalAppLoader,
  conditionalComponentsList,
  defaultComponentsList,
} from "@xwiki/cristal-lib";
import { ComponentInit as FileSystemLinkSuggestComponentInit } from "@xwiki/cristal-link-suggest-filesystem";
import { ComponentInit as ModelReferenceFilesystemComponentInit } from "@xwiki/cristal-model-reference-filesystem";
import { ComponentInit as ModelRemoteURLFilesystemComponentInit } from "@xwiki/cristal-model-remote-url-filesystem-default";
import { ComponentInit as FileSystemNavigationTreeComponentInit } from "@xwiki/cristal-navigation-tree-filesystem";
import { ComponentInit as FileSystemRenameComponentInit } from "@xwiki/cristal-rename-filesystem";

CristalAppLoader.init(
  [
    "skin",
    "dsvuetify",
    "dsshoelace",
    "macros",
    "storage",
    "extension-menubuttons",
    "sharedworker",
  ],
  // With Electron, configs are only loaded through user settings.
  async () => ({}),
  true,
  true,
  "FileSystemSL",
  // eslint-disable-next-line max-statements
  async (container) => {
    await defaultComponentsList(container);
    new ElectronStorageComponentInit(container);
    new BrowserComponentInit(container);
    new FileSystemPageHierarchyComponentInit(container);
    new FileSystemNavigationTreeComponentInit(container);
    new XWikiAuthenticationComponentInit(container);
    new ModelReferenceFilesystemComponentInit(container);
    new ModelRemoteURLFilesystemComponentInit(container);
    new FileSystemLinkSuggestComponentInit(container);
    new FileSystemRenameComponentInit(container);
    new GitHubAuthenticationComponentInit(container);
    new NextcloudAuthenticationComponentInit(container);
    new SettingsComponentInit(container);
  },
  async (container, configuration) => {
    await conditionalComponentsList(container, configuration);
  },
);

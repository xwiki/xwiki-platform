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

import "reflect-metadata";
import { ComponentInit as BrowserComponentInit } from "@xwiki/cristal-browser-electron";
import { ComponentInit as XWikiAuthenticationComponentInit } from "@xwiki/cristal-electron-authentication-xwiki-renderer";
import { ComponentInit as ElectronStorageComponentInit } from "@xwiki/cristal-electron-storage";
import { ComponentInit as FileSystemPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-filesystem";
import { CristalAppLoader, loadConfig } from "@xwiki/cristal-lib";
import { ComponentInit as ModelReferenceFilesystemComponentInit } from "@xwiki/cristal-model-reference-filesystem";
import { ComponentInit as ModelRemoteURLFilesystemComponentInit } from "@xwiki/cristal-model-remote-url-filesystem";
import { ComponentInit as FileSystemNavigationTreeComponentInit } from "@xwiki/cristal-navigation-tree-filesystem";
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
    new FileSystemPageHierarchyComponentInit(container);
    new FileSystemNavigationTreeComponentInit(container);
    new XWikiAuthenticationComponentInit(container);
    new ModelReferenceFilesystemComponentInit(container);
    new ModelRemoteURLFilesystemComponentInit(container);
  },
);

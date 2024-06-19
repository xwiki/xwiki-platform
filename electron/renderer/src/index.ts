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
import { ComponentInit as XWikiPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-xwiki";
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
    new XWikiPageHierarchyComponentInit(container);
  },
);

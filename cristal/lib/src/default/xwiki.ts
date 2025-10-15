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

import { ComponentInit as XWikiPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-xwiki";
import { ComponentInit as XWikiPageHistoryComponentInit } from "@xwiki/cristal-history-xwiki";
import { ComponentInit as XWikiLinkSuggestComponentInit } from "@xwiki/cristal-link-suggest-xwiki";
import { ComponentInit as ModelReferenceXWikiComponentInit } from "@xwiki/cristal-model-reference-xwiki";
import { ComponentInit as ModelRemoteURLXWikiComponentInit } from "@xwiki/cristal-model-remote-url-xwiki";
import { ComponentInit as XWikiNavigationTreeComponentInit } from "@xwiki/cristal-navigation-tree-xwiki";
import type { Container } from "inversify";

export function load(container: Container) {
  new XWikiLinkSuggestComponentInit(container);
  new XWikiPageHierarchyComponentInit(container);
  new XWikiNavigationTreeComponentInit(container);
  new XWikiPageHistoryComponentInit(container);
  new ModelRemoteURLXWikiComponentInit(container);
  new ModelReferenceXWikiComponentInit(container);
}

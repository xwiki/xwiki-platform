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

import { ComponentInit as AlertsDefaultComponentInit } from "@xwiki/cristal-alerts-default";
import { ComponentInit as AttachmentsDefaultComponentInit } from "@xwiki/cristal-attachments-default";
import { ComponentInit as AttachmentsUIComponentInit } from "@xwiki/cristal-attachments-ui";
import { ComponentInit as AuthenticationDefaultComponentInit } from "@xwiki/cristal-authentication-default";
import { ComponentInit as AuthenticationUIComponentInit } from "@xwiki/cristal-authentication-ui";
import { ComponentInit as BackendAPIComponentInit } from "@xwiki/cristal-backend-api";
import { ComponentInit as DexieBackendComponentInit } from "@xwiki/cristal-backend-dexie";
import { ComponentInit as GithubBackendComponentInit } from "@xwiki/cristal-backend-github";
import { ComponentInit as NextcloudBackendComponentInit } from "@xwiki/cristal-backend-nextcloud";
import { ComponentInit as XWikiBackendComponentInit } from "@xwiki/cristal-backend-xwiki";
import { ComponentInit as DateAPIComponentInit } from "@xwiki/cristal-date-api";
import { ComponentInit as DocumentComponentInit } from "@xwiki/cristal-document-default";
import { ComponentInit as DSFRComponentInit } from "@xwiki/cristal-dsfr";
import { ComponentInit as ShoelaceComponentInit } from "@xwiki/cristal-dsshoelace";
import { ComponentInit as VueDSComponentInit } from "@xwiki/cristal-dsvuetify";
import { ComponentInit as EditorTiptapComponentInit } from "@xwiki/cristal-editors-tiptap";
import { ComponentInit as MenuButtonsComponentInit } from "@xwiki/cristal-extension-menubuttons";
import { ComponentInit as ExtraTabsComponentInit } from "@xwiki/cristal-extra-tabs-default";
import { ComponentInit as DefaultPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-default";
import { ComponentInit as GitHubPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-github";
import { ComponentInit as NextcloudPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-nextcloud";
import { ComponentInit as XWikiPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-xwiki";
import { ComponentInit as DefaultPageHistoryComponentInit } from "@xwiki/cristal-history-default";
import { ComponentInit as GitHubPageHistoryComponentInit } from "@xwiki/cristal-history-github";
import { ComponentInit as HistoryUIComponentInit } from "@xwiki/cristal-history-ui";
import { ComponentInit as XWikiPageHistoryComponentInit } from "@xwiki/cristal-history-xwiki";
import { ComponentInit as InfoActionsComponentInit } from "@xwiki/cristal-info-actions-default";
import { ComponentInit as LinkSuggestComponentInit } from "@xwiki/cristal-link-suggest-api";
import { ComponentInit as NextcloudLinkSuggestComponentInit } from "@xwiki/cristal-link-suggest-nextcloud";
import { ComponentInit as XWikiLinkSuggestComponentInit } from "@xwiki/cristal-link-suggest-xwiki";
import { ComponentInit as MacrosComponentInit } from "@xwiki/cristal-macros";
import { ComponentInit as MarkdownDefaultComponentInit } from "@xwiki/cristal-markdown-default";
import { ComponentInit as ClickListenerComponentInit } from "@xwiki/cristal-model-click-listener";
import { ComponentInit as ModelReferenceAPIComponentInit } from "@xwiki/cristal-model-reference-api";
import { ComponentInit as ModelReferenceNextcloudComponentInit } from "@xwiki/cristal-model-reference-nextcloud";
import { ComponentInit as ModelReferenceXWikiComponentInit } from "@xwiki/cristal-model-reference-xwiki";
import { ComponentInit as ModelRemoteURLAPIComponentInit } from "@xwiki/cristal-model-remote-url-api";
import { ComponentInit as ModelRemoteURLNextcloudComponentInit } from "@xwiki/cristal-model-remote-url-nextcloud";
import { ComponentInit as ModelRemoteURLXWikiComponentInit } from "@xwiki/cristal-model-remote-url-xwiki";
import { ComponentInit as DefaultNavigationTreeComponentInit } from "@xwiki/cristal-navigation-tree-default";
import { ComponentInit as GitHubNavigationTreeComponentInit } from "@xwiki/cristal-navigation-tree-github";
import { ComponentInit as NextcloudNavigationTreeComponentInit } from "@xwiki/cristal-navigation-tree-nextcloud";
import { ComponentInit as XWikiNavigationTreeComponentInit } from "@xwiki/cristal-navigation-tree-xwiki";
import { ComponentInit as ActionsPagesComponentInit } from "@xwiki/cristal-page-actions-default";
import { ComponentInit as ActionsPagesUIComponentInit } from "@xwiki/cristal-page-actions-ui";
import { ComponentInit as RenderingComponentInit } from "@xwiki/cristal-rendering";
import { ComponentInit as QueueWorkerComponentInit } from "@xwiki/cristal-sharedworker-impl";
import { ComponentInit as SkinComponentInit } from "@xwiki/cristal-skin";
import { ComponentInit as UIExtensionDefaultComponentInit } from "@xwiki/cristal-uiextension-default";
import type { Container } from "inversify";

export class StaticBuild {
  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  public static init(
    container: Container,
    forceStaticBuild: boolean,
    additionalComponents?: (container: Container) => void,
  ): void {
    if (
      (import.meta.env && import.meta.env.MODE == "development") ||
      forceStaticBuild
    ) {
      new SkinComponentInit(container);
      new MacrosComponentInit(container);
      new VueDSComponentInit(container);
      new DSFRComponentInit(container);
      new ShoelaceComponentInit(container);
      new DexieBackendComponentInit(container);
      new GithubBackendComponentInit(container);
      new NextcloudBackendComponentInit(container);
      new XWikiBackendComponentInit(container);
      new MenuButtonsComponentInit(container);
      new QueueWorkerComponentInit(container);
      new RenderingComponentInit(container);
      new EditorTiptapComponentInit(container);
      new ExtraTabsComponentInit(container);
      new InfoActionsComponentInit(container);
      new AttachmentsUIComponentInit(container);
      new AttachmentsDefaultComponentInit(container);
      new UIExtensionDefaultComponentInit(container);
      new AuthenticationUIComponentInit(container);
      new BackendAPIComponentInit(container);
      new AuthenticationDefaultComponentInit(container);
      new LinkSuggestComponentInit(container);
      new XWikiLinkSuggestComponentInit(container);
      new NextcloudLinkSuggestComponentInit(container);
      new DocumentComponentInit(container);
      new AlertsDefaultComponentInit(container);
      new ActionsPagesComponentInit(container);
      new ActionsPagesUIComponentInit(container);
      new DefaultPageHierarchyComponentInit(container);
      new GitHubPageHierarchyComponentInit(container);
      new NextcloudPageHierarchyComponentInit(container);
      new XWikiPageHierarchyComponentInit(container);
      new DefaultNavigationTreeComponentInit(container);
      new GitHubNavigationTreeComponentInit(container);
      new NextcloudNavigationTreeComponentInit(container);
      new XWikiNavigationTreeComponentInit(container);
      new DefaultPageHistoryComponentInit(container);
      new GitHubPageHistoryComponentInit(container);
      new XWikiPageHistoryComponentInit(container);
      new HistoryUIComponentInit(container);
      new ClickListenerComponentInit(container);
      new ModelRemoteURLAPIComponentInit(container);
      new ModelRemoteURLNextcloudComponentInit(container);
      new ModelRemoteURLXWikiComponentInit(container);
      new ModelReferenceAPIComponentInit(container);
      new ModelReferenceNextcloudComponentInit(container);
      new ModelReferenceXWikiComponentInit(container);
      new DateAPIComponentInit(container);
      new MarkdownDefaultComponentInit(container);
    }
    if (additionalComponents) {
      additionalComponents(container);
    }
  }
}

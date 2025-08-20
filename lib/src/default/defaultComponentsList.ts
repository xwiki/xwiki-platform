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

import { ComponentInit as AlertsDefaultComponentInit } from "@xwiki/cristal-alerts-default";
import { ComponentInit as AttachmentsDefaultComponentInit } from "@xwiki/cristal-attachments-default";
import { ComponentInit as AttachmentsUIComponentInit } from "@xwiki/cristal-attachments-ui";
import { ComponentInit as AuthenticationDefaultComponentInit } from "@xwiki/cristal-authentication-default";
import { ComponentInit as AuthenticationUIComponentInit } from "@xwiki/cristal-authentication-ui";
import { ComponentInit as BackendAPIComponentInit } from "@xwiki/cristal-backend-api";
import { ComponentInit as GithubBackendComponentInit } from "@xwiki/cristal-backend-github";
import { ComponentInit as NextcloudBackendComponentInit } from "@xwiki/cristal-backend-nextcloud";
import { ComponentInit as XWikiBackendComponentInit } from "@xwiki/cristal-backend-xwiki";
import { ComponentInit as CollaborationAPIComponentInit } from "@xwiki/cristal-collaboration-api";
import { ComponentInit as CollaborationHocusPocusComponentInit } from "@xwiki/cristal-collaboration-hocuspocus";
import { ComponentInit as DateAPIComponentInit } from "@xwiki/cristal-date-api";
import { ComponentInit as DocumentComponentInit } from "@xwiki/cristal-document-default";
import { ComponentInit as MenuButtonsComponentInit } from "@xwiki/cristal-extension-menubuttons";
import { ComponentInit as ExtraTabsComponentInit } from "@xwiki/cristal-extra-tabs-default";
import { ComponentInit as DefaultPageHierarchyComponentInit } from "@xwiki/cristal-hierarchy-default";
import { ComponentInit as DefaultPageHistoryComponentInit } from "@xwiki/cristal-history-default";
import { ComponentInit as HistoryUIComponentInit } from "@xwiki/cristal-history-ui";
import { ComponentInit as InfoActionsComponentInit } from "@xwiki/cristal-info-actions-default";
import { ComponentInit as LinkSuggestComponentInit } from "@xwiki/cristal-link-suggest-api";
import { ComponentInit as MarkdownDefaultComponentInit } from "@xwiki/cristal-markdown-default";
import { ComponentInit as ClickListenerComponentInit } from "@xwiki/cristal-model-click-listener";
import { ComponentInit as ModelReferenceAPIComponentInit } from "@xwiki/cristal-model-reference-api";
import { ComponentInit as ModelRemoteURLAPIComponentInit } from "@xwiki/cristal-model-remote-url-api";
import { ComponentInit as DefaultNavigationTreeComponentInit } from "@xwiki/cristal-navigation-tree-default";
import { ComponentInit as ActionsPagesComponentInit } from "@xwiki/cristal-page-actions-default";
import { ComponentInit as ActionsPagesUIComponentInit } from "@xwiki/cristal-page-actions-ui";
import { ComponentInit as RenameComponentInit } from "@xwiki/cristal-rename-default";
import { ComponentInit as SettingsConfigurationsComponentInit } from "@xwiki/cristal-settings-configurations";
import { ComponentInit as SettingsConfigurationsUIComponentInit } from "@xwiki/cristal-settings-configurations-ui";
import { ComponentInit as SettingsComponentInit } from "@xwiki/cristal-settings-default";
import { ComponentInit as SkinComponentInit } from "@xwiki/cristal-skin";
import { ComponentInit as UIExtensionDefaultComponentInit } from "@xwiki/cristal-uiextension-default";
import { ComponentInit as WikiConfigComponentInit } from "@xwiki/cristal-wiki-config-default";
import type { Configuration } from "@xwiki/cristal-configuration-api";
import type { Container } from "inversify";

/**
 * Loads all the components of the default distribution.
 *
 * @param container - the container the load the components in
 * @since 0.18
 */
// eslint-disable-next-line max-statements
async function defaultComponentsList(container: Container): Promise<void> {
  new SkinComponentInit(container);
  new XWikiBackendComponentInit(container);
  new NextcloudBackendComponentInit(container);
  new MenuButtonsComponentInit(container);
  new ExtraTabsComponentInit(container);
  new InfoActionsComponentInit(container);
  new AttachmentsUIComponentInit(container);
  new AttachmentsDefaultComponentInit(container);
  new UIExtensionDefaultComponentInit(container);
  new AuthenticationUIComponentInit(container);
  new BackendAPIComponentInit(container);
  new AuthenticationDefaultComponentInit(container);
  new LinkSuggestComponentInit(container);
  new DocumentComponentInit(container);
  new AlertsDefaultComponentInit(container);
  new ActionsPagesComponentInit(container);
  new ActionsPagesUIComponentInit(container);
  new DefaultPageHierarchyComponentInit(container);
  new DefaultNavigationTreeComponentInit(container);
  new DefaultPageHistoryComponentInit(container);
  new HistoryUIComponentInit(container);
  new ClickListenerComponentInit(container);
  new ModelRemoteURLAPIComponentInit(container);
  new ModelReferenceAPIComponentInit(container);
  new DateAPIComponentInit(container);
  new MarkdownDefaultComponentInit(container);
  new RenameComponentInit(container);
  new GithubBackendComponentInit(container);
  new SettingsComponentInit(container);
  new SettingsConfigurationsComponentInit(container);
  new SettingsConfigurationsUIComponentInit(container);
  new WikiConfigComponentInit(container);
  new CollaborationAPIComponentInit(container);
  new CollaborationHocusPocusComponentInit(container);
}

/**
 * Loads the components required specifically for the current configuration.
 *
 * @param container - the container to load the components in
 * @param configuration - the current configuration
 * @since 0.18
 */
// eslint-disable-next-line max-statements
async function conditionalComponentsList(
  container: Container,
  configuration: Configuration,
): Promise<void> {
  // Load only components that are required by the current configuration.
  if (configuration.designSystem == "vuetify") {
    (await import("./vuetify")).load(container);
  } else if (configuration.designSystem == "shoelace") {
    (await import("./shoelace")).load(container);
  }

  if (configuration.offline) {
    (await import("./offline")).load(container);
  }

  if (configuration.configType == "Nextcloud") {
    (await import("./nextcloud")).load(container);
  } else if (configuration.configType == "XWiki") {
    (await import("./xwiki")).load(container);
  } else if (configuration.configType == "GitHub") {
    (await import("./github")).load(container);
  }

  if (configuration.editor === "tiptap" || configuration.editor === undefined) {
    const { ComponentInit } = await import("@xwiki/cristal-editors-tiptap");
    new ComponentInit(container);
  } else if (configuration.editor === "blocknote") {
    const { ComponentInit } = await import("@xwiki/cristal-editors-blocknote");
    new ComponentInit(container);
  }
}

export { conditionalComponentsList, defaultComponentsList };

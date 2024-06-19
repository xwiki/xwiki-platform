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

import { ComponentInit as SkinComponentInit } from "@xwiki/cristal-skin";
import { ComponentInit as VueDSComponentInit } from "@xwiki/cristal-dsvuetify";
import { ComponentInit as DSFRComponentInit } from "@xwiki/cristal-dsfr";
import { ComponentInit as ShoelaceComponentInit } from "@xwiki/cristal-dsshoelace";
import { ComponentInit as DexieBackendComponentInit } from "@xwiki/cristal-backend-dexie";
import { ComponentInit as GithubBackendComponentInit } from "@xwiki/cristal-backend-github";
import { ComponentInit as NextcloudBackendComponentInit } from "@xwiki/cristal-backend-nextcloud";
import { ComponentInit as XWikiBackendComponentInit } from "@xwiki/cristal-backend-xwiki";
import { ComponentInit as MacrosComponentInit } from "@xwiki/cristal-macros";
import { ComponentInit as MenuButtonsComponentInit } from "@xwiki/cristal-extension-menubuttons";
import { ComponentInit as QueueWorkerComponentInit } from "@xwiki/cristal-sharedworker-impl";
import { ComponentInit as RenderingComponentInit } from "@xwiki/cristal-rendering";
import { ComponentInit as EditorTiptapComponentInit } from "@xwiki/cristal-editors-tiptap";
import { ComponentInit as ExtraTabsComponentInit } from "@xwiki/cristal-extra-tabs-default";
import { ComponentInit as AttachmentsUIComponentInit } from "@xwiki/cristal-attachments-ui";
import { ComponentInit as AttachmentsDefaultComponentInit } from "@xwiki/cristal-attachments-default";
import type { Container } from "inversify";

export class StaticBuild {
  public static init(
    container: Container,
    forceStaticBuild: boolean,
    additionalComponents?: (container: Container) => void,
  ) {
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
      new AttachmentsUIComponentInit(container);
      new AttachmentsDefaultComponentInit(container);
    }
    if (additionalComponents) {
      additionalComponents(container);
    }
  }
}

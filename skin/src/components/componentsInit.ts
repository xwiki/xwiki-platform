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

import { InformationExtraTab } from "./InformationTab";
import { DefaultSkinManager } from "./defaultSkinManager";
import { CommentsInfoAction } from "./info-actions/commentsInfoAction";
import { LikesInfoAction } from "./info-actions/likesInfoAction";
import { ConfigMenuUIExtension } from "./sidebar-actions/ConfigMenuUIExtension";
import { TemplateComponent } from "./templateTemplateProvider";
import { UIXLoginTemplateProvider } from "./uixLoginTemplateProvider";
import { UIXSearchTemplateProvider } from "./uixSearchTemplateProvider";
import { UIXVueTemplateProvider } from "./uixVueTemplateProvider";
import { ExtraTab } from "@xwiki/cristal-extra-tabs-api";
import { InfoAction } from "@xwiki/cristal-info-actions-api";
import { UIExtension } from "@xwiki/cristal-uiextension-api";
import type { UIXTemplateProvider } from "../api/uixTemplateProvider";
import type { VueTemplateProvider } from "../api/vueTemplateProvider";
import type { Logger, SkinManager } from "@xwiki/cristal-api";
import type { Container } from "inversify";

export default class ComponentInit {
  logger: Logger;

  constructor(container: Container) {
    this.logger = container.get<Logger>("Logger");
    this.logger.setModule("skin.components.componentsInit");

    this.logger?.debug("Init Skin components begin");
    container
      .bind<UIXTemplateProvider>("UIXTemplateProvider")
      .to(UIXLoginTemplateProvider)
      .whenTargetNamed(UIXLoginTemplateProvider.extensionPoint);
    container
      .bind<UIXTemplateProvider>("UIXTemplateProvider")
      .to(UIXSearchTemplateProvider)
      .whenTargetNamed(UIXSearchTemplateProvider.extensionPoint);
    container.bind<SkinManager>("SkinManager").to(DefaultSkinManager);
    container
      .bind<VueTemplateProvider>("VueTemplateProvider")
      .to(TemplateComponent);
    container
      .bind<VueTemplateProvider>("VueTemplateProvider")
      .to(UIXVueTemplateProvider);
    container
      .bind<ExtraTab>("ExtraTab")
      .to(InformationExtraTab)
      .inSingletonScope();
    container
      .bind<InfoAction>("InfoAction")
      .to(LikesInfoAction)
      .inSingletonScope();
    container
      .bind<InfoAction>("InfoAction")
      .to(CommentsInfoAction)
      .inSingletonScope();
    container
      .bind<UIExtension>("UIExtension")
      .to(ConfigMenuUIExtension)
      .inSingletonScope();
    this.logger?.debug("Init Skin components end");
  }
}

/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/

import type { App } from "vue";
import * as components from "vuetify/components";
import {
  VAvatar,
  VCol,
  VContainer,
  VImg,
  VRow,
  VTextField,
} from "vuetify/components";
import type { DesignSystemLoader } from "@cristal/api";

import { createVuetify } from "vuetify";
import { mdi } from "vuetify/iconsets/mdi";
import * as directives from "vuetify/directives";
import { injectable } from "inversify";
import { registerAsyncComponent } from "@cristal/api";

@injectable()
export class VuetifyDesignSystemLoader implements DesignSystemLoader {
  loadDesignSystem(app: App): void {
    /*
        // Manuel importing to reduce build size
        const vuetify = createVuetify({
            components : { VApp, VContainer, VRow, VCol, VAvatar },
            directives : {},
            icons: {
                defaultSet: 'fa',
                aliases,
                sets: {
                    fa,
                    mdi,
                }
          })
        */

    const vuetify = createVuetify({
      components,
      directives,
      icons: {
        defaultSet: "mdi",
        sets: {
          mdi,
        },
      },
    });
    app.use(vuetify);
    // Native Vuetify components
    app.component("XAvatar", VAvatar);
    app.component("XContainer", VContainer);
    app.component("XImg", VImg);
    app.component("XRow", VRow);
    app.component("XCol", VCol);
    app.component("XTextField", VTextField);

    // Custom wrapped components
    registerAsyncComponent(app, "XLoad", () => import("../vue/x-load.vue"));
    registerAsyncComponent(app, "XBtn", () => import("../vue/x-btn.vue"));
    registerAsyncComponent(
      app,
      "XDivider",
      () => import("../vue/x-divider.vue"),
    );
    registerAsyncComponent(app, "XCard", () => import("../vue/x-card.vue"));
    registerAsyncComponent(app, "XAlert", () => import("../vue/x-alert.vue"));
    registerAsyncComponent(app, "XDialog", () => import("../vue/x-dialog.vue"));
    registerAsyncComponent(app, "XMenu", () => import("../vue/x-menu.vue"));
    registerAsyncComponent(
      app,
      "XMenuItem",
      () => import("../vue/x-menu-item.vue"),
    );
    registerAsyncComponent(
      app,
      "XBreadcrumb",
      () => import("../vue/x-breadcrumb.vue"),
    );
    registerAsyncComponent(app, "XSearch", () => import("../vue/x-search.vue"));
  }
}

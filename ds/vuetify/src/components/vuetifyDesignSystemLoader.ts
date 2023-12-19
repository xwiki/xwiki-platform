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
import {
  VAvatar,
  VContainer,
  VRow,
  VCol,
  VImg,
  VTextField,
} from "vuetify/components";
import type { DesignSystemLoader } from "@cristal/api";

import "vuetify/styles";
import { createVuetify } from "vuetify";
import { mdi } from "vuetify/iconsets/mdi";
// import { aliases, fa } from 'vuetify/iconsets/fa'
import * as components from "vuetify/components";
import * as directives from "vuetify/directives";
import { injectable } from "inversify";
import XCard from "../vue/x-card.vue";
import XAlert from "../vue/x-alert.vue";
import XDivider from "../vue/x-divider.vue";
import XBtn from "../vue/x-btn.vue";
import XDialog from "../vue/x-dialog.vue";
import XMenu from "../vue/x-menu.vue";
import XMenuItem from "../vue/x-menu-item.vue";

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
    app.component("XBtn", XBtn);
    app.component("XDivider", XDivider);
    app.component("XCard", XCard);
    app.component("XAlert", XAlert);
    app.component("XDialog", XDialog);
    app.component("XMenu", XMenu);
    app.component("XMenuItem", XMenuItem);
  }
}

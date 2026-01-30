/**
 * See the NOTICE file distributed with this work for additional
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
import { registerAsyncComponent } from "@xwiki/platform-api";
import { injectable } from "inversify";
import type { DesignSystemLoader } from "@xwiki/platform-api";
import type { App } from "vue";

@injectable()
export class FlamingoDesignSystemLoader implements DesignSystemLoader {
  // eslint-disable-next-line max-statements
  loadDesignSystem(app: App): void {
    registerAsyncComponent(app, "XAlert", () => import("./vue/XAlert.vue"));
    registerAsyncComponent(app, "XAvatar", () => import("./vue/XAvatar.vue"));
    registerAsyncComponent(app, "XBtn", () => import("./vue/XBtn.vue"));
    registerAsyncComponent(
      app,
      "XBreadcrumb",
      () => import("./vue/XBreadcrumb.vue"),
    );
    registerAsyncComponent(app, "XCard", () => import("./vue/XCard.vue"));
    registerAsyncComponent(
      app,
      "XCheckbox",
      () => import("./vue/XCheckbox.vue"),
    );
    registerAsyncComponent(app, "XDialog", () => import("./vue/XDialog.vue"));
    registerAsyncComponent(app, "XDivider", () => import("./vue/XDivider.vue"));
    registerAsyncComponent(
      app,
      "XFileInput",
      () => import("./vue/XFileInput.vue"),
    );
    registerAsyncComponent(app, "XForm", () => import("./vue/XForm.vue"));
    registerAsyncComponent(app, "XImg", () => import("./vue/XImg.vue"));
    registerAsyncComponent(app, "XLoad", () => import("./vue/XLoad.vue"));
    registerAsyncComponent(app, "XMenu", () => import("./vue/XMenu.vue"));
    registerAsyncComponent(
      app,
      "XMenuItem",
      () => import("./vue/XMenuItem.vue"),
    );
    registerAsyncComponent(
      app,
      "XMenuLabel",
      () => import("./vue/XMenuLabel.vue"),
    );
    registerAsyncComponent(app, "XSelect", () => import("./vue/XSelect.vue"));
    registerAsyncComponent(app, "XTab", () => import("./vue/XTab.vue"));
    registerAsyncComponent(
      app,
      "XTabGroup",
      () => import("./vue/XTabGroup.vue"),
    );
    registerAsyncComponent(
      app,
      "XTabPanel",
      () => import("./vue/XTabPanel.vue"),
    );
    registerAsyncComponent(
      app,
      "XTextField",
      () => import("./vue/XTextField.vue"),
    );
    registerAsyncComponent(app, "XTree", () => import("./vue/XTree.vue"));
  }
}

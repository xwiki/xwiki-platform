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

import { DefaultUIXTemplateProvider } from "@xwiki/cristal-skin";
import Menu from "./c-menu.vue";
import { injectable } from "inversify";
import "reflect-metadata";
import type { Component } from "vue";

@injectable()
export class UIXMenuTemplateProvider extends DefaultUIXTemplateProvider {
  public static override cname = "cristal.vuejs.component";
  public static override hint = "menu";
  public static override priority = 1000;
  public static override singleton = true;
  public static extensionPoint = "sidebar.before";

  constructor() {
    super();
  }

  override getVueComponent(): Component {
    return Menu;
  }

  getMenuItems(): Array<string> {
    const menus: Array<string> = [];
    return menus;
  }

  override getVueName(): string {
    return "Menu";
  }

  override isGlobal(): boolean {
    return false;
  }
}

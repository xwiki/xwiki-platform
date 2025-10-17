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

import messages from "./translations";
import { AbstractUIExtension } from "@xwiki/cristal-uiextension-api";
import { injectable } from "inversify";
import type { Component } from "vue";

/**
 * Settings category {@link UIExtension} for Configurations management.
 *
 * @since 0.18
 * @beta
 */
@injectable()
export class SettingsConfigurationsUIExtension extends AbstractUIExtension {
  id = "settings.categories.configurations";
  uixpName = "settings.categories";
  order = 1000;
  parameters = { title: "" };

  constructor() {
    super(messages);
    this.parameters.title = this.t("settings.configurations.title");
  }

  async component(): Promise<Component> {
    return (await import("./vue/SettingsConfigurations.vue")).default;
  }

  async enabled(): Promise<boolean> {
    return true;
  }
}

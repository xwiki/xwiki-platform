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

import { DefaultMacroProvider } from "./defaultMacroProvider";
import type { MacroData } from "@xwiki/cristal-skin";
import Warning from "../vue/c-warning.vue";

export class AlertMacro extends DefaultMacroProvider {
  public static cname = "cristal.macro.alert";
  public static macroName = "alert";
  public static hint = "macro";
  public static priority = 1000;
  public static singleton = true;

  constructor() {
    super();
  }

  getMacroName(): string {
    return AlertMacro.macroName;
  }

  getVueComponent() {
    return Warning;
  }

  protected escapeHTML(source: string) {
    const escape = document.createElement("textarea");
    escape.textContent = source.toString();
    return escape.innerHTML;
  }

  renderMacroAsHTML(macroData: MacroData): string {
    const title = macroData.getMacroParameter("title");
    let result = "<div class='cristal-macro-" + this.getMacroName() + "'>";
    if (title && title != "") {
      result += this.escapeHTML(title) + "\n";
    }
    result +=
      "<p>" + this.escapeHTML(macroData.getMacroContent()) + "</p></div>";
    return result;
  }
}

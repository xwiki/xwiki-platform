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

import { DefaultMacroProvider } from "./defaultMacroProvider";
import type { MacroData } from "@xwiki/cristal-skin";
import Warning from "../vue/c-warning.vue";
import { Component } from "vue";

export class AlertMacro extends DefaultMacroProvider {
  public static override cname = "cristal.macro.alert";
  public static macroName = "alert";
  public static override hint = "macro";
  public static override priority = 1000;
  public static override singleton = true;

  constructor() {
    super();
  }

  getMacroName(): string {
    return AlertMacro.macroName;
  }

  override getVueComponent(): Component {
    return Warning;
  }

  protected escapeHTML(source: string): string {
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

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

import {
  DefaultMacroData,
  DefaultVueTemplateProvider,
} from "@xwiki/cristal-skin";
import type { MacroData, MacroProvider } from "@xwiki/cristal-skin";

export abstract class DefaultMacroProvider
  extends DefaultVueTemplateProvider
  implements MacroProvider
{
  public static override cname = "cristal.macro";
  public static override hint = "macro";
  public static override priority = 1000;
  public static override singleton = true;

  getMacroRenderingType() {
    return "vue";
  }

  parseParameters(element: HTMLElement): MacroData {
    const macroData = new DefaultMacroData();
    const attrs = element.getAttributeNames();
    for (const i in attrs) {
      const attr = attrs[i];
      if (attr != "class" && attr != "macro-name") {
        const value = element.getAttribute(attr);
        if (value) {
          macroData.getMacroParameters().set(attr, value);
        }
      }
    }
    const content = element.firstChild?.textContent?.replace(
      /\[CDATA\[(.*?)\]\]/,
      "$1",
    );
    if (content) {
      macroData.setMacroContent(content);
    }
    return macroData;
  }

  abstract getMacroName(): string;

  abstract renderMacroAsHTML(macroData: MacroData): string;
}

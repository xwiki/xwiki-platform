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

import ComponentInit from "./components/componentsInit";
import { DefaultMacroData } from "./components/defaultMacroData";
import { DefaultSkinManager } from "./components/defaultSkinManager";
import DefaultUIXTemplateProvider from "./components/defaultUIXTemplateProvider";
import DefaultVueTemplateProvider from "./components/defaultVueTemplateProvider";
import CTemplate from "./vue/c-template.vue";
import { ContentTools } from "./vue/contentTools";
import type { MacroData } from "./api/macroData";
import type { MacroProvider } from "./api/macroProvider";
import type { UIXTemplateProvider } from "./api/uixTemplateProvider";
import type { VueTemplateProvider } from "./api/vueTemplateProvider";

export type {
  MacroData,
  MacroProvider,
  UIXTemplateProvider,
  VueTemplateProvider,
};
export {
  CTemplate,
  ComponentInit,
  ContentTools,
  DefaultMacroData,
  DefaultSkinManager,
  DefaultUIXTemplateProvider,
  DefaultVueTemplateProvider,
};

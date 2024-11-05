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

import { ErrorMacro } from "./errorMacro";
import { WarningMacro } from "./warningMacro";
import { InfoMacro } from "./infoMacro";
import { SuccessMacro } from "./successMacro";
import type { MacroProvider } from "@xwiki/cristal-skin";
import type { Container } from "inversify";

export default class ComponentInit {
  constructor(container: Container) {
    container
      .bind<MacroProvider>("MacroProvider")
      .to(ErrorMacro)
      .whenTargetNamed(ErrorMacro.macroName);
    container
      .bind<MacroProvider>("MacroProvider")
      .to(WarningMacro)
      .whenTargetNamed(WarningMacro.macroName);
    container
      .bind<MacroProvider>("MacroProvider")
      .to(InfoMacro)
      .whenTargetNamed(InfoMacro.macroName);
    container
      .bind<MacroProvider>("MacroProvider")
      .to(SuccessMacro)
      .whenTargetNamed(SuccessMacro.macroName);
  }
}

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
import { Container, injectable } from "inversify";
import type { MacroCall, MacroWizard, MacroWizardOptions } from "./MacroWizard";

type XWikiMacroWizard = (
  input: { macroCall: Partial<MacroCall> } & MacroWizardOptions,
) => Promise<MacroCall>;

/**
 * Default MacroWizard implementation, using the Bootstrap-based Macro Wizard provided by xwiki-platform-wysiwyg-webjar.
 *
 * @since 18.3.0RC1
 */
@injectable("Singleton")
export class DefaultMacroWizard implements MacroWizard {
  public static bind(container: Container): void {
    container.bind("MacroWizard").to(DefaultMacroWizard).inSingletonScope();
  }

  public insertOrUpdate(
    macroCall: Partial<MacroCall>,
    options: MacroWizardOptions,
  ): Promise<MacroCall> {
    return new Promise((resolve, reject) => {
      requirejs(
        ["xwiki-wysiwyg-macro-wizard"],
        (macroWizard) => {
          (macroWizard as XWikiMacroWizard)({
            macroCall,
            ...options,
          })
            .then(resolve)
            .catch(reject);
        },
        reject,
      );
    });
  }
}

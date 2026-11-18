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
import type { EntityReference as XWikiEntityReference } from "@xwiki/platform-xwiki-model-api";

/**
 * Describes a macro call.
 *
 * @since 18.3.0RC1
 * @beta
 */
type MacroCall = {
  // The name of the macro being called.
  name: string;
  // The parameters passed to the macro. When passed as input to the Macro Wizard, the key is the lowercase parameter
  // name and the value is an object holding the case-sensitive parameter name and its value. When returned by the Macro
  // Wizard, the key is the case-sensitive parameter name and the value is the parameter value provided by the user.
  parameters: Record<string, { name: string; value: string } | string>;
  // The content of the macro, if any.
  content?: string;
  // Whether the macro is inline (i.e. used within a line of text) or block-level.
  inline: boolean;
  descriptor?: MacroDescriptor;
};

/**
 * Describes a rendering macro.
 *
 * @since 18.3.0RC1
 * @beta
 */
type MacroDescriptor = {
  id: string;
  name: string;
  description: string;
  supportsInlineMode: boolean;
  parametersMap: Record<string, MacroParameterDescriptor>;
};

/**
 * Describes a macro parameter.
 *
 * @since 18.3.0RC1
 * @beta
 */
type MacroParameterDescriptor = {
  id: string;
  name: string;
  description: string;
  advanced: boolean;
  mandatory: boolean;
  deprecated: boolean;
  displayType: string;
  defaultValue?: string;
  caseInsensitive: boolean;
};

/**
 * Configuration options for the Macro Wizard.
 *
 * @since 18.3.0RC1
 * @beta
 */
type MacroWizardOptions = {
  inlineParameters: Record<string, string>;
  inlineParametersSyntax: string;
  showInlineParameters: boolean;
  sourceDocumentReference: XWikiEntityReference;
  syntax: string;
};

/**
 * The component used to insert or update macro calls.
 *
 * @since 18.3.0RC1
 * @beta
 */
interface MacroWizard {
  /**
   * Inserts a new macro call or updates an existing one based on the provided options. If the given macro call doesn't
   * specify the macro name the user will be asked to select a macro first. Even if the macro name is specified, the
   * user can still change the macro. This means the output macro call can have a different macro name than the input
   * one.
   *
   * @param macroCall - the macro call to update, or, if the macro name is not specified, the default values to use
   *   after selecting a macro
   * @param options - the configuration options to use when inserting or updating the macro call
   * @returns a promise that resolves to the macro call that should be inserted or should replace the existing one
   */
  insertOrUpdate(
    macroCall: Partial<MacroCall>,
    options: MacroWizardOptions,
  ): Promise<MacroCall>;
}

export type { MacroCall, MacroWizard, MacroWizardOptions };

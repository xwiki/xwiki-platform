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
import { Container, inject, injectable } from "inversify";
import type { XWikiEntityReference } from "../model/reference/XWikiEntityReference";
import type {
  MacroWithUnknownParamsType,
  UnknownMacroParamsType,
} from "@xwiki/platform-macros-api";
import type { MacroInvocation } from "@xwiki/platform-uniast-api";

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
   * Inserts a new macro call or updates an existing one based on the provided options. If the given macro call doen't
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

/**
 * The component used to insert or update macro calls within the BlockNote editor.
 *
 * @since 18.3.0RC1
 * @beta
 */
interface BlockNoteMacroWizard {
  /**
   * Opens the Macro Wizard to either select a macro to insert or to edit the parameters of an existing macro call.
   *
   * @param macro - describes the (type of) client-side macro call to be updated or inserted; this is mainly used to
   *   determine whether the macro call is inline or block-level
   * @param parameters - the macro parameters, when updating an existing macro call, or the default values to use when
   *   inserting a new macro call
   * @param options - the configuration options to use when inserting or updating the macro call
   * @returns a promise that resolves to the parameters that should be used to call the macro
   */
  insertOrUpdate(
    macro: MacroWithUnknownParamsType,
    parameters: UnknownMacroParamsType,
    options: Partial<MacroWizardOptions>,
  ): Promise<UnknownMacroParamsType>;
}

/**
 * Default BlockNoteMacroWizard implementation, using the default MacroWizard implementation.
 *
 * @since 18.3.0RC1
 */
@injectable("Singleton")
export class DefaultBlockNoteMacroWizard implements BlockNoteMacroWizard {
  public static bind(container: Container): void {
    container
      .bind("BlockNoteMacroWizard")
      .to(DefaultBlockNoteMacroWizard)
      .inSingletonScope();
  }

  constructor(
    @inject("MacroWizard")
    private readonly macroWizard: MacroWizard,
  ) {}

  public async insertOrUpdate(
    macro: MacroWithUnknownParamsType,
    parameters: UnknownMacroParamsType,
    options: Partial<MacroWizardOptions>,
  ): Promise<UnknownMacroParamsType> {
    if (
      macro.infos.id !== "xwikiMacroBlock" &&
      macro.infos.id !== "xwikiInlineMacro"
    ) {
      // We don't know how to handle this macro. Leave the macro parameters unchanged.
      console.warn("[MacroWizard] Unknown macro id:", macro.infos.id);
      return parameters;
    }

    const macroInvocation: MacroInvocation = JSON.parse(
      parameters.call as string,
    );
    // Set default values for the configuration options.
    const actualOptions: MacroWizardOptions = {
      inlineParameters: this.getInlineParameters(macro, macroInvocation),
      inlineParametersSyntax: "uniast/1.0",
      showInlineParameters: true,
      sourceDocumentReference: XWiki.currentDocument.documentReference,
      syntax: XWiki.docsyntax,
      ...options,
    };
    const macroCall = await this.macroWizard.insertOrUpdate(
      this.getMacroCall(macro, macroInvocation, actualOptions.inlineParameters),
      actualOptions,
    );
    return this.getMacroParameters(
      this.getMacroInvocation(macroCall),
      parameters.output,
    );
  }

  private getInlineParameters(
    macro: MacroWithUnknownParamsType,
    macroInvocation: MacroInvocation,
  ): Record<string, string> {
    const inlineParameters = Object.fromEntries(
      Object.entries(macroInvocation.params)
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        .filter(([key, value]) => typeof value !== "string")
        .map(([key, value]) => [
          key,
          this.serializeInlineParameterValue(value),
        ]),
    );
    switch (macroInvocation.body.type) {
      case "inlineContent":
        inlineParameters.$content = this.serializeInlineParameterValue(
          macroInvocation.body.inlineContent,
        );
        break;
      case "inlineContents":
        inlineParameters.$content = this.serializeInlineParameterValue(
          macroInvocation.body.inlineContents,
        );
        break;
    }
    return inlineParameters;
  }

  private serializeInlineParameterValue(value: unknown): string {
    const blocks = Array.isArray(value) ? value : [value];
    return JSON.stringify({ blocks });
  }

  private getMacroCall(
    macro: MacroWithUnknownParamsType,
    macroInvocation: MacroInvocation,
    inlineParameters: Record<string, string>,
  ): MacroCall {
    const macroCall: MacroCall = {
      name: macroInvocation.id,
      // Remove parameters that are editable inline.
      parameters: Object.fromEntries(
        Object.entries(macroInvocation.params)
          .filter(([key]) => !inlineParameters[key])
          .map(([key, value]) => [
            key.toLowerCase(),
            { name: key, value: String(value) },
          ]),
      ),
      inline: macro.renderAs === "inline",
    };
    // Add the macro content only if it's not editable in-place. If the content is editable in-place then it must have
    // been already added to the inlineParameters field of MacroWizardOptions.
    if (macroInvocation.body.type === "raw") {
      macroCall.content = macroInvocation.body.content;
    }
    return macroCall;
  }

  private getMacroInvocation(macroCall: MacroCall): MacroInvocation {
    return {
      id: macroCall.name,
      params: Object.fromEntries(
        Object.entries(macroCall.parameters).map(([key, value]) => [
          key,
          value as string,
        ]),
      ),
      body: macroCall.content
        ? { type: "raw", content: macroCall.content }
        : { type: "none" },
    };
  }

  private getMacroParameters(
    macroInvocation: MacroInvocation,
    output: string | number | boolean,
  ): UnknownMacroParamsType {
    return {
      call: JSON.stringify(macroInvocation),
      // We don't update the macro output for now. We could make this function async and ask the server to execute the
      // macro call and return the output but the output can depend on the context where the macro is called so best is
      // to re-render the entire content after updating or inserting a macro call. This should be done outside of the
      // macro wizard, so in the end here we'll just return an empty output.
      output,
    };
  }
}

export type {
  BlockNoteMacroWizard,
  MacroCall,
  MacroWizard,
  MacroWizardOptions,
};

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
import { BLOCKNOTE_SYNTAX } from "../blocknote/syntax";
import { macroEditorName } from "@xwiki/platform-editors-blocknote-react";
import { toXWikiEntityReference } from "@xwiki/platform-model-xwiki";
import { Container, inject, injectable } from "inversify";
import type { MacroCall, MacroWizard, MacroWizardOptions } from "./MacroWizard";
import type {
  EditorInstance,
  InlineMacroInvocation,
  MacroBlockInvocation,
  MacroEditor,
  MacroInsertionEditorPrefillData,
} from "@xwiki/platform-editors-blocknote-react";

/**
 * A macro invocation, either block-level or inline. This is the editor-level representation of a macro call, used to
 * insert or edit the server-rendered xwikiMacroBlock / xwikiInlineMacro macros.
 */
type MacroInvocation = MacroBlockInvocation | InlineMacroInvocation;

/**
 * Default MacroEditor implementation, using the Macro Wizard: the macro calls are rendered in the syntax of the edited
 * content, for its source document.
 *
 * @since 18.10.0RC1
 */
@injectable("Singleton")
export class DefaultMacroEditor implements MacroEditor {
  public static bind(container: Container): void {
    container
      .bind<MacroEditor>(macroEditorName)
      .to(DefaultMacroEditor)
      .inSingletonScope();
  }

  constructor(
    @inject("MacroWizard")
    private readonly macroWizard: MacroWizard,
  ) {}

  public edit(
    instance: EditorInstance,
    invocation: MacroInvocation,
  ): Promise<MacroInvocation | undefined> {
    const inlineParameters = this.getInlineParameters(invocation);
    return this.showWizard(
      this.getMacroCall(invocation, inlineParameters),
      this.getWizardOptions(instance, inlineParameters),
    );
  }

  public insert(
    instance: EditorInstance,
    { kind, params, body }: MacroInsertionEditorPrefillData,
  ): Promise<MacroInvocation | undefined> {
    return this.showWizard(
      {
        inline: kind === "inline",
        parameters: params
          ? Object.fromEntries(
              Object.entries(params).map(([key, value]) => [
                key,
                value.toString(),
              ]),
            )
          : {},
        // Only a raw body can be prefilled through the Macro Wizard's content field.
        content: body?.type === "raw" ? body.content : undefined,
      },
      this.getWizardOptions(instance, {}),
    );
  }

  /**
   * @param macroCall - the macro call to edit, or the values to prefill the wizard with
   * @param options - the configuration of the wizard
   * @returns the macro invocation chosen by the user, or undefined if the user canceled
   */
  private async showWizard(
    macroCall: Partial<MacroCall>,
    options: MacroWizardOptions,
  ): Promise<MacroInvocation | undefined> {
    let result: MacroCall;
    try {
      result = await this.macroWizard.insertOrUpdate(macroCall, options);
    } catch (error) {
      // The Macro Wizard rejects without a reason when the user cancels.
      if (error) {
        throw error;
      }
      return undefined;
    }
    return this.getMacroInvocation(result);
  }

  /**
   * @param instance - the editor instance where the macro is inserted or edited
   * @param inlineParameters - the macro parameters (and content) that are edited in-place
   * @returns the configuration of the wizard: the macro is rendered in the syntax of the edited content, for its source
   *   document, like it will be when the content is saved
   */
  private getWizardOptions(
    instance: EditorInstance,
    inlineParameters: Record<string, string>,
  ): MacroWizardOptions {
    const { sourceDocumentReference, syntax } = instance.options;
    return {
      inlineParameters,
      // The macro parameters and content edited in-place are in the syntax of the editor content.
      inlineParametersSyntax: BLOCKNOTE_SYNTAX,
      showInlineParameters: true,
      sourceDocumentReference: sourceDocumentReference
        ? toXWikiEntityReference(sourceDocumentReference)
        : XWiki.currentDocument.documentReference,
      syntax,
    };
  }

  private getInlineParameters(
    macroInvocation: MacroInvocation,
  ): Record<string, string> {
    const inlineParameters = Object.fromEntries(
      Object.entries(macroInvocation.params)
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        .filter(([key, value]) => typeof value !== "string")
        .map(([key, value]) => [
          key.toLowerCase(),
          this.serializeInlineParameterValue(value),
        ]),
    );

    if (
      macroInvocation.body.type === "inlineContents" ||
      macroInvocation.body.type === "inlineContent"
    ) {
      inlineParameters.$content = this.serializeInlineParameterValue(
        macroInvocation.body.content,
      );
    }
    return inlineParameters;
  }

  private serializeInlineParameterValue(value: unknown): string {
    return JSON.stringify(Array.isArray(value) ? value : [value]);
  }

  private getMacroCall(
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
      inline: macroInvocation.kind === "inline",
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
      kind: macroCall.inline ? "inline" : "block",
      id: macroCall.name,
      params: Object.fromEntries(
        Object.entries(macroCall.parameters).map(([key, value]) => [
          key,
          typeof value === "string" ? value : value.value,
        ]),
      ),
      body: macroCall.content
        ? { type: "raw", content: macroCall.content }
        : { type: "none" },
    };
  }
}

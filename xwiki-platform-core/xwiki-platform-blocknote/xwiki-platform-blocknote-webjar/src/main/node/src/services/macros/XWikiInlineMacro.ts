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
import { eraseParamsTypeForMacroClass } from "@xwiki/platform-macros-api";
import { Container, injectable } from "inversify";
import type {
  GetConcreteMacroParametersType,
  InlineMacro,
  MacroInfos,
  MacroInlineContent,
  MacroWithUnknownParamsType,
} from "@xwiki/platform-macros-api";

const macroParams = {
  call: { type: "string" },
  output: { type: "string" },
} as const;

type MacroParams = typeof macroParams;

@injectable()
export class XWikiInlineMacro implements InlineMacro<MacroParams> {
  public static bind(container: Container): void {
    container
      .bind<MacroWithUnknownParamsType>("Macro")
      .to(eraseParamsTypeForMacroClass(XWikiInlineMacro));
  }

  readonly infos: MacroInfos<MacroParams> = {
    id: "xwikiInlineMacro",
    name: "XWiki Inline Macro",
    description: "Renders the output of an XWiki server-side inline macro.",
    params: macroParams,
    paramsDescription: {
      call: "The JSON stringified MacroInvocation of the server-side macro",
      output: "The JSON stringified output of the server-side macro",
    },
    defaultParameters: false,
    bodyType: "none",
  };

  renderAs = "inline" as const;

  render({
    output,
  }: GetConcreteMacroParametersType<MacroParams>): MacroInlineContent[] {
    return JSON.parse(output || "[]");
  }
}

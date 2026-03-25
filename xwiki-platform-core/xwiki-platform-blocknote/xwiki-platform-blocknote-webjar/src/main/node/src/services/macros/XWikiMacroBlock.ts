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
  BlockMacro,
  GetConcreteMacroParametersType,
  MacroBlock,
  MacroInfos,
  MacroWithUnknownParamsType,
} from "@xwiki/platform-macros-api";

const macroParams = {
  call: { type: "string" },
  output: { type: "string" },
} as const;

type MacroParams = typeof macroParams;

@injectable()
export class XWikiMacroBlock implements BlockMacro<MacroParams> {
  public static bind(container: Container): void {
    container
      .bind<MacroWithUnknownParamsType>("Macro")
      .to(eraseParamsTypeForMacroClass(XWikiMacroBlock));
  }

  readonly infos: MacroInfos<MacroParams> = {
    id: "xwikiMacroBlock",
    name: "XWiki Macro Block",
    description:
      "Renders the output of an XWiki server-side stand-alone macro.",
    params: macroParams,
    paramsDescription: {
      call: "The JSON stringified MacroInvocation of the server-side macro",
      output: "The JSON stringified output of the server-side macro",
    },
    defaultParameters: false,
    bodyType: "none",
  };

  renderAs = "block" as const;

  render({
    output,
  }: GetConcreteMacroParametersType<MacroParams>): MacroBlock[] {
    return JSON.parse(output || "[]");
  }
}

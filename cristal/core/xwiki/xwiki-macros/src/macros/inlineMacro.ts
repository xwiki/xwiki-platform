/**
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

import { injectable } from "inversify";
import type {
  GetConcreteMacroParametersType,
  InlineMacro,
  MacroInfos,
  MacroInlineContent,
} from "@xwiki/cristal-macros-api";

const macroParams = {
  html: { type: "string" },
  metadata: { type: "string" },
} as const;

type MacroParams = typeof macroParams;

@injectable()
export class XWikiInlineHtmlMacro implements InlineMacro<MacroParams> {
  readonly infos: MacroInfos<MacroParams> = {
    id: "xwikiInlineHtml",
    name: "XWiki HTML Inline Content",
    description: "HTML rendering of an XWiki macro inline content",
    params: macroParams,
    paramsDescription: {
      html: "Server-rendered HTML content",
      metadata: "Metadata used to generate the XWiki macro",
    },
    defaultParameters: false,
  };

  renderAs = "inline" as const;

  render({
    html,
  }: GetConcreteMacroParametersType<MacroParams>): MacroInlineContent[] {
    return [{ type: "rawHtml", html }];
  }
}

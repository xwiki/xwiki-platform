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

import { WikiModel } from "./wikimodel-teavm";
import { inject, injectable } from "inversify";
import type { Converter } from "../api/converter";
import type { Logger } from "@xwiki/cristal-api";

@injectable()
export class XWiki21ToHTMLConverter implements Converter {
  public static converterName = "xwiki_html";

  private logger: Logger;
  public wikimodel: WikiModel;

  constructor(@inject<Logger>("Logger") logger: Logger) {
    this.logger = logger;
    this.logger.setModule("rendering.xwiki");
    this.wikimodel = new WikiModel();
  }

  public async isConverterReady(): Promise<boolean> {
    return await this.wikimodel.isWikiModelLoaded();
  }

  public getSourceSyntax(): string {
    return "xwiki/2.1";
  }

  public getTargetSyntax(): string {
    return "html/5.0";
  }

  public getVersion(): string {
    return "1.0";
  }

  public getName(): string {
    return XWiki21ToHTMLConverter.converterName;
  }

  public async convert(source: string): Promise<string> {
    if (source == undefined) return "";

    return this.wikimodel.parse(source);
  }
}

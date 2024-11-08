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

import macro from "./marked-macro";
import DOMPurify from "dompurify";
import { inject, injectable } from "inversify";
import { marked } from "marked";
import { baseUrl } from "marked-base-url";
import type { Converter } from "../api/converter";
import type { Logger, WikiConfig } from "@xwiki/cristal-api";

@injectable()
export class MarkdownToHTMLConverter implements Converter {
  public static converterName = "md_html";

  private logger: Logger;
  public markedInit: boolean;
  public sanitizeConfig: DOMPurify.Config = {
    ADD_TAGS: ["#comment"],
    ADD_ATTR: ["macroname"],
    FORCE_BODY: true,
  };

  constructor(@inject<Logger>("Logger") logger: Logger) {
    this.logger = logger;
    this.logger.setModule("rendering.markdown");
    this.markedInit = false;
  }

  public async isConverterReady(): Promise<boolean> {
    return true;
  }

  public getSourceSyntax(): string {
    return "md/1.0";
  }

  public getTargetSyntax(): string {
    return "html/5.0";
  }

  public getVersion(): string {
    return "1.0";
  }

  public getName(): string {
    return MarkdownToHTMLConverter.converterName;
  }

  public async convert(
    source: string,
    wikiConfig: WikiConfig,
  ): Promise<string> {
    let content = "";
    if (!this.markedInit) {
      marked.use(baseUrl(wikiConfig.baseURL)).use(macro);
      this.markedInit = true;
    }
    const html = marked(source) as string;
    this.logger?.debug("HTML before sanitize", html);
    content = DOMPurify.sanitize(html, this.sanitizeConfig) as string;
    this.logger?.debug("HTML after sanitize", content);
    return content;
  }
}

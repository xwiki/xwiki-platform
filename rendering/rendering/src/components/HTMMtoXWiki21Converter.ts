/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/

import { inject, injectable } from "inversify";
import { Converter } from "../api/converter";
import { Logger, WikiConfig } from "@cristal/api";

@injectable()
export class HTMLToXWiki21Converter implements Converter {
  public static converterName: "html_xwiki";

  private logger: Logger;

  constructor(@inject<Logger>("Logger") logger: Logger) {
    this.logger = logger;
  }

  async convert(source: string, wikiConfig: WikiConfig): Promise<string> {
    this.logger.debug("Convert from", source, "using", wikiConfig);
    /*
     TODO: this conversion is theoretically possible by calling the xwiki
      backend.
     1. call /xwiki/rest/ and save the "XWiki-Form-Token" header
     2. /xwiki/bin/get/CKEditor/HTMLConverter with the same parameters as
     CKEditor when switching to code view.
     Some limitations:
     The server must return CORS headers + allow for authentication on CORS
     requests + allow the XWiki-Form-Token header to be accessed on CORS
     requests.
    */
    return "";
  }

  getName(): string {
    return HTMLToXWiki21Converter.converterName;
  }

  getSourceSyntax(): string {
    return "html/5.0";
  }

  getTargetSyntax(): string {
    return "xwiki/2.1";
  }

  getVersion(): string {
    return "";
  }

  isConverterReady(): Promise<boolean> {
    return Promise.resolve(true);
  }
}

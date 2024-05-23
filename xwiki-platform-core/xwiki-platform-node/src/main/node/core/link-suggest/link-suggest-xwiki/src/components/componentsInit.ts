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

import { Container, injectable, inject } from "inversify";
import {
  Link,
  LinkSuggestService,
  name,
} from "@xwiki/cristal-link-suggest-api";
import { CristalApp } from "@xwiki/cristal-api";

/**
 * Default implementation of the link suggestion service, for XWiki.
 * Currently limited to guests, and to XWiki instances that have explicitly
 * allowed CORS, see https://cristal.xwiki.org/xwiki/bin/view/Backends/XWiki/
 * @since 0.8
 */
@injectable()
class DefaultLinkSuggestService implements LinkSuggestService {
  private cristalApp: CristalApp;

  constructor(@inject<CristalApp>("CristalApp") cristalApp: CristalApp) {
    this.cristalApp = cristalApp;
  }

  async getLinks(query: string): Promise<Link[]> {
    // TODO: currently only proposing links available to guest
    const baseURL = this.cristalApp.getWikiConfig().baseURL;
    const getParams = new URLSearchParams({
      sheet: "CKEditor.LinkSuggestions",
      outputSyntax: "plain",
      language: "en", // TODO: add support for multiple languages
      input: query,
    }).toString();
    const response = await fetch(`${baseURL}/bin/get/Main/?${getParams}`, {
      headers: {
        Accept: "application/json",
      },
    });

    const json = await response.json();

    return json.map(({ id, url, reference, label, hint }: Link) => {
      return {
        id,
        url,
        reference,
        label,
        hint,
      };
    });
  }
}

export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<LinkSuggestService>(name)
      .to(DefaultLinkSuggestService)
      .inSingletonScope();
  }
}

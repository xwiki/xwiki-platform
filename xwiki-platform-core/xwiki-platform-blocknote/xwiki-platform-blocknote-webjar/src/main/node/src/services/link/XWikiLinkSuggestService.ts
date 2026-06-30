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
import { LinkType } from "@xwiki/platform-link-suggest-api";
import { Container, injectable } from "inversify";
import type {
  Link,
  LinkSuggestService,
} from "@xwiki/platform-link-suggest-api";

@injectable("Singleton")
export class XWikiLinkSuggestService implements LinkSuggestService {
  public static bind(container: Container): void {
    container
      .bind("LinkSuggestService")
      .to(XWikiLinkSuggestService)
      .inSingletonScope()
      .whenNamed("XWiki");
  }

  public async getLinks(
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    query: string,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    linkType?: LinkType,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    mimetype?: string,
  ): Promise<Link[]> {
    // TODO
    return Promise.resolve([]);
  }
}

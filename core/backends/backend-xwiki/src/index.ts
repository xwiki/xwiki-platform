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

import { XWikiWikiConfig } from "./XWikiWikiConfig";
import { XWikiStorage } from "./xwikiStorage";
import { whenNamedOrDefault } from "@xwiki/cristal-utils-inversify";
import { Container } from "inversify";
import type { Storage, WikiConfig } from "@xwiki/cristal-api";

/**
 * @beta
 */
export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<WikiConfig>("WikiConfig")
      .to(XWikiWikiConfig)
      .when(whenNamedOrDefault("XWiki"));
    container.bind<Storage>("Storage").to(XWikiStorage).whenNamed("XWiki");
  }
}

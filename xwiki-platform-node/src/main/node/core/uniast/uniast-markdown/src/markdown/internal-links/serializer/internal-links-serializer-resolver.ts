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
import { inject, injectable } from "inversify";
import type { InternalLinksSerializer } from "./internal-links-serializer";
import type { CristalApp } from "@xwiki/cristal-api";

/**
 * @since 0.22
 */
@injectable()
export class InternalLinksSerializerResolver {
  constructor(@inject("CristalApp") private readonly cristalApp: CristalApp) {}

  async get(): Promise<InternalLinksSerializer> {
    const type = this.cristalApp.getWikiConfig().getType();
    try {
      const factory: () => Promise<InternalLinksSerializer> =
        await this.cristalApp
          .getContainer()
          .getAsync("Factory<InternalLinksSerializer>", { name: type });
      return factory();
    } catch (e) {
      console.debug(e);
      throw new Error(`Could not resolve serializer for type ${type}`);
    }
  }
}

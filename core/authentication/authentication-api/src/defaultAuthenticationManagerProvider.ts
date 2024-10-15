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

import { inject, injectable } from "inversify";
import { AuthenticationManagerProvider } from "./authenticationManagerProvider";
import { AuthenticationManager } from "./authenticationManager";
import { type CristalApp } from "@xwiki/cristal-api";

/**
 * Default implementation of the authentication manager. Resolve the class
 * by looking for a component with the provided name in inversify.
 * @since 0.11
 */
@injectable()
class DefaultAuthenticationManagerProvider
  implements AuthenticationManagerProvider
{
  constructor(
    @inject<CristalApp>("CristalApp") private cristalApp: CristalApp,
  ) {}

  get(type?: string): AuthenticationManager | undefined {
    const resolvedType = type || this.cristalApp.getWikiConfig().getType();
    try {
      return this.cristalApp
        .getContainer()
        .getNamed("AuthenticationManager", resolvedType);
    } catch (e) {
      this.cristalApp
        .getLogger("authentication.api")
        .warn(
          `Couldn't resolve AuthenticationManager for type=[${resolvedType}]`,
          e,
        );
      return undefined;
    }
  }
}

export { DefaultAuthenticationManagerProvider };

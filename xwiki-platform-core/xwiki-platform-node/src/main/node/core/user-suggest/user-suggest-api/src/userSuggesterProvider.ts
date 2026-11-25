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

import type { UserSuggester } from "./userSuggester";

/**
 * The name under which a {@link UserSuggesterProvider} is bound in the service container.
 *
 * @since 19.0.0RC1
 * @beta
 */
const userSuggesterProviderName = "UserSuggesterProvider";

/**
 * Resolves the user suggester of a backend, since an application can be configured with several backends at once.
 *
 * @since 19.0.0RC1
 * @beta
 */
interface UserSuggesterProvider {
  /**
   * Resolve the user suggester of a backend.
   *
   * @param type - the type of the backend to look the users up in, the current one when not specified
   * @returns the user suggester of that backend, none when it can't look users up
   */
  get(type?: string): UserSuggester | undefined;
}

export { userSuggesterProviderName };
export type { UserSuggesterProvider };

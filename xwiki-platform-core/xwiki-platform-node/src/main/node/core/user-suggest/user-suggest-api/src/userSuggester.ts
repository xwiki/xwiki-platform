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

import type { SuggestedUser } from "./suggestedUser";

/**
 * The name under which a {@link UserSuggester} is bound in the service container, under the name of its backend.
 *
 * @since 19.0.0RC1
 * @beta
 */
const userSuggesterName = "UserSuggester";

/**
 * How to look the users up.
 *
 * @since 19.0.0RC1
 * @beta
 */
type UserSuggestOptions = {
  /**
   * How many users to suggest at most. The implementation decides when not specified.
   */
  limit?: number;
};

/**
 * Looks up the users of the backend, e.g. to mention one in a content or to pick one in a user picker. Which users
 * can be looked up, and how they are matched, is the backend's business.
 *
 * @since 19.0.0RC1
 * @beta
 */
interface UserSuggester {
  /**
   * Looks up the users matching the given query.
   *
   * @param query - the text the suggested users have to match
   * @param options - how to look the users up
   * @returns the users matching the given query
   */
  suggestUsers(
    query: string,
    options?: UserSuggestOptions,
  ): Promise<SuggestedUser[]>;
}

export { userSuggesterName };
export type { UserSuggestOptions, UserSuggester };

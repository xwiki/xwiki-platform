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
import type { MentionableUser } from "@xwiki/platform-editors-blocknote-react";

/**
 * The component suggesting the users that can be mentioned in the editor.
 *
 * @since 18.10.0RC1
 * @beta
 */
interface UserSuggest {
  /**
   * @param query - the text the suggested users have to match
   * @returns the users matching the given query
   */
  getUsers(query: string): Promise<MentionableUser[]>;
}

export type { UserSuggest };

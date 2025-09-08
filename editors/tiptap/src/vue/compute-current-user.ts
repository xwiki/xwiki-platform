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

import noavatar from "../images/noavatar.png";
import type { User } from "../extensions/user";
import type { AuthenticationManager } from "@xwiki/cristal-authentication-api";

/**
 *
 * @param authentication - an authentication manager components
 * @since 0.14
 * @beta
 */
export async function computeCurrentUser(
  authentication?: AuthenticationManager,
): Promise<User> {
  let ret = {
    name: "Anonymous",
    avatar: noavatar,
  };
  if (authentication && (await authentication.isAuthenticated())) {
    try {
      const userDetails = await authentication.getUserDetails();
      ret = {
        name: userDetails.name,
        avatar: userDetails.avatar ?? noavatar,
      };
    } catch (e) {
      console.error("Failed to get the user details", e);
      ret = {
        name: "Unknown",
        avatar: noavatar,
      };
    }
  }

  return ret;
}

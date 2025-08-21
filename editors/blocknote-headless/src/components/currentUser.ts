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

import { stringToColor } from "../utils";
import type { AuthenticationManager } from "@xwiki/cristal-authentication-api";

/**
 * Realtime user
 * @since 0.16
 */
export type User = {
  name: string;
  color: string;
};

/**
 *
 * @param authentication - an authentication manager components
 * @since 0.16
 */
export async function computeCurrentUser(
  authentication?: AuthenticationManager,
): Promise<User> {
  let name = "Anonymous";

  if (authentication && (await authentication.isAuthenticated())) {
    try {
      const userDetails = await authentication.getUserDetails();
      name = userDetails.name;
    } catch (e) {
      console.error("Failed to get the user details", e);
      name = "<Error>";
    }
  }

  return {
    name,
    color: stringToColor(name),
  };
}

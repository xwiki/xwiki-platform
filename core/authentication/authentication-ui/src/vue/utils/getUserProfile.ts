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

import { type AuthenticationManager } from "@xwiki/cristal-authentication-api";

/**
 * Access to user profile.
 *
 * @returns the name and profile url, and an error status
 * @since 0.11
 */
export async function getUserProfile(
  authenticationManager: AuthenticationManager,
): Promise<{
  name: string | undefined;
  profile: string | undefined;
  error: boolean;
}> {
  let profile = undefined;
  let name = undefined;
  let error = false;
  try {
    const details = await authenticationManager.getUserDetails();
    profile = details.profile;
    name = details.name;
  } catch (e) {
    console.log(e);
    error = true;
  }
  return { profile, name, error };
}

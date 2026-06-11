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

import type { UserDetails } from "@xwiki/platform-authentication-api";

/**
 * Holds information for a user connected to a realtime collaboration session.
 *
 * @since 18.4.0RC1
 * @beta
 */
export type Collaborator = {
  /**
   * The id that is assigned to the user when they join the realtime collaboration session.
   */
  id?: string;

  /**
   * The user that joins the realtime collaboration session. The same user can join multiple collaboration sessions,
   * multiple times.
   */
  user: UserDetails;

  /**
   * The color associated to the user in the realtime collaboration session (e.g. when displaying the user caret inside
   * the edited content).
   */
  color: string;
};

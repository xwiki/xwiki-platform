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

/**
 * How much of the edited content has been saved. The numeric values are part of the contract with the editors and
 * the real-time toolbar, which compare against them.
 *
 * @since 18.8.0RC1
 * @beta
 */
export enum SaveStatus {
  /**
   * There are changes that no client has saved yet.
   */
  Dirty = 0,

  /**
   * A save is in progress.
   */
  Saving = 1,

  /**
   * Every change has been saved.
   */
  Clean = 2,
}

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
import type { QueryConstraint } from "./queryConstraint";

/**
 * A set of constraints to apply to a given property.
 *
 * @since 18.2.0RC1
 * @beta
 */
export interface Filter {
  /**
   * The property of filter.
   */
  property: string;
  /**
   * When true, all the constraints must be true for the filter to allow the entry to be displayed. When false, only
   * on of the constraint must be true.
   */
  matchAll: boolean;
  /**
   * The set of constraints to apply on the query.
   */
  constraints: QueryConstraint[];
}

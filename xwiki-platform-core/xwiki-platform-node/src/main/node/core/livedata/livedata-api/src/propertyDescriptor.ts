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
import type { DisplayerDescriptor } from "./displayerDescriptor";
import type { Filter } from "./filter";

/**
 * Describes how the user interacts with a given property.
 * @since 18.2.0RC1
 * @beta
 */
export interface PropertyDescriptor {
  /**
   * The property id.
   */
  id: string;
  /**
   * The property type.
   */
  type: string;
  /**
   * The display of the property.
   */
  displayer: DisplayerDescriptor;
  /**
   * The filter of the property.
   */
  filter: Filter;
  /**
   * true when the property is editable.
   */
  editable?: boolean;
  /**
   * true when the property is sortable.
   */
  sortable?: boolean;
  /**
   * true when the property is visible.
   */
  visible?: boolean;
  /**
   * true when the property is filterable.
   */
  filterable?: boolean;
}

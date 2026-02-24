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

import type { ActionDescriptor } from "./actionDescriptor";
import type { DisplayerDescriptor } from "./displayerDescriptor";
import type { EntryDescriptor } from "./entryDescriptor";
import type { FilterDescriptor } from "./filterDescriptor";
import type { LayoutDescriptor } from "./layoutDescriptor";
import type { PropertyDescriptor } from "./propertyDescriptor";

/**
 * Describes the configuration used to display the live data.
 * @since 18.2.0RC1
 * @beta
 */
export interface Meta {
  /**
   * Sets the list of supported layouts.
   */
  layouts: LayoutDescriptor[];
  /**
   * The descriptor of the live data entries.
   */
  entryDescriptor: EntryDescriptor;
  /**
   * The list of known properties.
   */
  propertyDescriptors: PropertyDescriptor[];
  /**
   * The list of known property types.
   */
  propertyTypes: PropertyDescriptor[];
  /**
   * The default displayer used to display live data properties.
   */
  defaultDisplayer: string;
  /**
   * The list of known property displayers
   */
  displayers: DisplayerDescriptor[];
  /**
   * The list of known filter widgets.
   */
  filters: FilterDescriptor[];
  /**
   * The descriptors of supported live data actions
   */
  actions: ActionDescriptor[];
  /**
   * The live data entry selection.
   * TODO: the selection is currently partial an must be completed once a proper selection implementation is done.
   */
  selection: { enabled: boolean };
  /**
   * The default layout used to display the live data
   */
  defaultLayout: string;
}

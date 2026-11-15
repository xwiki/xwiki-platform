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
 * An item of the select component: either a string, used both as the value and as the label of the item, or an
 * object with a distinct value and label, for when the displayed label is not a suitable value (e.g. when it is
 * localized, or when several items can have the same label).
 * @since 18.10.0RC1
 * @beta
 */
type SelectItem = string | { value: string; label: string };

/**
 * Props of the select component.
 * @since 18.0.0RC1
 * @beta
 */
type SelectProps = {
  /**
   * Label to display.
   */
  label: string;
  /**
   * List of items to select from.
   */
  items: Array<SelectItem>;
  /**
   * Help message.
   */
  help?: string;
  /**
   * Whether or not selecting an item from the list is mandatory.
   * Default: false.
   */
  required?: boolean;
  /**
   * The v-model value of the currently selected item (its value, not its label).
   */
  modelValue?: string;
};

export type { SelectItem, SelectProps };

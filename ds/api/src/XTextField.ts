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

/**
 * Props of the text-field component.
 * @since 0.9
 */
type TextFieldProps = {
  name?: string;
  label: string;
  required?: boolean;
  /**
   * Whether the field should be focused on load or not (default: false).
   * @since 0.13
   */
  autofocus?: boolean;
  modelValue?: string;
  /**
   * Help message.
   * @since 0.15
   */
  help?: string;
  /**
   * Whether the field should be readonly (default: false).
   * @since 0.15
   */
  readonly?: boolean;
};

export type { TextFieldProps };

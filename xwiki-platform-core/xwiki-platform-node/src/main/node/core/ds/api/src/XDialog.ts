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
 * @since 18.2.0RC1
 * @beta
 */
type DialogProps = {
  title: string;
  width?: string | number | undefined;
  modelValue?: boolean;
  /**
   * Whether the dialog stays open when the user clicks outside of it, so that what is typed in it is not lost by
   * accident. It can still be closed with the Escape key or its close button. Default: false.
   * @since 18.10.0RC1
   */
  persistent?: boolean;
};

export type { DialogProps };

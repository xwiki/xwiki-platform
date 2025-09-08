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
import { Editor } from "@tiptap/core";
import type { Range } from "@tiptap/core";

/**
 * Defines the structure of a slash action descriptor.
 *
 * @since 0.8
 * @beta
 */
export interface ActionDescriptor {
  title: string;
  /**
   * An optional sort field to be used instead of the title when sorting the
   * actions.
   */
  sortField?: string;
  command: (commandParams: { editor: Editor; range: Range }) => void;
  icon: string;
  hint: string;
  /**
   * A list of strings that are not expected to be displayed but that are used
   * when filtering for actions in the UI.
   */
  aliases?: string[];
}

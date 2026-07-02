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
import type { BlockType } from "@xwiki/platform-editors-blocknote-react";

/**
 * Processes the BlockNote content before it is loaded in the editor and before it is submitted to be saved server-side.
 * In other words, it acts as a pre-processor and post-processor for the BlockNote content.
 *
 * @beta
 * @since 18.6.0RC1
 */
interface BlockNoteProcessor {
  /**
   * Prepares the BlockNote content for being loaded in the editor.
   *
   * @param blockNoteJSON - the BlockNote content in JSON format
   * @returns the BlockNote content to be loaded in the editor
   */
  load(blockNoteJSON: string): BlockType[];

  /**
   * Prepares the BlockNote content for being submitted to be saved server-side.
   *
   * @param blockNoteContent - the BlockNote content to be submitted to be saved server-side
   * @returns the JSON serialization of the BlockNote content to be saved
   */
  save(blockNoteContent: BlockType[]): string;
}

export { type BlockNoteProcessor };

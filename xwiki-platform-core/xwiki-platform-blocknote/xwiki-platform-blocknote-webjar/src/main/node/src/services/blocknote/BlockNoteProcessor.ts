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
import type {
  BlockType,
  MacroFeatureOptions,
} from "@xwiki/platform-editors-blocknote-react";

/**
 * Encapsulates the BlockNote content and the output of its macro calls (which must not be part of the content).
 */
class BlockNoteDocument {
  /**
   * The output of the macro calls of the BlockNote content (rendered for the current user), by block id or by macro id,
   * with the macro call it was rendered for. It is passed to the editor, which displays it (see
   * `MacroFeatureOptions.outputs`).
   */
  public readonly macroOutputs: NonNullable<MacroFeatureOptions["outputs"]> =
    {};

  constructor(public content: BlockType[]) {}
}

/**
 * Processes the BlockNote content before it is loaded in the editor and before it is submitted to be saved server-side.
 * In other words, it acts as a pre-processor and post-processor for the BlockNote content.
 *
 * @beta
 * @since 18.6.0RC1
 */
interface BlockNoteProcessor {
  /**
   * Parses the given BlockNote JSON into a BlockNote document that can be loaded in the editor.
   *
   * @param blockNoteJSON - the BlockNote document in JSON format
   * @returns the BlockNote document to be loaded in the editor
   */
  load(blockNoteJSON: string): BlockNoteDocument;

  /**
   * Serializes the given BlockNote document in the BlockNote JSON format that can be saved server-side.
   *
   * @param blockNoteDocument - the BlockNote document to be serialized to JSON
   * @returns the JSON serialization of the given BlockNote document
   */
  save(blockNoteDocument: BlockNoteDocument): string;
}

export { BlockNoteDocument, type BlockNoteProcessor };

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
 * The metadata associated to a BlockNote block. Unfortunately, it's not always easy to extend the BlockNote schema to
 * support custom metadata, so we need to keep some metadata outside of the BlockNote schema.
 */
type BlockMetadata = Record<string, unknown>;

/**
 * Encapsulates the BlockNote content and the metadata that couldn't be stored in the BlockNote schema.
 */
class BlockNoteDocument {
  /**
   * The metadata associated to the BlockNote content. The key is the block ID and the value is the metadata associated
   * to that block.
   */
  private readonly metadata: Record<string, BlockMetadata> = {};

  constructor(public content: BlockType[]) {}

  /**
   * @param id - the ID of the block for which to retrieve the metadata
   * @param create - if true, creates the metadata object if it doesn't exist yet
   * @returns the metadata associated to the specified block, or undefined if the block doesn't have any metadata and
   *   create is false
   */
  public getMetadata(id: string, create?: boolean): BlockMetadata | undefined {
    let metadata = this.metadata[id];
    if (!metadata && create) {
      metadata = {};
      this.metadata[id] = metadata;
    }
    return metadata;
  }
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

export { type BlockMetadata, BlockNoteDocument, type BlockNoteProcessor };

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
import { createBlockMetadataStore } from "@xwiki/platform-editors-blocknote-react";
import type {
  BlockMetadataStore,
  BlockType,
} from "@xwiki/platform-editors-blocknote-react";

/**
 * Encapsulates the BlockNote content and the metadata that couldn't be stored in the BlockNote schema.
 */
class BlockNoteDocument {
  /**
   * The metadata associated to the BlockNote content, by block ID (or by the ID injected in a link, or by macro ID).
   * It is passed to the editor, which reads from it the output of the macros.
   */
  public readonly metadata: BlockMetadataStore;

  /**
   * @param content - the BlockNote content
   * @param metadata - the metadata associated to the given content, e.g. the metadata of the editor instance whose
   *   content is serialized; a new metadata store is created if not specified
   */
  constructor(
    public content: BlockType[],
    metadata?: BlockMetadataStore,
  ) {
    this.metadata = metadata ?? createBlockMetadataStore();
  }
}

/**
 * How to serialize a BlockNote document.
 *
 * @since 18.10.0RC1
 */
type SaveOptions = {
  /**
   * Whether to keep the id of each macro call (used to look up its output), as a macro parameter, so that the output of
   * the macro calls rendered server-side can be associated with them. The content serialized this way is meant to be
   * rendered, not saved.
   */
  macroIds?: boolean;
};

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
   * @param options - how to serialize the given BlockNote document (since 18.10.0RC1)
   * @returns the JSON serialization of the given BlockNote document
   */
  save(blockNoteDocument: BlockNoteDocument, options?: SaveOptions): string;
}

export { BlockNoteDocument, type BlockNoteProcessor, type SaveOptions };

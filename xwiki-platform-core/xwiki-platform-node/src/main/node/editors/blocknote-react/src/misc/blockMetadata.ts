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
 * The data kept about a block (or a link, or a macro call) of the edited content that BlockNote doesn't know about,
 * as entries keyed by name: what the BlockNote schema can't hold (e.g. the resource reference of an image), or what
 * must not be shared with the other users editing the same content in realtime (e.g. the output of a macro, rendered
 * for the current user).
 *
 * @since 18.10.0RC1
 * @beta
 */
type BlockMetadata = Record<string, unknown>;

/**
 * What is known about the content holding a pasted metadata entry, to decide whether the entry can be kept.
 *
 * @since 18.10.0RC1
 * @beta
 */
type PastedMetadataContext = {
  /**
   * The URL displayed by the pasted node, when it has one: the URL of a link (without its metadata), or the source of
   * an image.
   */
  url?: string;
};

/**
 * Identifies a metadata entry. It is defined by the editing feature that owns the entry, and used by all the code that
 * reads or sets the entry.
 *
 * @since 18.10.0RC1
 * @beta
 */
type BlockMetadataKey = {
  /**
   * The name of the metadata entry.
   */
  name: string;

  /**
   * Decides what happens to a value of this entry found in pasted (or dropped) content. The pasted content can come
   * from anywhere (another page, another application, a malicious web page), whatever its format, so its metadata
   * can't be trusted: a value is kept only when it can't do anything that the user doesn't see. The entries of the
   * metadata kept in the edited content (see `getBlockMetadata` and `getLinkMetadata`) are dropped from the pasted
   * content when this is missing, as are the entries without a key declared by an editing feature.
   *
   * @param value - the pasted value
   * @param context - what is known about the content holding the pasted value
   * @returns the value to keep, or undefined to drop the entry
   */
  filterPasted?: (value: unknown, context: PastedMetadataContext) => unknown;
};

/**
 * Holds the private {@link BlockMetadata} of the edited content (e.g. the output of the macros, rendered with the rights
 * of the current user), by id: the block id for a block, the `id` property for an inline macro, or an id of the
 * integration's choosing otherwise. This metadata is local to the current user: it isn't part of the edited content,
 * so it isn't shared with the other users editing the same content in realtime, and it isn't restored by undo / redo.
 * It must be derived from the content (e.g. a cache), so that it doesn't matter when it gets out of sync with it. The
 * metadata that must follow the content is kept in the content instead (see `getBlockMetadata` and
 * `getLinkMetadata`).
 *
 * @since 18.10.0RC1
 * @beta
 */
interface BlockMetadataStore {
  /**
   * Gets the metadata of a block.
   *
   * @param id - identifies the block (or the link, or the macro call)
   * @returns the metadata of the specified block, or undefined if it has none; it must not be modified, use
   *   {@link BlockMetadataStore.set} instead
   */
  get(id: string): Readonly<BlockMetadata> | undefined;

  /**
   * Sets an entry of the metadata of a block.
   *
   * @param id - identifies the block (or the link, or the macro call)
   * @param key - identifies the metadata entry
   * @param value - the value of the metadata entry, or undefined to remove the entry (a block left without metadata
   *   entries has no metadata)
   */
  set(id: string, key: BlockMetadataKey, value: unknown): void;
}

class DefaultBlockMetadataStore implements BlockMetadataStore {
  private readonly entries = new Map<string, BlockMetadata>();

  public get(id: string): Readonly<BlockMetadata> | undefined {
    return this.entries.get(id);
  }

  public set(id: string, key: BlockMetadataKey, value: unknown): void {
    const metadata = this.entries.get(id);
    if (value !== undefined) {
      // The metadata returned by get() is replaced rather than modified, so that the callers holding it aren't
      // affected.
      this.entries.set(id, { ...metadata, [key.name]: value });
    } else if (metadata && key.name in metadata) {
      const rest = { ...metadata };
      delete rest[key.name];
      if (Object.keys(rest).length) {
        this.entries.set(id, rest);
      } else {
        this.entries.delete(id);
      }
    }
  }
}

/**
 * Creates the metadata store used when the integration doesn't provide one.
 *
 * @returns an empty metadata store, kept in memory
 *
 * @since 18.10.0RC1
 * @beta
 */
function createBlockMetadataStore(): BlockMetadataStore {
  return new DefaultBlockMetadataStore();
}

export { createBlockMetadataStore };
export type {
  BlockMetadata,
  BlockMetadataKey,
  BlockMetadataStore,
  PastedMetadataContext,
};

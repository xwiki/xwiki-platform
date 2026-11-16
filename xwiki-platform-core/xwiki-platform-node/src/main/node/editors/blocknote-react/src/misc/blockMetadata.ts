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
 * The data kept about a block (or a link, or a macro call) of the edited content, outside of the content itself: what
 * the BlockNote schema can't hold (e.g. the resource reference of an image), or what must not be shared with the other
 * users editing the same content in realtime (e.g. the output of a macro, rendered for the current user).
 *
 * @since 18.10.0RC1
 * @beta
 */
type BlockMetadata = Record<string, unknown>;

/**
 * Holds the {@link BlockMetadata} of the edited content, by id: the block id for a block, the `id` property for an
 * inline macro, or an id of the integration's choosing otherwise (e.g. for a link). The metadata is changed only
 * through {@link BlockMetadataStore.set}, so that a store can tell what changed (e.g. to synchronize it).
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
   * @param key - the name of the metadata entry
   * @param value - the value of the metadata entry, or undefined to remove the entry (a block left without metadata
   *   entries has no metadata)
   */
  set(id: string, key: string, value: unknown): void;
}

class DefaultBlockMetadataStore implements BlockMetadataStore {
  private readonly metadata = new Map<string, BlockMetadata>();

  public get(id: string): Readonly<BlockMetadata> | undefined {
    return this.metadata.get(id);
  }

  public set(id: string, key: string, value: unknown): void {
    const metadata = this.metadata.get(id);
    if (value !== undefined) {
      this.metadata.set(id, { ...metadata, [key]: value });
    } else if (metadata && key in metadata) {
      const rest = { ...metadata };
      delete rest[key];
      if (Object.keys(rest).length) {
        this.metadata.set(id, rest);
      } else {
        this.metadata.delete(id);
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
export type { BlockMetadata, BlockMetadataStore };

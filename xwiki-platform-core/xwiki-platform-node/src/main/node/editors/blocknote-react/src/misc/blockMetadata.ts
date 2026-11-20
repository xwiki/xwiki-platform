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
import { Map as YMap } from "yjs";

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
 * Identifies an entry of the {@link BlockMetadata}, and tells whether it can be shared with the other users editing the
 * same content in realtime. It is defined by the editing feature that owns the entry, and used by all the code that
 * sets the entry, so that they agree on whether it's shared.
 *
 * @since 18.10.0RC1
 * @beta
 */
type BlockMetadataKey = {
  /**
   * The name of the metadata entry, which is also its name in the metadata returned by {@link BlockMetadataStore.get}.
   */
  name: string;

  /**
   * Whether the metadata entry is shared with the other users editing the same content in realtime. Private by
   * default, because the metadata can hold information that only the current user is allowed to view (e.g. the output
   * of a macro, rendered with the rights of the current user). The value of a shared entry must be serializable to
   * JSON.
   */
  shared?: boolean;
};

/**
 * The shared metadata of the edited content, part of the shared Yjs document of a realtime collaboration session: the
 * shared metadata entries (see {@link BlockMetadataKey.shared}) by block id, then by entry name.
 *
 * @since 18.10.0RC1
 * @beta
 */
type SharedBlockMetadata = YMap<YMap<unknown>>;

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
   * Gets the metadata of a block: its shared and private entries.
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
   * @param key - identifies the metadata entry, and tells whether it's shared
   * @param value - the value of the metadata entry, or undefined to remove the entry (a block left without metadata
   *   entries has no metadata)
   */
  set(id: string, key: BlockMetadataKey, value: unknown): void;

  /**
   * Listens to the changes made with {@link BlockMetadataStore.set}, e.g. to display a block again when its metadata
   * changes. The changes made by the other users editing the same content in realtime are not reported: they reach the
   * shared metadata directly.
   *
   * @param listener - called with the id of the block (or of the link, or of the macro call) whose metadata was set
   * @returns a function that stops listening
   *
   * @since 18.10.0RC1
   * @beta
   */
  subscribe(listener: (id: string) => void): () => void;

  /**
   * Keeps the shared metadata entries in the given shared metadata, so that they are shared with the other users
   * editing the same content in realtime. The private entries stay in this store. Connecting again to the same shared
   * metadata does nothing.
   *
   * @param sharedMetadata - where to keep the shared metadata entries, in the shared Yjs document
   * @param options - `initialize`: whether to copy the shared entries of this store to the given shared metadata,
   *   when this store holds the metadata of the content that initializes the shared document; otherwise the shared
   *   entries of this store are dropped, in favor of those of the given shared metadata, because they are about the
   *   content loaded by this store, which is replaced by the content of the shared document (with different ids)
   * @returns a function that disconnects this store from the given shared metadata, keeping a copy of the shared
   *   entries, so that the metadata is still available after leaving the realtime collaboration session
   */
  connect(
    sharedMetadata: SharedBlockMetadata,
    options: { initialize: boolean },
  ): () => void;
}

/**
 * Where the shared metadata entries are kept: in memory until connected to a realtime collaboration session, in the
 * shared Yjs document after.
 */
interface SharedEntries {
  get(id: string): BlockMetadata | undefined;
  set(id: string, name: string, value: unknown): void;
}

class LocalEntries implements SharedEntries {
  public readonly entries = new Map<string, BlockMetadata>();

  public get(id: string): BlockMetadata | undefined {
    return this.entries.get(id);
  }

  public set(id: string, name: string, value: unknown): void {
    const metadata = this.entries.get(id);
    if (value !== undefined) {
      this.entries.set(id, { ...metadata, [name]: value });
    } else if (metadata && name in metadata) {
      const rest = { ...metadata };
      delete rest[name];
      if (Object.keys(rest).length) {
        this.entries.set(id, rest);
      } else {
        this.entries.delete(id);
      }
    }
  }
}

class YjsEntries implements SharedEntries {
  constructor(public readonly entries: SharedBlockMetadata) {}

  public get(id: string): BlockMetadata | undefined {
    const metadata = this.entries.get(id);
    return metadata?.size ? metadata.toJSON() : undefined;
  }

  public set(id: string, name: string, value: unknown): void {
    let metadata = this.entries.get(id);
    if (value !== undefined) {
      if (!metadata) {
        metadata = new YMap();
        this.entries.set(id, metadata);
      }
      metadata.set(name, value);
    } else {
      // The metadata of a block left without entries is kept (empty), because a user could set an entry on it at the
      // same time, which would be lost if the block metadata were removed.
      metadata?.delete(name);
    }
  }
}

class DefaultBlockMetadataStore implements BlockMetadataStore {
  private readonly privateEntries = new LocalEntries();

  private sharedEntries: SharedEntries = new LocalEntries();

  private readonly listeners = new Set<(id: string) => void>();

  public get(id: string): Readonly<BlockMetadata> | undefined {
    const privateMetadata = this.privateEntries.get(id);
    const sharedMetadata = this.sharedEntries.get(id);
    if (privateMetadata && sharedMetadata) {
      return { ...sharedMetadata, ...privateMetadata };
    }
    return privateMetadata ?? sharedMetadata;
  }

  public set(id: string, key: BlockMetadataKey, value: unknown): void {
    (key.shared ? this.sharedEntries : this.privateEntries).set(
      id,
      key.name,
      value,
    );
    // Copy the listeners, so that a listener can stop listening while being notified.
    [...this.listeners].forEach((listener) => listener(id));
  }

  public subscribe(listener: (id: string) => void): () => void {
    // Wrapped, so that subscribing the same function twice requires stopping twice.
    const wrapper = (id: string) => listener(id);
    this.listeners.add(wrapper);
    return () => {
      this.listeners.delete(wrapper);
    };
  }

  public connect(
    sharedMetadata: SharedBlockMetadata,
    { initialize }: { initialize: boolean },
  ): () => void {
    if (this.sharedEntries instanceof YjsEntries) {
      if (this.sharedEntries.entries === sharedMetadata) {
        return () => this.disconnect(sharedMetadata);
      }
      this.disconnect(this.sharedEntries.entries);
    }

    const yjsEntries = new YjsEntries(sharedMetadata);
    if (initialize) {
      this.copy(this.sharedEntries as LocalEntries, yjsEntries);
    }
    this.sharedEntries = yjsEntries;
    return () => this.disconnect(sharedMetadata);
  }

  private disconnect(sharedMetadata: SharedBlockMetadata): void {
    const entries = this.sharedEntries;
    if (entries instanceof YjsEntries && entries.entries === sharedMetadata) {
      const localEntries = new LocalEntries();
      sharedMetadata.forEach((metadata, id) =>
        metadata.forEach((value, name) => localEntries.set(id, name, value)),
      );
      this.sharedEntries = localEntries;
    }
  }

  private copy(from: LocalEntries, to: YjsEntries): void {
    const copy = () =>
      from.entries.forEach((metadata, id) =>
        Object.entries(metadata).forEach(([name, value]) =>
          to.set(id, name, value),
        ),
      );
    // Share all the metadata at once.
    if (to.entries.doc) {
      to.entries.doc.transact(copy);
    } else {
      copy();
    }
  }
}

/**
 * Creates the metadata store used when the integration doesn't provide one.
 *
 * @returns an empty metadata store, kept in memory until connected to a realtime collaboration session
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
  SharedBlockMetadata,
};

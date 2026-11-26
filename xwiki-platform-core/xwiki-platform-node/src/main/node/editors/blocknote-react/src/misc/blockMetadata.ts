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
 * The data kept about a block (or a link) of the edited content that the BlockNote schema can't hold (e.g. the resource
 * reference of an image), as entries keyed by name. It is part of the edited content (see `getBlockMetadata` and
 * `getLinkMetadata`), so it is shared with the other users editing the same content in realtime, and restored by undo /
 * redo.
 *
 * @since 18.10.0RC1
 * @beta
 */
type BlockMetadata = Record<string, unknown>;

/**
 * What is known about the content holding a pasted metadata entry, to decide whether the entry can be kept.
 *
 * @since 18.10.0
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
 * The name under which the {@link BlockMetadataKey}s are bound in the service container: the keys of the metadata
 * entries that the editing features keep in the edited content (see `setBlockMetadata` and `setLinkMetadata`), so that
 * the editor knows which pasted entries to keep (see {@link BlockMetadataKey.filterPasted}). They are bound either for
 * all the editor instances, or in the service container of an editor instance, for that editor instance only. The
 * pasted entries without a bound key are dropped.
 *
 * @since 18.10.0
 * @beta
 */
const blockMetadataKeyName = "BlockMetadataKey";

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
   * can't be trusted: a value is kept only when it can't do anything that the user doesn't see. The entries are
   * dropped from the pasted content when this is missing, as are the entries without a key bound in the service
   * container (see {@link blockMetadataKeyName}).
   *
   * @param value - the pasted value
   * @param context - what is known about the content holding the pasted value
   * @returns the value to keep, or undefined to drop the entry
   */
  filterPasted?: (value: unknown, context: PastedMetadataContext) => unknown;
};

export { blockMetadataKeyName };
export type { BlockMetadata, BlockMetadataKey, PastedMetadataContext };

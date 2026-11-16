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
 * inline macro, or an id of the integration's choosing otherwise (e.g. for a link).
 *
 * @since 18.10.0RC1
 * @beta
 */
interface BlockMetadataStore {
  /**
   * Gets the metadata of a block, creating it if there's none.
   *
   * @param id - identifies the block (or the link, or the macro call)
   * @param create - create the metadata when there's none
   * @returns the metadata of the specified block, which can be modified in place
   */
  get(id: string, create: true): BlockMetadata;

  /**
   * Gets the metadata of a block.
   *
   * @param id - identifies the block (or the link, or the macro call)
   * @param create - whether to create the metadata when there's none
   * @returns the metadata of the specified block, which can be modified in place, or undefined if there's none and
   *   it's not created
   */
  get(id: string, create?: boolean): BlockMetadata | undefined;
}

class DefaultBlockMetadataStore implements BlockMetadataStore {
  private readonly metadata = new Map<string, BlockMetadata>();

  public get(id: string, create: true): BlockMetadata;
  public get(id: string, create?: boolean): BlockMetadata | undefined;
  public get(id: string, create?: boolean): BlockMetadata | undefined {
    let metadata = this.metadata.get(id);
    if (!metadata && create) {
      metadata = {};
      this.metadata.set(id, metadata);
    }
    return metadata;
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

/**
 * Associates a server-rendered macro output with a macro call. The macro output is kept outside the edited content
 * because it can contain information that only the current user is allowed to view (macros are rendered with the
 * rights of the current user), so it must not be shared with the other users editing the same content in realtime.
 *
 * @param metadata - where to keep the macro output
 * @param id - identifies the macro call: the block id for a block macro, the `id` property for an inline macro or for
 *   a macro nested in the parameters or content of another macro call
 * @param call - the macro call (JSON) that produced the given output
 * @param output - the macro output (an array of BlockNote-shaped nodes)
 *
 * @since 18.10.0RC1
 * @beta
 */
function setMacroOutput(
  metadata: BlockMetadataStore,
  id: string,
  call: string,
  output: unknown[],
): void {
  Object.assign(metadata.get(id, true), {
    macroCall: call,
    macroOutput: output,
  });
}

/**
 * Gets the server-rendered output of a macro call, as long as the macro call wasn't modified since it was rendered.
 *
 * @param metadata - where the macro output is kept
 * @param id - identifies the macro call (see {@link setMacroOutput})
 * @param call - the current macro call (JSON)
 * @returns the output of the specified macro call, or undefined if the macro call doesn't have an output or if it was
 *   modified since its output was rendered
 *
 * @since 18.10.0RC1
 * @beta
 */
function getMacroOutput(
  metadata: BlockMetadataStore | undefined,
  id: string,
  call: string,
): unknown[] | undefined {
  const blockMetadata = id ? metadata?.get(id) : undefined;
  return blockMetadata?.macroCall === call
    ? (blockMetadata.macroOutput as unknown[])
    : undefined;
}

export { createBlockMetadataStore, getMacroOutput, setMacroOutput };
export type { BlockMetadata, BlockMetadataStore };

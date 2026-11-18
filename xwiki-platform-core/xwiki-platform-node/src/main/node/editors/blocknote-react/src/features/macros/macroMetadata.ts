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
import type { BlockMetadataStore } from "../../misc/blockMetadata";

// The metadata entries holding the output of a macro call, and the macro call it was rendered for.
const MACRO_CALL = "macroCall";
const MACRO_OUTPUT = "macroOutput";

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
  metadata.set(id, MACRO_CALL, call);
  metadata.set(id, MACRO_OUTPUT, output);
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
  const macroMetadata = id ? metadata?.get(id) : undefined;
  return macroMetadata?.[MACRO_CALL] === call
    ? (macroMetadata[MACRO_OUTPUT] as unknown[])
    : undefined;
}

export { getMacroOutput, setMacroOutput };

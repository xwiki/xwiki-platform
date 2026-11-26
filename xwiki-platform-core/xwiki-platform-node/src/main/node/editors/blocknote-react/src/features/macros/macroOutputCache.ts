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
import type { MacroFeatureOptions } from "../../editorOptions";

/**
 * The output of the server-rendered macro calls of an editor instance, keyed by id (see `MacroFeatureOptions.outputs`).
 * The output of the macro calls is kept outside the edited content because it is rendered with the rights of the
 * current user (it can contain information that only the current user is allowed to view), so it must not be shared
 * with the other users editing the same content in realtime.
 */
class MacroOutputCache {
  private readonly entries: Map<string, { call: string; output: unknown[] }>;

  /**
   * @param outputs - the initial output of the macro calls, keyed by id
   */
  constructor(outputs: MacroFeatureOptions["outputs"] = {}) {
    this.entries = new Map(Object.entries(outputs));
  }

  /**
   * Gets the output of a macro call, as long as the macro call wasn't modified since it was rendered.
   *
   * @param id - identifies the macro call: the block id for a block macro call, the `id` property for an inline macro
   *   call or for a macro call nested in the parameters or content of another macro call
   * @param call - the current macro call (JSON)
   * @returns the output of the specified macro call, or undefined if the macro call doesn't have an output or if it was
   *   modified since its output was rendered
   */
  public get(id: string, call: string): unknown[] | undefined {
    const entry = id ? this.entries.get(id) : undefined;
    return entry?.call === call ? entry.output : undefined;
  }
}

export { MacroOutputCache };

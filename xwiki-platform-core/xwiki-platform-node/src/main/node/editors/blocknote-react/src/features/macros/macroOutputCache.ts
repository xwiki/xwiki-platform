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
import { useCallback, useSyncExternalStore } from "react";
import type { MacroFeatureOptions } from "../../editorOptions";

/**
 * The output of the server-rendered macro calls of an editor instance, keyed by id (see `MacroFeatureOptions.outputs`).
 * The output of the macro calls is kept outside the edited content because it is rendered with the rights of the
 * current user (it can contain information that only the current user is allowed to view), so it must not be shared
 * with the other users editing the same content in realtime.
 */
class MacroOutputCache {
  private readonly entries: Map<string, { call: string; output: unknown[] }>;

  private readonly listeners = new Set<(id: string) => void>();

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

  /**
   * Caches the output of a macro call, replacing the output previously cached for the same id, and notifies the
   * listeners.
   *
   * @param id - identifies the macro call (see {@link MacroOutputCache.get})
   * @param call - the macro call (JSON) that produced the given output
   * @param output - the output of the macro call; an empty output is kept as well, to remember that the macro call was
   *   rendered
   */
  public set(id: string, call: string, output: unknown[]): void {
    this.entries.set(id, { call, output });
    // Copy the listeners, so that a listener can stop listening while being notified.
    [...this.listeners].forEach((listener) => listener(id));
  }

  /**
   * Listens to the output of the macro calls being cached, e.g. to render a macro call again when its output changes.
   *
   * @param listener - called with the id of the macro call whose output was cached
   * @returns a function that stops listening
   */
  public subscribe(listener: (id: string) => void): () => void {
    // Wrapped, so that subscribing the same function twice requires stopping twice.
    const wrapper = (id: string) => listener(id);
    this.listeners.add(wrapper);
    return () => {
      this.listeners.delete(wrapper);
    };
  }
}

/**
 * Gets the output of a macro call, like {@link MacroOutputCache.get}, and renders the calling component again when a
 * new output is cached for the macro call (e.g. once the macro call is rendered again, after being modified).
 *
 * @param outputs - the output of the macro calls of the editor instance
 * @param id - identifies the macro call (see {@link MacroOutputCache.get})
 * @param call - the current macro call (JSON)
 * @returns the output of the specified macro call, or undefined if the macro call doesn't have an output or if it was
 *   modified since its output was rendered
 */
function useMacroOutput(
  outputs: MacroOutputCache | undefined,
  id: string,
  call: string,
): unknown[] | undefined {
  const subscribe = useCallback(
    (onOutputChange: () => void) =>
      outputs && id
        ? outputs.subscribe((changedId) => {
            if (changedId === id) {
              onOutputChange();
            }
          })
        : () => {},
    [outputs, id],
  );
  // The output is cached as is, so it is the same object as long as no new output is cached for the macro call.
  return useSyncExternalStore(subscribe, () => outputs?.get(id, call));
}

export { MacroOutputCache, useMacroOutput };

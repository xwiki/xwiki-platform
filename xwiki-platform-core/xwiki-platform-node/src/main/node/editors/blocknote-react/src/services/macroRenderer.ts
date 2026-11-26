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
import type { EditorInstance } from "../editorInstance";
import type { MacroFeatureOptions } from "../editorOptions";

/**
 * The name under which the {@link MacroRenderer} is bound in the service container.
 *
 * @since 18.10.0
 * @beta
 */
const macroRendererName = "MacroRenderer";

/**
 * Renders the macro calls of the edited content, server-side. The editor asks for it when the output of some macro
 * calls is missing or out of date, e.g. after a macro call is inserted or modified, locally or by another user editing
 * the same content in realtime. Without it, such macro calls are displayed as placeholders.
 *
 * @since 18.10.0
 * @beta
 */
interface MacroRenderer {
  /**
   * Renders all the macro calls of the edited content, because the output of a macro call can depend on the rest of the
   * content (e.g. the table of contents, or the variables set by a script macro and used by another).
   *
   * @param instance - the editor instance whose content is rendered
   * @returns a promise that resolves with the output of the rendered macro calls, keyed by id, each associated with
   *   the macro call (JSON) as it was when the rendering started (like `MacroFeatureOptions.outputs`), so that the
   *   output of a macro call modified in the meantime is ignored; the promise rejects if the rendering fails
   */
  render(
    instance: EditorInstance,
  ): Promise<NonNullable<MacroFeatureOptions["outputs"]>>;
}

export { macroRendererName };
export type { MacroRenderer };

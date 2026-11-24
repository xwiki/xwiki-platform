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
import { insertMacroInvocation } from "../../misc/macroInvocation";
import type { EditorInstance } from "../../editorInstance";
import type { MentionRenderer, MentionableUser } from "../../services/mentions";

/**
 * Inserts a mention of the given user at the selection, as rendered by the given renderer.
 *
 * @param instance - the editor instance to insert the mention into
 * @param renderer - what renders the user as the macro call standing for the mention
 * @param user - the mentioned user
 */
function insertMention(
  instance: EditorInstance,
  renderer: MentionRenderer,
  user: MentionableUser,
): void {
  insertMacroInvocation(instance.editor, renderer.render(user));
  // A rendering macro is never inline-only, so a call alone in a paragraph is saved as a block macro. Same as the
  // CKEditor mentions: a space after the call keeps the mention inline, and lets the user keep typing after it.
  instance.editor.insertInlineContent(" ", { updateSelection: true });
}

export { insertMention };

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
import type { EditorType } from "../../blocknote";
import type { MentionFactory } from "../../services/mentionFactory";
import type { SuggestedUser } from "@xwiki/platform-user-suggest-api";

/**
 * Only a plain space, so that a mention inserted before a tab or a non-breaking space doesn't replace it with one.
 *
 * @param editor - the editor holding the selection the mention is inserted at
 * @returns whether the selection is already followed by a space
 */
function isFollowedBySpace(editor: EditorType): boolean {
  const { $to } = editor.prosemirrorState.selection;
  return (
    $to.parent.textBetween(
      $to.parentOffset,
      Math.min($to.parentOffset + 1, $to.parent.content.size),
    ) === " "
  );
}

/**
 * Inserts a mention of the given user at the selection, as built by the given factory.
 *
 * @param editor - the editor to insert the mention into, at its selection
 * @param factory - what builds the inline content standing for a mention of the user
 * @param user - the mentioned user
 */
function insertMention(
  editor: EditorType,
  factory: MentionFactory,
  user: SuggestedUser,
): void {
  const mention = factory.createMention(user);
  // A mention is followed by a space, so that the user can keep typing after it. In XWiki syntax it is also what
  // keeps a mention alone in a paragraph from being saved as a block macro, a rendering macro never being inline-only.
  const spaceFollows = isFollowedBySpace(editor);
  editor.transact((tr) => {
    editor.insertInlineContent([mention, " " as const], {
      updateSelection: true,
    });
    if (spaceFollows) {
      // The mention brought its own space, so drop the one that was already there. Keeping ours is what leaves the
      // caret behind the space: skipping it instead would leave it between the mention and that space, where the
      // next typed character would stick to the mention.
      tr.delete(tr.selection.to, tr.selection.to + 1);
    }
  });
}

export { insertMention };

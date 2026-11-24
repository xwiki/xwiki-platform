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
import "./mentionMenu.css";
import { insertMention } from "./mentionInsertion";
import { SuggestionMenu } from "@blocknote/core/extensions";
import { createElement } from "react";
import type { EditorType } from "../../blocknote";
import type { EditorInstance } from "../../editorInstance";
import type { MentionFactory, MentionableUser } from "../../services/mentions";
import type { SuggestionMenuOptions } from "@blocknote/core/extensions";
import type { DefaultReactSuggestionItem } from "@blocknote/react";

/**
 * The character that opens the mention menu when typed.
 */
const MENTION_TRIGGER = "@";

/**
 * Opens the mention menu at the cursor, showing the first suggested users right away (as opposed to typing the
 * trigger, which needs a query).
 *
 * @param editor - the editor to open the menu in
 */
function openMentionMenu(editor: EditorType): void {
  editor.getExtension(SuggestionMenu)?.openSuggestionMenu(MENTION_TRIGGER, {
    // The trigger is inserted, then removed when the menu closes.
    deleteTriggerCharacter: true,
    ignoreQueryLength: true,
  });
}

/**
 * A mention starts a word, so that the trigger typed inside an email address doesn't open the menu. The trigger is
 * matched before it is inserted, which puts the character it follows right before the selection.
 */
const shouldOpenMentionMenu: NonNullable<
  SuggestionMenuOptions["shouldOpen"]
> = (transaction) => {
  const { $from } = transaction.selection;
  const precedingCharacter = $from.parent.textBetween(
    Math.max($from.parentOffset - 1, 0),
    $from.parentOffset,
  );
  return !/[\p{L}\p{N}_]/u.test(precedingCharacter);
};

/**
 * @param instance - the editor instance where the mention is inserted
 * @param factory - what builds the inline content standing for a mention of the picked user
 * @param users - the suggested users
 * @param group - the label of the group of the suggestions, separating them from the other kinds of suggestions
 * @returns the menu items inserting a mention of the given users
 */
function toSuggestions(
  instance: EditorInstance,
  factory: MentionFactory,
  users: MentionableUser[],
  group: string,
): DefaultReactSuggestionItem[] {
  return users.map((user) => ({
    title: user.label,
    subtext: user.hint,
    group,
    icon: user.icon?.url
      ? createElement("img", {
          src: user.icon.url,
          alt: "",
          className: "xwiki-mention-avatar",
        })
      : createElement("span", { className: user.icon?.cssClass }),
    onItemClick: () => insertMention(instance, factory, user),
  }));
}

export {
  MENTION_TRIGGER,
  openMentionMenu,
  shouldOpenMentionMenu,
  toSuggestions,
};

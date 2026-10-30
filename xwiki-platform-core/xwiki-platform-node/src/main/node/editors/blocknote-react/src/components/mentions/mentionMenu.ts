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
import { insertMacroInvocation } from "../../blocknote/utils";
import { createElement } from "react";
import type { EditorType } from "../../blocknote";
import type { DefaultReactSuggestionItem } from "@blocknote/react";
import type { useTranslation } from "react-i18next";

/**
 * A user that can be mentioned in the editor.
 *
 * @since 18.10.0RC1
 * @beta
 */
type MentionableUser = {
  /** The user reference, in the form expected by the mention macro (e.g. "XWiki.Admin") */
  reference: string;
  /** The user name to display */
  label: string;
  /** Additional information about the user, e.g. its email address */
  hint?: string;
  /** The user's avatar, or the icon standing for it when the user has no avatar */
  icon?: { url?: string; cssClass?: string };
};

/**
 * Suggests the users matching the given query, for the mention menu. Provided by the host application because
 * looking up users is backend specific.
 *
 * @since 18.10.0RC1
 * @beta
 */
type UserSuggestFn = (query: string) => Promise<MentionableUser[]>;

/**
 * Parameters used for generating the mention menu
 */
type MentionMenuItemsParams = {
  /** The query to suggest users for */
  query: string;
  /** Instance of the BlockNote editor */
  editor: EditorType;
  /** Source of the user suggestions */
  userSuggest: UserSuggestFn;
  /** `t` property from `react-i18n`'s useTranslation() return value */
  t: ReturnType<typeof useTranslation>["t"];
};

/**
 * Compute the anchor identifying a mention, so that the notification sent for it can link back to it. Same scheme as
 * the CKEditor mentions: the mentioned reference followed by 6 random alphanumeric characters, which makes a
 * collision between two mentions of the same user on the same page unlikely enough.
 */
function getMentionAnchor(reference: string): string {
  const random = Math.random().toString(36).substring(2, 8).padEnd(6, "0");
  return `${reference.replaceAll(/[.:]/g, "-")}-${random}`;
}

/**
 * Suggests the users matching the query, inserting a mention macro for the selected one.
 *
 * @param params - see {@link MentionMenuItemsParams}
 */
async function queryMentionMenuItems({
  query,
  editor,
  userSuggest,
  t,
}: MentionMenuItemsParams): Promise<DefaultReactSuggestionItem[]> {
  const users = await userSuggest(query);

  return users.map((user) => ({
    title: user.label,
    subtext: user.hint,
    group: t("blocknote.mentionMenu.group"),
    icon: user.icon?.url
      ? createElement("img", {
          src: user.icon.url,
          alt: "",
          className: "xwiki-mention-avatar",
        })
      : createElement("span", { className: user.icon?.cssClass }),
    onItemClick: () =>
      insertMacroInvocation(editor, {
        kind: "inline",
        id: "mention",
        params: {
          reference: user.reference,
          style: "FULL_NAME",
          anchor: getMentionAnchor(user.reference),
        },
        body: { type: "none" },
      }),
  }));
}

export { getMentionAnchor, queryMentionMenuItems };
export type { MentionableUser, UserSuggestFn };

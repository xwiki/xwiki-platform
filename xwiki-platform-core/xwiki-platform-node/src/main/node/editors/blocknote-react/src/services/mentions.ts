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

/**
 * The name under which the {@link UserSuggester} is bound in the service container.
 *
 * @since 19.0.0RC1
 * @beta
 */
const userSuggesterName = "UserSuggester";

/**
 * The name under which the {@link MentionInserter} is bound in the service container.
 *
 * @since 19.0.0RC1
 * @beta
 */
const mentionInserterName = "MentionInserter";

/**
 * A user that can be mentioned in the edited content.
 *
 * @since 19.0.0RC1
 * @beta
 */
type MentionableUser = {
  /**
   * The reference of the user, in the form the {@link MentionInserter} writes into the content (e.g. "XWiki.Admin").
   */
  reference: string;

  /**
   * The user name to display.
   */
  label: string;

  /**
   * Additional information telling apart two users with the same name, e.g. an email address.
   */
  hint?: string;

  /**
   * The user avatar, or the icon standing for it when the user has no avatar.
   */
  icon?: { url?: string; cssClass?: string };
};

/**
 * Looks up the users that can be mentioned, which is backend specific. The mention menu is available only when it is
 * bound in the service container (see {@link userSuggesterName}).
 *
 * @since 19.0.0RC1
 * @beta
 */
interface UserSuggester {
  /**
   * Looks up the users that can be mentioned and that match the given query.
   *
   * @param instance - the editor instance the mention is inserted in
   * @param query - the text the suggested users have to match
   * @returns the users matching the given query
   */
  suggestUsers(
    instance: EditorInstance,
    query: string,
  ): Promise<MentionableUser[]>;
}

/**
 * Writes a mention of a user into the edited content. How a mention is represented is up to the backend that has to
 * save and render it, e.g. XWiki writes a call to its mention macro. The mention menu is available only when this is
 * bound in the service container, see {@link mentionInserterName}.
 *
 * @since 19.0.0RC1
 * @beta
 */
interface MentionInserter {
  /**
   * Writes a mention of the given user at the selection of the given editor instance.
   *
   * @param instance - the editor instance to insert the mention into
   * @param user - the mentioned user
   */
  insert(instance: EditorInstance, user: MentionableUser): void;
}

export { mentionInserterName, userSuggesterName };
export type { MentionInserter, MentionableUser, UserSuggester };

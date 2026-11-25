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
import type { InlineContentType } from "../blocknote";
import type { SuggestedUser } from "@xwiki/platform-user-suggest-api";

/**
 * The name under which the {@link MentionFactory} is bound in the service container.
 *
 * @since 19.0.0RC1
 * @beta
 */
const mentionFactoryName = "MentionFactory";

/**
 * Builds the inline content standing for a mention of a user, which the mentions feature then inserts into the edited
 * content. What a mention is depends on the backend that has to save and render it, e.g. XWiki builds a call to its
 * mention macro with `createInlineMacro`. The mention menu is available only when this is bound in the service
 * container, see {@link mentionFactoryName}.
 *
 * @since 19.0.0RC1
 * @beta
 */
interface MentionFactory {
  /**
   * Builds a mention of the given user, without inserting it.
   *
   * @param user - the mentioned user
   * @returns the inline content representing a mention of the given user
   */
  createMention(user: SuggestedUser): InlineContentType;
}

export { mentionFactoryName };
export type { MentionFactory };

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
import { injectable } from "inversify";
import type { UserSuggest } from "./UserSuggest";
import type { MentionableUser } from "@xwiki/platform-editors-blocknote-react";
import type { Container } from "inversify";

// As many suggestions as the CKEditor mentions show.
const SUGGESTION_LIMIT = "6";

/**
 * A user suggestion as returned by the user suggestion template.
 */
type UserSuggestion = {
  value: string;
  label: string;
  hint?: string | null;
  icon?: { url?: string; cssClass?: string };
};

/**
 * Suggests the users of the current wiki, using the same user suggestion template as the CKEditor mentions.
 *
 * @since 18.10.0RC1
 * @beta
 */
@injectable()
class DefaultUserSuggest implements UserSuggest {
  public static bind(container: Container): void {
    container.bind("UserSuggest").to(DefaultUserSuggest).inSingletonScope();
  }

  public async getUsers(query: string): Promise<MentionableUser[]> {
    const parameters = new URLSearchParams({
      xpage: "uorgsuggest",
      media: "json",
      uorg: "user",
      input: query,
      limit: SUGGESTION_LIMIT,
    });
    const response = await fetch(
      XWiki.currentDocument.getURL("get", parameters),
    );
    const suggestions: UserSuggestion[] = await response.json();

    return suggestions.map((suggestion) => ({
      // The suggestion value is the compact serialization of the user reference, which is also what the mention macro
      // expects.
      reference: suggestion.value,
      label: suggestion.label,
      hint: suggestion.hint ?? undefined,
      icon: suggestion.icon,
    }));
  }
}

export { DefaultUserSuggest };

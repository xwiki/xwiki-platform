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

import { userSuggesterName } from "@xwiki/platform-user-suggest-api";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/platform-api";
import type { AuthenticationManagerProvider } from "@xwiki/platform-authentication-api";
import type {
  SuggestedUser,
  UserSuggestOptions,
  UserSuggester,
} from "@xwiki/platform-user-suggest-api";
import type { Container } from "inversify";

// As many suggestions as the CKEditor mentions show.
const DEFAULT_LIMIT = 6;

// The user suggestion template renders against a document, and the wiki of that document is the wiki whose users are
// suggested. The wiki home page is the one page that exists on every wiki, and the one the caller is the most likely
// to be allowed to view.
const SUGGEST_PAGE = "Main/WebHome";

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
 * Suggests the users of an XWiki instance, using the same user suggestion template as the CKEditor mentions and the
 * user pickers, so that they all agree on which users exist and on how they are displayed. The template applies the
 * user scope of the wiki (see the `wiki=auto` mode of `uorgsuggest.vm`), which says whether the local users, the
 * users of the main wiki, or both can be suggested.
 *
 * @since 19.0.0RC1
 * @beta
 */
@injectable()
class XWikiUserSuggester implements UserSuggester {
  constructor(
    @inject("CristalApp") private readonly cristalApp: CristalApp,
    @inject("AuthenticationManagerProvider")
    private readonly authenticationManagerProvider: AuthenticationManagerProvider,
  ) {}

  async suggestUsers(
    query: string,
    options?: UserSuggestOptions,
  ): Promise<SuggestedUser[]> {
    const parameters = new URLSearchParams({
      xpage: "uorgsuggest",
      media: "json",
      uorg: "user",
      // Let the wiki decide which users can be suggested, rather than applying its user scope here.
      wiki: "auto",
      input: query,
      limit: `${options?.limit ?? DEFAULT_LIMIT}`,
    });
    const wikiConfig = this.cristalApp.getWikiConfig();
    const basePageURL = wikiConfig.basePageURL ?? `${wikiConfig.baseURL}/bin/`;
    const response = await fetch(
      `${basePageURL}get/${SUGGEST_PAGE}?${parameters}`,
      { headers: await this.getCredentials() },
    );
    if (!response.ok) {
      throw new Error(`Unexpected response status [${response.status}].`);
    }
    const suggestions: UserSuggestion[] = await response.json();

    return suggestions.map((suggestion) => ({
      // The suggestion value is the user reference serialized relative to the wiki the request was sent to, so a
      // user of another wiki carries its wiki prefix.
      reference: suggestion.value,
      label: suggestion.label,
      hint: suggestion.hint ?? undefined,
      icon: suggestion.icon,
    }));
  }

  private async getCredentials(): Promise<{ Authorization?: string }> {
    const authorizationHeader = await this.authenticationManagerProvider
      .get()
      ?.getAuthorizationHeader();
    return authorizationHeader ? { Authorization: authorizationHeader } : {};
  }
}

/**
 * Registers the XWiki implementation of the user suggester.
 *
 * @since 19.0.0RC1
 * @beta
 */
class ComponentInit {
  constructor(container: Container) {
    container
      .bind<UserSuggester>(userSuggesterName)
      .to(XWikiUserSuggester)
      .inSingletonScope()
      .whenNamed("XWiki");
  }
}

export { ComponentInit };

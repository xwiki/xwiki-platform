/*
 * See the LICENSE file distributed with this work for additional
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

import { UserDetails } from "@xwiki/cristal-authentication-api";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";
import type { AuthenticationManager } from "@xwiki/cristal-authentication-api";

// TODO: find out how to move the type declaration to a separate location.
// eslint-disable-next-line @typescript-eslint/prefer-namespace-keyword, @typescript-eslint/no-namespace
declare module window {
  interface authenticationXWiki {
    login: (oidcUrl: string) => void;

    isLoggedIn(): Promise<boolean>;

    getUserDetails(baseURL: string): Promise<UserDetails>;

    getAuthorizationValue(): Promise<{
      tokenType: string;
      accessToken: string;
    }>;

    logout(): Promise<void>;
  }

  // eslint-disable-next-line import/group-exports
  export const authenticationXWiki: authenticationXWiki;
}

@injectable()
class XWikiAuthenticationManager implements AuthenticationManager {
  constructor(
    @inject<CristalApp>("CristalApp") private cristalApp: CristalApp,
  ) {}

  start(): void {
    window.authenticationXWiki.login(this.cristalApp.getWikiConfig().baseURL);
  }

  callback(): Promise<void> {
    throw new Error("Method not implemented.");
  }

  async getAuthorizationHeader(): Promise<string | undefined> {
    const authenticated = await this.isAuthenticated();
    if (authenticated) {
      const { tokenType, accessToken } =
        await window.authenticationXWiki.getAuthorizationValue();
      return `${tokenType} ${accessToken}`;
    }
  }

  async isAuthenticated(): Promise<boolean> {
    return window.authenticationXWiki.isLoggedIn();
  }

  async getUserDetails(): Promise<UserDetails> {
    return window.authenticationXWiki.getUserDetails(
      this.cristalApp.getWikiConfig().baseURL,
    );
  }

  async logout(): Promise<void> {
    await window.authenticationXWiki.logout();
  }
}

// eslint-disable-next-line import/group-exports
export { XWikiAuthenticationManager };

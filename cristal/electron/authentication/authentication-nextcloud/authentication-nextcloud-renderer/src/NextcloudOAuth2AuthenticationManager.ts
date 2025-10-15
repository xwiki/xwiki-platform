/**
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

import AsyncLock from "async-lock";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";
import type {
  AuthenticationManager,
  UserDetails,
} from "@xwiki/cristal-authentication-api";

interface AuthenticationWindow extends Window {
  authenticationNextcloud: {
    loginOauth2: (
      baseUrl: string,
      authenticationBaseUrl: string,
    ) => Promise<void>;

    isLoggedIn(mode: string): Promise<boolean>;

    getUserDetails(baseUrl: string, mode: string): Promise<UserDetails>;

    getAuthorizationValue(mode: string): Promise<{
      tokenType: string;
      accessToken: string;
    }>;

    logout(mode: string): Promise<void>;

    refreshToken: (
      baseUrl: string,
      authenticationBaseUrl: string,
    ) => Promise<void>;

    getUserId: () => string | undefined;
  };
}
declare const window: AuthenticationWindow;

@injectable()
export class NextcloudOAuth2AuthenticationManager
  implements AuthenticationManager
{
  constructor(@inject("CristalApp") private readonly cristalApp: CristalApp) {}

  private readonly lock = new AsyncLock();

  async start(): Promise<void> {
    const config = this.cristalApp.getWikiConfig();

    await window.authenticationNextcloud.loginOauth2(
      config.baseURL,
      config.authenticationBaseURL!,
    );
  }

  async callback(): Promise<void> {
    throw new Error("Method not implemented.");
  }

  async getAuthorizationHeader(): Promise<string | undefined> {
    const authenticated = await this.isAuthenticated();
    if (authenticated) {
      // We lock this process to avoid sending multiple refresh requests at the
      // same time.
      await this.lock.acquire("refresh", async () => {
        await window.authenticationNextcloud.refreshToken(
          this.cristalApp.getWikiConfig().baseURL,
          this.cristalApp.getWikiConfig().authenticationBaseURL!,
        );
      });
      const { tokenType, accessToken } =
        await window.authenticationNextcloud.getAuthorizationValue("oauth2");
      return `${tokenType} ${accessToken}`;
    }
  }

  async isAuthenticated(): Promise<boolean> {
    return window.authenticationNextcloud.isLoggedIn("oauth2");
  }

  async getUserDetails(): Promise<UserDetails> {
    const config = this.cristalApp.getWikiConfig();
    return window.authenticationNextcloud.getUserDetails(
      config.baseURL,
      "oauth2",
    );
  }

  async logout(): Promise<void> {
    await window.authenticationNextcloud.logout("oauth2");
  }

  getUserId(): string | undefined {
    return window.authenticationNextcloud.getUserId();
  }
}

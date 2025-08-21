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

import { GitHubAuthenticationState } from "@xwiki/cristal-authentication-github-state";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";
import type {
  AuthenticationManager,
  UserDetails,
} from "@xwiki/cristal-authentication-api";

interface AuthenticationWindow extends Window {
  authenticationGitHub: {
    login: (
      baseUrl: string,
      deviceCode: string,
      interval: string,
      expiresIn: string,
    ) => Promise<void>;

    isLoggedIn(): Promise<boolean>;

    getUserDetails(): Promise<UserDetails>;

    getAuthorizationValue(): Promise<{
      tokenType: string;
      accessToken: string;
    }>;

    logout(): Promise<void>;
  };
}
declare const window: AuthenticationWindow;

@injectable()
class GitHubAuthenticationManager implements AuthenticationManager {
  constructor(
    @inject("CristalApp") private readonly cristalApp: CristalApp,
    @inject(GitHubAuthenticationState)
    private readonly authenticationState: GitHubAuthenticationState,
  ) {}

  private readonly localStorageDeviceCode = "authentication.device_code";

  private readonly localStorageUserCode = "authentication.user_code";

  private readonly localStorageExpiresIn = "authentication.expires_in";

  private readonly localStorageInterval = "authentication.interval";

  async start(): Promise<void> {
    const authorizationUrl = new URL(
      `${this.cristalApp.getWikiConfig().authenticationBaseURL}/device-login`,
    );

    const response = await fetch(authorizationUrl);
    const jsonResponse: {
      device_code: string;
      user_code: string;
      expires_in: number;
      interval: number;
    } = await response.json();

    window.localStorage.setItem(
      this.localStorageDeviceCode,
      jsonResponse.device_code,
    );
    window.localStorage.setItem(
      this.localStorageUserCode,
      jsonResponse.user_code,
    );
    window.localStorage.setItem(
      this.localStorageExpiresIn,
      jsonResponse.expires_in.toString(),
    );
    window.localStorage.setItem(
      this.localStorageInterval,
      jsonResponse.interval.toString(),
    );
    this.authenticationState.modalOpened.value = true;
  }

  async callback(): Promise<void> {
    return window.authenticationGitHub.login(
      this.cristalApp.getWikiConfig().authenticationBaseURL!,
      window.localStorage.getItem(this.localStorageDeviceCode)!,
      window.localStorage.getItem(this.localStorageInterval)!,
      window.localStorage.getItem(this.localStorageExpiresIn)!,
    );
  }

  async getAuthorizationHeader(): Promise<string | undefined> {
    const authenticated = await this.isAuthenticated();
    if (authenticated) {
      const { tokenType, accessToken } =
        await window.authenticationGitHub.getAuthorizationValue();
      return `${tokenType} ${accessToken}`;
    }
  }

  async isAuthenticated(): Promise<boolean> {
    return window.authenticationGitHub.isLoggedIn();
  }

  async getUserDetails(): Promise<UserDetails> {
    return window.authenticationGitHub.getUserDetails();
  }

  async logout(): Promise<void> {
    await window.authenticationGitHub.logout();
  }
}

export { GitHubAuthenticationManager };

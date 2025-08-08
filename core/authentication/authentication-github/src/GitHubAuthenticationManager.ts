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

import { AuthenticationManager } from "@xwiki/cristal-authentication-api";
import { GitHubAuthenticationState } from "@xwiki/cristal-authentication-github-state";
import axios from "axios";
import { inject, injectable } from "inversify";
import Cookies from "js-cookie";
import type { CristalApp, WikiConfig } from "@xwiki/cristal-api";
import type { UserDetails } from "@xwiki/cristal-authentication-api";

/**
 * {@link AuthenticationManager} for the GitHub backend.
 *
 * @since 0.15
 */
@injectable()
export class GitHubAuthenticationManager implements AuthenticationManager {
  constructor(
    @inject("CristalApp") private readonly cristalApp: CristalApp,
    @inject(GitHubAuthenticationState)
    private readonly authenticationState: GitHubAuthenticationState,
  ) {}

  private readonly localStorageConfigName = "currentConfigName";

  private readonly localStorageDeviceCode = "authentication.device_code";

  private readonly localStorageUserCode = "authentication.user_code";

  private readonly localStorageExpiresIn = "authentication.expires_in";

  private readonly localStorageInterval = "authentication.interval";

  private readonly tokenTypeCookieKeyPrefix = "tokenType";

  private readonly accessTokenCookieKeyPrefix = "accessToken";

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
      this.localStorageConfigName,
      this.cristalApp.getWikiConfig().name,
    );

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
    const verificationUrl = new URL(
      `${this.cristalApp.getWikiConfig().authenticationBaseURL}/device-verify`,
    );
    verificationUrl.searchParams.set(
      "device_code",
      window.localStorage.getItem(this.localStorageDeviceCode)!,
    );
    const configName = window.localStorage.getItem(
      this.localStorageConfigName,
    )!;

    // This converts the interval polling time and expiration time provided by
    // GitHub to milliseconds.
    // We need to add a little more delay to the interval when polling, just to
    // be sure that we don't go too fast for GitHub (or we might get rate
    // limited).
    const interval: number =
      parseInt(window.localStorage.getItem(this.localStorageInterval)!) * 1000 +
      500;
    let expiresIn: number =
      parseInt(window.localStorage.getItem(this.localStorageExpiresIn)!) * 1000;

    // This interval handles polling the backend for the access token, using
    // the interval time computed earlier.
    // The backend will return an error until the login process has succeeded.
    const intervalId = setInterval(async () => {
      const response = await fetch(verificationUrl);
      const jsonResponse: {
        error?: string;
        access_token?: string;
        token_type?: string;
      } = await response.json();
      if (!jsonResponse.error) {
        const options: Cookies.CookieAttributes = {
          secure: true,
          sameSite: "strict",
        };
        Cookies.set(
          this.getAccessTokenCookieKey(configName),
          jsonResponse.access_token!,
          options,
        );
        Cookies.set(
          this.getTokenTypeCookieKey(configName),
          jsonResponse.token_type!,
          options,
        );
        clearInterval(intervalId);
        // We reload the content on successful login.
        window.location.reload();
      } else if (expiresIn <= 0) {
        clearInterval(intervalId);
      }
      expiresIn -= interval;
    }, interval);
  }

  async getUserDetails(): Promise<UserDetails> {
    const userinfoUrl = "https://api.github.com/user";
    const data = {
      headers: {
        Authorization: await this.getAuthorizationHeader(),
      },
    };
    const {
      data: { login, html_url, name, avatar_url },
    } = await axios.get(userinfoUrl, data);

    return { profile: html_url, username: login, name, avatar: avatar_url };
  }

  async logout(): Promise<void> {
    // Logout is not supported through GitHub's API.
    // The best we can do is to remove the stored cookies.
    Cookies.remove(this.getTokenTypeCookieKey());
    Cookies.remove(this.getAccessTokenCookieKey());
  }

  async getAuthorizationHeader(): Promise<string | undefined> {
    const isAuthenticated = await this.isAuthenticated();
    if (isAuthenticated) {
      return `${this.getTokenType()} ${Cookies.get(this.getAccessTokenCookieKey())}`;
    }
  }

  async isAuthenticated(): Promise<boolean> {
    return (
      this.getTokenType() !== undefined && this.getAccessToken() !== undefined
    );
  }

  private getTokenType() {
    return Cookies.get(this.getTokenTypeCookieKey());
  }

  private getAccessToken() {
    return Cookies.get(this.getAccessTokenCookieKey());
  }

  private getAccessTokenCookieKey(configName?: string) {
    const config = this.resolveConfig(configName);
    return `${this.accessTokenCookieKeyPrefix}-${config?.baseURL}`;
  }

  private getTokenTypeCookieKey(configName?: string) {
    const baseURL = this.resolveConfig(configName).baseURL;
    return `${this.tokenTypeCookieKeyPrefix}-${baseURL}`;
  }

  private resolveConfig(configName?: string): WikiConfig {
    if (configName !== undefined) {
      return this.cristalApp.getAvailableConfigurations().get(configName)!;
    } else {
      return this.cristalApp.getWikiConfig();
    }
  }
}

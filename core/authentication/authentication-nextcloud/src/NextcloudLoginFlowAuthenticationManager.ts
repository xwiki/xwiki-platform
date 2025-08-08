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
import { inject, injectable } from "inversify";
import Cookies from "js-cookie";
import type { CristalApp, WikiConfig } from "@xwiki/cristal-api";
import type { UserDetails } from "@xwiki/cristal-authentication-api";

/**
 * {@link AuthenticationManager} for the Nextcloud backend, using login flow v2.
 *
 * @since 0.20
 */
@injectable()
export class NextcloudLoginFlowAuthenticationManager
  implements AuthenticationManager
{
  constructor(@inject("CristalApp") private readonly cristalApp: CristalApp) {}

  private readonly accessTokenCookieKeyPrefix = "accessToken";

  private readonly userIdCookieKeyPrefix = "userId";

  private readonly cookiesOptions: Cookies.CookieAttributes = {
    secure: true,
    sameSite: "strict",
  };

  async start(): Promise<void> {
    const config = this.cristalApp.getWikiConfig();

    const loginFlowUrl = `${config.baseURL}/index.php/login/v2`;

    const loginFlowResponse = await fetch(loginFlowUrl, { method: "POST" });
    const jsonLoginFlowResponse: {
      poll: { token: string; endpoint: string };
      login: string;
    } = await loginFlowResponse.json();

    window.open(jsonLoginFlowResponse.login, "_blank");

    // This interval handles polling Nextcloud for the access token.
    // It will return a 404 error until the login process has succeeded.
    const intervalId = setInterval(async () => {
      const response = await fetch(jsonLoginFlowResponse.poll.endpoint, {
        method: "POST",
        body: new URLSearchParams({
          token: jsonLoginFlowResponse.poll.token,
        }),
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
      });
      if (response.ok) {
        const jsonResponse: {
          loginName: string;
          appPassword: string;
        } = await response.json();
        Cookies.set(
          this.getAccessTokenCookieKey(config.name),
          btoa(`${jsonResponse.loginName}:${jsonResponse.appPassword}`),
          this.cookiesOptions,
        );
        Cookies.set(
          this.getUserIdCookieKey(config.name),
          jsonResponse.loginName,
          this.cookiesOptions,
        );
        clearInterval(intervalId);
        // We reload the content on successful login.
        window.location.reload();
      }
    }, 3000);
  }

  async callback(): Promise<void> {
    console.warn("No callback registered for login flow.");
  }

  async getUserDetails(): Promise<UserDetails> {
    const config = this.cristalApp.getWikiConfig();

    const userId = this.getUserIdFromCookie();
    return {
      profile: `${config.baseURL}/u/${userId}`,
      username: userId,
      name: userId!, // TODO: Find a way to get the display name (CRISTAL-589).
      avatar: `${config.baseURL}/avatar/${userId}/64`, // We want the 64x64 avatar.
    };
  }

  async logout(): Promise<void> {
    Cookies.remove(this.getAccessTokenCookieKey());
    Cookies.remove(this.getUserIdCookieKey());
  }

  async getAuthorizationHeader(): Promise<string | undefined> {
    const isAuthenticated = await this.isAuthenticated();
    if (isAuthenticated) {
      return `Basic ${Cookies.get(this.getAccessTokenCookieKey())}`;
    }
  }

  async isAuthenticated(): Promise<boolean> {
    return this.getAccessToken() !== undefined;
  }

  getUserId(): string | undefined {
    return this.getUserIdFromCookie();
  }

  private getAccessToken() {
    return Cookies.get(this.getAccessTokenCookieKey());
  }

  private getAccessTokenCookieKey(configName?: string) {
    const config = this.resolveConfig(configName);
    return `${this.accessTokenCookieKeyPrefix}-login-flow-${config?.baseURL}`;
  }

  private getUserIdCookieKey(configName?: string) {
    const config = this.resolveConfig(configName);
    return `${this.userIdCookieKeyPrefix}-login-flow-${config?.baseURL}`;
  }

  private resolveConfig(configName?: string): WikiConfig {
    if (configName !== undefined) {
      return this.cristalApp.getAvailableConfigurations().get(configName)!;
    } else {
      return this.cristalApp.getWikiConfig();
    }
  }

  private getUserIdFromCookie() {
    return Cookies.get(this.getUserIdCookieKey());
  }
}

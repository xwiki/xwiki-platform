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

import { AuthenticationManager } from "@xwiki/cristal-authentication-api";
import axios from "axios";
import { inject, injectable } from "inversify";
import Cookies from "js-cookie";
import type { CristalApp, WikiConfig } from "@xwiki/cristal-api";
import type { UserDetails } from "@xwiki/cristal-authentication-api";
import type { CookieAttributes } from "js-cookie";

/**
 * @since 0.11
 */
@injectable()
export class XWikiAuthenticationManager implements AuthenticationManager {
  constructor(
    @inject<CristalApp>("CristalApp") private cristalApp: CristalApp,
  ) {}

  private readonly localStorageOriginKey = "authentication.origin";

  private readonly localStorageRedirectUriKey = "authentication.redirect_uri";

  private readonly localStorageTokenUrlKey = "authentication.token_url";

  private readonly tokenTypeCookieKeyPrefix = "tokenType";

  private readonly accessTokenCookieKeyPrefix = "accessToken";

  start(): void {
    const config = this.cristalApp.getWikiConfig();
    const authorizationUrl = new URL(`${config.baseURL}/oidc/authorization`);
    authorizationUrl.searchParams.set("response_type", "code");
    // TODO: this client ID should be configurable somewhere.
    authorizationUrl.searchParams.set("client_id", "Cristal");
    const { host, protocol } = window.location;
    const redirectUri = `${protocol}//${host}/callback`;
    window.localStorage.setItem(this.localStorageRedirectUriKey, redirectUri);
    window.localStorage.setItem(
      this.localStorageTokenUrlKey,
      `${config.baseURL}/oidc/token`,
    );
    window.localStorage.setItem(
      this.localStorageOriginKey,
      window.location.toString(),
    );

    // TODO: to be moved as part of a more generic API
    window.localStorage.setItem("currentConfigType", config.getType());
    window.localStorage.setItem("currentConfigName", config.name);

    authorizationUrl.searchParams.set("redirect_uri", redirectUri);
    window.location.href = authorizationUrl.toString();
  }

  async callback(): Promise<void> {
    const href = new URL(window.location.href);
    const code = href.searchParams.get("code");
    const tokenUrl = window.localStorage.getItem(this.localStorageTokenUrlKey)!;
    const data = {
      grant_type: "authorization_code",
      code,
      redirect_uri: window.localStorage.getItem(
        this.localStorageRedirectUriKey,
      ),
      // TODO: access client id from configuration
      client_id: "Cristal",
    };
    const config = {
      // url: tokenUrl,
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
    };
    const { data: tokenData } = await axios.post(tokenUrl, data, config);
    const { access_token: accessToken, token_type: tokenType } = tokenData;
    const cookiesOptions: CookieAttributes = {
      secure: true,
      sameSite: "strict",
    };

    const configName = window.localStorage.getItem("currentConfigName")!;

    Cookies.set(
      this.getAccessTokenCookieKey(configName),
      accessToken,
      cookiesOptions,
    );
    Cookies.set(
      this.getTokenTypeCookieKey(configName),
      tokenType,
      cookiesOptions,
    );
    // Redirect to the page where the user was before starting the log-in.
    window.location.href = window.localStorage.getItem(
      this.localStorageOriginKey,
    )!;
  }

  async getUserDetails(): Promise<UserDetails> {
    const config = this.cristalApp.getWikiConfig();
    const userinfoUrl = `${config.baseURL}/oidc/userinfo`;
    const data = {
      headers: {
        Authorization: await this.getAuthorizationHeader(),
      },
    };
    const {
      data: { profile, name },
    } = await axios.get(userinfoUrl, data);

    return { profile, name };
  }

  async logout(): Promise<void> {
    const config = this.cristalApp.getWikiConfig();
    const logoutUrl = `${config.baseURL}/oidc/userinfo`;
    const data = {
      headers: {
        Authorization: await this.getAuthorizationHeader(),
      },
    };

    await axios.post(logoutUrl, {}, data);
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

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

import { UserDetails } from "@xwiki/cristal-authentication-api";
import { NextcloudAuthenticationState } from "@xwiki/cristal-authentication-nextcloud-state";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";
import type { AuthenticationManager } from "@xwiki/cristal-authentication-api";

interface AuthenticationWindow extends Window {
  authenticationNextcloud: {
    loginBasic: (username: string, password: string) => Promise<void>;

    isLoggedIn(mode: string): Promise<boolean>;

    getUserDetails(baseUrl: string, mode: string): Promise<UserDetails>;

    getAuthorizationValue(mode: string): Promise<{
      tokenType: string;
      accessToken: string;
    }>;

    logout(mode: string): Promise<void>;
  };
}
declare const window: AuthenticationWindow;

@injectable()
export class NextcloudBasicAuthenticationManager
  implements AuthenticationManager
{
  constructor(
    @inject("CristalApp") private readonly cristalApp: CristalApp,
    @inject(NextcloudAuthenticationState)
    private readonly authenticationState: NextcloudAuthenticationState,
  ) {}

  async start(): Promise<void> {
    const config = this.cristalApp.getWikiConfig();
    this.authenticationState.callback = async (): Promise<{
      success: boolean;
      status?: number;
    }> => {
      const token = btoa(
        `${this.authenticationState.username.value}:${this.authenticationState.password.value}`,
      );

      // We try to access the root folder to check if the login was succesful.
      const testLoginResponse = await fetch(config.baseRestURL, {
        method: "GET",
        headers: {
          Authorization: `Basic ${token}`,
        },
      });

      if (testLoginResponse.ok) {
        await window.authenticationNextcloud.loginBasic(
          this.authenticationState.username.value,
          this.authenticationState.password.value,
        );
        return { success: true };
      } else {
        return { success: false, status: testLoginResponse.status };
      }
    };
    this.authenticationState.modalOpened.value = true;
  }

  async callback(): Promise<void> {
    throw new Error("Method not implemented.");
  }

  async getAuthorizationHeader(): Promise<string | undefined> {
    const authenticated = await this.isAuthenticated();
    if (authenticated) {
      const { tokenType, accessToken } =
        await window.authenticationNextcloud.getAuthorizationValue("basic");
      return `${tokenType} ${accessToken}`;
    }
  }

  async isAuthenticated(): Promise<boolean> {
    return window.authenticationNextcloud.isLoggedIn("basic");
  }

  async getUserDetails(): Promise<UserDetails> {
    const config = this.cristalApp.getWikiConfig();
    return window.authenticationNextcloud.getUserDetails(
      config.baseURL,
      "basic",
    );
  }

  async logout(): Promise<void> {
    await window.authenticationNextcloud.logout("basic");
  }

  getUserId(): string | undefined {
    return this.authenticationState.username.value;
  }
}

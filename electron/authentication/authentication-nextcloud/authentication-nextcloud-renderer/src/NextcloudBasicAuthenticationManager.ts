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
import { NextcloudAuthenticationState } from "@xwiki/cristal-authentication-nextcloud-state";
import { inject, injectable } from "inversify";
import type { AuthenticationManager } from "@xwiki/cristal-authentication-api";

interface AuthenticationWindow extends Window {
  authenticationNextcloud: {
    loginBasic: (username: string, password: string) => Promise<void>;

    isLoggedIn(mode: string): Promise<boolean>;

    getUserDetails(mode: string): Promise<UserDetails>;

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
    @inject(NextcloudAuthenticationState)
    private readonly authenticationState: NextcloudAuthenticationState,
  ) {}

  async start(): Promise<void> {
    this.authenticationState.callback.value = async () => {
      await window.authenticationNextcloud.loginBasic(
        this.authenticationState.username.value,
        this.authenticationState.password.value,
      );
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
    return window.authenticationNextcloud.getUserDetails("basic");
  }

  async logout(): Promise<void> {
    await window.authenticationNextcloud.logout("basic");
  }
}

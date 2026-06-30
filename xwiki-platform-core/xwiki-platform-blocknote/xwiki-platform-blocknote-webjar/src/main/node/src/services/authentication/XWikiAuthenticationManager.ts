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
import { Container, inject, injectable } from "inversify";
import type { XWikiMeta } from "../meta/XWikiMeta";
import type {
  AuthenticationManager,
  UserDetails,
} from "@xwiki/platform-authentication-api";

@injectable("Singleton")
export class XWikiAuthenticationManager implements AuthenticationManager {
  private userDetails?: Promise<UserDetails>;

  public static bind(container: Container): void {
    container
      .bind("AuthenticationManager")
      .to(XWikiAuthenticationManager)
      .inSingletonScope()
      .whenNamed("XWiki");
  }

  constructor(@inject("XWikiMeta") private readonly xwikiMeta: XWikiMeta) {}

  public async start(): Promise<void> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public async callback(): Promise<void> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public async getAuthorizationHeader(): Promise<string | undefined> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public async isAuthenticated(): Promise<boolean> {
    return !!this.getUserId();
  }

  public async getUserDetails(): Promise<UserDetails> {
    this.userDetails ??= this.fetchUserDetails();
    return this.userDetails;
  }

  private async fetchUserDetails(): Promise<UserDetails> {
    const currentUserDetailsURL = `${XWiki.contextPath}/rest/wikis/${encodeURIComponent(XWiki.currentWiki)}/user`;
    const response = await fetch(currentUserDetailsURL, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    });

    if (!response.ok) {
      console.warn(
        `Failed to fetch user details: ${response.status} ${response.statusText}`,
      );
      return this.getDefaultUserDetails();
    }

    const userDetails = await response.json();
    return {
      name: userDetails.id,
      username: userDetails.displayName,
      avatar: userDetails.avatarUrl,
      profile: userDetails.xwikiRelativeUrl,
    };
  }

  private getDefaultUserDetails(): UserDetails {
    return {
      name: this.getUserId() ?? "xwiki:XWiki.XWikiGuest",
      username: this.xwikiMeta.userReference?.name ?? "XWikiGuest",
      avatar: `${XWiki.contextPath}/bin/skin/resources/icons/xwiki/noavatar.png`,
    };
  }

  public async logout(): Promise<void> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public getUserId(): string | undefined {
    return this.xwikiMeta.userReference
      ? XWiki.Model.serialize(this.xwikiMeta.userReference)
      : undefined;
  }
}

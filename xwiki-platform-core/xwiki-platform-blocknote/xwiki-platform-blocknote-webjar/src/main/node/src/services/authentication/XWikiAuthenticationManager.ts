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
import { AuthenticationManager, UserDetails } from "@xwiki/cristal-authentication-api";
import { Container, injectable } from "inversify";

@injectable("Singleton")
export class XWikiAuthenticationManager implements AuthenticationManager {
  public static bind(container: Container): void {
    container.bind("AuthenticationManager").to(XWikiAuthenticationManager).inSingletonScope().whenNamed("XWiki");
  }

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
    // TODO
    throw new Error("Method not implemented.");
  }

  public async getUserDetails(): Promise<UserDetails> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public async logout(): Promise<void> {
    // TODO
    throw new Error("Method not implemented.");
  }
}

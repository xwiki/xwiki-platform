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

import { NextcloudBasicAuthenticationManager } from "./NextcloudBasicAuthenticationManager";
import { NextcloudLoginFlowAuthenticationManager } from "./NextcloudLoginFlowAuthenticationManager";
import { NextcloudOAuth2AuthenticationManager } from "./NextcloudOAuth2AuthenticationManager";
import { NextcloudAuthenticationState } from "@xwiki/cristal-authentication-nextcloud-state";
import type { AuthenticationManager } from "@xwiki/cristal-authentication-api";
import type { Container } from "inversify";

/**
 * @beta
 */
export class ComponentInit {
  constructor(container: Container) {
    container
      .bind<AuthenticationManager>("AuthenticationManager")
      .to(NextcloudBasicAuthenticationManager)
      .inSingletonScope()
      .whenNamed("Nextcloud/basic");
    container
      .bind<AuthenticationManager>("AuthenticationManager")
      .to(NextcloudLoginFlowAuthenticationManager)
      .inSingletonScope()
      .whenNamed("Nextcloud/login-flow");
    container
      .bind<AuthenticationManager>("AuthenticationManager")
      .to(NextcloudOAuth2AuthenticationManager)
      .inSingletonScope()
      .whenNamed("Nextcloud/oauth2");
    container
      .bind<NextcloudAuthenticationState>(NextcloudAuthenticationState)
      .toSelf()
      .inSingletonScope();
  }
}

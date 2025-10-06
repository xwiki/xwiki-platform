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

import {
  CristalAppLoader,
  conditionalComponentsList,
  defaultComponentsList,
} from "@xwiki/cristal-lib";
import { ComponentInit as AuthenticationGitHubComponentInit } from "@xwiki/cristal-authentication-github";
import { ComponentInit as AuthenticationNextcloudComponentInit } from "@xwiki/cristal-authentication-nextcloud";
import { ComponentInit as AuthenticationXWikiComponentInit } from "@xwiki/cristal-authentication-xwiki";
import { ComponentInit as BrowserComponentInit } from "@xwiki/cristal-browser-default";
import { ComponentInit as BrowserSettingsComponentInit } from "@xwiki/cristal-settings-browser";
import { loadConfig } from "@xwiki/cristal-configuration-web";
import type { Container } from "inversify";
import type { Configuration } from "@xwiki/cristal-configuration-api";

CristalAppLoader.init(
  [
    "skin",
    "dsvuetify",
    "dsshoelace",
    "macros",
    "storage",
    "extension-menubuttons",
    "sharedworker",
  ],
  loadConfig("/config.json"),
  true,
  false,
  "XWiki",
  async (container: Container) => {
    await defaultComponentsList(container);
    new BrowserComponentInit(container);
    new AuthenticationGitHubComponentInit(container);
    new AuthenticationNextcloudComponentInit(container);
    new AuthenticationXWikiComponentInit(container);
    new BrowserSettingsComponentInit(container);
  },
  async (container: Container, configuration: Configuration) => {
    await conditionalComponentsList(container, configuration);
  },
);

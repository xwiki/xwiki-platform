/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/

import type { Container } from "inversify";
import type {
  Logger,
  Storage,
  WikiConfig,
  WrappingStorage,
} from "@cristal/api";
import { XWikiStorage } from "./xwiki/xwikiStorage";
import { XWikiWikiConfig } from "./xwiki/XWikiWikiConfig";
import { GitHubStorage } from "./github/githubStorage";
import { GitHubWikiConfig } from "./github/GitHubWikiConfig";
import { WrappingOfflineStorage } from "./wrappingOfflineStorage";
import type OfflineStorage from "../api/offlineStorage";
import DexieOfflineStorage from "./dexie/dexieOfflineStorage";

export default class ComponentInit {
  logger: Logger;

  constructor(container: Container) {
    this.logger = container.get<Logger>("Logger");
    this.logger.setModule("storage.components.componentsInit");

    this.logger?.debug("Init Sample Module components begin");
    container
      .bind<OfflineStorage>("OfflineStorage")
      .to(DexieOfflineStorage)
      .inSingletonScope();
    container
      .bind<WrappingStorage>("WrappingStorage")
      .to(WrappingOfflineStorage)
      .inSingletonScope();
    container
      .bind<WikiConfig>("WikiConfig")
      .to(XWikiWikiConfig)
      .whenTargetNamed("XWiki");
    container
      .bind<WikiConfig>("WikiConfig")
      .to(GitHubWikiConfig)
      .whenTargetNamed("GitHub");
    container
      .bind<Storage>("Storage")
      .to(XWikiStorage)
      .whenTargetNamed("XWiki");
    container
      .bind<Storage>("Storage")
      .to(GitHubStorage)
      .whenTargetNamed("GitHub");
    this.logger?.debug("Init Sample Module components end");
  }
}

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

import { CristalLoader } from "@cristal/extension-manager";
import { DefaultCristalApp } from "./DefaultCristalApp";
import { type CristalApp } from "@cristal/api";
import { Container } from "inversify";

export class CristalAppLoader extends CristalLoader {
  public cristal: DefaultCristalApp;

  public constructor(extensionList: Array<string>) {
    super(extensionList);
  }

  public loadApp<T>(
    config: { [s: string]: T },
    isElectron: boolean,
    configName: string,
  ) {
    const configMap = new Map<string, T>(Object.entries(config));
    this.cristal.setAvailableConfigurations(configMap);

    if (isElectron) {
      const localConfigName = window.localStorage.getItem("currentApp");
      if (localConfigName != null) configName = localConfigName;
    } else {
      configName = this.getConfigFromPathName(window.location.pathname);
    }

    const wikiConfig = this.cristal
      .getAvailableConfigurations()
      .get(configName);
    if (wikiConfig == null) {
      if (!isElectron) window.location.href = "/XWiki/#";
      this.logger?.error("Could not start cristal module");
      return;
    }

    this.cristal.setWikiConfig(wikiConfig);
    // setting design system
    if (wikiConfig.designSystem != "")
      this.cristal.skinManager.setDesignSystem(wikiConfig.designSystem);

    // Make sure we have initialized this config
    // This is necessary for offline mode
    wikiConfig.initialize();

    this.logger.debug("Wiki Config is:", wikiConfig);

    if (this.cristal != null) this.cristal.run();
    else this.logger?.error("Could not start cristal module");
  }

  public async launchApp(
    forceStaticMode: boolean,
    configPath: string,
    isElectron: boolean,
    configName: string,
    additionalComponents?: (container: Container) => void,
  ) {
    let staticMode = forceStaticMode;
    if (import.meta.env.MODE == "development" || staticMode) {
      staticMode = true;
      const StaticBuild = await import("../staticBuild");
      StaticBuild.StaticBuild.init(
        this.container,
        staticMode,
        additionalComponents,
      );
    }

    await this.loadExtensions(true, staticMode);
    this.container
      .bind<CristalApp>("CristalApp")
      .to(DefaultCristalApp)
      .inSingletonScope();
    this.cristal = this.container.get<CristalApp>(
      "CristalApp",
    ) as DefaultCristalApp;
    this.cristal.isElectron = isElectron;
    this.cristal.setContainer(this.container);

    const response = await fetch(configPath);
    const config = await response.json();
    this.loadApp(config, isElectron, configName);
  }

  public static init(
    extensionList: Array<string>,
    configPage: string,
    staticBuild: boolean,
    isElectron: boolean,
    configName: string,
    additionalComponents: (container: Container) => void,
  ) {
    const cristalLoader = new CristalAppLoader(extensionList);
    cristalLoader.initializeContainer();
    cristalLoader.launchApp(
      staticBuild,
      configPage,
      isElectron,
      configName,
      additionalComponents,
    );
  }
}

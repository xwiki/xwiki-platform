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

import ComponentInit from "./componentsInit";
import { DefaultLogger, DefaultLoggerConfig } from "@xwiki/cristal-api";
import { Container } from "inversify";
import type { ExtensionManager } from "../api/extensionManager";
import type { Logger, LoggerConfig } from "@xwiki/cristal-api";

export class CristalLoader {
  // @ts-expect-error logger can be temporarily undefined during class
  // initialization
  public logger: Logger;
  // @ts-expect-error container can be temporarily undefined during class
  // initialization
  public container: Container;
  public extensionList: Array<string>;

  public constructor(extensionList: Array<string>) {
    this.extensionList = extensionList;
  }
  public getLogger(module: string): Logger {
    let logger = this.container.get<Logger>("Logger");
    if (!logger) {
      logger = new DefaultLogger();
    }
    logger.setModule(module);
    return logger;
  }

  public async loadExtensionManager(
    staticExtensionManager: boolean,
  ): Promise<void> {
    const extensionName = "extension-manager";
    const importPath = "/apps/" + extensionName + "/main.bundle.dev.js";
    try {
      if (staticExtensionManager) {
        // Static version with extension manager bundled in app package
        this.logger?.debug("Loading static extension Manager");
        new ComponentInit(this.container);
      } else {
        // Dynamic version loading extension manager as an external library
        this.logger?.debug(
          "Dynamic extension Manager. Ready to import ",
          importPath,
        );
        const componentInitModule = await import(/* @vite-ignore */ importPath);
        if (componentInitModule != null) {
          this.logger?.debug(
            "Found components Init for ",
            extensionName,
            componentInitModule,
          );
          const ComponentInit = componentInitModule.ComponentInit;
          new ComponentInit(this.container);
          this.logger?.debug("Succesfull components Init for ", extensionName);
        } else {
          this.logger?.debug("No components to load for ", extensionName);
        }
      }
    } catch (e) {
      this.logger?.error(
        "Exception while loading extension Manager from ",
        importPath,
        e,
      );
    }
  }

  public async loadExtensions(
    staticExtensionManager: boolean,
    staticBuild: boolean,
  ): Promise<ExtensionManager | null> {
    this.logger?.debug("Start app");
    await this.loadExtensionManager(staticExtensionManager);

    const extensionManager =
      this.container.get<ExtensionManager>("ExtensionManager");
    this.logger?.debug("Extension Manager is ", extensionManager);

    if (extensionManager != null) {
      if (!staticBuild) {
        extensionManager.setRemoteExtensionsConfig(
          "/apps/",
          "main.bundle.dev.js",
        );
        for (const extName in this.extensionList) {
          extensionManager.addRemoteExtension(extName, "", true);
        }

        // loading all extensions from other modules
        await extensionManager.loadExtensions(this.container);
      }
      this.logger?.debug("After extensions loading");
    } else {
      this.logger?.debug("Could not start extension Manager");
    }
    return extensionManager;
  }

  public initializeContainer(): void {
    this.container = new Container();

    // Register main CristalApp component
    this.container
      .bind<LoggerConfig>("LoggerConfig")
      .to(DefaultLoggerConfig)
      .inSingletonScope();
    this.container.bind<Logger>("Logger").to(DefaultLogger);

    const loggerConfig = this.container.get<LoggerConfig>("LoggerConfig");
    if (loggerConfig) {
      loggerConfig.setDefaultLevel("debug");
    }
    this.logger = this.getLogger("index.ts");
    this.logger?.info("Logging initialized");
  }

  public getConfigFromPathName(pathname: string): string {
    let configName: string = "";

    if (pathname.startsWith("/")) {
      const i1 = pathname.indexOf("/", 1);
      if (i1 != -1) {
        configName = pathname.substring(1, i1);
      }
    }

    if (configName == null || configName == "") {
      configName = "XWiki";
    }

    return configName;
  }
}

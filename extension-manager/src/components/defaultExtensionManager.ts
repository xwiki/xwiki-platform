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

import "reflect-metadata";
import { DefaultComponent } from "./defaultComponent";
import { DefaultExtensionConfig } from "./defaultExtensionConfig";
import { inject, injectable } from "inversify";
import type { ExtensionConfig } from "../api/extensionConfig";
import type { ExtensionManager } from "../api/extensionManager";
import type { Logger } from "@xwiki/cristal-api";
import type { Container } from "inversify";

@injectable()
export class DefaultExtensionManager
  extends DefaultComponent
  implements ExtensionManager
{
  protected remoteExtensions: Map<string, ExtensionConfig> = new Map<
    string,
    ExtensionConfig
  >();
  protected remoteExtensionDefaultURL = "/apps/";
  protected remoteExtensionDefaultEntryFile = "main.bundle.js";

  public logger: Logger;

  constructor(@inject<Logger>("Logger") logger: Logger) {
    super();
    this.logger = logger;
    this.logger.setModule(
      "extensionmanager.components.defaultExtensionManager",
    );
  }

  addRemoteExtension(
    name: string,
    customEntryFile: string | null,
    staticModule: boolean | false,
  ): void {
    this.remoteExtensions.set(
      name,
      new DefaultExtensionConfig(name, customEntryFile, staticModule),
    );
  }

  setRemoteExtensionsConfig(
    remoteExtensionDefaultURL: string,
    remoteExtensionDefaultEntryFile: string,
  ): void {
    this.remoteExtensionDefaultURL = remoteExtensionDefaultURL;
    this.remoteExtensionDefaultEntryFile = remoteExtensionDefaultEntryFile;
  }

  async loadExtension(
    extensionName: string,
    container: Container,
  ): Promise<void> {
    try {
      const componentConfig = this.remoteExtensions.get(extensionName);
      const entryFile =
        componentConfig && componentConfig.entryFile
          ? componentConfig.entryFile
          : this.remoteExtensionDefaultEntryFile;
      const importPath =
        this.remoteExtensionDefaultURL + extensionName + "/" + entryFile;
      const cssImportPath =
        this.remoteExtensionDefaultURL + extensionName + "/style.css";

      this.logger?.debug("Ready to import ", importPath);
      // css loading
      try {
        const cssModule = await import(/* @vite-ignore */ cssImportPath, {
          assert: { type: "text/css" },
        });
        document.adoptedStyleSheets = [cssModule.default];
      } catch {
        // TODO handle exception or comment why can safely be ignored
      }
      // js loading
      const componentInitModule = await import(/* @vite-ignore */ importPath);
      if (componentInitModule != null) {
        this.logger?.debug(
          "Found components Init for ",
          extensionName,
          componentInitModule,
        );
        const ComponentInit = componentInitModule.ComponentInit;
        new ComponentInit(container);
        this.logger?.debug("Succesfull components Init for ", extensionName);
      } else {
        this.logger?.debug("No components to load for ", extensionName);
      }
      return;
    } catch (e) {
      this.logger?.error(
        "Exception while loading components from ",
        extensionName,
        e,
      );
    }
  }

  async loadExtensions(container: Container): Promise<void> {
    // loading component from App module
    for (const key of this.remoteExtensions.keys()) {
      await this.loadExtension(key, container);
    }
    return;
  }
}

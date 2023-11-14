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

import { Container } from "inversify";
import { App, Component, Ref } from "vue";
import { WikiConfig } from "./WikiConfig";
import { Router } from "vue-router";
import { Logger } from "./logger";
import { LoggerConfig } from "./loggerConfig";
import { SkinManager } from "./skinManager";

export interface CristalApp {

    getApp() : App;
    getRouter() : Router;
    getContainer() : Container;
    setContainer(container : Container) : void;
 
    getWikiConfig() : WikiConfig;
    setWikiConfig(wikiConfig : WikiConfig) : void;

    getSkinManager() : SkinManager;

    switchConfig(configName : string) : void;

    setAvailableConfigurations(config : Map<string, any>) : void;
    getAvailableConfigurations() : Map<string, WikiConfig>;

    run() : Promise<void>;

    getUIXTemplates(extensionPoint : string) : Array<Component>;
    getMenuEntries() : Array<string>;

    getCurrentPage(): string;
    setCurrentPage(page : string): void;
    setContentRef(ref : Ref) : void; 
    loadPageFromURL(url : string) : Promise<void>;
    loadPage() : Promise<void>;

    getLogger(module : string) : Logger;
    getLoggerConfig() : LoggerConfig;

    renderContent(source : string, sourceSyntax : string, targetSyntax : string, wikiConfig : WikiConfig) : string;
}

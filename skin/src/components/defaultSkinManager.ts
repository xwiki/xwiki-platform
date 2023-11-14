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

import View from "../vue/view.vue";
import Header from "../vue/header.vue";
import Config from "../vue/config.vue";
import Sidebar from "../vue/sidebar.vue";
import Footer from "../vue/footer.vue";
import Content from "../vue/content.vue";
import Panel from "../vue/panel.vue";
import Main from "../vue/main.vue";
import Edit from "../vue/edit.vue";
import Blog from "../vue/blog.vue";
import Movie from "../vue/movie.vue";

import PerfEmpty from "../vue/perf/perfEmpty.vue";
import PerfX from "../vue/perf/perfX.vue";
import PerfDirectVuetify from "../vue/perf/perfDirectVuetify.vue";
import PerfDirectDSFR from "../vue/perf/perfDirectDSFR.vue";
import PerfDirectSL from "../vue/perf/perfDirectSL.vue";


import { App, Component } from "vue";
import { DesignSystemLoader, SkinManager } from '@cristal/api';
import { Container, injectable } from "inversify";
import "reflect-metadata";

@injectable()
export class DefaultSkinManager implements SkinManager {
    public static DEFAULT_DESIGN_SYSTEM = "";
    public static cname = "cristal.skin.manager";
    public static hint = "default";
    public static priority = 1000;
    public static singleton = true;
    public templates : Map<string, Component>;
    public designSystem : string = "";

    constructor() {
        this.templates = new Map<string, Component>;
        this.templates.set("view", View);
        this.templates.set("header", Header);
        this.templates.set("config", Config);
        this.templates.set("sidebar", Sidebar);
        this.templates.set("content", Content);
        this.templates.set("footer", Footer);
        this.templates.set("panel", Panel);
        this.templates.set("main", Main);
        this.templates.set("edit", Edit);
        this.templates.set("blog", Blog);
        this.templates.set("movie", Movie);

        // performance measurements
        this.templates.set("perfX", PerfX);
        this.templates.set("perfempty", PerfEmpty);
        this.templates.set("perfvuetify", PerfDirectVuetify);
        this.templates.set("perfdsfr", PerfDirectDSFR);
        this.templates.set("perfsl", PerfDirectSL);
    }

    public setDesignSystem(designSystem : string) {
        this.designSystem = designSystem;
    }

    public getDesignSystem() {
        return this.designSystem;
    }

    public getTemplate(name : string) : Component | null {
        return this.getDefaultTemplate(name);
    }

    public getDefaultTemplate(name : string) : Component | null {
        try {
            const template = this.templates.get(name) as Object;
            return template;
        } catch (e) {
            console.error("Error loading default template ", name, e)
            return null;
        }
    }

    public loadDesignSystem(app : App, container : Container) {
        let designSystemLoader : DesignSystemLoader | null = null;

        try {
            designSystemLoader = container.getNamed<DesignSystemLoader>("DesignSystemLoader", this.designSystem);
        } catch (e) {
            console.error("Exception while loading design system ", this.designSystem);
            if (DefaultSkinManager.DEFAULT_DESIGN_SYSTEM!="")
               designSystemLoader = container.getNamed<DesignSystemLoader>("DesignSystemLoader", DefaultSkinManager.DEFAULT_DESIGN_SYSTEM);
        }
        if (designSystemLoader)
            designSystemLoader.loadDesignSystem(app);
        else
            console.error("Cannot initialize design system");
    }
} 



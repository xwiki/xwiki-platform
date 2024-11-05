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

import View from "../vue/c-view.vue";
import Config from "../vue/c-config.vue";
import Sidebar from "../vue/c-sidebar.vue";
import Footer from "../vue/c-footer.vue";
import Content from "../vue/c-content.vue";
import Panel from "../vue/c-panel.vue";
import Main from "../vue/c-main.vue";
import Edit from "../vue/c-edit.vue";
import Blog from "../vue/c-blog.vue";
import Movie from "../vue/c-movie.vue";

import { injectable } from "inversify";
import type { App, Component } from "vue";
import type { DesignSystemLoader, SkinManager } from "@xwiki/cristal-api";
import type { Container } from "inversify";
import "reflect-metadata";

@injectable()
export class DefaultSkinManager implements SkinManager {
  public static DEFAULT_DESIGN_SYSTEM = "";
  public static cname = "cristal.skin.manager";
  public static hint = "default";
  public static priority = 1000;
  public static singleton = true;
  public templates: Map<string, Component>;
  public designSystem: string = "";

  constructor() {
    this.templates = new Map<string, Component>();
    this.templates.set("view", View);
    this.templates.set("config", Config);
    this.templates.set("sidebar", Sidebar);
    this.templates.set("content", Content);
    this.templates.set("footer", Footer);
    this.templates.set("panel", Panel);
    this.templates.set("main", Main);
    this.templates.set("edit", Edit);
    this.templates.set("blog", Blog);
    this.templates.set("movie", Movie);
  }

  public setDesignSystem(designSystem: string): void {
    this.designSystem = designSystem;
  }

  public getDesignSystem(): string {
    return this.designSystem;
  }

  public getTemplate(name: string): Component | null {
    return this.getDefaultTemplate(name);
  }

  public getDefaultTemplate(name: string): Component | null {
    try {
      return this.templates.get(name) as object;
    } catch (e) {
      console.error("Error loading default template ", name, e);
      return null;
    }
  }

  public loadDesignSystem(app: App, container: Container): void {
    let designSystemLoader: DesignSystemLoader | null = null;

    try {
      designSystemLoader = container.getNamed<DesignSystemLoader>(
        "DesignSystemLoader",
        this.designSystem,
      );
    } catch {
      console.error(
        "Exception while loading design system ",
        this.designSystem,
      );
      if (DefaultSkinManager.DEFAULT_DESIGN_SYSTEM != "") {
        designSystemLoader = container.getNamed<DesignSystemLoader>(
          "DesignSystemLoader",
          DefaultSkinManager.DEFAULT_DESIGN_SYSTEM,
        );
      }
    }
    if (designSystemLoader) {
      designSystemLoader.loadDesignSystem(app);
    } else {
      console.error("Cannot initialize design system");
    }
  }
}

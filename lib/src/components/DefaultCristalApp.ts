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

// reflect-metadata is placed at the top because it needs to be imported
// before any use of an inversify annotation.
import "reflect-metadata";
import Index from "../c-index.vue";
import { DefaultLogger, DefaultPageData } from "@xwiki/cristal-api";
import { name as documentServiceName } from "@xwiki/cristal-document-api";
import { inject, injectable, multiInject } from "inversify";
import { createPinia } from "pinia";
import { createApp } from "vue";
import { createI18n } from "vue-i18n";
import {
  RouteRecordRaw,
  Router,
  createRouter,
  createWebHashHistory,
} from "vue-router";
import type {
  CristalApp,
  Logger,
  LoggerConfig,
  PageData,
  SkinManager,
  WikiConfig,
} from "@xwiki/cristal-api";
import type { BrowserApi } from "@xwiki/cristal-browser-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import "@mdi/font/css/materialdesignicons.css";
import type { ExtensionManager } from "@xwiki/cristal-extension-manager";
import type { MenuEntry } from "@xwiki/cristal-extension-menubuttons";
import type { Renderer } from "@xwiki/cristal-rendering";
import type {
  UIXTemplateProvider,
  VueTemplateProvider,
} from "@xwiki/cristal-skin";
import type { Container } from "inversify";
import type { App, Component, Ref } from "vue";

@injectable()
export class DefaultCristalApp implements CristalApp {
  public skinManager: SkinManager;
  public extensionManager: ExtensionManager;
  // @ts-expect-error app is temporarily undefined during class initialization
  public app: App;
  public page: PageData;
  public mode: string;
  public browserApi: BrowserApi;
  // @ts-expect-error currentContentRef is temporarily undefined during class
  // initialization
  public currentContentRef: Ref;
  // @ts-expect-error container is temporarily undefined during class
  // initialization
  public container: Container;
  public vueTemplateProviders: VueTemplateProvider[];
  // @ts-expect-error wikiConfig is temporarily undefined during class
  // initialization
  public wikiConfig: WikiConfig;
  // @ts-expect-error router is temporarily undefined during class
  // initialization
  public router: Router;
  public logger: Logger;
  // @ts-expect-error isElectron is temporarily undefined during class
  // initialization
  public isElectron: boolean;
  public availableConfigurations: Map<string, WikiConfig>;

  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  constructor(
    @inject("ExtensionManager") extensionManager: ExtensionManager,
    @inject("SkinManager") skinManager: SkinManager,
    @multiInject("VueTemplateProvider")
    vueTemplateProviders: VueTemplateProvider[],
    @inject("Logger") logger: Logger,
    @inject("BrowserApi") browserApi: BrowserApi,
  ) {
    // this.extensionManager = new DefaultExtensionManager();
    this.availableConfigurations = new Map<string, WikiConfig>();
    this.extensionManager = extensionManager;
    this.skinManager = skinManager;
    this.vueTemplateProviders = vueTemplateProviders;
    this.page = new DefaultPageData();
    this.mode = "view";
    this.browserApi = browserApi;
    this.logger = logger;
    this.logger.setModule("app.components.DefaultWikiApp");
    this.logger?.debug("Skin manager: ", skinManager);
    this.logger?.debug("Vue template providers: ", vueTemplateProviders);
  }

  setContainer(container: Container): void {
    this.container = container;
  }

  getContainer(): Container {
    return this.container;
  }

  getCurrentPage(): string {
    return this.page.name || this.wikiConfig.defaultPageName();
  }

  setCurrentPage(newPage: string, mode: string = "view"): void {
    this.router.push({
      name: mode,
      params: { page: newPage },
    });
  }

  handlePopState(name: string, revision?: string): void {
    this.logger?.debug("In handlePopState ", name);
    this.page.name = name || this.getWikiConfig().defaultPageName();
    this.page.version = revision;
    this.page.source = "";
    this.page.html = "";
    this.loadPage();
  }

  setWikiConfig(wikiConfig: WikiConfig): void {
    this.wikiConfig = wikiConfig;
  }

  getWikiConfig(): WikiConfig {
    return this.wikiConfig;
  }

  getSkinManager(): SkinManager {
    return this.skinManager;
  }

  switchConfig(configName: string): void {
    this.logger.debug("Switching config to", configName);
    const wikiConfig = this.availableConfigurations.get(configName);
    if (wikiConfig) {
      this.setWikiConfig(wikiConfig);
      if (wikiConfig.designSystem != "") {
        this.skinManager.setDesignSystem(wikiConfig.designSystem);
      }
      this.browserApi.switchLocation(wikiConfig);
    }
  }

  // TODO remplace any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  setAvailableConfigurations(config: Map<string, any>): void {
    console.log(config);
    // TODO remplace any
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    config.forEach((wikiConfigObject: any, key: string) => {
      const configType = wikiConfigObject?.configType;

      if (wikiConfigObject) {
        const wikiConfig = this.container.getNamed<WikiConfig>(
          "WikiConfig",
          configType,
        );
        wikiConfig.setConfigFromObject(wikiConfigObject);
        this.availableConfigurations.set(key, wikiConfig);
      }
    });
  }

  getAvailableConfigurations(): Map<string, WikiConfig> {
    return this.availableConfigurations;
  }

  getLogger(module: string): Logger {
    let logger = this.container.get<Logger>("Logger");
    if (!logger) {
      logger = new DefaultLogger();
    }
    logger.setModule(module);
    return logger;
  }

  getLoggerConfig(): LoggerConfig {
    return this.container.get<LoggerConfig>("LoggerConfig");
  }

  async renderContent(
    source: string,
    sourceSyntax: string,
    targetSyntax: string,
    wikiConfig: WikiConfig,
  ): Promise<string> {
    // Protection from rendering errors
    if (source == undefined) {
      return "";
    }

    this.logger.debug("Loading rendering module");
    try {
      const renderer = this.container.get<Renderer>("Renderer");
      return renderer.convert(source, sourceSyntax, targetSyntax, wikiConfig);
    } catch {
      this.logger.error("Could not find a rendering module");
      return source;
    }
  }

  async preloadConverters(): Promise<void> {
    this.logger.debug("Loading rendering module");
    try {
      const renderer = this.container.get<Renderer>("Renderer");
      await renderer.preloadConverters();
    } catch {
      this.logger.error("Could not find a rendering module");
    }
  }

  /**
   * Load the current page content.
   * @param options - an optional set of parameters. When a requeue key is
   * provided, it is used to determine if an asynchronous update of the page
   * content is allowed. When undefinied, default to true.
   * @since 0.8
   */
  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  async loadPage(options?: { requeue: boolean }): Promise<void> {
    try {
      this.logger?.debug("Loading page", this.page.name);
      const documentService =
        this.getContainer().get<DocumentService>(documentServiceName);
      documentService.setCurrentDocument(this.page.name, this.page.version);
      if (this.getWikiConfig().isSupported("jsonld")) {
        const pageData = await this.getWikiConfig().storage.getPageContent(
          this.page.name,
          "jsonld",
          this.page.version,
          options?.requeue,
        );

        if (!pageData) {
          this.logger.error(
            "Could not find page data",
            this.page.name,
            "with syntax",
            "jsonld",
          );
          return;
        }
        this.page.document = pageData.document;
        this.page.source = pageData.document.get("text");
        if (pageData.html == "") {
          this.page.html = await this.renderContent(
            this.page.source,
            pageData.syntax,
            "html",
            this.getWikiConfig(),
          );
          // Update JSON-LD format also
          this.page.document.set("html", this.page.html);
        } else {
          this.page.html = pageData.html;
        }

        if (this.currentContentRef != null) {
          this.currentContentRef.value = this.page;

          this.logger?.debug("Page content ", this.page.source);
          this.logger?.debug("Page loaded ", this.page.name);
        } else {
          console.error("Could not set content on vue page view component");
        }
      } else {
        const pageData = await this.getWikiConfig().storage.getPageContent(
          this.page.name,
          "html",
          this.page.version,
        );
        if (!pageData) {
          this.logger.error(
            "Could not find page data",
            this.page.name,
            "with syntax",
            "html",
          );
          return;
        }

        this.page.source = pageData.source;
        this.page.html = pageData.html;
        if (pageData.html == "") {
          this.page.html = await this.renderContent(
            this.page.source,
            pageData.syntax,
            "html",
            this.getWikiConfig(),
          );
          // Update JSON-LD format also
          this.page.document.set("html", this.page.html);
        } else {
          this.page.html = pageData.html;
        }
        if (this.currentContentRef != null) {
          this.currentContentRef.value = this.page;
          this.logger?.debug("Page content ", this.page.html);
          this.logger?.debug("Page loaded ", this.page.name);
        } else {
          console.error("Could not set content on vue page view component");
        }
      }
    } catch (e) {
      console.error("Failed to load page ", this.page.name, e);
    }
  }

  /**
   * @deprecated since 0.12
   */
  async loadPageFromURL(url: string): Promise<void> {
    this.logger?.debug("Trying to load", url);
    const page = this.getWikiConfig().storage.getPageFromViewURL(url);
    if (page != null) {
      this.logger?.debug("The link is evaluated as being page ", page);
      this.setCurrentPage(page);
    } else {
      this.logger?.debug("The link is evaluated as an external link");
      window.open(url, "_blank");
    }
  }

  getCurrentContent(): string {
    return this.page.html;
  }

  getCurrentSource(): string {
    return this.page.source;
  }

  getCurrentSyntax(): string {
    return this.page.document.getSource().encodingFormat;
  }

  setContentRef(ref: Ref): void {
    this.currentContentRef = ref;
    this.logger?.debug("Received ref from VUE ", ref);
  }

  getCurrentWiki(): string {
    return this.getWikiConfig().name;
  }

  getApp(): App {
    return this.app;
  }

  getRouter(): Router {
    return this.router;
  }

  getPageFromHash(hash: string): string | null {
    // TODO: this method must be deprecated, parsing the url like this is not ok
    // and this should be replace by the user of vue router APIs.
    let page = null;
    const i1 = hash.indexOf("/");
    const i2 = hash.indexOf("/", i1 + 1);

    if (i1 > 0) {
      if (i2 == -1) {
        page = hash.substring(i1 + 1);
      } else {
        page = hash.substring(i1 + 1, i2);
      }
    }
    this.logger?.debug("Page from hash is : ", page);
    return page;
  }

  private getPageFromRouter(): string {
    let page = this.getPageFromHash(location.hash);
    if (page == null) {
      page = this.wikiConfig.homePage;
    }
    this.logger?.debug("Page is:", page);
    return page;
  }

  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  async run(): Promise<void> {
    this.logger?.debug("Before vue");

    // initializing the page data
    const initialPage = this.getPageFromRouter();
    this.logger?.debug("Initial page is ", initialPage);

    this.page = new DefaultPageData("_jsonld", initialPage, "initial content");

    const routes = [
      {
        path: "/",
        component: this.skinManager.getTemplate("content"),
      } as RouteRecordRaw,
      {
        path: "/:page/view/:revision?",
        name: "view",
        component: this.skinManager.getTemplate("content"),
      } as RouteRecordRaw,
      {
        path: "/:page/edit",
        component: this.skinManager.getTemplate("edit"),
        name: "edit",
      } as RouteRecordRaw,
      {
        path: "/xwiki/search",
        component: this.skinManager.getTemplate("search"),
      } as RouteRecordRaw,
      {
        path: "/:page/",
        component: this.skinManager.getTemplate("content"),
      } as RouteRecordRaw,
    ];

    this.router = createRouter({
      /*
       4. Provide the history implementation to use. We are using the hash
       history for simplicity here. See
       https://router.vuejs.org/guide/essentials/history-mode.html for more
       details. The hash history is not the best for SEO, but does not require
       anything special server-side (which is good for partability), and works
       well with electron.
      */
      history: createWebHashHistory(),
      routes,
    });

    this.app = createApp(Index)
      .use(this.router)
      .use(createPinia())
      .use(createI18n({ legacy: false, fallbackLocale: "en" }));
    this.app.provide("count", 0);
    this.app.provide("skinManager", this.skinManager);
    this.app.provide("cristal", this);

    this.skinManager.loadDesignSystem(this.app, this.container);

    const vueComponents = this.container.getAll<VueTemplateProvider>(
      "VueTemplateProvider",
    );
    for (const vueComponentId in vueComponents) {
      const vueComponent = vueComponents[vueComponentId];
      this.logger?.debug(
        "Found vue component ",
        vueComponent.getVueName(),
        vueComponent,
      );
      if (vueComponent.isGlobal()) {
        this.logger?.debug("Vue component is ", vueComponent.getVueComponent());
        const vueComp = vueComponent.getVueComponent();
        this.app.component(vueComponent.getVueName(), vueComp);
      }
      // registering additional components
      this.logger?.debug(
        "Ready to register components of ",
        vueComponent.getVueName(),
      );
      vueComponent.registerComponents(this.app);
    }

    const uixComponents = this.container.getAll<VueTemplateProvider>(
      "UIXTemplateProvider",
    );
    for (const uixComponentId in uixComponents) {
      const uixComponent = uixComponents[uixComponentId];
      this.logger?.debug(
        "Found vue component ",
        uixComponent.getVueName(),
        uixComponent,
      );
      // registering additional components

      this.logger?.debug(
        "Ready to register components of ",
        uixComponent.getVueName(),
      );
      uixComponent.registerComponents(this.app);
    }

    this.app.mount("#xwCristalApp");

    this.router.beforeEach((to) => {
      this.handlePopState(
        to.params.page as string,
        to.params.revision as string,
      );
    });

    this.logger?.debug("After vue");
    this.logger?.debug("Replacing state in history " + this.getCurrentPage());
    history.replaceState(
      { page: this.getCurrentPage() },
      "",
      "/" + this.wikiConfig.name + "/#/" + this.getCurrentPage() + "/view",
    );

    // WikiModel ready
    await this.preloadConverters();
  }

  getMenuEntries(): Array<string> {
    const menuEntriesElements = new Array<string>();
    try {
      const menuEntries = this.container.getAll<MenuEntry>("MenuEntry");
      this.logger?.debug("All menu entries", menuEntries);
      for (const i in menuEntries) {
        menuEntriesElements.push(menuEntries[i].getMenuEntry());
      }
    } catch (e) {
      this.logger?.debug("No menu entry could be loaded ", e);
    }
    return menuEntriesElements;
  }

  getUIXTemplates(extensionPoint: string): Array<Component> {
    const uixTemplates = new Array<Component>();
    try {
      this.logger?.debug(
        "Searching for UIX with extension Point",
        extensionPoint,
      );
      const uixComponents =
        this.getContainer().getAllNamed<UIXTemplateProvider>(
          "UIXTemplateProvider",
          extensionPoint,
        );
      this.logger?.debug(
        "All uix components for extension point ",
        extensionPoint,
        uixComponents,
      );
      for (const i in uixComponents) {
        uixTemplates.push(uixComponents[i].getVueComponent());
      }
    } catch (e) {
      if (
        e instanceof Error &&
        e.message.indexOf("no matching bindings") == 0
      ) {
        this.logger?.debug("No uix entry found", e);
      }
    }
    return uixTemplates;
  }

  async getPage(
    page: string,
    options?: { requeue?: boolean; revision?: string },
  ): Promise<PageData | undefined> {
    const isJsonLD = this.getWikiConfig().isSupported("jsonld");
    const syntax = isJsonLD ? "jsonld" : "html";
    const pageData = await this.getWikiConfig().storage.getPageContent(
      page,
      syntax,
      options?.revision,
      options?.requeue,
    );
    if (isJsonLD && pageData) {
      const document = pageData.document;
      pageData.html = document.get("html");
      pageData.source = document.get("text");
      pageData.syntax = document.get("encodingFormat");
    }
    return pageData;
  }
}

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

import type { CristalApp, Logger } from "@xwiki/cristal-api";
import type { MacroProvider } from "../api/macroProvider";
import type { App, Component, VNode } from "vue";
import { createVNode, render } from "vue";

// import { DefaultMacroData } from "../components/defaultMacroData";

export class ContentTools {
  static logger: Logger;

  public static init(cristal: CristalApp) {
    this.logger = cristal?.getLogger("skin.vue.contenttools");
  }

  /*
        Method to intercept clicks in the HTML content and load the page using Cristal Wiki
    */
  public static listenToClicks(
    element: HTMLElement,
    cristal: CristalApp | undefined,
  ) {
    element.addEventListener(
      `click`,
      // TODO get rid of any
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      function handleClick(event: any) {
        const origin = event.target.closest(`a`);

        if (origin?.href) {
          ContentTools.logger?.debug("You clicked", origin.href);
          ContentTools.logger?.debug(event.target);
          ContentTools.logger?.debug(event.target.href);
          ContentTools.logger?.debug(location);
          ContentTools.logger?.debug(location.origin);
          ContentTools.logger?.debug(location.hostname);
          event.preventDefault();
          // Case 1: the link is relative and/or points to the current host.
          if (origin.href.startsWith(location.origin)) {
            ContentTools.logger?.debug("URL is relative URL");
            const page = origin.href.replace(
              location.origin + location.pathname,
              "",
            );
            if (!page.startsWith("#")) {
              ContentTools.logger?.debug("New page should be", page);
              cristal?.setCurrentPage(page, "view");
              cristal?.loadPage().then();
            } else {
              ContentTools.logger?.debug("Leaving alone page", page);
              location = page;
              return;
            }
          } else {
            // Case 2: the link points to an external server, in this case we try to resolve it to a known page.
            // Otherwise, the link is considered as external.
            if (cristal != null) {
              cristal.loadPageFromURL(origin.href).then();
            } else {
              ContentTools.logger?.error(
                "cristal object not injected properly in c-content.vue",
              );
            }
          }
        }
      },
      true,
    );
  }

  /*
        Method to load CSS sent by XWiki page
    */
  public static loadCSS(css: string[]) {
    if (css && css.length > 0) {
      // check new css
      ContentTools.logger?.debug("Current CSS is ", css.length);
      css.forEach((cssLink) => {
        const url = ContentTools.urlToLoad(cssLink);
        if (url != null) {
          const divEl = document.createElement("link");
          divEl.href = url;
          divEl.type = "text/css";
          divEl.rel = "stylesheet";
          ContentTools.logger?.debug("Ready to load css ", divEl);
          document.head.appendChild(divEl);
        } else {
          ContentTools.logger?.debug("CSS already loaded: ", cssLink);
        }
      });
    }
  }

  /*
        Method to load JS send by XWiki page
        THis code is not working
    */
  public static loadJS(js: string[]) {
    if (js && js.length > 0) {
      ContentTools.logger?.debug("Loading JS code for content");
      js.forEach((jsLink) => {
        ContentTools.logger?.debug("JS Link: ", jsLink);
        const url = ContentTools.urlToLoad(jsLink);
        if (url != null) {
          const divEl = document.createElement("script");
          divEl.src = url;
          divEl.async = false;
          ContentTools.logger?.debug("Ready to load js ", divEl);
          document.head.appendChild(divEl);
        } else {
          ContentTools.logger?.debug("JS already loaded: ", jsLink);
        }
      });
      ContentTools.logger?.debug("ready to fire event for main content");
      try {
        ContentTools.logger?.debug("prototype doc is", document);
        //// @ts-expect-error TODO describe
        // document.fire("xwiki:dom:loaded");
        // ContentTools.logger?.debug("dom loaded fired in content");
      } catch (e) {
        ContentTools.logger?.debug("exception firing event", e);
      }
    }
  }

  public static urlToLoad(html: string): string | null {
    let url1: string | null = null;
    const range = document.createRange();
    range.setStart(document.head, 0);
    const contentToInject = range.createContextualFragment(html);
    // TODO get rid of any
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    contentToInject
      .querySelectorAll("link[href], script[src]")
      // TODO get rid of any
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      .forEach(function (resource: any) {
        url1 = resource.src || resource.href;
        document.querySelectorAll("link[href], script[src]").forEach(function (
          // TODO get rid of any
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          resource2: any,
        ) {
          const url2 = resource2.src || resource2.href;
          if (url1 && url1 == url2) {
            return null;
          }
        });
      });
    return url1;
  }

  public static transformImages(
    cristal: CristalApp | undefined,
    element: string,
  ) {
    const xwikiContentEl = document.getElementById(element);
    if (xwikiContentEl) {
      const transform = function (img: HTMLImageElement | HTMLScriptElement) {
        const srcItem = img.attributes.getNamedItem("src");
        if (srcItem) {
          ContentTools.logger?.debug("Found image with url ", srcItem.value);
          if (srcItem.value.indexOf("http") != 0) {
            const src = cristal
              ?.getWikiConfig()
              .storage.getImageURL(cristal.getCurrentPage(), srcItem.value);
            if (src) {
              img.src = src;
              ContentTools.logger?.debug("Transforming image to url ", img.src);
            }
          }
        }
      };
      new MutationObserver(function (mutations: Array<MutationRecord>) {
        ContentTools.logger?.debug("Called in mutation records");
        const mutation = mutations[0];
        mutation.addedNodes.item;
        for (const { addedNodes } of mutations) {
          addedNodes.forEach((addedNode) => {
            if (addedNode.nodeType == 1) {
              const imgs = (addedNode as HTMLElement).querySelectorAll("img");
              imgs.forEach((img) => {
                transform(img);
              });
              if ((addedNode as HTMLElement).tagName === "IMG") {
                transform(addedNode as HTMLImageElement);
              }
            }
          });
        }
      }).observe(xwikiContentEl, { childList: true, subtree: true });
    }
  }

  /*
        Experimental function to transform scripts
    */
  public static transformScripts() {
    const transformScript = function (scriptEl: HTMLScriptElement) {
      const srcItem = scriptEl.attributes.getNamedItem("src");
      if (srcItem) {
        ContentTools.logger?.debug(
          "Content Found script with url ",
          srcItem.value,
        );
        if (srcItem.value.indexOf("/") == 0) {
          ContentTools.logger?.debug(
            "Content Ready to transform script url",
            srcItem.value,
          );
          scriptEl.src = "https://wiki30.demo.xwiki.com" + srcItem.value;
          ContentTools.logger?.debug(
            "Content Transforming script to url ",
            scriptEl.src,
          );
        } else {
          ContentTools.logger?.debug(
            "Content Do not transform script url",
            srcItem.value,
          );
        }
      }
    };

    new MutationObserver(function (mutations: Array<MutationRecord>) {
      ContentTools.logger?.debug("Called in mutation records");
      const mutation = mutations[0];
      mutation.addedNodes.item;
      for (const { addedNodes } of mutations) {
        addedNodes.forEach((addedNode) => {
          if (addedNode.nodeType == 1) {
            const jsel = (addedNode as HTMLElement).querySelectorAll("script");
            jsel.forEach((jsel) => {
              ContentTools.logger?.debug("Transforming script url " + jsel);
              transformScript(jsel);
            });
            if ((addedNode as HTMLElement).tagName === "SCRIPT") {
              ContentTools.logger?.debug(
                "Transforming script url " +
                  (addedNode as HTMLScriptElement).src,
              );
              transformScript(addedNode as HTMLScriptElement);
            }
          }
        });
      }
    }).observe(document.head, { childList: true, subtree: true });
  }

  public static mount(
    component: Component,
    // TODO get rid of any
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    props: any,
    children: unknown,
    element: HTMLElement,
    app: App,
  ): () => void {
    let el: HTMLElement | null = element;
    let vNode: VNode | null = createVNode(component, props, children);

    if (app && app._context) {
      vNode.appContext = app._context;
    }
    if (el) {
      render(vNode, el);
    } else if (typeof document !== "undefined") {
      render(vNode, (el = document.createElement("div")));
    }

    const destroy = () => {
      if (el) {
        render(null, el);
      }
      el = null;
      vNode = null;
    };
    return destroy;
  }

  /*
        Method to look for Macros in client side rendering
        Macros are inserted by the WikiModel parser using the following syntax
          <pre class="wikimodel-macro" macroname="MACRONAME" param1="PARAMVALUE1" param2="PARAMVALUE2">
            <!--[CDATA[CONTENT]]-->
          </pre>

        Example with warning macro:
          <pre class="wikimodel-macro" macroname="warning" title="WARNING">
            <!--[CDATA[This is a warning message]]-->
          </pre>
    */
  public static transformMacros(element: HTMLElement, cristal: CristalApp) {
    const macroTagList = element.getElementsByTagName("pre");
    for (let i = 0; i < macroTagList.length; i++) {
      const macroTag = macroTagList[i];
      if (macroTag.className == "wikimodel-macro") {
        const macroName = macroTag.getAttribute("macroname");
        if (macroName != null && macroName != "") {
          ContentTools.logger?.debug("Found macro", macroName);
          try {
            const macroProvider = cristal
              ?.getContainer()
              .getNamed<MacroProvider>("MacroProvider", macroName);
            const vueComponent = macroProvider?.getVueComponent();
            if (vueComponent && cristal?.getApp()) {
              macroTag.id = "wikimodel-macro-" + macroName + "-1";
              const macroData = macroProvider.parseParameters(macroTag);
              this.mount(
                vueComponent,
                { macroData: macroData },
                null,
                macroTag,
                cristal.getApp(),
              );
            }
          } catch (e) {
            ContentTools.logger?.debug(
              "Could not find macro implementation for",
              macroName,
              e,
            );
          }
        }
      }
    }
  }
}

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
import { Extension } from "@tiptap/vue-3";
import Suggestion from "@tiptap/suggestion";

import { App, createApp } from "vue";
import LinkSuggestVue from "../../vue/c-tiptap-link-suggest.vue";
import { PluginKey } from "@tiptap/pm/state";
import { Container } from "inversify";
import { SkinManager, WikiConfig } from "@cristal/api";
import { createPinia } from "pinia";
import linkSuggestStore, {
  LinkSuggestStore,
} from "../../stores/link-suggest-store";
import { queryEqualityOperator } from "./filter-helper";
import { Link, LinkSuggestService } from "@cristal/link-suggest-api";

/**
 * Describe a link suggestion action (i.e., a search result entry).
 */
export interface LinkSuggestionActionDescriptor {
  title: string;
  segments: string[];
  reference: string;
  url: string;
}

/**
 * Initialize a link suggest tiptap extension with the provided components.
 * TODO: This should be migrate to a component with proper injects and a method
 * retuning the tiptap extension
 * @param skinManager a skin manager component instance
 * @param container a container manager instance
 * @param wikiConfig a wiki configuration component instance
 * @param linkSuggest a link suggest service instance
 * @since 0.8
 */
function loadLinkSuggest(
  skinManager: SkinManager,
  container: Container,
  wikiConfig: WikiConfig,
  linkSuggest?: LinkSuggestService,
) {
  return Extension.create({
    name: "link-suggest",
    addOptions() {
      return {
        suggestion: {
          char: "[",
          command: () => {
            // props.command({ editor, range, props });
            // TODO: probably unused?
          },
          items: getSuggestionItems(wikiConfig, linkSuggest),
          render: renderItems(skinManager, container, linkSuggest),
        },
      };
    },
    addProseMirrorPlugins() {
      return [
        Suggestion({
          editor: this.editor,
          ...this.options.suggestion,
          pluginKey: new PluginKey("linkSuggestInternal"),
        }),
      ];
    },
  });
}

function getSuggestionItems(
  wikiConfig: WikiConfig,
  linkSuggest?: LinkSuggestService,
) {
  return async function ({
    query,
  }: {
    query: string;
  }): Promise<LinkSuggestionActionDescriptor[]> {
    // TODO: add upload attachment action
    // TODO: add create new page action
    // TODO: add links suggestions
    let links: Link[];
    try {
      if (linkSuggest) {
        links = await linkSuggest.getLinks(query);
      } else {
        links = [];
      }
    } catch (e) {
      console.group("Failed to fetch remote links");
      console.error(e);
      console.groupEnd();
      links = [];
    }
    const equalityOperator = queryEqualityOperator(query);
    return links
      .filter((link) => equalityOperator(link.label))
      .map((link) => {
        // FIXME: this is very ugly and we need to architecture link management
        // better.
        const url = wikiConfig.baseURL + link.url.replace(/^\/xwiki/, "");
        // FIXME: relate to link management is reference management, here too we
        // need to think me precisely of the architecture we want for this.
        const segments = link.reference.split(/\./);
        return {
          title: link.label,
          segments,
          reference: link.reference,
          url,
        };
      });
  };
}

function renderItems(
  skinManager: SkinManager,
  container: Container,
  linkSuggest?: LinkSuggestService,
) {
  return () => {
    let app: App;
    let elemDiv: HTMLDivElement;
    let store: LinkSuggestStore;

    return {
      onExit() {
        app?.unmount();
      },
      onKeyDown({ event }: { event: KeyboardEvent }) {
        const key = event.key;
        if (key === "Escape") {
          app?.unmount();
          document.body.removeChild(elemDiv);
          return true;
        }

        if (key === "ArrowDown" || key === "ArrowUp" || key === "Enter") {
          // Get the root element of the Vue template and forward it the events.
          const templateRoot = app._container.children[0];
          return templateRoot.dispatchEvent(
            new KeyboardEvent("keydown", { key: key }),
          );
        }

        return false;
      },
      onStart(props: { items: LinkSuggestionActionDescriptor[] }) {
        elemDiv = document.createElement("div");
        document.body.appendChild(elemDiv);
        const pinia = createPinia();
        this.container = new Container();
        app = createApp(LinkSuggestVue, {
          props,
          hasSuggestService: linkSuggest != undefined,
        });
        app.use(pinia);
        // Allow the abstract design components to be available in the sub-app
        // for link suggestions.
        skinManager.loadDesignSystem(app, container);
        // The store must be initialized after pinia is created.
        store = linkSuggestStore();
        store.updateLinks(props.items);
        store.updateProps(props);
        app.mount(elemDiv);
      },
      onUpdate(props: {
        items: LinkSuggestionActionDescriptor[];
        text: string;
      }) {
        store.updateLinks(props.items);
        store.updateText(props.text);
        store.updateProps(props);
      },
    };
  };
}

export { loadLinkSuggest };

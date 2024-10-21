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

import { Editor, Extension, Range } from "@tiptap/vue-3";
import Suggestion from "@tiptap/suggestion";

import { App, createApp } from "vue";
import LinkSuggestVue from "../../vue/c-tiptap-link-suggest.vue";
import { PluginKey } from "@tiptap/pm/state";
import { Container } from "inversify";
import { SkinManager } from "@xwiki/cristal-api";
import { createPinia } from "pinia";
import linkSuggestStore, {
  LinkSuggestStore,
} from "../../stores/link-suggest-store";
import { queryEqualityOperator } from "./filter-helper";
import { Link, type LinkSuggestService } from "@xwiki/cristal-link-suggest-api";

/**
 * @since 0.11
 */
enum LinkType {
  PAGE,
  ATTACHMENT,
}

/**
 * Describe a link suggestion action (i.e., a search result entry).
 */
export interface LinkSuggestionActionDescriptor {
  title: string;
  segments: string[];
  reference: string;
  url: string;
  type: LinkType;
}

/**
 * Initialize a link suggest tiptap extension with the provided components.
 * TODO: This should be migrate to a component with proper injects and a method
 * retuning the tiptap extension
 * @param skinManager a skin manager component instance
 * @param container a container manager instance
 * @param linkSuggest a link suggest service instance
 * @since 0.8
 */
function loadLinkSuggest(
  skinManager: SkinManager,
  container: Container,
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
          items: getSuggestionItems(linkSuggest),
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

/**
 * Build a function returning an array of link suggestions from a string.
 * @param linkSuggest the link suggestion service to use
 * @param wikiConfig the wiki configuration to use
 */
function initSuggestionsService(linkSuggest: LinkSuggestService | undefined) {
  // Return an array of suggestions from a query
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
        // FIXME: relate to link management is reference management, here too we
        // need to think me precisely of the architecture we want for this.
        const segments = link.reference.split(/\./);
        return {
          title: link.label,
          segments,
          reference: link.reference,
          url: link.url,
          type: link.type,
        };
      });
  };
}

/**
 * Initialize a link suggestion function based on the values provided during the
 * extension initialization of the link-suggest extension
 * @param linkSuggest the link suggestion service to use
 */
function getSuggestionItems(linkSuggest?: LinkSuggestService) {
  return initSuggestionsService(linkSuggest);
}

function renderItems(
  skinManager: SkinManager,
  container: Container,
  linkSuggest?: LinkSuggestService,
) {
  // The editor and range to be used for the link creation action.
  // They need to be updated during "onUpdate" to avoid using outdated
  // information.
  let editor: Editor;
  let range: Range;
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
      onStart(props: {
        items: LinkSuggestionActionDescriptor[];
        editor: Editor;
        range: Range;
      }) {
        elemDiv = document.createElement("div");
        document.body.appendChild(elemDiv);
        const pinia = createPinia();

        editor = props.editor;
        range = props.range;

        function existingLinkAction(link: LinkSuggestionActionDescriptor) {
          editor
            .chain()
            .focus()
            .deleteRange(range)
            .setLink({ href: link.url })
            .command(({ tr }) => {
              tr.insertText(link.title);
              return true;
            })
            .run();
        }

        function newLinkAction(href: string) {
          editor
            .chain()
            .focus()
            .deleteRange(range)
            .setLink({ href: href })
            .command(({ tr }) => {
              tr.insertText(href);
              return true;
            })
            .run();
        }

        app = createApp(LinkSuggestVue, {
          props,
          hasSuggestService: linkSuggest != undefined,
          existingLinkAction,
          newLinkAction,
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
        editor: Editor;
        range: Range;
      }) {
        editor = props.editor;
        range = props.range;
        store.updateLinks(props.items);
        store.updateText(props.text);
      },
    };
  };
}

export { loadLinkSuggest, initSuggestionsService, LinkType };

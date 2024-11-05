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

import { LinkSuggestionActionDescriptor } from "../components/extensions/link-suggest";
import { defineStore, Store, StoreDefinition } from "pinia";

type State = {
  links: LinkSuggestionActionDescriptor[];
  text: string;
  props: unknown | undefined;
};

type Getters = Record<string, never>;

type Actions = {
  updateLinks: (links: LinkSuggestionActionDescriptor[]) => void;
  updateText: (text: string) => void;
  updateProps: (props: unknown) => void;
};

type StoreId = "link-suggest-store";
export type LinkSuggestStore = Store<StoreId, State, Getters, Actions>;
type LinkSuggestStoreDefinition = StoreDefinition<
  StoreId,
  State,
  Getters,
  Actions
>;

const store: LinkSuggestStoreDefinition = defineStore<
  StoreId,
  State,
  Getters,
  Actions
>("link-suggest-store", {
  state: () => {
    return {
      links: [],
      text: "",
      props: undefined,
    };
  },
  actions: {
    updateLinks(links): void {
      this.links = links;
    },
    updateText(text: string): void {
      // Remove the starting '['
      this.text = text.substring(1);
    },
    updateProps(props: unknown): void {
      this.props = props;
    },
  },
});
export default store;

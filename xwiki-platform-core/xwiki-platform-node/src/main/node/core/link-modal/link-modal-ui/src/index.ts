/**
 * See the NOTICE file distributed with this work for additional
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

import { default as _LinkConfig } from "./vue/LinkConfig.vue";
import { default as _LinkModal } from "./vue/LinkModal.vue";
import { default as _SearchBox } from "./vue/SearchBox.vue";

/**
 * Link modal UI component
 *
 * @since 18.5.0RC1
 * @beta
 */
const LinkModal = _LinkModal;

/**
 * Shared "chrome" rendered by every link target type's configuration component: the display-text field, the
 * link type selector, and the "Options" section. Re-exported so that link target type extensions defined in
 * other packages (e.g., `@xwiki/platform-link-modal-default`) can wrap their own configuration component with it.
 *
 * @since 18.7.0RC1
 * @beta
 */
const LinkConfig = _LinkConfig;

/**
 * Generic search/typeahead input, used by link target types backed by a suggestion service (e.g., the built-in
 * "page" and "attachment" types). Re-exported so that other link target type extensions can reuse it.
 *
 * @since 18.7.0RC1
 * @beta
 */
const SearchBox = _SearchBox;

export { LinkConfig, LinkModal, SearchBox };

export type {
  SearchLinkSuggestion,
  SearchLinkSuggestor,
} from "./vue/SearchBox.vue";

export { createLinkSuggestor } from "./linkSuggest";
export type {
  LinkEditionContext,
  LinkSuggestion,
  LinkSuggestor,
} from "./linkSuggest";

export type {
  LinkData,
  LinkTarget,
  LinkTargetTypeExtension,
  LinkTargetUrlContext,
} from "@xwiki/platform-link-modal-api";
export {
  linkTargetTypeExtensionRole,
  listEnabledLinkTargetTypeExtensions,
  listLinkTargetTypeExtensions,
  parseLinkTarget,
  serializeLinkTarget,
} from "@xwiki/platform-link-modal-api";

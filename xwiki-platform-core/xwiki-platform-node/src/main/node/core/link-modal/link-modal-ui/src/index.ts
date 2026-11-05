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

import { default as _LinkForm } from "./vue/LinkForm.vue";
import { default as _LinkModal } from "./vue/LinkModal.vue";
import { default as _PlainTextLinkTargetForm } from "./vue/PlainTextLinkTargetForm.vue";
import { default as _SearchBox } from "./vue/SearchBox.vue";

/**
 * Link modal UI component
 *
 * @since 18.5.0RC1
 * @beta
 */
const LinkModal = _LinkModal;

/**
 * The part of the link form shared by all the link target types: the display-text field, the link type selector, and
 * the "Options" section. The form of each link target type (see `LinkTargetTypeExtension.component()` in
 * `@xwiki/platform-link-type-api`) wraps its own fields with it, using its `config` and `options` slots, so that link
 * target type extensions defined in other packages (e.g., `@xwiki/platform-link-type-default`) render a complete link
 * form.
 *
 * @since 18.10.0RC1
 * @beta
 */
const LinkForm = _LinkForm;

/**
 * Generic search/typeahead input, used by link target types backed by a suggestion service (e.g., the built-in
 * "page" and "attachment" types). Re-exported so that other link target type extensions can reuse it.
 *
 * @since 18.10.0RC1
 * @beta
 */
const SearchBox = _SearchBox;

/**
 * Form of the link target types designated by a plain text location (see `PlainTextLinkTargetConfig`), with props for
 * the label, the `data-test` attribute and the type of its location field. The link modal also uses it for the link
 * target types registered without a form. Re-exported so that the form of such a link target type can customize its
 * field.
 *
 * @since 18.10.0RC1
 * @beta
 */
const PlainTextLinkTargetForm = _PlainTextLinkTargetForm;

export { LinkForm, LinkModal, PlainTextLinkTargetForm, SearchBox };

export type { SearchBoxItem, SearchBoxSource } from "./vue/SearchBox.vue";
export type { LinkEditionContext } from "./linkEditionContext";
export { linkTargetTypeFormRole } from "./linkTargetTypeForm";
export type { LinkTargetTypeForm } from "./linkTargetTypeForm";

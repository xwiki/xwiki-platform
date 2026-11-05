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
import type { Component } from "vue";

/**
 * Inversify role used to register the {@link LinkTargetTypeForm} of a link target type, next to the link target type
 * itself (see `linkTargetTypeExtensionRole` in `@xwiki/platform-link-type-api`). Like the link target types, the forms
 * are unconstrained multi-bindings, matched to their link target type by their `type` field.
 *
 * @since 18.10.0RC1
 * @beta
 */
const linkTargetTypeFormRole = "LinkTargetTypeForm";

/**
 * The form used by the link modal to edit the links of a link target type. It's the UI counterpart of the
 * framework-independent `LinkTargetTypeExtension` (see `@xwiki/platform-link-type-api`), registered separately so
 * that the link target types don't depend on the UI framework of the link modal. A link target type without a form is
 * edited with a plain text field (see `PlainTextLinkTargetConfig`).
 *
 * @since 18.10.0RC1
 * @beta
 */
interface LinkTargetTypeForm {
  /**
   * The type of the link target type that this form edits (see `LinkTargetTypeExtension.type`).
   */
  readonly type: string;

  /**
   * @returns the Vue component rendering the form, which receives the edited link data (`linkData` prop) and the
   *   configuration of the link target (`v-model`)
   */
  component(): Component;
}

export { linkTargetTypeFormRole };
export type { LinkTargetTypeForm };

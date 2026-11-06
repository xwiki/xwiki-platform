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
 * itself (see `linkTargetTypeRole` in `@xwiki/platform-link-type-api`). Like the link target types, the forms
 * are unconstrained multi-bindings, matched to their link target type by their `type` field.
 *
 * @since 18.10.0RC1
 * @beta
 */
const linkTargetTypeFormRole = "LinkTargetTypeForm";

/**
 * The part of the link form that is specific to a link target type: the field designating the target of the link, and
 * the fields setting the options of the link. It's the UI counterpart of the framework-independent `LinkTargetType`
 * (see `@xwiki/platform-link-type-api`), registered separately so that the link target types don't depend on the UI
 * framework of the link modal. A link target type without a form is edited with a plain text field (see
 * `PlainTextLinkTargetConfig`).
 *
 * @since 18.10.0RC1
 * @beta
 */
interface LinkTargetTypeForm {
  /**
   * The type of the link target type that this form edits (see `LinkTargetType.type`).
   */
  readonly type: string;

  /**
   * The field designating the target of the link (e.g. a page search box, an e-mail address field, a URL field).
   *
   * @returns the Vue component rendering the field in the link form, below the fields shared by all the link target
   *   types (display text and link type), which the link modal renders itself. It receives the configuration of the
   *   link target (`v-model`), and the container of the link modal (`depsContainer` prop), to resolve the services it
   *   needs. Services that depend on the current wiki should be resolved through their provider (e.g.
   *   `ModelReferenceParserProvider`). It must declare the `depsContainer` prop even when it doesn't use it, so that
   *   the container doesn't fall through as an attribute of its root element.
   */
  targetField(): Component;

  /**
   * The fields setting the options of the link that are specific to this link target type (e.g. the anchor of a page
   * link, the subject of an e-mail link). Optional: a link target type without specific options doesn't need it.
   *
   * @returns the Vue component rendering the fields in the "Options" section of the link form, below the options
   *   shared by all the link target types; it receives the same props as {@link LinkTargetTypeForm.targetField}
   */
  optionFields?(): Component;
}

export { linkTargetTypeFormRole };
export type { LinkTargetTypeForm };

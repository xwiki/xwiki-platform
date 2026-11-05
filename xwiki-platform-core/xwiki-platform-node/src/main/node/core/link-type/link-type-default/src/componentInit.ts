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
import { AttachmentLinkTargetType } from "./extensions/AttachmentLinkTargetType";
import { EmailLinkTargetType } from "./extensions/EmailLinkTargetType";
import { PageLinkTargetType } from "./extensions/PageLinkTargetType";
import { PathLinkTargetType } from "./extensions/PathLinkTargetType";
import { UNCLinkTargetType } from "./extensions/UNCLinkTargetType";
import { URLLinkTargetType } from "./extensions/URLLinkTargetType";
import AttachmentLinkTargetForm from "./vue/AttachmentLinkTargetForm.vue";
import AttachmentLinkTargetOptions from "./vue/AttachmentLinkTargetOptions.vue";
import EmailLinkTargetForm from "./vue/EmailLinkTargetForm.vue";
import EmailLinkTargetOptions from "./vue/EmailLinkTargetOptions.vue";
import PageLinkTargetForm from "./vue/PageLinkTargetForm.vue";
import PageLinkTargetOptions from "./vue/PageLinkTargetOptions.vue";
import { URLLinkTargetForm } from "./vue/URLLinkTargetForm";
import { linkTargetTypeFormRole } from "@xwiki/platform-link-modal-ui";
import { linkTargetTypeExtensionRole } from "@xwiki/platform-link-type-api";
import type { LinkTargetTypeForm } from "@xwiki/platform-link-modal-ui";
import type { Container } from "inversify";
import type { Component } from "vue";

/**
 * Registers the built-in link target types (page, attachment, email, path, UNC path, URL) against the given
 * `depsContainer`, as unconstrained bindings of {@link linkTargetTypeExtensionRole} — see
 * `@xwiki/platform-link-type-api`'s `LinkTargetTypeExtension` for the rationale (mirrors
 * `@xwiki/platform-markdown-syntax-config` and its siblings' own `"SyntaxConfig"` registration).
 *
 * Registers also the forms of these link target types, as bindings of `linkTargetTypeFormRole` (see
 * `@xwiki/platform-link-modal-ui`), except for the path and UNC path ones, which are edited with the plain text form
 * that the link modal uses for the link target types without a form.
 *
 * A consumer that does not want one (or several) of the built-in link target types (e.g. to offer a reduced set)
 * should simply not instantiate this class, and instead register only the extensions it wants, the same way.
 *
 * @since 18.10.0RC1
 * @beta
 */
class ComponentInit {
  constructor(container: Container) {
    container.bind(linkTargetTypeExtensionRole).to(PageLinkTargetType);
    container.bind(linkTargetTypeExtensionRole).to(AttachmentLinkTargetType);
    container.bind(linkTargetTypeExtensionRole).to(EmailLinkTargetType);
    container.bind(linkTargetTypeExtensionRole).to(PathLinkTargetType);
    container.bind(linkTargetTypeExtensionRole).to(UNCLinkTargetType);
    container.bind(linkTargetTypeExtensionRole).to(URLLinkTargetType);

    bindForm(container, "page", PageLinkTargetForm, PageLinkTargetOptions);
    bindForm(
      container,
      "attachment",
      AttachmentLinkTargetForm,
      AttachmentLinkTargetOptions,
    );
    bindForm(container, "email", EmailLinkTargetForm, EmailLinkTargetOptions);
    bindForm(container, "url", URLLinkTargetForm);
  }
}

function bindForm(
  container: Container,
  type: string,
  form: Component,
  options?: Component,
): void {
  container.bind<LinkTargetTypeForm>(linkTargetTypeFormRole).toConstantValue({
    type,
    component: () => form,
    ...(options ? { optionsComponent: () => options } : {}),
  });
}

export { ComponentInit };

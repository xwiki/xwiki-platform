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
import AttachmentLinkTargetField from "./vue/AttachmentLinkTargetField.vue";
import AttachmentLinkTargetOptionFields from "./vue/AttachmentLinkTargetOptionFields.vue";
import EmailLinkTargetField from "./vue/EmailLinkTargetField.vue";
import EmailLinkTargetOptionFields from "./vue/EmailLinkTargetOptionFields.vue";
import PageLinkTargetField from "./vue/PageLinkTargetField.vue";
import PageLinkTargetOptionFields from "./vue/PageLinkTargetOptionFields.vue";
import { URLLinkTargetField } from "./vue/URLLinkTargetField";
import { linkTargetTypeFormRole } from "@xwiki/platform-link-modal-ui";
import type { LinkTargetTypeForm } from "@xwiki/platform-link-modal-ui";
import type { Container } from "inversify";
import type { Component } from "vue";

/**
 * Registers the forms of the built-in link target types (see `@xwiki/platform-link-type-default`), as bindings of
 * `linkTargetTypeFormRole` (see `@xwiki/platform-link-modal-ui`), except for the path and UNC path ones, which are
 * edited with the plain text target field that the link modal uses for the link target types without a form.
 *
 * A consumer that does not want the forms of one (or several) of the built-in link target types should not instantiate
 * this class, and instead register only the forms it wants, the same way.
 *
 * @since 18.10.0RC1
 * @beta
 */
class ComponentInit {
  constructor(container: Container) {
    bindForm(
      container,
      "page",
      PageLinkTargetField,
      PageLinkTargetOptionFields,
    );
    bindForm(
      container,
      "attachment",
      AttachmentLinkTargetField,
      AttachmentLinkTargetOptionFields,
    );
    bindForm(
      container,
      "email",
      EmailLinkTargetField,
      EmailLinkTargetOptionFields,
    );
    bindForm(container, "url", URLLinkTargetField);
  }
}

function bindForm(
  container: Container,
  type: string,
  targetField: Component,
  optionFields?: Component,
): void {
  container.bind<LinkTargetTypeForm>(linkTargetTypeFormRole).toConstantValue({
    type,
    targetField: () => targetField,
    ...(optionFields ? { optionFields: () => optionFields } : {}),
  });
}

export { ComponentInit };

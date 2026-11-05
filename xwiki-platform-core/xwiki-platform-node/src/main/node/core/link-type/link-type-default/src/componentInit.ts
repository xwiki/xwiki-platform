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
import { AttachmentLinkTargetType } from "./linkTargetTypes/AttachmentLinkTargetType";
import { EmailLinkTargetType } from "./linkTargetTypes/EmailLinkTargetType";
import { PageLinkTargetType } from "./linkTargetTypes/PageLinkTargetType";
import { PathLinkTargetType } from "./linkTargetTypes/PathLinkTargetType";
import { UNCLinkTargetType } from "./linkTargetTypes/UNCLinkTargetType";
import { URLLinkTargetType } from "./linkTargetTypes/URLLinkTargetType";
import { linkTargetTypeRole } from "@xwiki/platform-link-type-api";
import type { Container } from "inversify";

/**
 * Registers the built-in link target types (page, attachment, email, path, UNC path, URL), as bindings of
 * {@link linkTargetTypeRole}. Their forms, used to edit them in the link modal, are registered separately (see
 * `@xwiki/platform-link-type-ui`), so that the link target types don't depend on the UI framework of the link modal.
 *
 * A consumer that does not want one (or several) of the built-in link target types (e.g. to offer a reduced set)
 * should simply not instantiate this class, and instead register only the link target types it wants, the same way.
 *
 * @since 18.10.0RC1
 * @beta
 */
class ComponentInit {
  constructor(container: Container) {
    container.bind(linkTargetTypeRole).to(PageLinkTargetType);
    container.bind(linkTargetTypeRole).to(AttachmentLinkTargetType);
    container.bind(linkTargetTypeRole).to(EmailLinkTargetType);
    container.bind(linkTargetTypeRole).to(PathLinkTargetType);
    container.bind(linkTargetTypeRole).to(UNCLinkTargetType);
    container.bind(linkTargetTypeRole).to(URLLinkTargetType);
  }
}

export { ComponentInit };

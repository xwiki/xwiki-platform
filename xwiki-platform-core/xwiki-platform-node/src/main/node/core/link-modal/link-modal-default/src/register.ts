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
import { linkTargetTypeExtensionRole } from "@xwiki/platform-link-modal-api";
import type { Manager } from "@xwiki/platform-component-manager-api";

/**
 * Registers the built-in link target types (page, attachment, url, email) with the given component manager.
 * Must be called exactly once per `manager`, before `init()` is called on it — see
 * `@xwiki/platform-component-manager-default`.
 *
 * A consumer that does not want one (or several) of the built-in link target types (e.g. to fully replace it, or
 * to offer a reduced set) should not call this function, and instead register only the extensions it wants,
 * directly.
 *
 * @param manager - the component manager to register the built-in link target types with
 *
 * @since 18.7.0RC1
 * @beta
 */
function registerLinkModalDefaults(manager: Manager): void {
  manager.registerComponent(
    linkTargetTypeExtensionRole,
    async () =>
      (await import("./extensions/PageLinkTargetType")).PageLinkTargetType,
    { name: "page" },
  );

  manager.registerComponent(
    linkTargetTypeExtensionRole,
    async () =>
      (await import("./extensions/AttachmentLinkTargetType"))
        .AttachmentLinkTargetType,
    { name: "attachment" },
  );

  manager.registerComponent(
    linkTargetTypeExtensionRole,
    async () =>
      (await import("./extensions/EmailLinkTargetType")).EmailLinkTargetType,
    { name: "email" },
  );

  manager.registerComponent(
    linkTargetTypeExtensionRole,
    async () =>
      (await import("./extensions/UrlLinkTargetType")).UrlLinkTargetType,
    { name: "url" },
  );
}

export { registerLinkModalDefaults };

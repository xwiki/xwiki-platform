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

// Dedicated, minimal entry point registering the built-in link target types (page, attachment, url, email) with
// the shared client-side component manager, so that `LinkModal` (see `@xwiki/platform-link-modal-ui`) can resolve
// them. It only registers *lazy* loaders (see `registerLinkModalDefaults`), so this module stays tiny even though
// the 4 built-in types themselves (their Vue components, `@xwiki/platform-link-modal-ui`, `vue-i18n`, etc.) are
// not.
//
// This is deployed as its own eager importmap module (see this module's pom.xml) rather than being registered
// from `main.js`, because `main.js` is only loaded on demand (as an AMD/RequireJS module, see
// `BlockNoteRequireJSModuleUIExtension`) when a BlockNote editor actually mounts — by then, the manager may
// already have been initialized (see
// `org.xwiki.platform.component.front.internal.ClientSideComponentsResolverUIExtension`, which calls `init()`
// once per page, after every eagerly-loaded module has had a chance to register). Registering only from `main.js`
// would race against that and, in practice, likely lose.
import { manager } from "@xwiki/platform-component-manager-default";
import { registerLinkModalDefaults } from "@xwiki/platform-link-modal-default";

registerLinkModalDefaults(manager);

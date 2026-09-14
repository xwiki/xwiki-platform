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

import {
  AttachmentReference,
  DocumentReference,
  EntityReference,
  EntityReferenceResolver,
  EntityReferenceSerializer,
  EntityReferenceTree,
  EntityReferenceTreeNode,
  EntityType,
  Model,
  SpaceReference,
  WikiReference,
} from "@xwiki/platform-xwiki-model-api";

const api = {
  EntityType,
  EntityReference,
  WikiReference,
  SpaceReference,
  DocumentReference,
  AttachmentReference,
  EntityReferenceTreeNode,
  EntityReferenceTree,
  EntityReferenceResolver,
  EntityReferenceSerializer,
  Model,
};

// Extend the XWiki API with the Entity Reference API, creating the global object when it doesn't exist yet. This
// bundle is loaded as a classic, non deferred script because js/xwiki/xwiki.js (which doesn't use RequireJS) and some
// inline scripts call XWiki.Model.resolve() while the page is still being parsed. Note that a top level "var" would
// not create a global here, because the bundler wraps this module in an IIFE.
globalThis.XWiki = Object.assign(globalThis.XWiki ?? {}, api);

// Register the RequireJS module under the name it has always had. It resolves to the whole global XWiki object, which
// is what the RequireJS shim it replaces was exporting.
if (typeof define === "function" && define.amd) {
  define("xwiki-entityReference", [], () => globalThis.XWiki);
}

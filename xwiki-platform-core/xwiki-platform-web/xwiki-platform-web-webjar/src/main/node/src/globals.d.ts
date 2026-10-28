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

export {};

// This module must not use the shared "@xwiki/platform-xwiki-utils/global" declaration of the XWiki global: that one
// describes the object as read once the page is loaded, while this module creates or extends it, possibly before
// xwiki.js has run, so it sees a possibly undefined object holding none of the members declared there yet.
declare global {
  // A global "var" declaration, rather than a "const" one, is what makes globalThis.XWiki assignable.
  var XWiki: Record<string, unknown> | undefined;
}

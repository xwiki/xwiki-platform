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

import { STANDARD_TOOLBAR_SELECTOR } from "../ui/Toolbar";

/**
 * Finds the edit form of the current XWiki page, i.e. the form holding the standard edit toolbar (Save, Cancel, etc.)
 * that the realtime toolbar replaces. Use it when the code creating the realtime toolbar doesn't know the edit form.
 *
 * @returns the edit form of the current page, or the page body when the standard edit toolbar is not inside a form
 * @since 18.9.0RC1
 * @beta
 */
function findXWikiEditForm(): HTMLElement {
  return (
    document
      .querySelector(STANDARD_TOOLBAR_SELECTOR)
      ?.closest<HTMLElement>("form, .form") ?? document.body
  );
}

export { findXWikiEditForm };

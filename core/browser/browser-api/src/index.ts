/*
 * See the LICENSE file distributed with this work for additional
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

import { WikiConfig } from "@xwiki/cristal-api";

/**
 * Provide the operations to interact with the browser. We need this abstraction
 * because interacting with the browser is not equivalent when Cristal is
 * running directly on a browser, or in an Electron app managing a browser.
 * @since 0.8
 */
export interface BrowserApi {
  /**
   * Change the wiki configuration of the Cristal instance
   * @param wikiConfig - the new wiki config to use
   */
  switchLocation(wikiConfig: WikiConfig): void;

  /**
   * Fully reload the current window.
   *
   * @since 0.11
   */
  reload(): void;
}

export const name = "BrowserApi";

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

import type { Storage } from "./storage";

/**
 * @since 18.0.0RC1
 * @beta
 */
interface WikiConfig {
  name: string;
  // The base url of the backend endpoint
  baseURL: string;
  // The rest base url of the backend endpoint
  baseRestURL: string;
  /**
   * Realtime endpoint URL.
   * Defaults to http://localhost:15681/collaboration when undefined.
   * @since 18.0.0RC1
   * @beta
   */
  realtimeURL?: string;
  /**
   * Realtime provider hint. When undefined, the default hocuspocus provider is used.
   * @since 18.0.0RC1
   * @beta
   */
  realtimeHint?: string;
  /**
   * Authentication server base URL.
   * @since 18.0.0RC1
   * @beta
   */
  authenticationBaseURL?: string;
  /**
   * Authentication Manager component to use.
   * By default, resolves to configuration type.
   * @since 18.0.0RC1
   * @beta
   */
  authenticationManager?: string;
  homePage: string;
  storage: Storage;
  serverRendering: boolean;
  designSystem: string;
  offline: boolean;
  /**
   * Root location to store pages.
   * @since 18.0.0RC1
   * @beta
   */
  storageRoot?: string;
  /**
   * The (optional) type of the editor. The default is "blocknote".
   *
   * @since 18.0.0RC1
   * @beta
   */
  editor?: string;

  setConfig(
    name: string,
    baseURL: string,
    baseRestURL: string,
    homePage: string,
    serverRendering: boolean,
    designSystem: string,
    offline: boolean,
    editor: string,
  ): void;

  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  setConfigFromObject(configObject: any): void;

  isSupported(format: string): boolean;

  initialize(): void;

  /**
   * The default page name for the current configuration.
   * For instance, "README" for Github, or "Main.WebHome" for XWiki.
   */
  defaultPageName(): string;

  /**
   * Returns the type of the WikiConfig implementation.
   *
   * @returns the type of the implementation
   * @since 18.0.0RC1
   * @beta
   */
  getType(): string;

  /**
   * Returns the default name for a newly created page.
   *
   * @returns the default name
   * @since 18.0.0RC1
   * @beta
   */
  getNewPageDefaultName(): string;
}

export type { WikiConfig };

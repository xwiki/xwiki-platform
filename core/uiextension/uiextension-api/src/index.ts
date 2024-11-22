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

import { Component } from "vue";

/**
 * Define the information held by a UI Extension (UIX).
 *
 * @since 0.11
 */
interface UIExtension {
  /**
   * The unique id of an UI Extension
   */
  id: string;

  /**
   * The id of the extension point where this UIX should be injected.
   */
  uixpName: string;

  /**
   * The order of the UIX. The lowest values are expected to be presented
   * first.
   */
  order: number;

  /**
   * A free set of parameters.
   */
  parameters: { [key: string]: unknown };

  /**
   * Compute if the UIX should be displayed.
   */
  enabled(): Promise<boolean>;

  /**
   * The UI component of the UIX.
   */
  component(): Promise<Component>;
}

/**
 * @since 0.11
 */
interface UIExtensionsManager {
  /**
   *
   * @param name - the name of the UIXP
   * @returns a list of UIExtension components, sorted by ascending order. disabled UIExtensions are excluded
   */
  list(name: string): Promise<UIExtension[]>;
}

export type { UIExtension, UIExtensionsManager };

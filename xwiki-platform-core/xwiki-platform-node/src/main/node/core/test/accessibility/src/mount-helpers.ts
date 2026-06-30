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
import { mount } from "@vue/test-utils";
import type { VueWrapper } from "@vue/test-utils";
import type { Component } from "vue";

/**
 * Helper to initialize a Vue Wrapper with the right configuration. In particular making sure that the component is
 * shallow mounted with and attached to an HTML document body.
 * Returns a method that expect additional shallow mount option to be merged with the default
 * @param component - the component to mount
 * @param baseConfig - an optional base config, can be useful to if many tests shared a very close component
 * configuration, otherwise it is better to use the config of the returned method
 * @since 18.2.0RC1
 * @public
 */
function shallowMountHelper(
  component: Component,
  baseConfig?: Record<string, unknown>,
): (config?: Record<string, unknown>) => VueWrapper {
  return mountHelper(component, { ...baseConfig, shallow: true });
}

/**
 * Helper to initialize a Vue Wrapper with the right configuration. In particular making sure that the component is
 * mounted with and attached to an HTML document body.
 * Returns a method that expect additional mount option to be merged with the default
 * @param component - the component to mount
 * @param baseConfig - an optional base config, can be useful to if many tests shared a very close component
 * configuration, otherwise it is better to use the config of the returned method
 * @since 18.2.0RC1
 * @public
 */

function mountHelper(
  component: Component,
  baseConfig?: Record<string, unknown>,
): (config?: Record<string, unknown>) => VueWrapper {
  return (config) => {
    return mount(component, {
      attachTo: document.body,
      ...(baseConfig ?? {}),
      ...(config ?? {}),
    });
  };
}

export { mountHelper, shallowMountHelper };

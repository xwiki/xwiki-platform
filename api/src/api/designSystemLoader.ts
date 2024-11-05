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

import {
  AsyncComponentLoader,
  AsyncComponentOptions,
  Component,
  ComponentPublicInstance,
  defineAsyncComponent,
} from "vue";
import type { App } from "vue";

export interface DesignSystemLoader {
  loadDesignSystem(app: App): void;
}

/**
 * Register a component as async to lazy-load it. Avoiding loading resources
 * from all design systems at once (e.g., creating CSS conflicts).
 * @param app - the app to load the component to
 * @param name - the name of the Vue component
 * @param source - the source loaded (i.e., a call to import). The import must be
 * in the package where the component is as otherwise the import is made
 * relative to this package and the dependency is not found
 * @since 0.7
 */
export function registerAsyncComponent<
  T extends Component = {
    new (): ComponentPublicInstance;
  },
>(
  app: App,
  name: string,
  source: AsyncComponentLoader<T> | AsyncComponentOptions<T>,
): void {
  // Register a component as an async component.
  app.component(name, defineAsyncComponent(source));
}

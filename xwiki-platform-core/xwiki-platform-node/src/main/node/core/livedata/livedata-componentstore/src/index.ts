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

import type { Component } from "vue";

/**
 * Store containing the resolved or pending components.
 * It's a two levels map, the first level with components kinds as the key, and components id for the second level.
 * The values are the components
 */
const store: {
  [kind: string]: { [key: string]: (() => Promise<Component>) | Component };
} = {};
/**
 * The awaiting map has the same structure as the store, but contains promises that exists only of a component is
 * required before it is registered. The promises are resolved once the requested component is registered.
 */
const awaiting: {
  [kind: string]: {
    [key: string]: {
      promise: Promise<Component>;
      resolve: (value: Component | PromiseLike<Component>) => void;
    };
  };
} = {};

/**
 * Provide the operations to register and resolve Live Data components.
 * @since 18.2.0RC1
 * @beta
 */
const componentStore = {
  register(
    kind: string,
    key: string,
    componentProvider: () => Promise<Component>,
  ) {
    if (!store[kind]) {
      store[kind] = {};
    }
    store[kind][key] = componentProvider;

    // If the component was requested before registration, it is resolved and registered directly.
    if (awaiting?.[kind]?.[key]) {
      void (async () => {
        const resolved = await componentProvider();
        store[kind][key] = resolved;
        awaiting[kind][key].resolve(resolved);
        delete awaiting[kind][key];
      })();
    }
  },

  // eslint-disable-next-line max-statements
  async load(kind: string, key: string) {
    if (!store[kind] || !store[kind][key]) {
      // If the component was already awaited for, we return the same promise again.
      if (awaiting?.[kind]?.[key]) {
        return awaiting[kind][key].promise;
      }

      // Otherwise, a promise is created and stored in the awaiting map. Once the expected component is registered,
      // the promise is resolved.
      let _resolve:
        | ((value: Component | PromiseLike<Component>) => void)
        | undefined = undefined;
      const promise = new Promise<Component>((resolve) => {
        _resolve = resolve;
      });
      if (!awaiting[kind]) {
        awaiting[kind] = {};
      }
      awaiting[kind][key] = { promise, resolve: _resolve! };
      return promise;
    } else {
      // On the first access, we resolve the component provider and save its result instead.
      // On the next access, the cached result is returned without a lookup.
      // This is fine since the result of the componentProvider function is expected to be stable.
      if (typeof store[kind][key] === "function") {
        // @ts-expect-error at this point we know store[kind][key] is a function.
        store[kind][key] = await store[kind][key]();
      }

      return store[kind][key];
    }
  },

  /**
   * Return a component synchronously, when it is already loaded.
   * A component is loaded once a call to {@link componentStore.load} has resolved it. Callers can then render it
   * right away, instead of waiting for a promise that would delay the rendering to a later tick.
   *
   * @param kind - the kind of the component (e.g., "displayer")
   * @param key - the id of the component
   * @returns the loaded component, or undefined when it is not registered or not loaded yet
   * @since 18.10.0RC1
   */
  get(kind: string, key: string): Component | undefined {
    const component = store[kind]?.[key];
    return typeof component === "function" ? undefined : component;
  },

  /**
   * Tell whether a component is registered, loaded or not.
   * Unlike {@link componentStore.load}, a call for a component that is not registered yet does not wait for its
   * registration.
   *
   * @param kind - the kind of the component (e.g., "displayer")
   * @param key - the id of the component
   * @returns true when a component is registered for this kind and id
   * @since 18.10.0RC1
   */
  isRegistered(kind: string, key: string): boolean {
    return store[kind]?.[key] !== undefined;
  },
};

export { componentStore };

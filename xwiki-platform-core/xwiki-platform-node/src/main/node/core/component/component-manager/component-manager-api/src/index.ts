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
/**
 * Declare the signature of newable element, most generally class constructors.
 * @beta
 * @since 18.4.0RC1
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Newable<TInstance = unknown, TArgs extends unknown[] = any[]> = new (
  ...args: TArgs
) => TInstance;

/**
 * Component resolver role identifier.
 * @beta
 * @since 18.4.0RC1
 */
const resolverRole: unique symbol = Symbol("Resolver");

/**
 * Manage components registration.
 * @beta
 * @since 18.4.0RC1
 */
interface Manager {
  /**
   * Registers a component with the manager.
   * @param symbol - The unique symbol identifier for the component
   * @param component - An async factory function that returns a newable component constructor
   * @param options - Optional configuration object for the component with two properties. name: Optional name
   * identifier for disambiguation, the default is "default", priority: Optional priority for component resolution
   * order, the default is 1000.
   * @returns This manager instance for method chaining
   * @beta
   * @since 18.4.0RC1
   */
  registerComponent(
    symbol: symbol,
    component: () => Promise<Newable>,
    options?: { name?: string; priority?: number },
  ): Manager;

  /**
   * Builds and returns a resolver instance with all registered components.
   * @returns A promise that resolves to a Resolver instance
   * @beta
   * @since 18.4.0RC1
   */
  build(): Promise<Resolver>;
}

/**
 * Resolve and retrieve registered components.
 * @beta
 * @since 18.4.0RC1
 */
interface Resolver {
  /**
   * Retrieves a single component instance by its identifier and optional name.
   * The component module is dynamically imported on first call and cached as a singleton.
   * @typeParam T - the type of the component being resolved
   * @param identifier - the unique identifier for the component
   * @param name - an optional name identifier to resolve a non-default component
   * @returns a promise resolving to the component instance of type T
   * @beta
   * @since 18.4.0RC1
   */
  getAsync<T>(identifier: symbol, name?: string): Promise<T>;

  /**
   * Retrieves all component instances registered with the given symbol identifier.
   * Loads every registered module for the role on first call and caches the singletons.
   * @typeParam T - The type of the components being resolved
   * @param identifier - The unique identifier for the components
   * @returns a promise resolving to an array of component instances of type T
   * @beta
   * @since 18.4.0RC1
   */
  getAllAsync<T>(identifier: symbol): Promise<T[]>;
}
export { resolverRole };
export type { Manager, Newable, Resolver };

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

type ModuleFactory = (...dependencies: unknown[]) => unknown;

type FakeRequire = (
  ids: string[],
  onLoad: (...modules: unknown[]) => void,
  onError?: (error: unknown) => void,
) => void;

type FakeDefine = (
  id: string,
  dependencyIdsOrModule: unknown,
  factory?: ModuleFactory,
) => void;

/**
 * A fake RequireJS installed on the global object, controlling which modules are available to the
 * code under test.
 *
 * @since 18.8.0RC1
 * @public
 */
interface RequireJSMock {
  /**
   * Register a module, or replace an already registered one.
   *
   * @param id - the identifier under which the module is loaded
   * @param module - the value the module resolves to
   */
  set(id: string, module: unknown): void;

  /**
   * Remove the fake globals, restoring the values they had before this mock was installed.
   */
  restore(): void;
}

type Globals = Record<string, unknown>;

/**
 * Overwrite some globals.
 *
 * @param values - the globals to install, indexed by name
 * @returns a function restoring the overwritten globals
 */
function installGlobals(values: Globals): () => void {
  const globals = globalThis as unknown as Globals;
  const saved = new Map(
    Object.keys(values).map((name) => [
      name,
      Object.getOwnPropertyDescriptor(globals, name),
    ]),
  );
  Object.assign(globals, values);
  return () => {
    for (const [name, descriptor] of saved) {
      if (descriptor) {
        Object.defineProperty(globals, name, descriptor);
      } else {
        delete globals[name];
      }
    }
  };
}

/**
 * Install a fake RequireJS on the global object: `require`, its `requirejs` alias and `define`, all
 * backed by an in-memory module registry. Loading an identifier that is not registered calls the
 * error callback, so that failing loads can be tested as well.
 *
 * @param modules - the modules available for loading, indexed by identifier
 * @returns the installed mock, which must be restored once the test is over
 * @since 18.8.0RC1
 * @public
 */
function mockRequireJS(modules: Record<string, unknown> = {}): RequireJSMock {
  const registry = new Map(Object.entries(modules));

  const resolve = (ids: string[]): unknown[] =>
    ids.map((id) => {
      if (!registry.has(id)) {
        throw new Error(
          `No RequireJS module registered with identifier [${id}].`,
        );
      }
      return registry.get(id);
    });

  const fakeRequire: FakeRequire = (ids, onLoad, onError) => {
    let modules;
    try {
      modules = resolve(ids);
    } catch (error) {
      if (!onError) {
        throw error;
      }
      // As with the real RequireJS, only failures to load report to the error callback: errors thrown by onLoad
      // itself are left to propagate.
      onError(error);
      return;
    }
    onLoad(...modules);
  };

  // Support both define(id, module) and define(id, dependencyIds, factory).
  const fakeDefine: FakeDefine = (id, dependencyIdsOrModule, factory) => {
    const module = factory ?? dependencyIdsOrModule;
    const dependencyIds = factory ? (dependencyIdsOrModule as string[]) : [];
    registry.set(
      id,
      typeof module === "function"
        ? (module as ModuleFactory)(...resolve(dependencyIds))
        : module,
    );
  };

  const restore = installGlobals({
    define: fakeDefine,
    require: fakeRequire,
    requirejs: fakeRequire,
  });

  return {
    set: (id, module) => registry.set(id, module),
    restore,
  };
}

export { mockRequireJS };
export type { RequireJSMock };

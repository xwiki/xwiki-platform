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
import { resolverRole } from "@xwiki/platform-component-manager-api";
import { Container, injectable } from "inversify";
import type {
  Manager,
  Newable,
  Resolver,
} from "@xwiki/platform-component-manager-api";

/**
 * Internal representation of a registered component.
 */
type Registered = {
  symbol: symbol;
  component: () => Promise<Newable>;
  name: string;
  priority: number;
};

function formatRegistered(r: Registered): string {
  const symbolLabel = r.symbol.description ?? String(r.symbol);
  return `{symbol: ${symbolLabel}, name: ${r.name}, priority: ${r.priority}}`;
}

/**
 * Default implementation of the Resolver interface using Inversify container.
 */
@injectable()
class DefaultResolver implements Resolver {
  private _container!: Container;

  /**
   * Sets the Inversify container instance.
   */
  set container(value: Container) {
    this._container = value;
  }

  /**
   * Gets the Inversify container instance.
   */
  get container() {
    return this._container;
  }

  async getAsync<T>(role: symbol, name?: string): Promise<T> {
    return name === undefined
      ? this._container.getAsync<T>(role)
      : this._container.getAsync<T>(role, { name });
  }

  async getAllAsync<T>(role: symbol): Promise<T[]> {
    return this._container.getAllAsync<T>(role, {
      tag: { key: "all", value: true },
    });
  }
}

/**
 * Default implementation of the Manager interface using Inversify container.
 * @beta
 * @since 18.4.0RC1
 */
class DefaultManager implements Manager {
  /**
   * Array of registered components.
   */
  private readonly registered: Registered[] = [];

  registerComponent(
    symbol: symbol,
    component: () => Promise<Newable>,
    options?: { name?: string; priority?: number },
  ): Manager {
    const newRegistered: Registered = {
      component,
      symbol: symbol,
      name: options?.name ?? "default",
      priority: options?.priority ?? 1000,
    };

    const similarRegistered = this.registered.find(
      (r) =>
        r.symbol === newRegistered.symbol &&
        r.name === newRegistered.name &&
        r.priority === newRegistered.priority,
    );
    if (similarRegistered) {
      throw new Error(
        `A component with the same role, name and priority as ${formatRegistered(newRegistered)} is already registered: ${formatRegistered(similarRegistered)}`,
      );
    }

    this.registered.push(newRegistered);
    return this;
  }

  async build(): Promise<Resolver> {
    // autobind lets the lazy trampoline resolve the loaded class via container.getAsync(Ctor)
    // while still performing full constructor injection on its @inject(...) dependencies.
    const container = new Container({ autobind: true });
    container
      .bind(resolverRole)
      .to(DefaultResolver)
      .inSingletonScope()
      .whenDefault();
    const defaultResolver = container.get<DefaultResolver>(resolverRole);
    defaultResolver.container = container;
    this.bindAllLazy(container);
    return defaultResolver;
  }

  private bindAllLazy(container: Container): void {
    const allRoles: Set<symbol> = new Set(this.registered.map((r) => r.symbol));
    for (const role of allRoles) {
      const forRole = this.registered.filter((r) => r.symbol === role);
      const allNames = new Set(forRole.map((r) => r.name));
      for (const name of allNames) {
        const winner = forRole
          .filter((r) => r.name === name)
          .sort((r1, r2) => r1.priority - r2.priority)[0]!;
        // No await: the loader is captured and only invoked on first resolution.
        this.bindLazy(container, role, name, winner.component);
      }
    }
  }

  /**
   * Binds a lazy trampoline for a (role, name) into the container. The component
   * module is loaded on first resolution; once loaded, the class is resolved through
   * the container with autobind so that constructor `@inject(...)` dependencies are
   * fully wired by Inversify, then cached as a singleton.
   */
  private bindLazy(
    container: Container,
    role: symbol,
    name: string,
    loader: () => Promise<Newable>,
  ) {
    // Share one cached resolution across the (up to) three bindings registered below for this (role, name). Each
    // binding has its own inSingletonScope() cache, and container.getAsync(Ctor) is not guaranteed to be
    // singleton-scoped, so without this closure the same component would be instantiated once per resolution
    // path (default / named / tagged-all).
    let cached: Promise<unknown> | undefined;
    const trampoline = async () => {
      if (!cached) {
        // getAsync + autobind triggers Inversify's resolution pipeline on Ctor:
        // it walks @inject metadata and awaits any further async (lazy) bindings.
        cached = loader().then((Ctor) =>
          container.getAsync(Ctor, { autobind: true }),
        );
      }
      return cached;
    };

    if (name === "default") {
      container
        .bind(role)
        .toDynamicValue(trampoline)
        .inSingletonScope()
        .whenDefault();
    }
    container
      .bind(role)
      .toDynamicValue(trampoline)
      .inSingletonScope()
      .whenNamed(name);
    container
      .bind(role)
      .toDynamicValue(trampoline)
      .inSingletonScope()
      .whenTagged("all", true);
  }
}

/**
 * Creates a new, isolated component manager instance. Prefer this over the shared {@link manager} singleton in tests,
 * or whenever multiple independent containers are needed.
 * @beta
 * @since 18.4.0RC1
 */
function createManager(): Manager {
  return new DefaultManager();
}

/**
 * Default shared component manager. Most application code should use this
 * instance so that all webjars register into the same container. For test
 * isolation, prefer {@link createManager}.
 * @beta
 * @since 18.4.0RC1
 */
const manager: Manager = createManager();

export { DefaultManager, createManager, manager };

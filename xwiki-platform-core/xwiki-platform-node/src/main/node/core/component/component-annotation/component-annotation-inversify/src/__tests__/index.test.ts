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
import { inject, injectAll, injectable, named } from "../index";
import { Container } from "inversify";
import { describe, expect, it } from "vitest";

const PLUGIN_ROLE = Symbol("Plugin");
const HOST_ROLE = Symbol("Host");

describe("component-annotation-inversify", () => {
  it("injects a single dependency by identifier with @inject", async () => {
    @injectable()
    class Dependency {
      public readonly tag = "dep";
    }

    @injectable()
    class Consumer {
      constructor(
        @inject(PLUGIN_ROLE) public readonly dependency: Dependency,
      ) {}
    }

    const container = new Container();
    container.bind(PLUGIN_ROLE).to(Dependency);
    container.bind(HOST_ROLE).to(Consumer);

    const consumer = container.get<Consumer>(HOST_ROLE);
    expect(consumer.dependency).toBeInstanceOf(Dependency);
  });

  it("disambiguates by name with @named", async () => {
    @injectable()
    class Alpha {
      public readonly tag = "alpha";
    }
    @injectable()
    class Beta {
      public readonly tag = "beta";
    }

    @injectable()
    class Consumer {
      constructor(
        @inject(PLUGIN_ROLE)
        @named("beta")
        public readonly plugin: Alpha | Beta,
      ) {}
    }

    const container = new Container();
    container.bind(PLUGIN_ROLE).to(Alpha).whenNamed("alpha");
    container.bind(PLUGIN_ROLE).to(Beta).whenNamed("beta");
    container.bind(HOST_ROLE).to(Consumer);

    expect(container.get<Consumer>(HOST_ROLE).plugin.tag).toBe("beta");
  });

  // eslint-disable-next-line max-statements
  it("injects every registered components with @injectAll", async () => {
    interface PluginTest {
      tag: string;
    }

    @injectable()
    class Alpha implements PluginTest {
      public readonly tag = "alpha";
    }
    @injectable()
    class Beta implements PluginTest {
      public readonly tag = "beta";
    }

    @injectable()
    class Consumer {
      constructor(
        @injectAll(PLUGIN_ROLE)
        public readonly plugins: PluginTest[],
      ) {}
    }

    const container = new Container();
    // Mirror what DefaultManager does internally: each contribution is bound
    // both with a name (or default) AND with the "all"=true tag, so @injectAll
    // can collect them via multiInject + tag filter.
    container.bind(PLUGIN_ROLE).to(Alpha).whenNamed("alpha");
    container.bind(PLUGIN_ROLE).to(Alpha).whenTagged("all", true);
    container.bind(PLUGIN_ROLE).to(Beta).whenNamed("beta");
    container.bind(PLUGIN_ROLE).to(Beta).whenTagged("all", true);
    container.bind(HOST_ROLE).to(Consumer);

    const consumer = container.get<Consumer>(HOST_ROLE);
    const tags = consumer.plugins.map((p) => p.tag).sort();
    expect(tags).toEqual(["alpha", "beta"]);
  });

  it("@injectAll resolves to an empty array when no bindings exist", async () => {
    @injectable()
    class Consumer {
      constructor(
        @injectAll(PLUGIN_ROLE)
        public readonly plugins: ReadonlyArray<unknown>,
      ) {}
    }

    const container = new Container();
    container.bind(HOST_ROLE).to(Consumer);

    expect(container.get<Consumer>(HOST_ROLE).plugins).toEqual([]);
  });
});

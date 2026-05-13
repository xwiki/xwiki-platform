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
import { createManager } from "../index";
import { inject, injectable } from "inversify";
import { describe, expect, it } from "vitest";

const COMPONENT_ROLE = Symbol("TestComponent");
const OTHER_ROLE = Symbol("OtherComponent");

@injectable()
class TestComponent {
  public name: string = "test";
}

@injectable()
class OtherComponent {
  public value: number = 42;
}

// eslint-disable-next-line max-statements
describe("DefaultManager", () => {
  it("register a single component", async () => {
    const manager = createManager();
    manager.registerComponent(COMPONENT_ROLE, async () => TestComponent);

    const resolver = await manager.build();
    const instance = await resolver.getAsync<TestComponent>(COMPONENT_ROLE);

    expect(instance).toBeInstanceOf(TestComponent);
    expect(instance.name).toBe("test");
  });

  it("register multiple components with different roles", async () => {
    const manager = createManager();
    manager
      .registerComponent(COMPONENT_ROLE, async () => TestComponent)
      .registerComponent(OTHER_ROLE, async () => OtherComponent);

    const resolver = await manager.build();
    const testComponent =
      await resolver.getAsync<TestComponent>(COMPONENT_ROLE);
    const otherComponent = await resolver.getAsync<OtherComponent>(OTHER_ROLE);

    expect(testComponent).toBeInstanceOf(TestComponent);
    expect(otherComponent).toBeInstanceOf(OtherComponent);
  });

  it("support named components", async () => {
    const manager = createManager();
    manager
      .registerComponent(COMPONENT_ROLE, async () => TestComponent, {
        name: "default",
      })
      .registerComponent(COMPONENT_ROLE, async () => TestComponent, {
        name: "custom-name",
      });

    const resolver = await manager.build();
    const defaultInstance =
      await resolver.getAsync<TestComponent>(COMPONENT_ROLE);
    const namedInstance = await resolver.getAsync<TestComponent>(
      COMPONENT_ROLE,
      "custom-name",
    );

    expect(defaultInstance).toBeInstanceOf(TestComponent);
    expect(namedInstance).toBeInstanceOf(TestComponent);
  });

  it("support component priority", async () => {
    const manager = createManager();
    manager
      .registerComponent(COMPONENT_ROLE, async () => TestComponent, {
        priority: 1,
      })
      .registerComponent(COMPONENT_ROLE, async () => TestComponent, {
        priority: 100,
      });

    const resolver = await manager.build();
    const instance = await resolver.getAsync<TestComponent>(COMPONENT_ROLE);

    expect(instance).toBeInstanceOf(TestComponent);
  });

  it("throw error when registering duplicate component with same role, name and priority", () => {
    const manager = createManager();
    manager.registerComponent(COMPONENT_ROLE, async () => TestComponent);

    expect(() => {
      manager.registerComponent(COMPONENT_ROLE, async () => TestComponent);
    }).toThrow();
  });

  // eslint-disable-next-line max-statements
  it("perform constructor injection on lazily-loaded components", async () => {
    const DEPENDENCY_ROLE = Symbol("Dependency");
    const CONSUMER_ROLE = Symbol("Consumer");

    @injectable()
    class Dependency {
      public readonly tag = "dep";
    }

    @injectable()
    class Consumer {
      constructor(
        @inject(DEPENDENCY_ROLE) public readonly dependency: Dependency,
      ) {}
    }

    let depLoaded = 0;
    let consumerLoaded = 0;

    const manager = createManager();
    manager
      .registerComponent(DEPENDENCY_ROLE, async () => {
        depLoaded++;
        return Dependency;
      })
      .registerComponent(CONSUMER_ROLE, async () => {
        consumerLoaded++;
        return Consumer;
      });

    const resolver = await manager.build();
    expect(depLoaded).toBe(0);
    expect(consumerLoaded).toBe(0);

    const consumer = await resolver.getAsync<Consumer>(CONSUMER_ROLE);

    expect(consumer).toBeInstanceOf(Consumer);
    // Constructor injection wired the dependency:
    expect(consumer.dependency).toBeInstanceOf(Dependency);
    expect(consumer.dependency.tag).toBe("dep");
    // Both modules were loaded transitively, each exactly once:
    expect(consumerLoaded).toBe(1);
    expect(depLoaded).toBe(1);

    // Same singleton dependency when resolved directly:
    const depDirect = await resolver.getAsync<Dependency>(DEPENDENCY_ROLE);
    expect(depDirect).toBe(consumer.dependency);
  });

  it("only loads a component module on first request", async () => {
    let loadCount = 0;
    const loader = async () => {
      loadCount++;
      return TestComponent;
    };

    const manager = createManager();
    manager.registerComponent(COMPONENT_ROLE, loader);

    const resolver = await manager.build();
    expect(loadCount).toBe(0);

    await resolver.getAsync<TestComponent>(COMPONENT_ROLE);
    expect(loadCount).toBe(1);

    await resolver.getAsync<TestComponent>(COMPONENT_ROLE);
    expect(loadCount).toBe(1);
  });

  describe("getAsync", () => {
    it("return singleton instances", async () => {
      const manager = createManager();
      manager.registerComponent(COMPONENT_ROLE, async () => TestComponent);

      const resolver = await manager.build();
      const instance1 = await resolver.getAsync<TestComponent>(COMPONENT_ROLE);
      const instance2 = await resolver.getAsync<TestComponent>(COMPONENT_ROLE);

      expect(instance1).toBe(instance2);
    });

    it("throw error for unregistered role", async () => {
      const manager = createManager();
      const resolver = await manager.build();

      await expect(
        resolver.getAsync<TestComponent>(COMPONENT_ROLE),
      ).rejects.toThrow();
    });
  });

  describe("getAllAsync", () => {
    it("retrieve all components with given role", async () => {
      const manager = createManager();
      manager
        .registerComponent(COMPONENT_ROLE, async () => TestComponent, {
          name: "one",
        })
        .registerComponent(COMPONENT_ROLE, async () => TestComponent, {
          name: "two",
        });

      const resolver = await manager.build();
      const instances =
        await resolver.getAllAsync<TestComponent>(COMPONENT_ROLE);

      expect(instances).toHaveLength(2);
      expect(instances.every((i) => i instanceof TestComponent)).toBe(true);
    });

    it("return empty array for role with no components", async () => {
      const manager = createManager();
      const resolver = await manager.build();

      const instances =
        await resolver.getAllAsync<TestComponent>(COMPONENT_ROLE);

      expect(instances).toEqual([]);
    });

    it("return all instances for a role regardless of name", async () => {
      const manager = createManager();
      manager
        .registerComponent(COMPONENT_ROLE, async () => TestComponent, {
          name: "named1",
        })
        .registerComponent(COMPONENT_ROLE, async () => TestComponent, {
          name: "named2",
        })
        .registerComponent(COMPONENT_ROLE, async () => TestComponent, {
          name: "default",
        });

      const resolver = await manager.build();
      const instances =
        await resolver.getAllAsync<TestComponent>(COMPONENT_ROLE);

      expect(instances).toHaveLength(3);
    });
  });

  describe("priority semantics", () => {
    it("resolves to the registration with the lowest priority value", async () => {
      @injectable()
      class Loser {
        public readonly tag = "loser";
      }
      @injectable()
      class Winner {
        public readonly tag = "winner";
      }

      const manager = createManager();
      manager
        .registerComponent(COMPONENT_ROLE, async () => Loser, { priority: 100 })
        .registerComponent(COMPONENT_ROLE, async () => Winner, { priority: 1 });

      const resolver = await manager.build();
      const instance = await resolver.getAsync<{ tag: string }>(COMPONENT_ROLE);

      expect(instance.tag).toBe("winner");
      expect(instance).toBeInstanceOf(Winner);
    });

    // eslint-disable-next-line max-statements
    it("silently drops higher-priority registrations for the same (role, name)", async () => {
      let loserLoaded = 0;
      let winnerLoaded = 0;

      @injectable()
      class Loser {}
      @injectable()
      class Winner {}

      const manager = createManager();
      manager
        .registerComponent(
          COMPONENT_ROLE,
          async () => {
            loserLoaded++;
            return Loser;
          },
          { priority: 100 },
        )
        .registerComponent(
          COMPONENT_ROLE,
          async () => {
            winnerLoaded++;
            return Winner;
          },
          { priority: 1 },
        );

      const resolver = await manager.build();
      await resolver.getAsync(COMPONENT_ROLE);
      await resolver.getAllAsync(COMPONENT_ROLE);

      expect(winnerLoaded).toBe(1);
      // The loser's loader is captured by bindAllLazy but never invoked because
      // its priority lost. Pinning this behavior so future authors realize the
      // override is silent.
      expect(loserLoaded).toBe(0);
    });
  });

  describe("duplicate-registration error message", () => {
    // eslint-disable-next-line max-statements
    it("formats the offending registration with its symbol, name and priority", () => {
      const manager = createManager();
      manager.registerComponent(COMPONENT_ROLE, async () => TestComponent, {
        name: "alpha",
        priority: 42,
      });

      let caught: unknown;
      try {
        manager.registerComponent(COMPONENT_ROLE, async () => TestComponent, {
          name: "alpha",
          priority: 42,
        });
      } catch (e) {
        caught = e;
      }

      expect(caught).toBeInstanceOf(Error);
      const message = (caught as Error).message;
      expect(message).not.toContain("[object Object]");
      expect(message).toContain("TestComponent");
      expect(message).toContain("alpha");
      expect(message).toContain("42");
    });
  });

  describe("singleton identity across resolution paths", () => {
    it("returns the same instance via default, named and getAllAsync lookups", async () => {
      const manager = createManager();
      manager.registerComponent(COMPONENT_ROLE, async () => TestComponent);

      const resolver = await manager.build();
      const viaDefault = await resolver.getAsync<TestComponent>(COMPONENT_ROLE);
      const viaNamed = await resolver.getAsync<TestComponent>(
        COMPONENT_ROLE,
        "default",
      );
      const viaAll = await resolver.getAllAsync<TestComponent>(COMPONENT_ROLE);

      expect(viaAll).toHaveLength(1);
      // All three resolution paths must return the same singleton instance.
      expect(viaNamed).toBe(viaDefault);
      expect(viaAll[0]).toBe(viaDefault);
    });
  });
});

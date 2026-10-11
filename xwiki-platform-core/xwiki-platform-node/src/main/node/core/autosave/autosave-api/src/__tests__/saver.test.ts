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

import { SAVE_DELAY, SAVE_INTERVAL } from "../constants";
import { SaveStatus } from "../saveStatus";
import { SaveTarget } from "../saveTarget";
import { SaveTransport } from "../saveTransport";
import { Saver } from "../saver";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { SaveResult } from "../saveTarget";
import type { SaverState } from "../saverState";

type TestContext = { priority?: number };

/**
 * A transport that keeps the states in memory, so that the tests can inject the states of the other clients and
 * observe what the saver publishes.
 */
class FakeTransport extends SaveTransport<TestContext> {
  public readonly states: Record<string, SaverState> = {};

  public readonly disconnected: Set<string> = new Set();

  public disposed: boolean = false;

  public pushCount: number = 0;

  public constructor(
    saver: Saver<TestContext>,
    private readonly clientId: string = "alice",
  ) {
    super(saver);
    this.states[clientId] = this.state;
  }

  public override initialize(): void {}

  public override async toBeReady(): Promise<void> {}

  public override getClientId(): string {
    return this.clientId;
  }

  public override getStates(): Record<string, SaverState> {
    return this.states;
  }

  public override async updateLocalState(
    patch: Partial<SaverState>,
    options?: { push?: boolean; immediate?: boolean },
  ): Promise<void> {
    Object.assign(this.state, patch);
    this.states[this.clientId] = this.state;
    if (options?.push) {
      this.pushCount++;
    }
  }

  public override isConnected(state: SaverState): boolean {
    const clientId = Object.keys(this.states).find(
      (key) => this.states[key] === state,
    );
    return !clientId || !this.disconnected.has(clientId);
  }

  public override async dispose(): Promise<void> {
    this.disposed = true;
  }

  /**
   * Simulate the arrival of the state of another client.
   */
  public receive(clientId: string, state: SaverState): void {
    this.states[clientId] = state;
    this.saver.onRemoteStatesChanged();
  }
}

/**
 * A target that records the saves it was asked to perform.
 */
class FakeTarget extends SaveTarget<TestContext> {
  public readonly submitted: TestContext[] = [];

  public disposed: boolean = false;

  public result: SaveResult = { version: "1.2" };

  public failure?: Error;

  public saveInterval?: number;

  public override async initialize(): Promise<void> {}

  public override getSaveInterval(): number | undefined {
    return this.saveInterval;
  }

  public override getSavePriority(context: TestContext): number {
    return context.priority ?? 1;
  }

  public override async submit(context: TestContext): Promise<SaveResult> {
    this.submitted.push(context);
    if (this.failure) {
      throw this.failure;
    }
    return this.result;
  }

  public override onStatesChanged(): void {}

  public override dispose(): void {
    this.disposed = true;
  }
}

let transport: FakeTransport;
let target: FakeTarget;
let localStatuses: SaveStatus[];
let globalStatuses: SaveStatus[];

function createSaver(clientId: string = "alice"): Saver<TestContext> {
  return new Saver<TestContext>(
    {
      onLocalStatusChange: (status) => localStatuses.push(status),
      onStatusChange: (status) => globalStatuses.push(status),
    },
    (saver) => (transport = new FakeTransport(saver, clientId)),
    (saver) => (target = new FakeTarget(saver)),
  );
}

describe("Saver", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    // The saver adds a random amount to the save interval. Remove it so that the scheduling tests can advance the
    // timers by an exact amount.
    vi.spyOn(Math, "random").mockReturnValue(0);
    localStatuses = [];
    globalStatuses = [];
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe("dirty state", () => {
    it("is clean until the content is modified", async () => {
      const saver = createSaver();
      await saver.toBeReady();

      expect(saver.isDirty()).toBe(false);

      saver.contentModifiedLocally();

      expect(saver.isDirty()).toBe(true);
      expect(transport.getLocalState().updateCount).toBe(1);
    });

    it("becomes clean when another client reports having saved our changes", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();

      transport.receive("bob", { savedUpdateCount: { alice: 1 } });

      expect(saver.isDirty()).toBe(false);
    });

    it("stays dirty when another client only saved earlier changes", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();
      saver.contentModifiedLocally();

      transport.receive("bob", { savedUpdateCount: { alice: 1 } });

      expect(saver.isDirty()).toBe(true);
    });

    it("remembers the saved update count after the client that saved leaves", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();
      transport.receive("bob", { savedUpdateCount: { alice: 1 } });
      expect(saver.isDirty()).toBe(false);

      // Bob leaves the editing session, taking with him the only state that recorded our save.
      delete transport.states.bob;
      saver.onRemoteStatesChanged();

      expect(saver.isDirty()).toBe(false);
    });
  });

  describe("scheduling", () => {
    it("auto-saves once the save interval is reached", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();

      await vi.advanceTimersByTimeAsync(SAVE_INTERVAL - 1);
      expect(target.submitted).toHaveLength(0);

      await vi.advanceTimersByTimeAsync(1 + SAVE_DELAY);
      expect(target.submitted).toHaveLength(1);
    });

    it("saves right away when the content has been dirty for longer than the save interval", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();

      // Keep typing, which reschedules the save, until past the interval.
      await vi.advanceTimersByTimeAsync(SAVE_INTERVAL - 1);
      saver.contentModifiedLocally();
      expect(target.submitted).toHaveLength(0);

      await vi.advanceTimersByTimeAsync(2);
      saver.contentModifiedLocally();

      // The save was not deferred by another full interval.
      await vi.advanceTimersByTimeAsync(SAVE_DELAY);
      expect(target.submitted).toHaveLength(1);
    });

    it("does not auto-save when nothing is dirty", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();
      transport.receive("bob", { savedUpdateCount: { alice: 1 } });

      await vi.advanceTimersByTimeAsync(SAVE_INTERVAL + SAVE_DELAY);

      expect(target.submitted).toHaveLength(0);
    });

    it("uses the save interval asked for by the target", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      target.saveInterval = 5000;
      saver.contentModifiedLocally();

      await vi.advanceTimersByTimeAsync(5000 - 1);
      expect(target.submitted).toHaveLength(0);

      await vi.advanceTimersByTimeAsync(1 + SAVE_DELAY);
      expect(target.submitted).toHaveLength(1);
    });

    it("picks up a save interval that changes during the editing session", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();

      // The interval is read each time a save is scheduled, so the new value applies to the next one.
      target.saveInterval = 5000;
      saver.contentModifiedLocally();

      await vi.advanceTimersByTimeAsync(5000 + SAVE_DELAY);
      expect(target.submitted).toHaveLength(1);
    });

    it("adds a random amount to the save interval", async () => {
      vi.spyOn(Math, "random").mockReturnValue(0.5);
      const saver = createSaver();
      await saver.toBeReady();
      target.saveInterval = 5000;
      saver.contentModifiedLocally();

      // Half of a tenth of the interval on top of it.
      await vi.advanceTimersByTimeAsync(5250 - 1);
      expect(target.submitted).toHaveLength(0);

      await vi.advanceTimersByTimeAsync(1 + SAVE_DELAY);
      expect(target.submitted).toHaveLength(1);
    });

    it("schedules a new attempt when the save fails", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();
      target.failure = new Error("Save failed.");

      await vi.advanceTimersByTimeAsync(SAVE_INTERVAL + SAVE_DELAY);
      expect(target.submitted).toHaveLength(1);

      target.failure = undefined;
      await vi.advanceTimersByTimeAsync(SAVE_INTERVAL + SAVE_DELAY);
      expect(target.submitted).toHaveLength(2);
    });
  });

  describe("save election", () => {
    it("saves when this client has the highest priority", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();
      transport.states.bob = { saving: 1, dirty: true };

      const saving = saver.save({ priority: 3 });
      await vi.advanceTimersByTimeAsync(SAVE_DELAY);
      await saving;

      expect(target.submitted).toEqual([{ priority: 3 }]);
    });

    it("lets the client with the highest priority save", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();
      transport.states.bob = { saving: 3, dirty: true };

      const saving = saver.save({ priority: 1 });
      await vi.advanceTimersByTimeAsync(SAVE_DELAY);
      await saving;

      expect(target.submitted).toHaveLength(0);
    });

    it("breaks a priority tie on the lowest client id", async () => {
      const saver = createSaver("zoe");
      await saver.toBeReady();
      saver.contentModifiedLocally();
      transport.states.bob = { saving: 1, dirty: true };

      const saving = saver.save({ priority: 1 });
      await vi.advanceTimersByTimeAsync(SAVE_DELAY);
      await saving;

      // "bob" sorts before "zoe", so bob saves.
      expect(target.submitted).toHaveLength(0);
    });

    it("ignores the clients that left the editing session", async () => {
      const saver = createSaver("zoe");
      await saver.toBeReady();
      saver.contentModifiedLocally();
      transport.states.bob = { saving: 3, dirty: true };
      transport.disconnected.add("bob");

      const saving = saver.save({ priority: 1 });
      await vi.advanceTimersByTimeAsync(SAVE_DELAY);
      await saving;

      expect(target.submitted).toHaveLength(1);
    });

    it("records the update counts of every client and the created version", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();
      transport.states.bob = { updateCount: 7 };

      const saving = saver.save({ priority: 3 });
      await vi.advanceTimersByTimeAsync(SAVE_DELAY);
      await saving;

      expect(transport.getLocalState().savedUpdateCount).toEqual({
        alice: 1,
        bob: 7,
      });
      expect(transport.getLocalState().version).toBe("1.2");
      expect(transport.getLocalState().saving).toBe(0);
    });

    it("does not record a version when the save created none", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();
      target.result = {};

      const saving = saver.save({ priority: 3 });
      await vi.advanceTimersByTimeAsync(SAVE_DELAY);
      await saving;

      expect(transport.getLocalState().version).toBeUndefined();
    });

    it("rejects and clears the saving flag when the save fails", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();
      target.failure = new Error("Save failed.");

      // Assert on the rejection before advancing the timers, so that the rejection handler is attached before the
      // save actually fails.
      const saving = expect(saver.save({ priority: 3 })).rejects.toThrow(
        "Save failed.",
      );
      await vi.advanceTimersByTimeAsync(SAVE_DELAY);
      await saving;

      expect(saver.isSaving()).toBe(false);
    });
  });

  describe("status reporting", () => {
    it("reports becoming dirty only once, however many changes are made", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      expect(localStatuses).toEqual([SaveStatus.Clean]);

      saver.contentModifiedLocally();
      saver.contentModifiedLocally();

      expect(localStatuses).toEqual([SaveStatus.Clean, SaveStatus.Dirty]);
    });

    it("reports saving and then clean around a save", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();

      const saving = saver.save({ priority: 3 });
      await vi.advanceTimersByTimeAsync(SAVE_DELAY);
      await saving;

      expect(localStatuses).toEqual([
        SaveStatus.Clean,
        SaveStatus.Dirty,
        SaveStatus.Saving,
        SaveStatus.Clean,
      ]);
    });

    it("reports the global status of the editing session", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      expect(globalStatuses).toEqual([SaveStatus.Clean]);

      // Another client has unsaved changes, even though we have none.
      transport.receive("bob", { dirty: true });

      expect(saver.isDirty()).toBe(false);
      expect(globalStatuses).toEqual([SaveStatus.Clean, SaveStatus.Dirty]);
    });

    it("ignores the disconnected clients in the global status", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      transport.disconnected.add("bob");

      transport.receive("bob", { dirty: true });

      expect(globalStatuses).toEqual([SaveStatus.Clean]);
    });
  });

  describe("stop", () => {
    it("pushes the pending state and disposes the transport and the target", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      const pushCount = transport.pushCount;

      await saver.stop();

      expect(transport.pushCount).toBe(pushCount + 1);
      expect(transport.disposed).toBe(true);
      expect(target.disposed).toBe(true);
    });

    it("does not schedule a save anymore", async () => {
      const saver = createSaver();
      await saver.toBeReady();
      saver.contentModifiedLocally();
      await saver.stop();

      await vi.advanceTimersByTimeAsync(SAVE_INTERVAL + SAVE_DELAY);

      expect(target.submitted).toHaveLength(0);
    });
  });
});

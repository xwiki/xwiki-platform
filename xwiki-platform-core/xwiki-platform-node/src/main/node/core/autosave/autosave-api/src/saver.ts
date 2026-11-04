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

import { SAVE_DELAY, SAVE_INTERVAL } from "./constants";
import { SaveStatus } from "./saveStatus";
import type { SaveTarget } from "./saveTarget";
import type { SaveTransport } from "./saveTransport";
import type { SaverState } from "./saverState";
import type { Logger } from "@xwiki/platform-api";

/**
 * How the saver reports what it is doing. Its save status is reported to the listeners added with
 * {@link Saver.addLocalStatusListener} and {@link Saver.addStatusListener}.
 *
 * @since 18.8.0RC1
 * @beta
 */
type SaverConfig = {
  /**
   * Where to report what the saver is doing. The caller passes the logger it resolved from the component manager,
   * with the module name it wants the messages attributed to.
   */
  logger?: Logger;
};

/**
 * Generic auto-saver that keeps track of the local update count and schedules saves when the content is modified.
 * The way the saver states are synchronized between the clients is delegated to a {@link SaveTransport} and the way
 * the content is saved is delegated to a {@link SaveTarget}.
 *
 * @typeParam C - the type of the save context, defined by the target
 * @since 18.8.0RC1
 * @beta
 */
class Saver<C extends object = object> {
  private readonly localStatusListeners: Set<(status: SaveStatus) => void> =
    new Set();

  private readonly statusListeners: Set<(status: SaveStatus) => void> =
    new Set();

  private readonly logger?: Logger;

  private readonly transport: SaveTransport<C>;

  private readonly target: SaveTarget<C>;

  /**
   * The highest number of local changes that we know have been saved, by us or by another client. We remember the
   * highest value ever seen rather than looking it up in the saver states each time, because the state of the client
   * that performed the save can disappear (e.g. when that client leaves the editing session) and we would then
   * wrongly consider our changes unsaved.
   */
  private savedUpdateCount: number = 0;

  /**
   * Whether this saver was stopped, in which case no new save must be scheduled.
   */
  private stopped: boolean = false;

  private saveTimer?: ReturnType<typeof setTimeout>;

  /**
   * The last save that was started, settled (never rejected) once its result has been published to the other clients.
   */
  private lastSave?: Promise<void>;

  private dirtyTimestamp?: number;

  private previousLocalStatus?: SaveStatus;

  private previousGlobalStatus?: SaveStatus;

  /**
   * @param config - how the saver reports what it is doing
   * @param createTransport - creates the transport used to synchronize the saver states, called with this saver
   * @param createTarget - creates the target used to save the edited content, called with this saver
   */
  public constructor(
    config: SaverConfig,
    createTransport: (saver: Saver<C>) => SaveTransport<C>,
    createTarget: (saver: Saver<C>) => SaveTarget<C>,
  ) {
    this.logger = config.logger;

    this.transport = createTransport(this);
    this.target = createTarget(this);

    // Connect only after both the transport and the target have been created, because the transport starts notifying
    // us as soon as it is initialized.
    this.transport.initialize();
  }

  /**
   * Gets the current save status of the local client.
   *
   * @returns the save status of the local client, or undefined if it is not known yet
   * @since 18.9.0RC1
   */
  public getLocalStatus(): SaveStatus | undefined {
    return this.previousLocalStatus;
  }

  /**
   * Adds a listener notified when the save status of the local client changes. The transports deliver the saver
   * states asynchronously, so a listener added right after the saver is created misses no change.
   *
   * @param listener - called with the new save status
   * @returns a function that removes the listener
   * @since 18.9.0RC1
   */
  public addLocalStatusListener(
    listener: (status: SaveStatus) => void,
  ): () => void {
    this.localStatusListeners.add(listener);
    return () => this.localStatusListeners.delete(listener);
  }

  /**
   * Gets the current save status of the editing session as a whole.
   *
   * @returns the save status of the editing session, i.e. taking every connected client into account, or undefined
   *   if it is not known yet
   * @since 18.9.0RC1
   */
  public getStatus(): SaveStatus | undefined {
    return this.previousGlobalStatus;
  }

  /**
   * Adds a listener notified when the save status of the editing session as a whole changes, i.e. taking every
   * connected client into account. The transports deliver the saver states asynchronously, so a listener added
   * right after the saver is created misses no change.
   *
   * @param listener - called with the new save status
   * @returns a function that removes the listener
   * @since 18.9.0RC1
   */
  public addStatusListener(listener: (status: SaveStatus) => void): () => void {
    this.statusListeners.add(listener);
    return () => this.statusListeners.delete(listener);
  }

  /**
   * @returns a promise that resolves when the saver is connected and ready to save
   */
  public async toBeReady(): Promise<void> {
    await this.transport.toBeReady();
    await this.target.initialize();
    this.notifyStatusChange();
  }

  /**
   * Called each time the edited content is modified locally.
   */
  public contentModifiedLocally(): void {
    const updateCount = (this.transport.getLocalState().updateCount ?? 0) + 1;
    this.updateState({ updateCount }, true);
    this.scheduleSave();
  }

  /**
   * @returns whether the local content has changes that no client has saved yet
   */
  public isDirty(): boolean {
    return !!this.transport.getLocalState().dirty;
  }

  /**
   * @returns whether this client is currently saving
   */
  public isSaving(): boolean {
    return !!this.transport.getLocalState().saving;
  }

  /**
   * @returns the identifier of the local client
   */
  public getClientId(): string {
    return this.transport.getClientId();
  }

  /**
   * @returns a promise that resolves when the local state has been received by the other clients, including the
   *   result of the save in progress, if any
   */
  public async whenSettled(): Promise<void> {
    // A save publishes its result (e.g. the version it created) only once the save target reports it, and the save
    // target may need to know when the result was received before that (e.g. to leave the edit mode right after the
    // document is saved). Leaving before the save result is published would prevent the other clients from learning
    // about the new version, leading to merge conflicts on their next save.
    await this.lastSave;
    await this.transport.whenSettled();
  }

  /**
   * Called by the transport when the saver states of the other clients have changed.
   */
  public onRemoteStatesChanged(): void {
    this.updateState();
  }

  /**
   * Save the edited content, provided that this client wins the save election.
   *
   * @param context - the save context, e.g. telling whether the user asked for the save; defaults to the context the
   *   target gives to the auto-saves
   * @returns a promise that rejects if the content could not be saved
   */
  public save(context?: C): Promise<void> {
    const saving = this.saveAndPublish(context);
    this.lastSave = (async () => {
      try {
        await saving;
      } catch {
        // The callers waiting for the save result to be published don't care whether the save succeeded.
      }
    })();
    return saving;
  }

  private async saveAndPublish(context?: C): Promise<void> {
    const saveContext = context ?? this.target.getAutoSaveContext();

    // Let the others know immediately that we are saving, in order to reduce concurrent saves.
    this.updateState(
      { saving: this.target.getSavePriority(saveContext) },
      true,
      true,
    );

    try {
      await this.saveIfElected(saveContext);
    } catch (error) {
      this.logger?.warn("Failed to save.", error);
      // Let the caller know that the content has not been saved.
      throw error;
    } finally {
      this.afterSave();
    }
  }

  /**
   * Perform the save, but only if this client won the save election.
   */
  private async saveIfElected(context: C): Promise<void> {
    const savingClientId = await this.getSavingClientId();
    if (savingClientId !== this.transport.getClientId()) {
      return;
    }

    const savedUpdateCount = this.getUpdateCounts();
    this.logger?.debug("Saving ", savedUpdateCount);

    const { version } = await this.target.submit(context);
    // Record the save result locally: afterSave() propagates it to the other clients.
    await this.transport.updateLocalState(
      version ? { savedUpdateCount, version } : { savedUpdateCount },
    );
  }

  private afterSave(): void {
    // Propagate the state immediately after the save attempt because the user may leave the edit mode and this will
    // close the connection.
    this.updateState({ saving: 0 }, true, true);

    if (this.isDirty()) {
      // The content is still dirty, either because the save failed or because another client was elected to save and
      // didn't manage to save yet. Schedule a new save attempt.
      this.scheduleSave();
    }
  }

  /**
   * Stop the auto-save, e.g. when the user leaves the edit mode or the connection is closed.
   */
  public async stop(): Promise<void> {
    this.stopped = true;
    // Cancel the scheduled save.
    clearTimeout(this.saveTimer);

    try {
      // Push uncommitted changes to the server before disconnecting.
      await this.transport.updateLocalState(
        {},
        { push: true, immediate: true },
      );
    } catch (error) {
      // Disconnect anyway, otherwise we would keep listening to a channel we're leaving.
      this.logger?.warn(
        "Failed to push the local state before stopping.",
        error,
      );
    } finally {
      await this.transport.dispose();
      this.target.dispose();
    }
  }

  private scheduleSave(): void {
    // Cancel the previous scheduled save.
    clearTimeout(this.saveTimer);
    if (this.stopped) {
      // Don't schedule a new save after the saver was stopped (e.g. when the user leaves the edit mode).
      return;
    }
    const saveInterval = this.getSaveInterval();
    if (
      !this.dirtyTimestamp ||
      Date.now() - this.dirtyTimestamp < saveInterval
    ) {
      this.saveTimer = setTimeout(this.maybeSave.bind(this), saveInterval);
    } else {
      // Save right away because too much time has passed since the last time the content became dirty.
      void this.maybeSave();
    }
  }

  /**
   * @returns how long to wait before the next save, the interval the target asks for (or the default) plus a small
   *   random amount, so that the clients of a session don't all save at the same time
   */
  private getSaveInterval(): number {
    const base = this.target.getSaveInterval() ?? SAVE_INTERVAL;
    return base + Math.random() * (base / 10);
  }

  /**
   * Recompute the local state, optionally propagating it to the other clients.
   *
   * @param patch - the local state properties to modify
   * @param push - whether to propagate the new state to the other clients
   * @param immediate - whether to wait for the new state to reach the other clients
   */
  private updateState(
    patch?: Partial<SaverState>,
    push?: boolean,
    immediate?: boolean,
  ): void {
    const localState = { ...this.transport.getLocalState(), ...patch };
    const wasDirty = !!this.transport.getLocalState().dirty;
    const dirty = this.isLocalStateDirty(localState);
    // Notify immediately that the content is clean, otherwise, if the user saving the content is not the one that
    // made the changes, the save status will remain dirty after the save success notification.
    const becameClean = wasDirty && !dirty;
    this.updateDirtyTimestamp(wasDirty, dirty);
    // We don't wait for the new state to reach the other clients because the callers don't depend on it.
    void this.transport.updateLocalState(
      { ...patch, dirty },
      { push: push || becameClean, immediate: immediate || becameClean },
    );

    this.notifyStatusChange();
    this.target.onStatesChanged(
      this.transport.getStates(),
      this.transport.getClientId(),
    );
  }

  /**
   * Remember when the content became dirty, so that a save that is already overdue is not deferred again.
   */
  private updateDirtyTimestamp(wasDirty: boolean, dirty: boolean): void {
    if (wasDirty !== dirty) {
      if (!wasDirty) {
        // Remember the last time when the content became dirty in order to be able to save immediately when the save
        // interval is reached (even if the user is still making changes).
        this.dirtyTimestamp = Date.now();
      }
    } else if (this.isSomeoneSaving()) {
      // Avoid auto-saving more often than the save interval. It's possible that the save interval is reached for
      // multiple users that are editing at the same time. In this case the auto-save should be triggered for only
      // one of them. For the others the auto-save should be delayed until the save interval is reached again.
      delete this.dirtyTimestamp;
    }
  }

  /**
   * @param localState - the new local state
   * @returns whether the local content has changes that no client has saved yet
   */
  private isLocalStateDirty(localState: SaverState): boolean {
    const clientId = this.transport.getClientId();
    for (const state of Object.values(this.transport.getStates())) {
      this.savedUpdateCount = Math.max(
        this.savedUpdateCount,
        state.savedUpdateCount?.[clientId] ?? 0,
      );
    }
    return (localState.updateCount ?? 0) > this.savedUpdateCount;
  }

  private notifyStatusChange(): void {
    const localState = this.transport.getLocalState();
    const localStatus = localState.saving
      ? SaveStatus.Saving
      : localState.dirty
        ? SaveStatus.Dirty
        : SaveStatus.Clean;
    if (this.previousLocalStatus !== localStatus) {
      this.previousLocalStatus = localStatus;
      this.notifyListeners(this.localStatusListeners, localStatus);
    }

    const globalStatus = this.isSomeoneSaving()
      ? SaveStatus.Saving
      : this.isSomeoneDirty()
        ? SaveStatus.Dirty
        : SaveStatus.Clean;
    if (this.previousGlobalStatus !== globalStatus) {
      this.previousGlobalStatus = globalStatus;
      this.notifyListeners(this.statusListeners, globalStatus);
    }
  }

  /**
   * Call the given listeners, making sure that a failing listener doesn't prevent the others from being called, nor
   * breaks the saver.
   */
  private notifyListeners<T>(listeners: Set<(value: T) => void>, value: T) {
    for (const listener of listeners) {
      try {
        listener(value);
      } catch (error) {
        this.logger?.warn("A saver listener failed.", error);
      }
    }
  }

  private async maybeSave(): Promise<void> {
    if (!this.isSomeoneSaving() && this.isSomeoneDirty()) {
      try {
        await this.save();
      } catch {
        // The auto-save failure is already logged by the saver and a new save attempt is scheduled.
      }
    }
  }

  private isSomeoneSaving(): boolean {
    return this.someState(
      (state) => !!state.saving && this.transport.isConnected(state),
    );
  }

  private isSomeoneDirty(): boolean {
    return this.someState(
      (state) => !!state.dirty && this.transport.isConnected(state),
    );
  }

  private someState(predicate: (state: SaverState) => boolean): boolean {
    return Object.values(this.transport.getStates()).some(predicate);
  }

  private getConnectedStates(): Record<string, SaverState> {
    return Object.fromEntries(
      Object.entries(this.transport.getStates()).filter(([, state]) =>
        this.transport.isConnected(state),
      ),
    );
  }

  /**
   * The auto-save can be triggered on multiple clients at the same time (i.e. multiple clients can set their own
   * saving flag before they received the saving flag from the other clients). This method is used to determine which
   * client should save the content in this case. By default the client with the highest save priority and the lowest
   * id (in alphabetical order) wins.
   *
   * @returns the id of the client that should save the content
   */
  private getSavingClientId(): Promise<string | undefined> {
    return new Promise((resolve) => {
      setTimeout(() => {
        // Initialize with minimum save priority.
        let savePriority = 1;
        let savingClientId: string | undefined;
        for (const [clientId, state] of Object.entries(
          this.getConnectedStates(),
        )) {
          const saving = state.saving ?? 0;
          if (
            saving > savePriority ||
            (saving === savePriority &&
              (!savingClientId || savingClientId > clientId))
          ) {
            savePriority = saving;
            savingClientId = clientId;
          }
        }
        resolve(savingClientId);
      }, SAVE_DELAY);
    });
  }

  private getUpdateCounts(): Record<string, number> {
    const updateCounts: Record<string, number> = {};
    for (const [clientId, state] of Object.entries(
      this.transport.getStates(),
    )) {
      updateCounts[clientId] = state.updateCount ?? 0;
    }
    return updateCounts;
  }
}

export { Saver };
export type { SaverConfig };

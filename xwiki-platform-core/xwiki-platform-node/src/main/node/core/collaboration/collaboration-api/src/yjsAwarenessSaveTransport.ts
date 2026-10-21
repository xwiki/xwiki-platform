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

import { SaveTransport } from "@xwiki/platform-autosave-api";
import type { Collaboration } from "./collaboration";
import type { Logger } from "@xwiki/platform-api";
import type { Saver, SaverState } from "@xwiki/platform-autosave-api";
import type { Awareness } from "y-protocols/awareness";

/**
 * The awareness field carrying the saver state of a client.
 */
const SAVER_STATE_FIELD = "saver";

/**
 * How often to check whether the WebSocket send buffer has drained, in milliseconds.
 */
const SETTLE_POLL_INTERVAL = 20;

/**
 * How long to wait for the WebSocket send buffer to drain before giving up, in milliseconds.
 */
const SETTLE_TIMEOUT = 2000;

/**
 * What this transport needs from a Yjs collaboration provider. Typing it structurally, rather than importing a
 * specific provider, is what keeps this module usable with any of them (y-websocket, hocuspocus, ...).
 */
type YjsProvider = {
  awareness: Awareness;
  ws?: WebSocket | null;
  wsconnected?: boolean;
  on(event: "status", handler: (payload: { status: string }) => void): void;
  off(event: "status", handler: (payload: { status: string }) => void): void;
};

/**
 * How to synchronize the saver states over a Yjs collaboration session.
 *
 * @since 18.8.0RC1
 * @beta
 */
type YjsAwarenessSaveTransportConfig = {
  /**
   * The collaboration session whose clients take part in the save election.
   */
  collaboration: Collaboration;

  /**
   * Where to report what the transport is doing. The caller passes the logger it resolved from the component
   * manager, with the module name it wants the messages attributed to.
   */
  logger?: Logger;
};

/**
 * Replicates the saver states over the awareness of a Yjs collaboration session.
 *
 * The states travel over awareness rather than over the shared Yjs document on purpose. The XWiki Yjs WebSocket
 * end-point treats every document update as a content change by its sender and makes that sender the effective
 * script author of the room, so publishing a save status through the document would silently rewrite the effective
 * author and grant the room the script rights of whoever last reported being clean. Awareness updates carry no such
 * meaning. Awareness also drops the state of a client as soon as it disconnects, which is exactly the notion of
 * "still taking part in the editing session" that the saver needs.
 *
 * @typeParam C - the type of the save context the saver passes to its target
 * @since 18.8.0RC1
 * @beta
 */
class YjsAwarenessSaveTransport<
  C extends object = object,
> extends SaveTransport<C> {
  private readonly provider: YjsProvider;

  private readonly awareness: Awareness;

  private readonly logger?: Logger;

  private readonly revertList: (() => void)[] = [];

  /**
   * The remote saver states we last reported to the saver, serialized. The awareness state of a client is
   * re-broadcast whenever its caret moves, so this is what tells an actual saver state change from the cursor noise.
   */
  private remoteStates: string = "";

  /**
   * @param saver - the saver using this transport, notified when the remote states change
   * @param config - the collaboration session to synchronize the states over
   */
  public constructor(saver: Saver<C>, config: YjsAwarenessSaveTransportConfig) {
    super(saver);

    this.provider = config.collaboration.provider as YjsProvider;
    this.awareness = this.provider.awareness;
    this.logger = config.logger;
  }

  public override initialize(): void {
    // Take the baseline before listening, so that the first change is compared against the states of the clients
    // that were already there rather than against nothing, which would report a change that didn't happen.
    this.remoteStates = this.getRemoteStates();

    this.awareness.on("change", this.onAwarenessChange);
    this.revertList.push(() => {
      this.awareness.off("change", this.onAwarenessChange);
    });

    document.addEventListener("visibilitychange", this.onVisibilityChange);
    this.revertList.push(() => {
      document.removeEventListener("visibilitychange", this.onVisibilityChange);
    });
  }

  public override async toBeReady(): Promise<void> {
    if (!this.provider.wsconnected) {
      await new Promise<void>((resolve) => {
        const onStatus = ({ status }: { status: string }): void => {
          if (status === "connected") {
            this.provider.off("status", onStatus);
            resolve();
          }
        };
        this.provider.on("status", onStatus);
        this.revertList.push(() => {
          this.provider.off("status", onStatus);
        });
      });
    }

    // Let the other clients know that we joined the save election. Note that awareness has no counterpart to the
    // document synchronization event, so we cannot tell whether we already know every other client's state at this
    // point. It doesn't matter: the states of the room reach us right after we join, which is orders of magnitude
    // before the first save election.
    await this.updateLocalState({}, { push: true });
  }

  public override getClientId(): string {
    return `${this.awareness.clientID}`;
  }

  public override getStates(): Record<string, SaverState> {
    const states: Record<string, SaverState> = {};
    this.awareness.getStates().forEach((clientState, clientId) => {
      const saverState = clientState?.[SAVER_STATE_FIELD] as
        | SaverState
        | undefined;
      if (saverState) {
        states[`${clientId}`] = saverState;
      }
    });

    // Our own state is the one we hold, not the one we published: it can carry changes we haven't pushed yet, and
    // our awareness entry can be missing entirely (before the first push, or after the provider dropped it).
    states[this.getClientId()] = this.state;

    return states;
  }

  public override async updateLocalState(
    patch: Partial<SaverState>,
    { push, immediate }: { push?: boolean; immediate?: boolean } = {},
  ): Promise<void> {
    Object.assign(this.state, patch);
    if (push || immediate) {
      this.publish();
    }
    if (immediate) {
      await this.whenSettled();
    }
  }

  /**
   * The Yjs protocol has no acknowledgement that could be matched with a given awareness update: the server
   * broadcasts the update and tells its sender nothing back (it does echo it, but an echo whose clock is not greater
   * than the current one is ignored, so it fires no event). The strongest guarantee available to a client is
   * therefore that the bytes have left the browser, which is what this waits for. It does not mean that the other
   * clients have applied the update.
   *
   * @returns a promise that resolves when the local state has been sent, or when waiting for it has taken too long
   */
  public override whenSettled(): Promise<void> {
    const ws = this.provider.ws;
    if (!ws || ws.readyState !== ws.OPEN || !ws.bufferedAmount) {
      return Promise.resolve();
    }

    return new Promise((resolve) => {
      // Give up after a while, because the saver waits for this before leaving the edit mode and the edit form
      // target waits for it before redirecting: a socket that never drains must not block either.
      const deadline = Date.now() + SETTLE_TIMEOUT;
      const check = (): void => {
        if (!ws.bufferedAmount || Date.now() > deadline) {
          resolve();
        } else {
          setTimeout(check, SETTLE_POLL_INTERVAL);
        }
      };
      check();
    });
  }

  public override async dispose(): Promise<void> {
    this.revertList.forEach((revert) => revert());
    this.revertList.length = 0;
  }

  /**
   * Publish the local state to the other clients. Note that we publish a copy, because the awareness stores the
   * value by reference and we keep mutating our state: pushing the very object we mutate would rewrite what the
   * other clients see without the awareness clock ever noticing.
   */
  private publish(): void {
    if (this.awareness.getLocalState() === null) {
      // We're disconnected (or disposed), so we can only keep the new state locally.
      return;
    }
    // setLocalStateField merges, which is what lets the saver state coexist with the fields the editor publishes
    // (the collaborator and the cursor position).
    this.awareness.setLocalStateField(SAVER_STATE_FIELD, { ...this.state });
  }

  private readonly onAwarenessChange = (): void => {
    const remoteStates = this.getRemoteStates();
    if (remoteStates !== this.remoteStates) {
      this.remoteStates = remoteStates;
      this.logger?.debug("Received remote saver states: ", remoteStates);
      this.saver.onRemoteStatesChanged();
    }
  };

  private readonly onVisibilityChange = (): void => {
    if (document.visibilityState === "hidden") {
      // Push uncommitted changes to the server because when a document is hidden its window can be closed without
      // notice, so this might be the last chance to propagate our local state to the other collaborators.
      void this.updateLocalState({}, { push: true, immediate: true });
    }
  };

  /**
   * @returns the saver states of the other clients, serialized so that they can be compared with the ones we
   *   reported to the saver last time
   */
  private getRemoteStates(): string {
    const localClientId = this.getClientId();
    return JSON.stringify(
      Object.entries(this.getStates())
        .filter(([clientId]) => clientId !== localClientId)
        .sort(([alice], [bob]) => alice.localeCompare(bob)),
    );
  }
}

export { YjsAwarenessSaveTransport };
export type { YjsAwarenessSaveTransportConfig };

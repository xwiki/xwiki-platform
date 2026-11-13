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
import { SESSION_REPLACED_EVENT } from "./events";
import * as decoding from "lib0/decoding";
import * as encoding from "lib0/encoding";
import { removeAwarenessStates } from "y-protocols/awareness";
import {
  messageYjsSyncStep2,
  messageYjsUpdate,
  writeUpdate,
} from "y-protocols/sync";
import { WebsocketProvider } from "y-websocket";
import {
  Doc,
  applyUpdate,
  decodeStateVector,
  encodeStateAsUpdate,
  encodeStateVector,
  encodeStateVectorFromUpdate,
  mergeUpdates,
} from "yjs";

/**
 * The type of the messages used to synchronize the Yjs document.
 */
const MESSAGE_SYNC = 0;

/**
 * The state of the synchronization with the server, for the current connection.
 */
type SyncGate = {
  /**
   * Whether we are waiting for the server to finish sending its history, i.e. the synchronization updates it stored.
   */
  syncing: boolean;

  /**
   * The synchronization updates received from the server while syncing.
   */
  updates: Uint8Array[];

  /**
   * Whether the document was modified locally since the end of the last synchronization with the server.
   */
  changedSinceLastSync: boolean;
};

/**
 * Note that this is not an order relation: state vectors are only partially ordered, so two documents that were modified
 * concurrently each have updates missing from the other (i.e. both `hasUpdatesMissingFrom(a, b)` and
 * `hasUpdatesMissingFrom(b, a)` are true). In our case the server history is usually ahead of a reconnecting client too
 * (the changes made by the others meanwhile), which is expected: the client applies them and pushes its own.
 *
 * @param stateVector - the state vector to check
 * @param otherStateVector - the state vector to compare with
 * @returns whether the given state vector has a client clock beyond the other state vector, i.e. whether the
 *   corresponding document has updates that are missing from the other document
 */
function hasUpdatesMissingFrom(
  stateVector: Map<number, number>,
  otherStateVector: Map<number, number>,
): boolean {
  return [...stateVector].some(
    ([client, clock]) => clock > (otherStateVector.get(client) ?? 0),
  );
}

/**
 * Every collaboration session starts with the updates that initialize the document content, which all the clients
 * receive when they join. So the server history shares something with our document as long as the session continues,
 * even after the room is discarded (when all the clients leave) if someone resumes it by sending back the session
 * history. A new session, started from the saved document, doesn't share anything with our document. An empty document
 * (we didn't synchronize yet) or an empty server history (no one is editing) are compatible with any session.
 *
 * @param doc - our document
 * @param serverStateVector - the state vector of the server history
 * @returns whether our document and the server history belong to the same collaboration session
 */
function sharesSession(
  doc: Doc,
  serverStateVector: Map<number, number>,
): boolean {
  const localStateVector = decodeStateVector(encodeStateVector(doc));
  return (
    localStateVector.size === 0 ||
    serverStateVector.size === 0 ||
    [...localStateVector].some(([client]) => serverStateVector.has(client))
  );
}

/**
 * Make sure the server gets the changes it misses when we (re)connect. The server doesn't hold a Yjs document, only
 * the synchronization updates it received, which it sends back to us, followed by an empty sync step 2, when we connect.
 * Unlike a standard Yjs server, it never asks us (with a sync step 1) for the updates it misses, so the changes we made
 * while disconnected, or that we sent while the connection was already lost, would never reach the other clients.
 *
 * So we compute ourselves what the server misses: we hold back the updates received from the server until it finishes
 * sending its history, which tells us what the server has, then we apply them all at once and send back the missing
 * updates. Holding them back also allows us to detect, before touching our document, that the room was discarded (all
 * clients left) while we were disconnected, and that a new session was started from the saved document. The new
 * session's history doesn't share anything with ours, so merging them would duplicate the content: we stop the
 * synchronization instead and notify the listeners of the {@link SESSION_REPLACED_EVENT}.
 *
 * @param websocketProvider - the websocket provider to synchronize
 * @returns the synchronization gate, which must be opened on each connection
 */
function pushMissingUpdatesOnSync(
  websocketProvider: WebsocketProvider,
): SyncGate {
  const gate: SyncGate = {
    syncing: false,
    updates: [],
    changedSinceLastSync: false,
  };

  websocketProvider.doc.on("update", (_update: Uint8Array, origin: unknown) => {
    if (origin !== websocketProvider) {
      gate.changedSinceLastSync = true;
    }
  });

  const handleSyncMessage = websocketProvider.messageHandlers[MESSAGE_SYNC];
  websocketProvider.messageHandlers[MESSAGE_SYNC] = (
    encoder,
    decoder,
    provider,
    emitSynced,
    messageType,
  ) => {
    // Messages received through the BroadcastChannel (emitSynced is false) come from other browser tabs, that are
    // responsible for synchronizing their own changes with the server.
    if (!gate.syncing || !emitSynced || !holdBack(provider, gate, decoder)) {
      handleSyncMessage?.(encoder, decoder, provider, emitSynced, messageType);
    }
  };

  return gate;
}

/**
 * Holds back the updates received from the server while it sends its history, and ends the synchronization when the
 * server finishes.
 *
 * @param provider - the websocket provider being synchronized
 * @param gate - the synchronization state
 * @param decoder - the decoder of the received synchronization message, positioned after the message type
 * @returns whether the message must not be handled further
 */
function holdBack(
  provider: WebsocketProvider,
  gate: SyncGate,
  decoder: decoding.Decoder,
): boolean {
  const peekDecoder = decoding.clone(decoder);
  const syncMessageType = decoding.readVarUint(peekDecoder);
  if (
    syncMessageType !== messageYjsUpdate &&
    syncMessageType !== messageYjsSyncStep2
  ) {
    return false;
  }
  gate.updates.push(decoding.readVarUint8Array(peekDecoder));
  if (syncMessageType === messageYjsUpdate) {
    // Hold back the update until the server finishes sending its history.
    return true;
  }
  // The server finished sending its history, with an (empty) sync step 2.
  const synced = endSync(provider, gate.updates, gate.changedSinceLastSync);
  Object.assign(gate, {
    syncing: false,
    updates: [],
    changedSinceLastSync: false,
  });
  // Let the provider handle the sync step 2 (which marks the end of the synchronization) only if we're still synced.
  return !synced;
}

/**
 * Sends to the server the updates it misses.
 *
 * @param provider - the websocket provider being synchronized
 * @param serverStateVector - the encoded state vector of the server history
 */
function sendMissingUpdates(
  provider: WebsocketProvider,
  serverStateVector: Uint8Array,
): void {
  const encoder = encoding.createEncoder();
  encoding.writeVarUint(encoder, MESSAGE_SYNC);
  writeUpdate(encoder, encodeStateAsUpdate(provider.doc, serverStateVector));
  provider.ws?.send(encoding.toUint8Array(encoder));
}

/**
 * Stops the synchronization because the room was replaced by a new collaboration session.
 *
 * @param provider - the websocket provider to stop
 */
function stopReplacedSession(provider: WebsocketProvider): void {
  console.warn(
    "The collaboration session was replaced while we were disconnected. Stopping the synchronization.",
  );
  provider.disconnect();
  // @ts-expect-error The provider doesn't declare our custom event.
  provider.emit(SESSION_REPLACED_EVENT, []);
}

/**
 * Called when the server finishes sending its history.
 *
 * @param provider - the websocket provider being synchronized
 * @param serverUpdates - the updates received from the server while synchronizing
 * @param changedSinceLastSync - whether the document was modified locally since the last synchronization
 * @returns whether the synchronization succeeded, false if the room was replaced by a new collaboration session
 */
function endSync(
  provider: WebsocketProvider,
  serverUpdates: Uint8Array[],
  changedSinceLastSync: boolean,
): boolean {
  const serverUpdate = mergeUpdates(serverUpdates);
  // Stops counting a client at the first missing update, so the updates that didn't reach the server are counted as
  // missing.
  const encodedServerStateVector = encodeStateVectorFromUpdate(serverUpdate);
  const serverStateVector = decodeStateVector(encodedServerStateVector);

  if (!sharesSession(provider.doc, serverStateVector)) {
    stopReplacedSession(provider);
    return false;
  }

  applyUpdate(provider.doc, serverUpdate, provider);
  // Deletions don't advance the clock so we can't tell whether the server misses some of our deletions. We push all of
  // them (the update includes the entire delete set) if we made local changes since the last synchronization.
  if (
    changedSinceLastSync ||
    hasUpdatesMissingFrom(
      decodeStateVector(encodeStateVector(provider.doc)),
      serverStateVector,
    )
  ) {
    sendMissingUpdates(provider, encodedServerStateVector);
  }
  return true;
}

/**
 * Send the current client id to the server.
 *
 * @param websocketProvider - the websocket provider to register
 */
function registerClient(websocketProvider: WebsocketProvider) {
  // Put a messagetype of value 3, then the clientId
  // readVarUint for both values
  // server side, intercept the messagae if the type is 3 and instead of broadcasting, save the client id
  // then when the client disconnects, broadcast with message type 4 and send the client id back
  // this should be enough for other clients to cleanup their awareness status
  const encoder = encoding.createEncoder();
  encoding.writeVarUint(encoder, 2);
  encoding.writeVarUint(encoder, websocketProvider.doc.clientID);
  const buf = encoding.toUint8Array(encoder);
  websocketProvider.ws?.send(buf);
}

/**
 * Ask the other clients for their awareness state. The server relays the query to them, and their replies to us. They
 * send their awareness state only when it changes or when it is renewed (every 15 seconds) otherwise, so we wouldn't
 * know right away who is already there.
 *
 * @param websocketProvider - the websocket provider used to send the query
 */
function queryAwareness(websocketProvider: WebsocketProvider) {
  const encoder = encoding.createEncoder();
  encoding.writeVarUint(encoder, 3);
  websocketProvider.ws?.send(encoding.toUint8Array(encoder));
}

/**
 * Decode the client id from a message handler of index 4.
 *
 * @param handlerDecoder - the decoder, usually provided by the handler
 */
function readClientId(handlerDecoder: decoding.Decoder) {
  const decoder = decoding.createDecoder(handlerDecoder.arr);
  // Read and ignore the first uint value (message type).
  decoding.readVarUint(decoder);
  return decoding.readVarUint(decoder);
}

/**
 * Remove our awareness state when the page is hidden (closed, reloaded or left), so that the other clients see us
 * leave right away. Browser tabs of the same origin also exchange awareness updates directly, through a
 * BroadcastChannel, so the updates we send while leaving can reach them after the server announced our departure, and
 * bring us back to their list of collaborators until our awareness times out (30 seconds). The removal has a higher
 * clock than any of these updates, and it's the last message we send on both channels, so it wins whatever order the
 * messages arrive in.
 *
 * @param websocketProvider - the websocket provider whose local awareness state is removed
 */
function removeAwarenessOnPageHide(websocketProvider: WebsocketProvider) {
  const onPageHide = () => {
    removeAwarenessStates(
      websocketProvider.awareness,
      [websocketProvider.doc.clientID],
      "page hide",
    );
  };
  window.addEventListener("pagehide", onPageHide);
  const destroy = websocketProvider.destroy.bind(websocketProvider);
  websocketProvider.destroy = () => {
    window.removeEventListener("pagehide", onPageHide);
    destroy();
  };
}

/**
 * Initialize a websocket provider for a XWiki yjs endpoint.
 *
 * @param url - the url of the websocket endpoint
 * @param room - the string serialization of the room
 * @since 18.4.0RC1
 * @beta
 */
export function createXWikiWebSocketProvider(
  url: string,
  room: string,
  locale: string,
): WebsocketProvider {
  const splits = url.split("/");
  const roomname = splits.at(-1)!;
  const doc = new Doc();
  const websocketProvider = new WebsocketProvider(
    // Yjs WebSocket provider expects the room name to be concatenated to the URL as its last path segment, but our
    // endpoint reads the room name from the query parameters. Therefore, we have to artificially split out the last
    // path segment to have it concatenated back by Yjs WebSocket provider later.
    splits.slice(0, -1).join("/"),
    roomname,
    doc,
    {
      params: { room, locale },
    },
  );
  removeAwarenessOnPageHide(websocketProvider);
  const syncGate = pushMissingUpdatesOnSync(websocketProvider);

  websocketProvider.messageHandlers[4] = (
    encoder,
    decoder: decoding.Decoder,
    provider: WebsocketProvider,
  ): void => {
    const disconnectedClientId = readClientId(decoder);
    // Go through the awareness protocol rather than dropping the entry from the map, so that the local listeners
    // are notified that the client left and the awareness meta data stays consistent.
    removeAwarenessStates(
      provider.awareness,
      [disconnectedClientId],
      "disconnected",
    );
  };
  // Each time we (re)connect, which is a new connection for the server: send our client id, so that the server can
  // notify the others when we leave, and ask the others who they are.
  websocketProvider.on("status", ({ status }) => {
    if (status == "connected") {
      // The server sends its history in response to the sync step 1 that the provider sends after this event.
      syncGate.syncing = true;
      syncGate.updates = [];
      registerClient(websocketProvider);
      queryAwareness(websocketProvider);
    }
  });
  return websocketProvider;
}

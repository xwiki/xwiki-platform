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
import * as decoding from "lib0/decoding";
import * as encoding from "lib0/encoding";
import { removeAwarenessStates } from "y-protocols/awareness";
import { WebsocketProvider } from "y-websocket";
import { Doc } from "yjs";

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
      registerClient(websocketProvider);
      queryAwareness(websocketProvider);
    }
  });
  return websocketProvider;
}

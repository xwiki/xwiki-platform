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
 * Decode the client id from a message handler of index 4.
 *
 * @param handlerDecoder - the decoder, usually provided by the hander
 */
function readClientId(handlerDecoder: decoding.Decoder) {
  const decoder = decoding.createDecoder(handlerDecoder.arr);
  // Read and ignore the first uint value (message type).
  decoding.readVarUint(decoder);
  return decoding.readVarUint(decoder);
}

/**
 * The browser may keep the page in its back/forward cache (bfcache) instead of destroying it when the user navigates
 * away. The page is then frozen with its WebSocket connection still open, so the server keeps the room, and its
 * history, as long as the page stays in the cache: the next session for the same document (e.g. after the document was
 * saved from another editor) would join this room instead of loading the saved document. So we disconnect when the page
 * is stored in the cache. We reload the page when it is restored from the cache, instead of joining back, because the
 * provider doesn't send to the server the changes it misses when (re)connecting.
 *
 * @param websocketProvider - the websocket provider to disconnect
 */
function leaveWhenStoredInBackForwardCache(
  websocketProvider: WebsocketProvider,
) {
  const onPageHide = (event: PageTransitionEvent) => {
    // When the page is not persisted it is destroyed, which closes the WebSocket connection.
    if (event.persisted) {
      websocketProvider.disconnect();
    }
  };
  const onPageShow = (event: PageTransitionEvent) => {
    if (event.persisted) {
      window.location.reload();
    }
  };
  window.addEventListener("pagehide", onPageHide);
  window.addEventListener("pageshow", onPageShow);
  const destroy = websocketProvider.destroy.bind(websocketProvider);
  websocketProvider.destroy = () => {
    window.removeEventListener("pagehide", onPageHide);
    window.removeEventListener("pageshow", onPageShow);
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
  leaveWhenStoredInBackForwardCache(websocketProvider);

  websocketProvider.messageHandlers[4] = (
    encoder,
    decoder: decoding.Decoder,
    provider: WebsocketProvider,
  ): void => {
    const disconnectedClientId = readClientId(decoder);
    provider.awareness.getStates().delete(disconnectedClientId);
  };
  // Send the client id to the server once on connect.
  websocketProvider.once("status", ({ status }) => {
    if (status == "connected") {
      registerClient(websocketProvider);
    }
  });
  return websocketProvider;
}

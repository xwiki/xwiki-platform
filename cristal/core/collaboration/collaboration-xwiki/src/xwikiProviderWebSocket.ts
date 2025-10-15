/**
 * See the LICENSE file distributed with this work for additional
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
 * Initialize a websocket provider for a XWiki yjs endpoint.
 *
 * @param url - the url of the websocket endpoint
 * @param room - the string serialization of the room
 * @since 0.21
 * @beta
 */
export function createXWikiWebSocketProvider(
  url: string,
  room: string,
): WebsocketProvider {
  const splits = url.split("/");
  const roomname = splits[splits.length - 1];
  const doc = new Doc();
  // Creates a new editor instance.
  const websocketProvider = new WebsocketProvider(
    // Since WebSocket provider force having a roomname that is concatenated to the url with a '/'.
    // Therefore, we have to artificially split out the last segment to have it concatenated back by WebSocket
    // provider later.
    splits.slice(0, splits.length - 1).join("/"),
    roomname,
    doc,
    {
      params: { room },
    },
  );

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

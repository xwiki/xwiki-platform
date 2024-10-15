/*
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

import express from "express";
import expressWebsockets from "express-ws";
import { Server as HocuspocusServer } from "@hocuspocus/server";

const hocuspocusServer = HocuspocusServer.configure({
  // See https://developer.chrome.com/blog/timer-throttling-in-chrome-88/
  timeout: 60000,
});

const { app }: expressWebsockets.Instance = expressWebsockets(express());

// Add a WebSocket route for Hocuspocus.
app.ws("/collaboration", (webSocket, request) => {
  // We can pass contextual information to the Hocuspocus server. For instance, we could authenticate the user here.
  const context = {
    //user: {
    //  id: 'mflorea',
    //  name: "Marius Florea",
    //},
  };

  hocuspocusServer.handleConnection(webSocket, request, context);
});

let port = 15681;
const env_port = parseInt(process.env.REALTIME_HTTP_PORT || "");
if (!isNaN(env_port) && env_port > 0) {
  port = env_port;
}

app.listen(port, () => {
  console.log(`Application is running on http://localhost:${port}`);
});

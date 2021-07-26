/*
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
package org.xwiki.websocket.internal;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.websocket.CloseReason;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.websocket.EndpointComponent;
import org.xwiki.websocket.WebSocketContext;

/**
 * A statically registered WebSocket end-point that echoes all messages is receives.
 * 
 * @version $Id$
 * @since 13.7RC1
 */
@Component
@Named("org.xwiki.websocket.internal.StaticEchoEndpoint")
@ServerEndpoint("/echo")
@Singleton
public class StaticEchoEndpoint implements EndpointComponent
{
    @Inject
    protected Logger logger;

    @Inject
    protected WebSocketContext context;

    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    private ModelContext modelContext;

    /**
     * Called when a new WebSocket connection is opened to this end-point.
     * 
     * @param session the WebSocket session
     */
    @OnOpen
    public void onOpen(Session session)
    {
        this.context.run(session, () -> {
            if (this.bridge.getCurrentUserReference() == null) {
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT,
                        "We don't accept connections from guest users. Please login first."));
                } catch (IOException e) {
                    this.logger.warn("Failed to close the session.", e);
                }
            }
        });
    }

    /**
     * Called each time a WebSocket message is received.
     * 
     * @param session the WebSocket session
     * @param message the received message
     * @return the message to send back
     */
    @OnMessage
    public String onMessage(Session session, String message) throws Exception
    {
        return this.context.call(session, () -> {
            String currentWiki =
                this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName();
            return String.format("[%s] %s -> %s", currentWiki, this.bridge.getCurrentUserReference(), message);
        });
    }
}

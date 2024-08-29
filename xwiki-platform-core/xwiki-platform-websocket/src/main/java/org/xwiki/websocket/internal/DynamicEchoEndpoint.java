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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.websocket.AbstractXWikiEndpoint;

/**
 * A dynamically registered WebSocket end-point that echoes all messages is receives.
 * 
 * @version $Id$
 * @since 13.7RC1
 */
@Component
@Named("echo")
@Singleton
public class DynamicEchoEndpoint extends AbstractXWikiEndpoint
{
    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    private ModelContext modelContext;

    @Override
    public void onOpen(Session session, EndpointConfig config)
    {
        this.context.run(session, () -> {
            if (this.bridge.getCurrentUserReference() == null) {
                close(session, CloseReason.CloseCodes.CANNOT_ACCEPT,
                    "We don't accept connections from guest users. Please login first.");
            } else {
                session.addMessageHandler(new MessageHandler.Whole<String>()
                {
                    @Override
                    public void onMessage(String message)
                    {
                        handleMessage(session, message);
                    }
                });
            }
        });
    }

    /**
     * Handles received messages.
     *
     * @param message the received message
     * @return the message to send back
     */
    public String onMessage(String message)
    {
        String currentWiki = this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName();
        return String.format("[%s] %s -> %s", currentWiki, this.bridge.getCurrentUserReference(), message);
    }
}

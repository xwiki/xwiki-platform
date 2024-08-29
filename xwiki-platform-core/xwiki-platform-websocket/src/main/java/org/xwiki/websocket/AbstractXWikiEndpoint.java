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
package org.xwiki.websocket;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.Endpoint;
import javax.websocket.Session;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;

/**
 * Base class for WebSocket end-points that require the XWiki execution context to be properly set-up.
 * 
 * @version $Id$
 * @since 13.7RC1
 */
public abstract class AbstractXWikiEndpoint extends Endpoint implements EndpointComponent
{
    private static final String ON_MESSAGE = "onMessage";

    @Inject
    protected Logger logger;

    @Inject
    protected WebSocketContext context;

    /**
     * Handles a received message by calling either {@code onMessage(Session, T)} or {@code onMessage(T)}. If the value
     * returned by {@code onMessage} matches the message type then we send it back.
     * 
     * @param <T> the message type
     * @param session the WebSocket session in which the message was received
     * @param message the received message
     */
    protected <T> void handleMessage(Session session, T message)
    {
        handleMessage(session, message, true);
    }

    /**
     * Handles a received message by calling either {@code onMessage(Session, T)} or {@code onMessage(T)}. If the value
     * returned by {@code onMessage} matches the message type then we send it back.
     * 
     * @param <T> the message type
     * @param session the WebSocket session in which the message was received
     * @param message the received message
     * @param last whether this is the last message chunk or not, useful when handling partial messages
     */
    protected <T> void handleMessage(Session session, T message, boolean last)
    {
        Method onMessage = getOnMessageMethod(message.getClass());
        if (onMessage == null) {
            this.logger.warn("Failed to handle WebSocket message because onMessage method is missing.");
            return;
        }

        this.context.run(session, () -> {
            try {
                Object output = invokeOnMessage(onMessage, session, message, last);
                if (onMessage.getReturnType().equals(message.getClass()) && output != null) {
                    session.getBasicRemote().sendObject(output);
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                this.logger.warn("Failed to call onMessage. Root cause is [{}].",
                    ExceptionUtils.getRootCauseMessage(e));
            } catch (EncodeException | IOException e) {
                this.logger.warn("Failed to send back the WebSocket message. Root cause is [{}].",
                    ExceptionUtils.getRootCauseMessage(e));
            }
        });
    }

    private <T> Method getOnMessageMethod(Class<T> messageType)
    {
        List<Class<?>[]> onMessageSignatures = new ArrayList<>();
        // Handles partial messages.
        onMessageSignatures.add(new Class[] {Session.class, messageType, boolean.class});
        // Needs the session to send back messages.
        onMessageSignatures.add(new Class[] {Session.class, messageType});
        // Uses the return value to send back messages.
        onMessageSignatures.add(new Class[] {messageType});

        for (Class<?>[] signature : onMessageSignatures) {
            try {
                return this.getClass().getMethod(ON_MESSAGE, signature);
            } catch (NoSuchMethodException | SecurityException e) {
                // Try the next signature.
            }
        }

        return null;
    }

    private <T> Object invokeOnMessage(Method onMessage, Session session, T message, boolean last)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        if (onMessage.getParameterCount() == 1) {
            return onMessage.invoke(this, message);
        } else if (onMessage.getParameterCount() == 3) {
            return onMessage.invoke(this, session, message, last);
        } else if (onMessage.getParameterTypes()[0] == Session.class) {
            return onMessage.invoke(this, session, message);
        } else {
            return onMessage.invoke(this, message, last);
        }
    }

    /**
     * Try to close the given session using the specified code and reason.
     * 
     * @param session the session to close
     * @param closeCode the error code
     * @param reasonPhrase the reason to close the session
     */
    protected void close(Session session, CloseReason.CloseCode closeCode, String reasonPhrase)
    {
        try {
            session.close(new CloseReason(closeCode, reasonPhrase));
        } catch (IOException e) {
            this.logger.warn("Failed to close the session.", e);
        }
    }
}

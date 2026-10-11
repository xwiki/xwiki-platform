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
package org.xwiki.yjs.websocket.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.websocket.CloseReason;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import static jakarta.websocket.CloseReason.CloseCodes.GOING_AWAY;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Default implementation of {@link PingPongManager}.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component
@Singleton
public class DefaultPingPongManager implements PingPongManager, Initializable, Disposable
{
    private static final ByteBuffer EMPTY_PING_PAYLOAD = ByteBuffer.allocate(0).asReadOnlyBuffer();

    private static final class TrackedSession
    {
        private final AtomicInteger missedPongs = new AtomicInteger();

        private final MessageHandler.Whole<@NonNull PongMessage> pongHandler;

        private TrackedSession(MessageHandler.Whole<@NonNull PongMessage> pongHandler)
        {
            this.pongHandler = pongHandler;
        }
    }

    @Inject
    private Logger logger;

    @Inject
    private YjsEndpointConfiguration configuration;

    private final Map<Session, TrackedSession> trackedSessions = new ConcurrentHashMap<>();

    private ScheduledExecutorService pingScheduler;

    @Override
    public void initialize() throws InitializationException
    {
        this.pingScheduler = Executors.newSingleThreadScheduledExecutor();
        this.pingScheduler.scheduleAtFixedRate(this::pingAllSessions, this.configuration.getPingInterval(),
            this.configuration.getPingInterval(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (this.pingScheduler != null) {
            this.pingScheduler.shutdownNow();
            this.pingScheduler = null;
        }
    }

    @Override
    public void startPinging(Session session)
    {
        this.trackedSessions.computeIfAbsent(session, s -> {
            MessageHandler.Whole<@NonNull PongMessage> pongHandler = message -> {
                // Reset the missed pongs count whenever we receive a pong message.
                TrackedSession trackedSession = this.trackedSessions.get(s);
                if (trackedSession != null) {
                    trackedSession.missedPongs.set(0);
                }
            };
            s.addMessageHandler(PongMessage.class, pongHandler);
            return new TrackedSession(pongHandler);
        });
    }

    @Override
    public void stopPinging(Session session)
    {
        TrackedSession trackedSession = this.trackedSessions.remove(session);
        if (trackedSession != null) {
            session.removeMessageHandler(trackedSession.pongHandler);
        }
    }

    void pingAllSessions()
    {
        this.trackedSessions.entrySet().forEach(this::pingSession);
    }

    private void pingSession(Map.Entry<Session, TrackedSession> entry)
    {
        Session session = entry.getKey();
        TrackedSession trackedSession = entry.getValue();

        if (!session.isOpen()) {
            this.stopPinging(session);
            return;
        }

        try {
            if (trackedSession.missedPongs.get() >= this.configuration.getPingMaxMissedPongs()) {
                closeSession(session, GOING_AWAY, "Client did not respond to server ping.");
                return;
            }

            trackedSession.missedPongs.incrementAndGet();
            session.getBasicRemote().sendPing(EMPTY_PING_PAYLOAD.duplicate());
        } catch (IOException e) {
            this.logger.warn("Failed to send ping message to session [{}]. Cause: [{}]", session.getId(),
                getRootCauseMessage(e));
            closeSession(session, GOING_AWAY, "Failed to send ping message to the client.");
        }
    }

    private void closeSession(Session session, CloseReason.CloseCode closeCode, String reasonPhrase)
    {
        this.stopPinging(session);
        try {
            session.close(new CloseReason(closeCode, reasonPhrase));
        } catch (IOException e) {
            this.logger.debug("Failed to close session [{}]. Cause: [{}]", session.getId(), getRootCauseMessage(e));
        }
    }
}

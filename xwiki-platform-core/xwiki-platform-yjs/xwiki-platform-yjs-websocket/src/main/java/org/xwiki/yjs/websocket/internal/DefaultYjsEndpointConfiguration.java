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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Default implementation of {@link YjsEndpointConfiguration}.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component
@Singleton
public class DefaultYjsEndpointConfiguration implements YjsEndpointConfiguration
{
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @Override
    public long getMaxIdleTimeout()
    {
        // The client side keeps the connection alive by sending a PING message from time to time, using a timer
        // (setTimeout). The browsers are slowing down timers used by inactive tabs / windows (that don't have
        // the user focus). This is called timer throttling and can go up to 1 minute, which means inactive browser tabs
        // won't be able to send PING messages more often than every minute. We set the session idle timeout higher than
        // the timer throttling value in order to take into account the time it takes for the ping request to arrive.
        // This ensures the WebSocket connection is not closed in background tabs.
        // See https://developer.chrome.com/blog/timer-throttling-in-chrome-88/
        return getProperty("maxIdleTimeout", 60000L + 15000L);
    }

    @Override
    public int getMaxBinaryMessageBufferSize()
    {
        return getProperty("maxBinaryMessageBufferSize", 1024 * 8);
    }

    @Override
    public int getMaxTextMessageBufferSize()
    {
        return getProperty("maxTextMessageBufferSize", 1024 * 8);
    }

    @Override
    public int getMaxMessageSize()
    {
        // Limit the maximum message size to 1MB by default since the Yjs messages are expected to be small.
        return getProperty("maxMessageSize", 1024 * 1024);
    }

    @Override
    public long getPingInterval()
    {
        // The default value of 25 seconds allows us to send 3 ping messages before the default session idle timeout is
        // reached, see #getMaxIdleTimeout() and #getPingMaxMissedPongs().
        return getProperty("pingInterval", 25000L);
    }

    @Override
    public int getPingMaxMissedPongs()
    {
        // The default value allows us to send 3 ping messages before the default session idle timeout is reached, see
        // #getMaxIdleTimeout() and #getPingInterval().
        return getProperty("pingMaxMissedPongs", 3);
    }

    @Override
    public long getMaxHistorySize()
    {
        // Limit the maximum history size to 5MB by default.
        return getProperty("maxHistorySize", 5 * 1024 * 1024);
    }

    private <T> T getProperty(String propertyName, T defaultValue)
    {
        return this.xwikiPropertiesSource.getProperty("yjs.websocket." + propertyName, defaultValue);
    }
}

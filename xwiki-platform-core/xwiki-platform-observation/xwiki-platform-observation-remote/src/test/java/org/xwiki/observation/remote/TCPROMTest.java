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
package org.xwiki.observation.remote;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.remote.test.AbstractROMTestCase;
import org.xwiki.observation.remote.test.TestEvent;
import org.xwiki.test.annotation.AllComponents;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test checking that event are well converted and transported from one cluster member to another.
 * 
 * @version $Id$
 */
@AllComponents
public class TCPROMTest extends AbstractROMTestCase
{
    static class Unserializable
    {
    }

    @Override
    @BeforeEach
    public void beforeEach() throws Exception
    {
        super.beforeEach();

        System.setProperty("jgroups.bind_addr", "localhost");

        getConfigurationSource1().setProperty("observation.remote.channels", Arrays.asList("tcp"));
        RemoteObservationManager rom = getComponentManager2().getInstance(RemoteObservationManager.class);
        rom.startChannel("tcp");
    }

    /**
     * Validate sharing a simple Serializable event between two instances of {@link RemoteObservationManager}.
     */
    @Test
    public void testSerializableEvent() throws InterruptedException
    {
        EventListener localListener = mock(EventListener.class, "local");
        EventListener remoteListener = mock(EventListener.class, "remote");

        TestEvent event = new TestEvent();

        Unserializable unserializable = new Unserializable();

        when(localListener.getName()).thenReturn("mylistener");
        when(remoteListener.getName()).thenReturn("mylistener");
        when(localListener.getEvents()).thenReturn(Arrays.asList(event));
        when(remoteListener.getEvents()).thenReturn(Arrays.asList(event));

        getObservationManager1().addListener(localListener);
        getObservationManager2().addListener(remoteListener);

        getObservationManager1().notify(event, "some source", "some data");
        getObservationManager1().notify(event, unserializable, unserializable);
        getObservationManager1().notify(new LogEvent(), "some source", "some data");

        // Make sure JGroups has enough time to send the message
        Thread.sleep(1000);

        verify(localListener).onEvent(same(event), eq("some source"), eq("some data"));
        verify(localListener).onEvent(same(event), same(unserializable), same(unserializable));
        verify(remoteListener).onEvent(eq(event), eq("some source"), eq("some data"));
    }
}

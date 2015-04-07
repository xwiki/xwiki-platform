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

import org.jmock.Expectations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.remote.test.AbstractROMTestCase;
import org.xwiki.observation.remote.test.TestEvent;

public class TCPROMTest extends AbstractROMTestCase
{
    static class Unserializable { }

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        System.setProperty("jgroups.bind_addr", "localhost");

        getConfigurationSource1().setProperty("observation.remote.channels", Arrays.asList("tcp"));
        RemoteObservationManager rom = getComponentManager2().getInstance(RemoteObservationManager.class);
        rom.startChannel("tcp");
    }

    @After
    public void tearDown() throws Exception
    {
        this.mockery.assertIsSatisfied();
    }

    /**
     * Validate sharing a simple Serializable event between two instances of {@link RemoteObservationManager}.
     */
    @Test
    public void testSerializableEvent() throws InterruptedException
    {
        final EventListener localListener = this.mockery.mock(EventListener.class, "local");
        final EventListener remoteListener = this.mockery.mock(EventListener.class, "remote");

        final TestEvent event = new TestEvent();

        final Unserializable unserializable = new Unserializable();

        this.mockery.checking(new Expectations()
        {{
                allowing(localListener).getName();
                will(returnValue("mylistener"));
                allowing(remoteListener).getName();
                will(returnValue("mylistener"));
                allowing(localListener).getEvents();
                will(returnValue(Arrays.asList(event)));
                allowing(remoteListener).getEvents();
                will(returnValue(Arrays.asList(event)));
                oneOf(localListener).onEvent(with(same(event)), with(equal("some source")), with(equal("some data")));
                oneOf(localListener).onEvent(with(same(event)), with(same(unserializable)), with(same(unserializable)));
                oneOf(remoteListener).onEvent(with(equal(event)), with(equal("some source")), with(equal("some data")));
            }});

        getObservationManager1().addListener(localListener);
        getObservationManager2().addListener(remoteListener);

        getObservationManager1().notify(event, "some source", "some data");
        getObservationManager1().notify(event, unserializable, unserializable);
        getObservationManager1().notify(new LogEvent(), "some source", "some data");

        // Make sure JGroups has enough time to send the message
        Thread.sleep(1000);
    }
}
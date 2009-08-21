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
 *
 */
package org.xwiki.observation.remote;

import java.util.Arrays;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.remote.test.AbstractROMTestCase;
import org.xwiki.observation.remote.test.TestEvent;

public class TCPROMTest extends AbstractROMTestCase
{
    private Mockery context = new Mockery();

    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        
        getConfigurationSource1().setProperty("observation.remote.channels", "tcp1");
        getComponentManager2().lookup(RemoteObservationManager.class).startChannel("tcp2");
    }
    
    @After
    public void tearDown() throws Exception
    {
        this.context.assertIsSatisfied();
    }

    /**
     * Validate sharing a simple Serializable event between two instances of {@link RemoteObservationManager}.
     */
    @Test
    public void testSerializableEvent() throws InterruptedException
    {
        final EventListener listener = this.context.mock(EventListener.class);
        final TestEvent event = new TestEvent();

        this.context.checking(new Expectations() {{
                allowing(listener).getName(); will(returnValue("mylistener"));
                allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
                oneOf(listener).onEvent(with(equal(event)), with(equal("some source")), with(equal("some data")));
            }});

        getObservationManager2().addListener(listener);

        getObservationManager1().notify(event, "some source", "some data");

        // Make sure JGroups has enough time to send the message
        Thread.sleep(1000);
    }
}
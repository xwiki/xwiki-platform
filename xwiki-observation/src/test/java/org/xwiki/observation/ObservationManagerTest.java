/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package org.xwiki.observation;

import junit.framework.TestCase;
import org.xwiki.observation.event.DocumentSaveEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.internal.DefaultObservationManager;

public class ObservationManagerTest extends TestCase
{
    public class TestListener implements EventListener
    {
        public boolean hasListenerBeenCalled = false;

        public void onEvent(Event event, Object source, Object data)
        {
            assertEquals(DocumentSaveEvent.class.getName(), event.getClass().getName());
            assertEquals("some source", (String) source);
            assertEquals("some data", (String) data);
            this.hasListenerBeenCalled = true;
        }
    }

    public void testNotify()
    {
        ObservationManager manager = new DefaultObservationManager();
        TestListener listener = new TestListener();
        manager.addListener(new DocumentSaveEvent("SomeDocument"), listener);

        manager.notify(new DocumentSaveEvent("SomeDocument"), "some source", "some data");
        assertTrue("Listener has not been called", listener.hasListenerBeenCalled);
    }

}

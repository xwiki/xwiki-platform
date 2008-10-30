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
package com.xpn.xwiki.wysiwyg.client.util;

import java.util.ArrayList;
import java.util.List;

import com.xpn.xwiki.wysiwyg.client.WysiwygClientTest;
import com.xpn.xwiki.wysiwyg.client.util.Timer;
import com.xpn.xwiki.wysiwyg.client.util.TimerListener;

/**
 * Unit tests for {@link Timer}.
 */
public class TimerTest extends WysiwygClientTest
{
    /**
     * Finish the test after all the listeners have been notified.
     */
    public class FinishTestTimerListener implements TimerListener
    {
        private final Timer timer;

        private final List<TimerListener> listeners;

        private final Object lock;

        public FinishTestTimerListener(Timer timer, List<TimerListener> listeners, Object lock)
        {
            this.timer = timer;
            timer.addTimerListener(this);

            this.listeners = listeners;
            listeners.add(this);

            this.lock = lock;
        }

        public void onElapsed(Timer sender)
        {
            assertEquals(timer, sender);
            // This assertion will fail if this listener is notified twice
            assertTrue(listeners.contains(this));
            synchronized (lock) {
                listeners.remove(this);
                if (listeners.size() == 0) {
                    finishTest();
                }
            }
        }
    }

    /**
     * Test if we can register timer listeners and if they are indeed notified.
     */
    public void testAddTimerListener()
    {
        Timer timer = new Timer();
        List<TimerListener> listeners = new ArrayList<TimerListener>();
        Object lock = new Object();

        new FinishTestTimerListener(timer, listeners, lock);
        new FinishTestTimerListener(timer, listeners, lock);

        delayTestFinish(500);
        timer.schedule(100);
    }

    /**
     * Test if we can unregister a timer listeners and if it isn't notified thereafter.
     */
    public void testRemoveTimerListener()
    {
        Timer timer = new Timer();
        List<TimerListener> listeners = new ArrayList<TimerListener>();
        Object lock = new Object();

        TimerListener listener = new FinishTestTimerListener(timer, listeners, lock);
        new FinishTestTimerListener(timer, listeners, lock);

        // We fire the first listener in order to remove it from the listener list.
        listener.onElapsed(timer);
        // We unregister the first listener.
        timer.removeTimerListener(listener);

        delayTestFinish(300);
        timer.schedule(100);
    }
}

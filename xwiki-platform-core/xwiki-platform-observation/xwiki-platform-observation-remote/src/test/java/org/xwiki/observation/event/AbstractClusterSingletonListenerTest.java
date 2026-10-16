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
package org.xwiki.observation.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.observation.remote.ClusterSingletonManager;
import org.xwiki.observation.remote.ClusterSingletonNodeChangedEvent;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractClusterSingletonListener}.
 *
 * @version $Id$
 */
class AbstractClusterSingletonListenerTest
{
    private static final Event READY = new NamedEvent("ready");

    private static final Event OTHER = new NamedEvent("other");

    private static final long TIMEOUT = 10;

    private record NamedEvent(String name) implements Event
    {
        @Override
        public boolean matches(Object otherEvent)
        {
            return equals(otherEvent);
        }
    }

    private static class TestListener extends AbstractClusterSingletonListener
    {
        private final List<String> calls = new CopyOnWriteArrayList<>();

        private boolean singletonNodeRequired = true;

        /**
         * Run (once) at the beginning of the next action, used to block it.
         */
        private volatile Runnable nextActionHook;

        TestListener(Event... events)
        {
            super("test", events);
        }

        TestListener(List<Event> events)
        {
            super("test", events);
        }

        @Override
        protected boolean isSingletonNodeRequired()
        {
            return this.singletonNodeRequired;
        }

        @Override
        protected void processSingletonEvent(Event event, Object source, Object data)
        {
            act("process " + ((NamedEvent) event).name());
        }

        @Override
        protected void onSingletonNodeGained()
        {
            act("gained");
        }

        @Override
        protected void onSingletonNodeLost()
        {
            act("lost");
        }

        private void act(String call)
        {
            Runnable hook = this.nextActionHook;
            this.nextActionHook = null;
            if (hook != null) {
                hook.run();
            }

            // Recorded at the end of the action, so that the order of the calls is the order in which the actions
            // completed
            this.calls.add(call);
        }
    }

    private static class DefaultListener extends AbstractClusterSingletonListener
    {
        private final List<String> calls = new CopyOnWriteArrayList<>();

        DefaultListener()
        {
            super("default", READY);
        }

        @Override
        protected void processSingletonEvent(Event event, Object source, Object data)
        {
            this.calls.add("process");
        }
    }

    private ClusterSingletonManager singletonManager;

    private RemoteObservationManagerContext remoteContext;

    private TestListener listener;

    @BeforeEach
    void beforeEach()
    {
        this.singletonManager = mock(ClusterSingletonManager.class);
        this.remoteContext = mock(RemoteObservationManagerContext.class);
        this.listener = initialize(new TestListener(READY, OTHER));
    }

    private <T extends AbstractClusterSingletonListener> T initialize(T newListener)
    {
        newListener.singletonManager = this.singletonManager;
        newListener.remoteObservationManagerContext = this.remoteContext;

        return newListener;
    }

    private void notifyChange(AbstractClusterSingletonListener target, boolean singletonNode)
    {
        // The manager notifies the change once its answer changed
        when(this.singletonManager.isSingletonNode()).thenReturn(singletonNode);
        target.onEvent(new ClusterSingletonNodeChangedEvent(), this.singletonManager, singletonNode);
    }

    /**
     * Make the next action of the listener wait until the returned latch is released, and count down the passed latch
     * when it starts.
     */
    private CountDownLatch blockNextAction(CountDownLatch started)
    {
        CountDownLatch release = new CountDownLatch(1);
        this.listener.nextActionHook = () -> {
            started.countDown();
            await(release);
        };

        return release;
    }

    private static void await(CountDownLatch latch)
    {
        try {
            assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static Thread start(Runnable runnable)
    {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();

        return thread;
    }

    /**
     * Wait until the passed thread waits for the lock of the listener (or ended, which is what happens if the action
     * running on the other thread does not hold the lock).
     */
    private static void waitUntilBlocked(Thread thread)
    {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT);
        while (thread.isAlive() && thread.getState() != Thread.State.BLOCKED) {
            assertTrue(System.nanoTime() < deadline, "The thread was never blocked");
            Thread.onSpinWait();
        }
    }

    private static void join(Thread thread) throws InterruptedException
    {
        thread.join(TimeUnit.SECONDS.toMillis(TIMEOUT));
        assertFalse(thread.isAlive());
    }

    @Test
    void getEvents()
    {
        assertEquals(3, this.listener.getEvents().size());
        assertInstanceOf(ClusterSingletonNodeChangedEvent.class, this.listener.getEvents().get(2));

        List<Event> events = new TestListener(List.of(READY)).getEvents();
        assertEquals(2, events.size());
        assertInstanceOf(ClusterSingletonNodeChangedEvent.class, events.get(1));
    }

    @Test
    void singletonNodeAtStartup()
    {
        when(this.singletonManager.isSingletonNode()).thenReturn(true);

        this.listener.onEvent(READY, null, null);
        this.listener.onEvent(OTHER, null, null);
        notifyChange(this.listener, false);

        assertEquals(List.of("process ready", "process other", "lost"), this.listener.calls);
    }

    @Test
    void becomesSingletonNodeAfterStartup()
    {
        // Changes received before the first event are ignored: the first event checks the current state
        notifyChange(this.listener, true);
        notifyChange(this.listener, false);

        this.listener.onEvent(READY, null, null);
        this.listener.onEvent(OTHER, null, null);
        notifyChange(this.listener, true);
        notifyChange(this.listener, true);

        assertEquals(List.of("gained"), this.listener.calls);
    }

    @Test
    void staleGainAfterReady()
    {
        // The manager changed its answer, the ready event sees it before the change notification is delivered
        when(this.singletonManager.isSingletonNode()).thenReturn(true);
        this.listener.onEvent(READY, null, null);
        this.listener.onEvent(new ClusterSingletonNodeChangedEvent(), this.singletonManager, true);

        assertEquals(List.of("process ready"), this.listener.calls);
    }

    @Test
    void gainDeliveredAfterStop()
    {
        this.listener.onEvent(READY, null, null);

        // A gain notified before the instance started stopping, delivered once the manager answers false
        this.listener.onEvent(new ClusterSingletonNodeChangedEvent(), this.singletonManager, true);

        assertEquals(List.of(), this.listener.calls);
    }

    @Test
    void initializeSingletonState()
    {
        when(this.singletonManager.isSingletonNode()).thenReturn(true);

        // Registered after the ready event: the work is started by the gain
        assertTrue(this.listener.initializeSingletonState());

        notifyChange(this.listener, false);

        assertEquals(List.of("gained", "lost"), this.listener.calls);
    }

    @Test
    void initializeSingletonStateOnOtherMember()
    {
        assertFalse(this.listener.initializeSingletonState());

        notifyChange(this.listener, true);

        assertEquals(List.of("gained"), this.listener.calls);
    }

    @Test
    void lossDuringReadyAction() throws InterruptedException
    {
        when(this.singletonManager.isSingletonNode()).thenReturn(true);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = blockNextAction(started);

        // The ready event decides to start the work
        Thread ready = start(() -> this.listener.onEvent(READY, null, null));
        await(started);

        // The manager thread processes a loss before the work is started
        when(this.singletonManager.isSingletonNode()).thenReturn(false);
        ClusterSingletonNodeChangedEvent change = new ClusterSingletonNodeChangedEvent();
        Thread loss = start(() -> this.listener.onEvent(change, this.singletonManager, false));
        waitUntilBlocked(loss);

        release.countDown();
        join(ready);
        join(loss);

        // The loss is processed after the start, the work is not left running
        assertEquals(List.of("process ready", "lost"), this.listener.calls);
    }

    @Test
    void lossAtStopDuringGainAction() throws InterruptedException
    {
        this.listener.onEvent(READY, null, null);

        when(this.singletonManager.isSingletonNode()).thenReturn(true);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = blockNextAction(started);

        // The manager thread decides that the instance gained the singleton node
        ClusterSingletonNodeChangedEvent gainChange = new ClusterSingletonNodeChangedEvent();
        Thread gain = start(() -> this.listener.onEvent(gainChange, this.singletonManager, true));
        await(started);

        // The application stops: the loss is notified synchronously by the stopping thread
        when(this.singletonManager.isSingletonNode()).thenReturn(false);
        ClusterSingletonNodeChangedEvent lossChange = new ClusterSingletonNodeChangedEvent();
        Thread stop = start(() -> this.listener.onEvent(lossChange, this.singletonManager, false));
        waitUntilBlocked(stop);

        release.countDown();
        join(gain);
        join(stop);

        // When the notification of the loss returns, the work started by the gain is stopped
        assertEquals(List.of("gained", "lost"), this.listener.calls);
    }

    @Test
    void remoteEventsAreIgnored()
    {
        when(this.singletonManager.isSingletonNode()).thenReturn(true);
        when(this.remoteContext.isRemoteState()).thenReturn(true);

        this.listener.onEvent(READY, null, null);
        // Not ready yet
        notifyChange(this.listener, true);

        assertEquals(List.of(), this.listener.calls);
    }

    @Test
    void singletonNodeNotRequired()
    {
        this.listener.singletonNodeRequired = false;
        when(this.remoteContext.isRemoteState()).thenReturn(true);

        this.listener.onEvent(READY, null, null);
        this.listener.onEvent(OTHER, null, null);
        notifyChange(this.listener, true);
        notifyChange(this.listener, false);

        assertEquals(List.of("process ready", "process other"), this.listener.calls);
    }

    @Test
    void defaultImplementation()
    {
        DefaultListener defaultListener = initialize(new DefaultListener());
        when(this.singletonManager.isSingletonNode()).thenReturn(true);

        defaultListener.onEvent(READY, null, null);
        notifyChange(defaultListener, false);
        notifyChange(defaultListener, true);

        assertEquals(List.of("process"), defaultListener.calls);
    }
}

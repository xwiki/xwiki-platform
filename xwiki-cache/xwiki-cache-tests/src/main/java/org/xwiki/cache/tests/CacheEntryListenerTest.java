package org.xwiki.cache.tests;

import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.CacheEntryListener;

/**
 * Class used to test cache event management.
 * 
 * @version $Id$
 */
public class CacheEntryListenerTest implements CacheEntryListener<Object>
{
    /**
     * 
     */
    public enum EventType
    {
        /**
         * Add event.
         */
        ADD,

        /**
         * Modify event.
         */
        MODIFY,

        /**
         * Remove event.
         */
        REMOVE
    };

    /**
     * Event object received with last insertion.
     */
    private CacheEntryEvent<Object> addedEvent;

    /**
     * Event object received with last modification.
     */
    private CacheEntryEvent<Object> modifiedEvent;

    /**
     * Event object received with last remove.
     */
    private CacheEntryEvent<Object> removedEvent;

    /**
     * @return event object received with last insertion.
     */
    public CacheEntryEvent<Object> getAddedEvent()
    {
        return this.addedEvent;
    }

    /**
     * Set add event to null.
     */
    public void reinitAddEvent()
    {
        this.addedEvent = null;
    }

    /**
     * @return event object received with last modification.
     */
    public CacheEntryEvent<Object> getModifiedEvent()
    {
        return this.modifiedEvent;
    }

    /**
     * Set modified event to null.
     */
    public void reinitModifiedEvent()
    {
        this.modifiedEvent = null;
    }

    /**
     * @return event object received with last remove.
     */
    public CacheEntryEvent<Object> getRemovedEvent()
    {
        return this.removedEvent;
    }

    /**
     * Set removed event to null.
     */
    public void reinitRemovedEvent()
    {
        this.removedEvent = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.event.CacheEntryListener#cacheEntryAdded(org.xwiki.cache.event.CacheEntryEvent)
     */
    public void cacheEntryAdded(CacheEntryEvent<Object> event)
    {
        this.addedEvent = event;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.event.CacheEntryListener#cacheEntryModified(org.xwiki.cache.event.CacheEntryEvent)
     */
    public void cacheEntryModified(CacheEntryEvent<Object> event)
    {
        this.modifiedEvent = event;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.event.CacheEntryListener#cacheEntryRemoved(org.xwiki.cache.event.CacheEntryEvent)
     */
    public void cacheEntryRemoved(CacheEntryEvent<Object> event)
    {
        this.removedEvent = event;
    }

    /**
     * @param eventType event type.
     * @return wait until it receive a entry removed event.
     * @throws InterruptedException error
     */
    public boolean waitForEntryEvent(EventType eventType) throws InterruptedException
    {
        EventWaiter eventWaiter = new EventWaiter(eventType);

        Thread thread = new Thread(eventWaiter);
        thread.start();
        thread.join(100000);

        if (thread.isAlive()) {
            eventWaiter.stop();

            return false;
        }

        return true;
    }

    /**
     * Event waiter.
     */
    class EventWaiter implements Runnable
    {
        /**
         * Indicate that the thread should continue to run or have to stop.
         */
        private boolean run = true;

        /**
         * The event type.
         */
        private EventType eventType;

        /**
         * @param eventType the event type.
         */
        public EventWaiter(EventType eventType)
        {
            this.eventType = eventType;
        }

        /**
         * Stop.
         */
        void stop()
        {
            this.run = false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            CacheEntryEvent<Object> event;

            while (this.run) {
                if (this.eventType == EventType.ADD) {
                    event = addedEvent;
                } else if (this.eventType == EventType.MODIFY) {
                    event = modifiedEvent;
                } else {
                    event = removedEvent;
                }

                if (event != null) {
                    break;
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }
}

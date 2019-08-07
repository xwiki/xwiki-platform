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
package org.xwiki.notifications.rest.internal;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;

/**
 * Cache notification request results and limit the number of threads allowed to retrieve notification events.
 * 
 * @version $Id$
 * @since 10.11.4
 * @since 11.2
 */
@Component(roles = NotificationEventExecutor.class)
@Singleton
public class NotificationEventExecutor implements Initializable, Disposable
{
    @Inject
    private ConfigurationSource configurationSource;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private ExecutionContextManager contextManager;

    @Inject
    private Execution execution;

    @Inject
    private Logger logger;

    private final AtomicLong counter = new AtomicLong();

    private final ConcurrentMap<String, CallableEntry> queue = new ConcurrentHashMap<>();

    private ThreadPoolExecutor executor;

    /**
     * Cache to keep the result of a task until the client have access to it.
     */
    private Cache<Object> shortCache;

    /**
     * Cache used to store task events result until the result might change (for example when a new notification is
     * created).
     */
    private Cache<List<CompositeEvent>> longEventCache;

    /**
     * Cache used to store task count result until the result might change (for example when a new notification is
     * created).
     */
    private Cache<Integer> longCountCache;

    private class CallableEntry implements Callable<Object>
    {
        private final String cacheKey;

        private final Callable<List<CompositeEvent>> callable;

        private final Set<String> asyncIds = ConcurrentHashMap.newKeySet();

        private final boolean count;

        private final String initialAsyncId;

        CallableEntry(String longCacheKey, Callable<List<CompositeEvent>> callable, boolean count)
        {
            this(longCacheKey, callable, count, null);
        }

        CallableEntry(String longCacheKey, Callable<List<CompositeEvent>> callable, boolean count, String asyncId)
        {
            this.cacheKey = longCacheKey;
            this.callable = callable;
            this.count = count;

            this.initialAsyncId = asyncId;

            if (asyncId != null) {
                addAsyncId(asyncId);
            }
        }

        public void addAsyncId(String asyncId)
        {
            this.asyncIds.add(asyncId);
        }

        @Override
        public Object call() throws Exception
        {
            logger.debug("Starting execution [{}]", this);

            // Remember the thread name
            String threadName = Thread.currentThread().getName();

            // Make the thread name match what it's currently doing
            Thread.currentThread().setName(toString());

            Object result = null;
            try {
                result = execute();

                return result;
            } catch (Throwable e) {
                result = e;

                // Log the exception since it's really not expected
                logger.error("Failed to retrieve notifications for cache key [{}]", this.cacheKey, e);

                throw e;
            } finally {
                logger.debug("Finishing execution [{}]", this);

                // Clean the queue
                // "result" should never by null but just in case...
                onFinish(result != null ? result : new NotificationException("No result"));

                // Restore the thread name
                Thread.currentThread().setName(threadName);
            }
        }

        private Object execute() throws Exception
        {
            // Check if the result is already in the event cache
            Object result = getFromCache(this.cacheKey, this.count);
            if (result != null) {
                return result;
            }

            try {
                // Initialize a proper execution context
                contextManager.initialize(new ExecutionContext());

                // Execute the callable
                List<CompositeEvent> events = this.callable.call();
                result = setInCache(this.cacheKey, events, this.count);
            } finally {
                // Get rid of the execution context
                execution.removeContext();
            }

            return result;
        }

        private void onFinish(Object result)
        {
            // Avoid race condition where an async id is added after the result is put in the cache
            synchronized (queue) {
                // Remove from the queue map
                if (queue.remove(this.cacheKey, this)) {
                    logger.debug("Removed [{}] from the queue", this);
                } else {
                    logger.debug("Tried to remove [{}] from the queue but it could not be found", this);
                }

                // Notify the waiting client that the execution is done
                this.asyncIds.stream().forEach(asyncId -> shortCache.set(asyncId, result));
            }
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder(
                String.format("Notification event executor: %s : %s", (this.count ? "count" : "list"), this.cacheKey));

            // The initial async id can be null when async is not enabled in the first request
            if (this.initialAsyncId != null) {
                builder.append(" : ");
                builder.append(this.initialAsyncId);
            }

            return builder.toString();
        }
    }

    private class CallableEntryExecutor extends ThreadPoolExecutor implements ThreadFactory
    {
        private static final String THREAD_NAME = "Notification pool thread";

        private final ThreadFactory threadFactory = Executors.defaultThreadFactory();

        CallableEntryExecutor(int poolSize)
        {
            super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

            setThreadFactory(this);
        }

        @Override
        public Thread newThread(Runnable r)
        {
            Thread thread = this.threadFactory.newThread(r);

            thread.setDaemon(true);
            thread.setName(THREAD_NAME);
            thread.setPriority(Thread.NORM_PRIORITY);

            return thread;
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t)
        {
            // Reset thread name since it's not used anymore
            Thread.currentThread().setName(THREAD_NAME);
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        int poolSize = this.configurationSource.getProperty("notifications.rest.poolSize", 2);
        boolean longCacheEnabled = this.configurationSource.getProperty("notifications.rest.cache", true);

        if (poolSize > 0) {
            this.executor = new CallableEntryExecutor(poolSize);

            try {
                this.shortCache = this.cacheManager
                    .createNewCache(new LRUCacheConfiguration("notification.rest.shortCache", 1000, 6000));
            } catch (CacheException e) {
                throw new InitializationException("Failed to create short cache", e);
            }
        }

        if (longCacheEnabled) {
            try {
                this.longEventCache = this.cacheManager
                    .createNewCache(new LRUCacheConfiguration("notification.rest.longCache.events", 100, 86400));
            } catch (CacheException e) {
                throw new InitializationException("Failed to create long event cache", e);
            }

            try {
                this.longCountCache = this.cacheManager
                    .createNewCache(new LRUCacheConfiguration("notification.rest.longCache.count", 10000, 86400));
            } catch (CacheException e) {
                throw new InitializationException("Failed to create long count cache", e);
            }
        }
    }

    /**
     * @param cacheKey the cache key
     * @param callable the callable to execute
     * @param async true if the method should return immediately with the task id (or the cached value)
     * @param count true if if the size of the list should be returned/cache instead of the list
     * @return one of the following:
     *         <ul>
     *         <li>a {@link String} when an asynchronous execution has been started</li>
     *         <li>a List<CompositeEvent> when the result was cached or the execution is synchronous</li>
     *         </ul>
     * @throws Exception when failing to execute the passed {@link Callable}
     */
    public Object submit(String cacheKey, Callable<List<CompositeEvent>> callable, boolean async, boolean count)
        throws Exception
    {
        Object cached = getFromCache(cacheKey, count);

        if (cached != null) {
            return cached;
        }

        if (this.executor != null) {
            if (async) {
                String asyncId = String.valueOf(this.counter.incrementAndGet());

                submit(cacheKey, callable, count, asyncId);

                return asyncId;
            } else {
                // Even when not asynchronous we want to make sure only a configured number of threads is allowed to
                // search for notifications
                Future<?> future = this.executor.submit(new CallableEntry(cacheKey, callable, count));

                // Wait for the result
                return future.get();
            }
        } else {
            return callable.call();
        }
    }

    private void submit(String longCacheKey, Callable<List<CompositeEvent>> callable, boolean count, String asyncId)
    {
        synchronized (this.queue) {
            CallableEntry entry = this.queue.get(longCacheKey);

            // If not already in the queue, start a new one
            if (entry == null) {
                entry = new CallableEntry(longCacheKey, callable, count, asyncId);
                this.queue.put(longCacheKey, entry);

                this.logger.debug("Added [{}] in the queue", entry);

                this.executor.submit(entry);
            } else {
                entry.addAsyncId(asyncId);
            }
        }
    }

    /**
     * Get and remove result of the asynchronous execution associated to the passed id.
     * 
     * @param asyncId the identifier of the asynchronous execution
     * @return the result of the asynchronous execution
     * @throws NotificationException if an exception was thrown by the asynchronous execution
     */
    public Object popAsync(String asyncId) throws NotificationException
    {
        Object result = this.shortCache.get(asyncId);

        if (result != null) {
            // Remove from the cache
            this.shortCache.remove(asyncId);
        }

        if (result instanceof Throwable) {
            throw new NotificationException("Asynchronous notifications gathering failed", (Throwable) result);
        }

        return result;
    }

    /**
     * @param cacheKey the cache key
     * @param count true if the value to return is a count instead of a list of events
     * @return the value associated with the passed cache key
     */
    public Object getFromCache(String cacheKey, boolean count)
    {
        if (count) {
            return this.longCountCache != null ? this.longCountCache.get(cacheKey) : null;
        } else {
            return this.longEventCache != null ? this.longEventCache.get(cacheKey) : null;
        }
    }

    private Object setInCache(String cacheKey, List<CompositeEvent> events, boolean count)
    {
        Object result;
        if (count) {
            result = events.size();

            if (this.longCountCache != null) {
                this.longCountCache.set(cacheKey, (Integer) result);
            }
        } else {
            result = events;

            if (this.longEventCache != null) {
                this.longEventCache.set(cacheKey, events);
            }
        }

        return result;
    }

    /**
     * Empty the long cache.
     */
    public void flushLongCache()
    {
        if (this.longEventCache != null) {
            this.longEventCache.removeAll();
        }

        if (this.longCountCache != null) {
            this.longCountCache.removeAll();
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (this.executor != null) {
            this.executor.shutdownNow();
        }

        if (this.shortCache != null) {
            this.shortCache.dispose();
        }

        if (this.longCountCache != null) {
            this.longCountCache.dispose();
        }
    }
}

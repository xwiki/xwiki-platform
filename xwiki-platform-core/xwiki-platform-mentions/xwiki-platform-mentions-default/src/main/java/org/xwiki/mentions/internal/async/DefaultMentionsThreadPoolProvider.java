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
package org.xwiki.mentions.internal.async;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.mentions.internal.MentionsThreadPoolProvider;

import static java.lang.Thread.NORM_PRIORITY;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Default implementation of {@link MentionsThreadPoolProvider}.
 *
 * @version $Id$
 * @since 12.6RC1
 */
@Component
@Singleton
public class DefaultMentionsThreadPoolProvider implements MentionsThreadPoolProvider
{
    private static final int POOL_SIZE = 4;

    @Override
    public ThreadPoolExecutor initializePool()
    {
        return new MentionsThreadPoolExecutor(POOL_SIZE);
    }

    @Override
    public int getPoolSize()
    {
        return POOL_SIZE;
    }

    /**
     * A {@link ThreadPoolExecutor} dedicated to the asynchronous tasks of the mentions.
     *
     * @version $Id$
     * @since 12.6RC1
     */
    private static class MentionsThreadPoolExecutor extends ThreadPoolExecutor
    {
        /**
         * Default constructor.
         *
         * @param poolSize size of the thead pool.
         */
        MentionsThreadPoolExecutor(int poolSize)
        {
            super(poolSize, poolSize, 0L, MILLISECONDS, new LinkedBlockingQueue<>());
            setThreadFactory(new MentionsThreadFactory());
        }

        /**
         * {@link ThreadFactory} dedicated to asynchronous tasks needed for the mentions.
         *
         * @version $Id$
         * @since 12.6RC1
         */
        private static class MentionsThreadFactory implements ThreadFactory
        {
            private static final String THREAD_NAME = "Mentions pool thread";

            private final ThreadFactory threadFactory = Executors.defaultThreadFactory();

            @Override
            public Thread newThread(Runnable runnable)
            {
                Thread thread = this.threadFactory.newThread(runnable);
                thread.setName(THREAD_NAME);
                thread.setPriority(NORM_PRIORITY - 1);
                return thread;
            }
        }
    }
}

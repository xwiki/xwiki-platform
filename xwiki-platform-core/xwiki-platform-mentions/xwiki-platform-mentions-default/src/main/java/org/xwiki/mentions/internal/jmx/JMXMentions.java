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
package org.xwiki.mentions.internal.jmx;

import java.util.function.Supplier;

/**
 * Implementation of the Mentions JXM MBean.
 *
 * @version $Id$
 * @since 12.6
 */
public class JMXMentions implements JMXMentionsMBean
{
    private final Supplier<Long> queueSize;

    private final Supplier<Integer> threadNumber;

    private final Runnable clearQueue;

    /**
     * Default construct.
     * @param queueSize The mentions analysis task queue size.
     * @param clearQueue The method to call on clear queue
     * @param threadNumber The current number of threads
     */
    public JMXMentions(Supplier<Long> queueSize, Runnable clearQueue, Supplier<Integer> threadNumber)
    {
        this.queueSize = queueSize;
        this.threadNumber = threadNumber;
        this.clearQueue = clearQueue;
    }

    @Override
    public long getQueueSize()
    {
        return this.queueSize.get();
    }

    @Override
    public void clearQueue()
    {
        this.clearQueue.run();
    }

    @Override
    public Integer getThreadNumber()
    {
        return this.threadNumber.get();
    }
}

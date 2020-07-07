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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A {@link ThreadPoolExecutor} dedicated to the asynchronous tasks of the mentions.
 *
 * @version $Id$
 * @since 12.6RC1
 */
public class MentionsThreadPoolExecutor extends ThreadPoolExecutor
{
    /**
     * Default constructor.
     * 
     * @param poolSize size of the thead pool.
     */
    public MentionsThreadPoolExecutor(int poolSize)
    {
        super(poolSize, poolSize, 0L, MILLISECONDS, new LinkedBlockingQueue<>());
        setThreadFactory(new MentionsThreadFactory());
    }
}

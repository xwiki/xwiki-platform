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
package org.xwiki.index.internal.jmx;

import java.util.Map;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Implementation of the JMXTasks MBean.
 *
 * @version $Id$
 * @since 14.1RC1
 */
public class JMXTasks implements JMXTasksMBean
{
    private final LongSupplier queueSize;

    private final Supplier<Map<String, Long>> queueSizePerType;

    /**
     * Default constructor, let the method initializing the MBean provide the suppliers for the MBean operations.
     *
     * @param queueSize the queue size supplier
     * @param queueSizePerType the queue size per type supplier
     */
    public JMXTasks(LongSupplier queueSize, Supplier<Map<String, Long>> queueSizePerType)
    {
        this.queueSize = queueSize;
        this.queueSizePerType = queueSizePerType;
    }

    @Override
    public long getQueueSize()
    {
        return this.queueSize.getAsLong();
    }

    @Override
    public Map<String, Long> getQueueSizePerType()
    {
        return this.queueSizePerType.get();
    }
}

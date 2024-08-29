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
package org.xwiki.activeinstalls2.internal.data;

/**
 * Represents memory-related Ping data.
 *
 * @version $Id$
 * @since 14.4RC1
 */
public class MemoryPing
{
    private long max;

    private long total;

    private long free;

    private long used;

    /**
     * @return the maximum amount of memory that the Java virtual machine will attempt to use
     */
    public long getMax()
    {
        return max;
    }

    /**
     * @param max see {@link #getMax()}
     */
    public void setMax(long max)
    {
        this.max = max;
    }

    /**
     * @return the total amount of memory in the Java virtual machine
     */
    public long getTotal()
    {
        return this.total;
    }

    /**
     * @param total see {@link #getTotal()}
     */
    public void setTotal(long total)
    {
        this.total = total;
    }

    /**
     * @return the amount of free memory in the Java Virtual Machine.
     */
    public long getFree()
    {
        return this.free;
    }

    /**
     * @param free see {@link #getFree()}
     */
    public void setFree(long free)
    {
        this.free = free;
    }

    /**
     * @return the total memory minus the free memory
     */
    public long getUsed()
    {
        return this.used;
    }

    /**
     * @param used see {@link #getUsed()}
     */
    public void setUsed(long used)
    {
        this.used = used;
    }
}

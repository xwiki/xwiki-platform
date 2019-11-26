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
package org.xwiki.rendering.async.internal;

import java.util.Set;

/**
 * Contains async framework properties.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
public class AsyncProperties
{
    private final boolean asyncAllowed;

    private final boolean cacheAllowed;

    private final Set<String> contextElements;

    /**
     * Create with default asynchronous properties.
     */
    public AsyncProperties()
    {
        this(false, false, null);
    }

    /**
     * @param asyncAllowed true if asynchronous execution is allowed
     * @param cacheAllowed true if caching of the execution result is allowed
     * @param contextElements the context elements required for the execution
     */
    public AsyncProperties(boolean asyncAllowed, boolean cacheAllowed, Set<String> contextElements)
    {
        this.asyncAllowed = asyncAllowed;
        this.cacheAllowed = cacheAllowed;
        this.contextElements = contextElements;
    }

    /**
     * @return true if asynchronous execution is allowed
     */
    public boolean isAsyncAllowed()
    {
        return this.asyncAllowed;
    }

    /**
     * @return true if caching of the execution result is allowed
     */
    public boolean isCacheAllowed()
    {
        return this.cacheAllowed;
    }

    /**
     * @return the context elements required for the execution
     */
    public Set<String> getContextElements()
    {
        return this.contextElements;
    }
}

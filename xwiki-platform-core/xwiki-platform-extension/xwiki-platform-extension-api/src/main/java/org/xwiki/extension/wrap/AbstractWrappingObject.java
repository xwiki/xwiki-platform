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
package org.xwiki.extension.wrap;

/**
 * Wrap an Object.
 * 
 * @param <T> the type of the wrapped object
 * @version $Id$
 */
public abstract class AbstractWrappingObject<T>
{
    /**
     * @see #getWrapped()
     */
    private T wrapped;

    /**
     * @param wrapped the wrapped object
     */
    public AbstractWrappingObject(T wrapped)
    {
        this.wrapped = wrapped;
    }

    /**
     * @return the wrapped object
     */
    protected T getWrapped()
    {
        return this.wrapped;
    }

    // Object

    @Override
    public int hashCode()
    {
        return getWrapped().hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return getWrapped().equals(obj);
    }

    @Override
    public String toString()
    {
        return getWrapped().toString();
    }
}

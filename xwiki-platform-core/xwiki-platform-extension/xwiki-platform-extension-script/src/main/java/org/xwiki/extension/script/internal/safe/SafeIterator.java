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
package org.xwiki.extension.script.internal.safe;

import java.lang.reflect.Constructor;
import java.util.Iterator;

import org.xwiki.extension.internal.safe.AbstractSafeObject;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;

/**
 * Provide a public script access to an iterator.
 * 
 * @param <E> the type of the iterated value
 * @param <I> the type of the Iterator
 * @version $Id$
 * @since 4.1M1
 */
public class SafeIterator<E, I extends Iterator<E>> extends AbstractSafeObject<I> implements Iterator<E>
{
    /**
     * Safe implementation of the iterator elements.
     */
    private Constructor< ? extends E> safeConstructor;

    /**
     * @param it the wrapped iterator
     * @param safeProvider the provider of instances safe for public scripts
     * @param safeConstructor the constructor used to create new safe wrapper for iterator elements
     */
    public SafeIterator(I it, ScriptSafeProvider< ? > safeProvider, Constructor< ? extends E> safeConstructor)
    {
        super(it, safeProvider);

        this.safeConstructor = safeConstructor;
    }

    /**
     * @param element the element to wrap
     * @return the wrapped element
     */
    protected E safeElement(E element)
    {
        if (this.safeConstructor != null) {
            try {
                return this.safeConstructor.newInstance(element, this.safeProvider);
            } catch (Exception e) {
                return safe(element);
            }
        } else {
            return safe(element);
        }
    }

    // Iterator
    @Override
    public boolean hasNext()
    {
        return getWrapped().hasNext();
    }

    @Override
    public E next()
    {
        return safeElement(getWrapped().next());
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }
}

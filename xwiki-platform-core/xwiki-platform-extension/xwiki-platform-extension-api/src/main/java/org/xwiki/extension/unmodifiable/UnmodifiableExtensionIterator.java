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
package org.xwiki.extension.unmodifiable;

import java.util.Iterator;

import org.xwiki.extension.Extension;

/**
 * Provide a readonly access to an iterator on an extension.
 * 
 * @param <E> the extension type
 * @version $Id$
 */
public class UnmodifiableExtensionIterator<E extends Extension> implements Iterator<E>
{
    /**
     * The wrapped iterator.
     */
    private Iterator<E> iterator;

    /**
     * @param iterator the wrapped iterator
     */
    public UnmodifiableExtensionIterator(Iterator<E> iterator)
    {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext()
    {
        return this.iterator.hasNext();
    }

    @Override
    public E next()
    {
        return UnmodifiableUtils.unmodifiableExtension(this.iterator.next());
    }

    @Override
    public void remove()
    {
        throw new ForbiddenException("Calling remove is forbidden in readonly proxy");
    }

}

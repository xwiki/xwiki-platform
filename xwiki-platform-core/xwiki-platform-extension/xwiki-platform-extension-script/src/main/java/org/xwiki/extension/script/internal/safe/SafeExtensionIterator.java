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

import java.util.Iterator;

import org.xwiki.extension.Extension;
import org.xwiki.extension.internal.safe.AbstractSafeObject;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;

/**
 * Provide a public script access to an iterator on an extension.
 * 
 * @param <E> the extension type
 * @version $Id$
 * @since 4.0M2
 */
public class SafeExtensionIterator<E extends Extension> extends AbstractSafeObject<Iterator<E>> implements Iterator<E>
{
    /**
     * @param iterator the wrapped iterator
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafeExtensionIterator(Iterator<E> iterator, ScriptSafeProvider< ? > safeProvider)
    {
        super(iterator, safeProvider);
    }

    @Override
    public boolean hasNext()
    {
        return getWrapped().hasNext();
    }

    @Override
    public E next()
    {
        return safe(getWrapped().next());
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("Calling remove is forbidden in readonly proxy");
    }
}

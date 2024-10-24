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
package org.xwiki.container;

import java.util.Collections;
import java.util.Enumeration;

import org.xwiki.stability.Unstable;

/**
 * Represents a session.
 * 
 * @version $Id$
 */
public interface Session
{
    /**
     * Returns the object bound with the specified name in this session, or <code>null</code> if no object is bound
     * under the name.
     *
     * @param name a string specifying the name of the object
     * @return the object with the specified name
     * @exception IllegalStateException if this method is called on an invalidated session
     * @since 42.0.0
     */
    @Unstable
    default Object getAttribute(String name)
    {
        return null;
    }

    /**
     * Binds an object to this session, using the name specified. If an object of the same name is already bound to the
     * session, the object is replaced.
     * <p>
     * If the value passed in is null, this has the same effect as calling <code>removeAttribute()<code>.
     *
     * @param name the name to which the object is bound; cannot be null
     * @param value the object to be bound
     * @exception IllegalStateException if this method is called on an invalidated session
     * @since 42.0.0
     */
    @Unstable
    default void setAttribute(String name, Object value)
    {

    }

    /**
     * Removes the object bound with the specified name from this session. If the session does not have an object bound
     * with the specified name, this method does nothing.
     *
     * @param name the name of the object to remove from this session
     * @exception IllegalStateException if this method is called on an invalidated session
     * @since 42.0.0
     */
    @Unstable
    default void removeAttribute(String name)
    {

    }

    /**
     * Returns an <code>Enumeration</code> of <code>String</code> objects containing the names of all the objects bound
     * to this session.
     *
     * @return an <code>Enumeration</code> of <code>String</code> objects specifying the names of all the objects bound
     *         to this session
     * @exception IllegalStateException if this method is called on an invalidated session
     * @since 42.0.0
     */
    @Unstable
    default Enumeration<String> getAttributeNames()
    {
        return Collections.emptyEnumeration();
    }
}

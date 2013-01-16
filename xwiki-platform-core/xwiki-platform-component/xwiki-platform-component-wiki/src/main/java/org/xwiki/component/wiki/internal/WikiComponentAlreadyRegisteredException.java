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
package org.xwiki.component.wiki.internal;

import org.xwiki.component.wiki.WikiComponentException;

/**
 * An exception when trying to register a component which is already registered.
 *
 * @version $Id$
 * @since 4.4.1
 */
public class WikiComponentAlreadyRegisteredException extends WikiComponentException
{
    /**
     * Constructor of this exception.
     *
     * @param message the message associated with the exception
     */
    public WikiComponentAlreadyRegisteredException(String message)
    {
        super(message);
    }

    /**
     * Constructor of this exception.
     *
     * @param message the message associated with the exception
     * @param t the root cause
     */
    public WikiComponentAlreadyRegisteredException(String message, Throwable t)
    {
        super(message, t);
    }
}

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
package org.xwiki.mail.internal.thread.context;

import org.apache.commons.lang3.exception.CloneFailedException;
import org.xwiki.component.annotation.Role;

/**
 * Generic component in charge of cloning a given object of a given type. The main purpose of this is to add
 * {@link java.lang.Cloneable}-like behavior on objects that don't implement it.
 *
 * @param <T> the type of object to clone
 * @version $Id$
 * @since 7.1M2
 */
@Role
public interface Cloner<T>
{
    /**
     * @param original the original object to clone
     * @return a clone of the original object
     * @throws CloneFailedException if problems occur
     */
    T clone(T original) throws CloneFailedException;
}

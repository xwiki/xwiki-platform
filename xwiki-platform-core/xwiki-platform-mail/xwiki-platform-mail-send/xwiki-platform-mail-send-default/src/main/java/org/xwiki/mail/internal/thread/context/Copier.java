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
 * Generic component in charge of copying a given object of a given type. The main purpose of this is to add
 * {@link java.lang.Cloneable}-like behavior on objects that don't implement it.
 * <p>
 * Depending on the implementation, this can be different from {@link Object#clone()} as the result might need to be
 * modified to make sense on its own (e.g.: cloning a {@link javax.servlet.http.HttpServletRequest HttpServletRequest}).
 *
 * @param <T> the type of object to copy
 * @version $Id$
 * @since 7.1M2
 */
@Role
public interface Copier<T>
{
    /**
     * @param original the original object to copy
     * @return a copy of the original object
     * @throws CloneFailedException if problems occur
     */
    T copy(T original) throws CloneFailedException;
}

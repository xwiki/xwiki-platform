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
package org.xwiki.component.wiki;

/**
 * Utility for wiki methods to return a value. An implementation of this interface is binded in the context of a
 * wiki method execution, so that such method scripts can return a value using {@link #returnValue(Object)}.
 * 
 * @version $Id$
 * @since 4.2M3
 */
public interface MethodOutputHandler
{
    /**
     * Stores a value in the method invocation context for further return.
     * Note that if this method is called multiple times during the invocation, the last one wins.
     * 
     * @param value the value to return
     */
    void returnValue(Object value);
 
    /**
     * @return the current stored return value (null if not set yet).
     */
    Object getReturnValue();   
}

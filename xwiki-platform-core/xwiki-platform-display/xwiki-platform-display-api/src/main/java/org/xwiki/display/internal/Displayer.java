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
package org.xwiki.display.internal;

import org.xwiki.rendering.block.XDOM;

/**
 * Component used to display data.
 * 
 * @param <T> the type of data displayed by this class
 * @param <P> the type of the display parameters bean
 * @version $Id$
 * @since 3.2M3
 */
public interface Displayer<T, P>
{
    /**
     * Displays the given data.
     * 
     * @param data the data to be displayed
     * @param parameters display parameters
     * @return the result of displaying the given data
     */
    XDOM display(T data, P parameters);
}

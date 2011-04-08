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
package org.xwiki.rendering.internal.parser.pygments;

import java.util.Map;

/**
 * Interface to implement for a Pygments formatter listener.
 * 
 * @version $Id$
 * @since 1.7RC1
 */
public interface PygmentsListener
{
    /**
     * Called by Pygments each time token type changed.
     * 
     * @param tokenType the token type.
     * @param value the value.
     * @param style the highlight style.
     */
    void format(String tokenType, String value, Map<String, Object> style);
}

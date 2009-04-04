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
package org.xwiki.rendering.macro;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Tag interface representing an aggregated {@link MacroManager}. A macro source is a specific macro manager that knows
 * how to retrieve macros in a certain manner.
 * Examples of macro sources can be:
 * <ul>
 * <li>A java class loader source that provides macros directly from compiled java classes</li>
 * <li>A wiki macro source that creates instances of macros upon code and configuration retrieved in macro pages</li>
 * <li>A macro source that consumes a web service to load macros whose definition is located on a distant server</li>
 * </ul>
 * 
 * @since 1.9M1
 * @version $Id$
 */
@ComponentRole
public interface MacroSource extends MacroManager
{
}

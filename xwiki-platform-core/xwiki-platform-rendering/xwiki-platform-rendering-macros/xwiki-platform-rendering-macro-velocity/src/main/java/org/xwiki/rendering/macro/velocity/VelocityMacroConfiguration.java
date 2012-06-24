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
package org.xwiki.rendering.macro.velocity;

import org.xwiki.component.annotation.Role;

/**
 * Configuration properties for the Velocity macro.
 * <p>
 * You can override the default values for each of the configuration properties below by defining them in XWiki's global
 * configuration file using a prefix of "rendering.macro.velocity" followed by the property name. For example:
 * <code>rendering.macro.velocity.cleaner = none</code>
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Role
public interface VelocityMacroConfiguration
{
    /**
     * @return the hint of the {@link org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter} component to use
     *         to modify velocity content before or after script execution.
     */
    String getFilter();
}

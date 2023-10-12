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
package com.xpn.xwiki.render;

import java.util.Set;

import org.apache.velocity.VelocityContext;

/**
 * Maintains the current ScriptContext in sync with any modification of the VelocityContext.
 * 
 * @version $Id$
 * @since 8.3M1
 * @deprecated use {@link org.xwiki.velocity.ScriptVelocityContext} instead
 */
@Deprecated(since = "15.9RC1")
public class ScriptVelocityContext extends org.xwiki.velocity.ScriptVelocityContext
{
    /**
     * @param parent the initial Velocity context
     * @param reservedBindings the binding that should not be synchronized
     */
    public ScriptVelocityContext(VelocityContext parent, Set<String> reservedBindings)
    {
        super(parent, reservedBindings);
    }

    /**
     * @param parent the initial Velocity context
     * @param logDeprecated true if use of deprecated binding should be logged
     * @param reservedBindings the binding that should not be synchronized
     * @since 12.10
     * @since 12.6.5
     */
    public ScriptVelocityContext(VelocityContext parent, boolean logDeprecated, Set<String> reservedBindings)
    {
        super(parent, logDeprecated, reservedBindings);
    }
}

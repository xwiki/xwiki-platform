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
package org.xwiki.uiextension.internal.parameters;

import org.apache.velocity.VelocityContext;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.XWikiVelocityContext;

/**
 * The Velocity engine and context used to evaluate the Velocity parameter values of one UI extension, obtained once
 * for all its values.
 *
 * @param engine the Velocity engine
 * @param context the current Velocity context
 * @param deprecatedLogEnabled whether the use of deprecated Velocity bindings should be logged
 * @version $Id$
 * @since 18.10.0RC1
 */
public record VelocityParameterSession(VelocityEngine engine, VelocityContext context, boolean deprecatedLogEnabled)
{
    /**
     * Creates a new Velocity context for evaluating a single value. The current context is used as inner context, so
     * existing bindings can be read, but variables set by the value don't leak into the current context or into the
     * other values.
     *
     * @return the new Velocity context
     */
    public VelocityContext newContext()
    {
        return new XWikiVelocityContext(this.context, this.deprecatedLogEnabled);
    }
}

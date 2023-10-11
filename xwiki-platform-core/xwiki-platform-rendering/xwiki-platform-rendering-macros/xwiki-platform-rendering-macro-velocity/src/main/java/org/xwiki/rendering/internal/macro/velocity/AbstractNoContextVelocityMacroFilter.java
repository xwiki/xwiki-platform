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
package org.xwiki.rendering.internal.macro.velocity;

import org.apache.velocity.VelocityContext;
import org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter;
import org.xwiki.velocity.VelocityTemplate;

/**
 * A helper for filters which don't manipulate the context.
 * 
 * @version $Id$
 * @since 15.9RC1
 */
public abstract class AbstractNoContextVelocityMacroFilter implements VelocityMacroFilter
{
    @Override
    public boolean isPreparationSupported()
    {
        return true;
    }

    @Override
    public String before(String content, VelocityContext velocityContext)
    {
        return prepare(content);
    }

    @Override
    public void before(VelocityTemplate content, VelocityContext velocityContext)
    {
        // Do nothing
    }

    @Override
    public String after(String content, VelocityContext velocityContext)
    {
        return content;
    }
}

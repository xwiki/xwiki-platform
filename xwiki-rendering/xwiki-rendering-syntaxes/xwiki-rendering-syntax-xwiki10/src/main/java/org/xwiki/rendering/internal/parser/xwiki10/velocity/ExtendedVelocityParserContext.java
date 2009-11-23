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
package org.xwiki.rendering.internal.parser.xwiki10.velocity;

import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.velocity.internal.util.VelocityParserContext;

/**
 * Extends {@link VelocityParserContext} for specifi needs.
 * 
 * @version $Id$
 */
public class ExtendedVelocityParserContext extends VelocityParserContext
{
    private boolean velocity = false;

    private boolean inline = true;

    private boolean conversion = false;

    private FilterContext filterContext;

    private boolean protectedBlock;

    public ExtendedVelocityParserContext(FilterContext filterContext)
    {
        this.filterContext = filterContext;
    }

    public boolean isVelocity()
    {
        return this.velocity;
    }

    public void setVelocity(boolean velocity)
    {
        this.velocity = velocity;
    }

    public boolean isConversion()
    {
        return this.conversion;
    }

    public void setConversion(boolean conversion)
    {
        this.conversion = conversion;
    }

    public boolean isInline()
    {
        return this.inline;
    }

    public void setInline(boolean inline)
    {
        this.inline = inline;
    }

    public FilterContext getFilterContext()
    {
        return this.filterContext;
    }

    public boolean isProtectedBlock()
    {
        return this.protectedBlock;
    }

    public void setProtectedBlock(boolean protectedBlock)
    {
        this.protectedBlock = protectedBlock;
    }
}

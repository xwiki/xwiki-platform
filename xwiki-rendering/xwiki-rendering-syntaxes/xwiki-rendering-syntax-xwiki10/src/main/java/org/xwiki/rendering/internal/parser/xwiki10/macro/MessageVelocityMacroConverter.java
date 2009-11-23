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
package org.xwiki.rendering.internal.parser.xwiki10.macro;

import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.internal.parser.xwiki10.VelocityFilter;
import org.xwiki.rendering.parser.xwiki10.Filter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.AbstractVelocityMacroConverter;

/**
 * @version $Id$
 * @since 2.0
 */
@Component(hints = {"info", "warning", "error"})
public class MessageVelocityMacroConverter extends AbstractVelocityMacroConverter
{
    @Requirement("velocity")
    private Filter velocityFilter;

    @Override
    protected String convertContent(List<String> parameters, FilterContext context)
    {
        String content = cleanQuotes(parameters.get(0));

        content = this.velocityFilter.filter(content, context);

        content = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(content).replaceFirst("");
        content = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(content).replaceFirst("");

        return this.velocityFilter.filter(content, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.xwiki10.macro.AbstractVelocityMacroConverter#protectResult()
     */
    @Override
    public boolean protectResult()
    {
        return false;
    }
}

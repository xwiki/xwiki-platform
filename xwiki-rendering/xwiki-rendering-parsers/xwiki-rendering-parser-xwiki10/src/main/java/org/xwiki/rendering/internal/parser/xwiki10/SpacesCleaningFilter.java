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
package org.xwiki.rendering.internal.parser.xwiki10;

import java.util.regex.Pattern;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;

/**
 * XWiki 1.0 does not interpret multiples spaces in paragraph. This is because it render in as is in XHTML (which does
 * not interpret them).
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("spacescleanning")
public class SpacesCleaningFilter extends AbstractFilter implements Initializable
{
    private static final Pattern SPACES_PATTERN = Pattern.compile("([^ ])[ \\t]+([^ ])");

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(4001);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.xwiki10.Filter#filter(java.lang.String,
     *      org.xwiki.rendering.parser.xwiki10.FilterContext)
     */
    public String filter(String content, FilterContext filterContext)
    {
        return SPACES_PATTERN.matcher(content).replaceAll("$1 $2");
    }
}

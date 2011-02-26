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
package org.xwiki.rendering.parser.xwiki10;

import java.util.regex.Pattern;

/**
 * Convert 1.0 bold syntax into 2.0 bold syntax.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public abstract class AbstractSyntaxFilter extends AbstractFilter
{
    private Pattern syntax10Pattern;

    private String syntax20;

    protected AbstractSyntaxFilter(Pattern syntax10Pattern, String syntax20)
    {
        this.syntax10Pattern = syntax10Pattern;
        this.syntax20 = syntax20;
    }

    public String filter(String content, FilterContext filterContext)
    {
        String protectedString = filterContext.addProtectedContent(this.syntax20, true);

        return this.syntax10Pattern.matcher(content).replaceAll(protectedString + "$1" + protectedString);
    }
}

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

import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;

/**
 * XWiki 1.0 does not interpret standalone new line in paragraph. This is because it render in as is in XHTML (which
 * does not interpret them).
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class StandaloneNewLineCleaningFilter extends AbstractFilter
{
    private static final Pattern SANDALONENEWLINE_PATTERN = Pattern.compile("([^\\00\\n])\\n([^\\00\\n])");

    public String filter(String content, FilterContext filterContext)
    {
        return SANDALONENEWLINE_PATTERN.matcher(content).replaceAll("$1 $2");
    }
}

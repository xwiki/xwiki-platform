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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The XWiki 1.0 to 2.0 conversion context.
 * <p>
 * A filter can register a part of the content to protect it. That way this content is not re-parsed by another filter.
 * for example VelocityCommentsFilter register all Velocity comments.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class FilterContext
{
    public static final String XWIKI1020TOKEN_O = "\1";

    public static final String XWIKI1020TOKEN_OP = "\\01";

    public static final String XWIKI1020TOKEN_C = "\1";

    public static final String XWIKI1020TOKEN_CP = "\\01";

    /**
     * Match registered content identifier.
     */
    public static final Pattern XWIKI1020TOKEN_PATTERN =
        Pattern.compile(XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKEN + "(\\p{Alpha}*)([\\d]+)" + XWIKI1020TOKEN_CP);

    public static final String XWIKI1020TOKEN = "XWIKI1020TOKEN";

    private List<String> protectedContentList = new LinkedList<String>();

    /**
     * Register a content and return the corresponding identifier to be able the reinsert it after the conversion
     * process.
     * 
     * @param content the content to protect/register.
     * @return the content identifier to insert in place of provided content.
     */
    public String addProtectedContent(String content)
    {
        return addProtectedContent(content, "");
    }

    public String addProtectedContent(String content, String suffix)
    {
        this.protectedContentList.add(content);

        return XWIKI1020TOKEN_O + XWIKI1020TOKEN + suffix + (this.protectedContentList.size() - 1) + XWIKI1020TOKEN_C;
    }

    /**
     * @param index the identifier of the registered content.
     * @return the registered content.
     */
    public String getProtectedContent(int index)
    {
        return this.protectedContentList.get(index);
    }
}

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

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
    public static final String XWIKI1020TOKEN_O = "\255";

    public static final String XWIKI1020TOKEN_OP = Pattern.quote(XWIKI1020TOKEN_O);

    public static final String XWIKI1020TOKEN_C = "\255";

    public static final String XWIKI1020TOKEN_CP = Pattern.quote(XWIKI1020TOKEN_C);

    private static final String XWIKI1020TOKEN_SF = "XWIKI1020TOKEN";

    private static final String XWIKI1020TOKENS_SF = XWIKI1020TOKEN_SF + "S";

    private static final String XWIKI1020TOKENI_SF = XWIKI1020TOKEN_SF + "I";

    public static final String XWIKI1020TOKEN_SF_SPATTERN = "(?:" + XWIKI1020TOKEN_SF + "[IS])";

    public static final String XWIKI1020TOKENS_SF_SPATTERN = "(?:" + XWIKI1020TOKEN_SF + "S)";

    public static final String XWIKI1020TOKENI_SF_SPATTERN = "(?:" + XWIKI1020TOKEN_SF + "I)";

    /**
     * Match registered inline content identifier.
     * <ul>
     * <li>$1: the suffix</li>
     * <li>$2: the index</li>
     * </ul>
     */
    public static final Pattern XWIKI1020TOKENI_PATTERN =
        Pattern.compile(XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENI_SF_SPATTERN + "(\\p{Alpha}*)(\\d+)"
            + XWIKI1020TOKEN_CP);

    /**
     * Match registered content identifier.
     * <ul>
     * <li>$1: the suffix</li>
     * <li>$2: the index</li>
     * </ul>
     */
    public static final Pattern XWIKI1020TOKENS_PATTERN =
        Pattern.compile(XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENS_SF_SPATTERN + "(\\p{Alpha}*)(\\d+)"
            + XWIKI1020TOKEN_CP);

    /**
     * Match registered content identifier.
     * <ul>
     * <li>$1: the suffix</li>
     * <li>$2: the index</li>
     * </ul>
     */
    public static final Pattern XWIKI1020TOKEN_PATTERN =
        Pattern.compile(XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKEN_SF_SPATTERN + "(\\p{Alpha}*)(\\d+)"
            + XWIKI1020TOKEN_CP);

    private List<String> protectedContentList = new LinkedList<String>();

    /**
     * Register a content and return the corresponding identifier to be able the reinsert it after the conversion
     * process.
     * 
     * @param content the content to protect/register.
     * @param inline indicate if i's a inline or not inline string.
     * @return the content identifier to insert in place of provided content.
     */
    public String addProtectedContent(String content, boolean inline)
    {
        return addProtectedContent(content, "", inline);
    }

    public String addProtectedContent(String content, String suffix, boolean inline)
    {
        if (StringUtils.isEmpty(content)) {
            return "";
        }

        this.protectedContentList.add(content);

        StringBuffer str = new StringBuffer();

        str.append(XWIKI1020TOKEN_O);
        str.append(inline ? XWIKI1020TOKENI_SF : XWIKI1020TOKENS_SF);
        str.append(suffix);
        str.append(this.protectedContentList.size() - 1);
        str.append(XWIKI1020TOKEN_C);

        return str.toString();
    }

    /**
     * @param index the identifier of the registered content.
     * @return the registered content.
     */
    public String getProtectedContent(int index)
    {
        return this.protectedContentList.get(index);
    }

    /**
     * Re-insert all protected/registered strings in to the global content.
     * 
     * @param content the global content.
     * @return the complete content.
     */
    public String unProtect(String content)
    {
        StringBuffer result = new StringBuffer();
        Matcher matcher = FilterContext.XWIKI1020TOKEN_PATTERN.matcher(content);

        int current = 0;
        while (matcher.find()) {
            result.append(content.substring(current, matcher.start()));
            current = matcher.end();

            int index = Integer.valueOf(matcher.group(2));

            String storedContent = getProtectedContent(index);

            result.append(storedContent);
        }

        if (current == 0) {
            return content;
        }

        result.append(content.substring(current));

        return unProtect(result.toString());
    }
}

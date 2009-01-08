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
 */
public class FilterContext
{
    /**
     * Match registered content identifier.
     */
    public static final Pattern XWIKI1020TOKEN_PATTERN =
        Pattern.compile("\\{" + FilterContext.XWIKI1020TOKEN + "([\\d]+)\\}");

    public static final String XWIKI1020TOKEN = "XWIKI1020TOKEN";

    private List<String> protectedContentList = new LinkedList<String>();

    /**
     * Register a cotent and return the corresponding identifier to be able the reinsert it after the conversion
     * process.
     * 
     * @param content the content to protect/register.
     * @return the content identifier to insert in place of provided content.
     */
    public String addProtectedContent(String content)
    {
        this.protectedContentList.add(content);

        return "{" + XWIKI1020TOKEN + (this.protectedContentList.size() - 1) + "}";
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

package org.xwiki.rendering.internal.parser.xwiki10;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;

/**
 * Convert 1.0 velocity macros to 2.0 macro. A conversion can be added by implementing VelocityMacroConverter.
 * 
 * @version $Id$
 */
public class VelocityCommentsFilter extends AbstractFilter
{
    public static final Pattern VELOCITY_COMMENT_PATTERN = Pattern.compile("((?m)\\#\\#.*$?)|((?s)\\#\\*(.*?)\\*\\#)");

    public String filter(String content, FilterContext filterContext)
    {
        content = filterMacros(content, filterContext);

        return content;
    }

    private String filterMacros(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();
        Matcher matcher = VELOCITY_COMMENT_PATTERN.matcher(content);
        int current = 0;
        while (matcher.find()) {
            result.append(content.substring(current, matcher.start()));
            current = matcher.end();

            String allcontent = matcher.group(0);

            result.append(filterContext.addProtectedContent(allcontent));
        }

        if (current == 0) {
            return content;
        }

        result.append(content.substring(current));

        return result.toString();
    }
}

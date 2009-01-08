package org.xwiki.rendering.internal.parser.xwiki10;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

public class PreFilter extends AbstractFilter
{
    private static final Pattern PRE_PATTERN =
        Pattern.compile("\\{pre\\}(.*?)\\{/pre\\}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public String filter(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        Matcher matcher = PRE_PATTERN.matcher(content);
        int current = 0;
        while (matcher.find()) {
            String before = content.substring(current, matcher.start());
            current = matcher.end();

            // a standalone new line is not interpreted by XWiki 1.0 rendering
            result.append(CleanUtil.removeLastStandaloneNewLine(before));

            result.append("{{{");
            result.append(filterContext.addProtectedContent(CleanUtil.cleanSpacesAndNewLines(matcher.group(1)).trim()));
            result.append("}}}");
        }

        if (current == 0) {
            return content;
        }

        result.append(content.substring(current));

        return result.toString();
    }
}

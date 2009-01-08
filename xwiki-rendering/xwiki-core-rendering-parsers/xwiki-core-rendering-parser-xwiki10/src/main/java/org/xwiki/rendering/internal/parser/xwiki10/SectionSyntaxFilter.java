package org.xwiki.rendering.internal.parser.xwiki10;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

public class SectionSyntaxFilter extends AbstractFilter
{
    private static final Pattern SECTIONSYNTAX_PATTERN =
        Pattern.compile("^[ \\t]*+(1(\\.1){0,5}+)[ \\t]++(.++)$", Pattern.MULTILINE);

    public String filter(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        Matcher matcher = SECTIONSYNTAX_PATTERN.matcher(content);
        int currentIndex = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            if (currentIndex > 0) {
                before = CleanUtil.setFirstNL(before, 2);
            }

            result.append(before);
            result.append(filterContext.addProtectedContent(StringUtils
                .repeat("=", (matcher.group(1).length() + 1) / 2) + ' '));
            result.append(matcher.group(3));
        }

        if (currentIndex == 0) {
            return content;
        }

        result.append(CleanUtil.setFirstNL(content.substring(currentIndex), 2));

        return result.toString();
    }
}

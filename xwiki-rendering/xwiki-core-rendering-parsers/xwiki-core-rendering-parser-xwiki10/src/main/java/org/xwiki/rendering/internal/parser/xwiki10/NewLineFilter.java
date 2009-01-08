package org.xwiki.rendering.internal.parser.xwiki10;

import java.util.regex.Pattern;

import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;

public class NewLineFilter extends AbstractFilter
{
    private static final Pattern MSNEWLINE_PATTERN = Pattern.compile("\\r\\n|\\r");

    public String filter(String content, FilterContext filterContext)
    {
        return MSNEWLINE_PATTERN.matcher(content).replaceAll("\n");
    }
}

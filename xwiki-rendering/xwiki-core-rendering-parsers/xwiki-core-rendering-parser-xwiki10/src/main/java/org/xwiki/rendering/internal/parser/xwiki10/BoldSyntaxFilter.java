package org.xwiki.rendering.internal.parser.xwiki10;

import java.util.regex.Pattern;

import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;

public class BoldSyntaxFilter extends AbstractFilter
{
    private static final Pattern BOLDSYNTAX_PATTERN =
        Pattern.compile("(?<!\\*)\\*([^\\p{Space}](?:[^*\n]*+|\\*)*?(?<=[^\\p{Space}]))\\*(?!\\*)");

    public String filter(String content, FilterContext filterContext)
    {
        return BOLDSYNTAX_PATTERN.matcher(content).replaceAll("**$1**");
    }
}

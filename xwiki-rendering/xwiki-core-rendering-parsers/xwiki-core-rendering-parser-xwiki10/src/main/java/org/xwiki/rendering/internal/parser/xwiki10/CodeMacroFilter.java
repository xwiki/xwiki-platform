package org.xwiki.rendering.internal.parser.xwiki10;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroConverter;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

public class CodeMacroFilter extends AbstractFilter
{
    private static final Pattern CODEMACRO_PATTERN =
        Pattern.compile("\\{(code)(?::([^\\}]*))?\\}(.*?)\\{code\\}", Pattern.MULTILINE);

    private RadeoxMacroConverter codeMacroCoverter;

    public String filter(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        Matcher matcher = CODEMACRO_PATTERN.matcher(content);
        int currentIndex = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            if (currentIndex > 0) {
                before = CleanUtil.setFirstNL(before, 2);
            }

            String macroResult =
                this.codeMacroCoverter.convert("code", RadeoxMacrosFilter.getMacroParameters(matcher.group(3)), matcher
                    .group(4));

            result.append(before);
            result.append(filterContext.addProtectedContent(macroResult));
        }

        if (currentIndex == 0) {
            return content;
        }

        result.append(CleanUtil.setFirstNL(content.substring(currentIndex), 2));

        return result.toString();
    }
}

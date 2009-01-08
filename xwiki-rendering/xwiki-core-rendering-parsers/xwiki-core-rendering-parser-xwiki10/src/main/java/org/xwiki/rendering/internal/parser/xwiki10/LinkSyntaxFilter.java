package org.xwiki.rendering.internal.parser.xwiki10;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;

public class LinkSyntaxFilter extends AbstractFilter
{
    private static final Pattern LINKSYNTAX_PATTERN = Pattern.compile("\\[(.+?)\\]");

    public String filter(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        Matcher matcher = LINKSYNTAX_PATTERN.matcher(content);
        int current = 0;
        while (matcher.find()) {
            result.append(content.substring(current, matcher.start()));
            current = matcher.end();

            StringBuffer linkResult = new StringBuffer();
            linkResult.append("[[");

            String str = matcher.group(1);
            if (str != null) {
                // TODO: This line creates bug XWIKI-188. The encoder seems to be broken. Fix this!
                // The only unescaping done should be %xx => char,
                // since &#nnn; must be preserved (the active encoding cannot handle the character)
                // and + should be preserved (for "Doc.C++ examples").
                // Anyway, this unescaper only treats &#nnn;
                // trim the name and unescape it
                // str = Encoder.unescape(str.trim());
                str = str.trim();
                String text = null, href = null, target = null;

                // Is there an alias like [alias|link] ?
                int pipeIndex = str.indexOf('|');
                int pipeLength = 1;
                if (pipeIndex == -1) {
                    pipeIndex = str.indexOf('>');
                }
                if (pipeIndex == -1) {
                    pipeIndex = str.indexOf("&gt;");
                    pipeLength = 4;
                }
                if (-1 != pipeIndex) {
                    text = str.substring(0, pipeIndex).trim();
                    str = str.substring(pipeIndex + pipeLength);
                }

                // Is there a target like [alias|link|target] ?
                pipeIndex = str.indexOf('|');
                pipeLength = 1;
                if (pipeIndex == -1) {
                    pipeIndex = str.indexOf('>');
                }
                if (pipeIndex == -1) {
                    pipeIndex = str.indexOf("&gt;");
                    pipeLength = 4;
                }
                if (-1 != pipeIndex) {
                    target = str.substring(pipeIndex + pipeLength).trim();
                    str = str.substring(0, pipeIndex);
                }
                // Done splitting

                // Fill in missing components
                href = str.trim();

                // Done, now print the link
                if (text != null) {
                    linkResult.append(text);
                    linkResult.append(">>");
                }

                linkResult.append(href);

                if (target != null) {
                    linkResult.append("||target=");
                    linkResult.append(target);
                }
            }

            linkResult.append("]]");

            result.append(filterContext.addProtectedContent(linkResult.toString()));
        }

        if (current == 0) {
            return content;
        }

        result.append(content.substring(current));

        return result.toString();
    }
}

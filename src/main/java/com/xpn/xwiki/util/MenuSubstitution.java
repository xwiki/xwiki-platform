package com.xpn.xwiki.util;

import org.apache.oro.text.regex.*;
import com.xpn.xwiki.render.XWikiWikiBaseRenderer;
import com.xpn.xwiki.render.WikiSubstitution;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 6 oct. 2005
 * Time: 22:19:39
 * To change this template use File | Settings | File Templates.
 */
public class MenuSubstitution extends WikiSubstitution {

    public MenuSubstitution(com.xpn.xwiki.util.Util util) {
        super(util,"\\\"\\.\\./\\.\\./view/(.*/.*)\\\"");
    }

    public void appendSubstitution(StringBuffer stringBuffer, MatchResult matchResult, int i, PatternMatcherInput patternMatcherInput, PatternMatcher patternMatcher, Pattern pattern) {
        String page = matchResult.group(1);
        stringBuffer.append("\"$xwiki.getURL(\"");
        stringBuffer.append(page.replace('/', '.'));
        stringBuffer.append("\",\"view\")\"");
    }
}

package com.xpn.xwiki.render;

import org.apache.oro.text.regex.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 22 févr. 2004
 * Time: 21:20:11
 * To change this template use File | Settings | File Templates.
 */
public class PreTagSubstitution extends WikiSubstitution {
    private int counter = 0;
    private List list = new ArrayList();

    public PreTagSubstitution(com.xpn.xwiki.util.Util util) {
        super(util);
        setPattern("<pre>.*?</pre>", Perl5Compiler.CASE_INSENSITIVE_MASK | Perl5Compiler.SINGLELINE_MASK);
    }

    public void appendSubstitution(StringBuffer stringBuffer, MatchResult matchResult, int i, PatternMatcherInput minput, PatternMatcher patternMatcher, Pattern pattern) {
        String content = matchResult.group(0);
        content = getUtil().substitute("s/<pre>//ig", content);
        content = getUtil().substitute("s/<\\/pre>//ig", content);
        getList().add(content);
        stringBuffer.append("%_" + counter + "_%");
        counter++;
    }

    public List getList() {
        return list;
    }

    public void setList(List list) {
        this.list = list;
    }

    public String insertNonWikiText(String content) {
        for (int i=0;i<list.size();i++)
            content = StringUtils.replace(content, "%_" + i + "_%", (String) list.get(i));
         return content;
    }

}

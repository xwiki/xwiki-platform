/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.render;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;

public class WikiNameSubstitution extends WikiSubstitution
{
    public static final int TYPE_ONE = 1;

    public static final int TYPE_TWO = 2;

    public static final int TYPE_THREE = 3;

    public static final int TYPE_FOUR = 4;

    private static final String onepattern =
        "([\\*\\s][\\(\\-\\*\\s]*)([A-Z]+[a-z]*)\\.([A-Z]+[a-z]+[A-Z]+[a-zA-Z0-9]*)(\\#[a-zA-Z0-9_]*)";

    private static final String twopattern =
        "([\\*\\s][\\(\\-\\*\\s]*)([A-Z]+[a-z]*)\\.([A-Z]+[a-z]+[A-Z]+[a-zA-Z0-9]*)";

    private static final String threepattern =
        "([\\*\\s][\\(\\-\\*\\s]*)([A-Z]+[a-z]+[A-Z]+[a-zA-Z0-9]*)(\\#[a-zA-Z0-9_]*)";

    private static final String fourpattern = "([\\*\\s][\\(\\-\\*\\s]*)([A-Z]+[a-z]+[A-Z]+[a-zA-Z0-9]*)";

    /*
     * # 'Web.TopicName#anchor' link:
     * s/([\*\s][\(\-\*\s]*)([A-Z]+[a-z]*)\.([A-Z]+[a-z]+[A-Z]+[a-zA-Z0-9]*)(\#[a-zA-Z0-9_]*)/&internalLink($1,$2,$3,"$TranslationToken$3$4$TranslationToken",$4,1)/geo; #
     * 'Web.TopicName' link:
     * s/([\*\s][\(\-\*\s]*)([A-Z]+[a-z]*)\.([A-Z]+[a-z]+[A-Z]+[a-zA-Z0-9]*)/&internalLink($1,$2,$3,"$TranslationToken$3$TranslationToken","",1)/geo; #
     * 'TopicName#anchor' link:
     * s/([\*\s][\(\-\*\s]*)([A-Z]+[a-z]+[A-Z]+[a-zA-Z0-9]*)(\#[a-zA-Z0-9_]*)/&internalLink($1,$theWeb,$2,"$TranslationToken$2$3$TranslationToken",$3,1)/geo; #
     * 'TopicName' link: s/([\*\s][\(\-\*\s]*)([A-Z]+[a-z]+[A-Z]+[a-zA-Z0-9]*)/&internalLink($1,$theWeb,$2,$2,"",1)/geo;
     */

    private int type;

    private XWikiContext context;

    public WikiNameSubstitution(XWikiContext context, int type, Util util)
    {
        super(util);
        this.setType(type);
        this.setContext(context);
    }

    @Override
    public void appendSubstitution(StringBuffer stringBuffer, MatchResult matchResult, int i,
        PatternMatcherInput minput, PatternMatcher patternMatcher, Pattern pattern)
    {
        switch (getType()) {
            case TYPE_ONE:
                XWikiWikiBaseRenderer.internalLink(stringBuffer, matchResult.group(1), matchResult.group(2),
                    matchResult.group(3), matchResult.group(3) + matchResult.group(4), matchResult.group(4), true,
                    getContext(), getUtil());
                break;
            case TYPE_TWO:
                XWikiWikiBaseRenderer.internalLink(stringBuffer, matchResult.group(1), matchResult.group(2),
                    matchResult.group(3), matchResult.group(3), matchResult.group(4), true, getContext(), getUtil());
                break;
            case TYPE_THREE:
                XWikiWikiBaseRenderer.internalLink(stringBuffer, matchResult.group(1), getDoc().getSpace(),
                    matchResult.group(2), matchResult.group(2) + matchResult.group(3), matchResult.group(3), true,
                    getContext(), getUtil());
                break;
            case TYPE_FOUR:
                XWikiWikiBaseRenderer.internalLink(stringBuffer, matchResult.group(1), getDoc().getSpace(),
                    matchResult.group(2), matchResult.group(2), "", true, getContext(), getUtil());
                break;
            default:
                stringBuffer.append(minput.getInput());
                break;
        }
    }

    public String substitute(String line, int type)
    {
        this.setType(type);
        switch (type) {
            case TYPE_ONE:
                setPattern(onepattern);
                break;
            case TYPE_TWO:
                setPattern(twopattern);
                break;
            case TYPE_THREE:
                setPattern(threepattern);
                break;
            case TYPE_FOUR:
                setPattern(fourpattern);
                break;
        }
        return super.substitute(line); // To change body of overriden methods use Options | File Templates.
    }

    public int getType()
    {
        return type;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public XWikiDocument getDoc()
    {
        return (XWikiDocument) getContext().get("doc");
    }

    public static String substitute(XWikiContext context, int type, Util util, String line)
    {
        return (new WikiNameSubstitution(context, type, util)).substitute(line, type);
    }

    public XWikiContext getContext()
    {
        return context;
    }

    public void setContext(XWikiContext context)
    {
        this.context = context;
    }
}

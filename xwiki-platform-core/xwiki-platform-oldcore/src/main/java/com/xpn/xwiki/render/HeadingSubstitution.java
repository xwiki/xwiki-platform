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

import com.xpn.xwiki.util.Util;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;

public class HeadingSubstitution extends WikiSubstitution
{
    public static final int HT = 0;

    public static final int DA = 1;

    private int type;

    public HeadingSubstitution(Util util, String patternparam, int type)
    {
        super(util, patternparam);
        this.type = type;
    }

    @Override
    public void appendSubstitution(StringBuffer stringBuffer, MatchResult matchResult, int i,
        PatternMatcherInput minput, PatternMatcher patternMatcher, Pattern pattern)
    {
        switch (type) {
            case HeadingSubstitution.DA:
                XWikiWikiBaseRenderer.makeHeading(stringBuffer, "" + matchResult.group(1).length(), matchResult
                    .group(2), getUtil());
                break;
            case HeadingSubstitution.HT:
                XWikiWikiBaseRenderer.makeHeading(stringBuffer, matchResult.group(1), matchResult.group(2), getUtil());
                break;
            default:
                stringBuffer.append(minput.getInput());
                break;
        }
    }

    public static String substitute(Util util, String pattern, int type, String line)
    {
        return (new HeadingSubstitution(util, pattern, type)).substitute(line);
    }

}

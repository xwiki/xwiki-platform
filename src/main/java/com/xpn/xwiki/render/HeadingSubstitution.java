/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 27 nov. 2003
 * Time: 17:43:13
 */
package com.xpn.xwiki.render;

import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;

import com.xpn.xwiki.util.Util;

public class HeadingSubstitution extends WikiSubstitution {
    public static final int HT = 0;
    public static final int DA = 1;

    private int type;

     public HeadingSubstitution(Util util, String patternparam, int type) {
       super(util, patternparam);
       this.type = type;
    }

    public void appendSubstitution(StringBuffer stringBuffer, MatchResult matchResult, int i, PatternMatcherInput minput, PatternMatcher patternMatcher, Pattern pattern) {
        switch (type) {
            case HeadingSubstitution.DA:
                XWikiWikiBaseRenderer.makeHeading(stringBuffer, "" + matchResult.group(1).length(), matchResult.group(2), getUtil());
                break;
            case HeadingSubstitution.HT:
                XWikiWikiBaseRenderer.makeHeading(stringBuffer, matchResult.group(1), matchResult.group(2), getUtil());
                break;
            default:
                stringBuffer.append(minput.getInput());
                break;
        }
    }

    public static String substitute(Util util, String pattern, int type, String line) {
     return (new HeadingSubstitution(util, pattern, type)).substitute(line);
    }

}

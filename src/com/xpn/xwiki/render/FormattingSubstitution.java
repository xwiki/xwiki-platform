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
 * Date: 28 nov. 2003
 * Time: 11:40:53
 */
package com.xpn.xwiki.render;

import com.xpn.xwiki.util.Util;
import org.apache.oro.text.regex.MatchResult;

public class FormattingSubstitution extends WikiSubstitution {

    private static final String postfixpattern = "([\\s\\,\\.\\;\\:\\!\\?\\)])";
    private static final String validcharpattern = "([^\\s]+?|[^\\s].*?[^\\s])";
    private static final String prefixpattern = "([\\s\\(])";

    public static final int TYPE_STRONG = 1;
    public static final int TYPE_ITALIC = 2;
    public static final int TYPE_STRONGITALIC = 3;
    public static final int TYPE_FIXED = 4;
    public static final int TYPE_BOLDFIXED = 5;

    private int type = 0;

    public FormattingSubstitution(Util util, int type) {
        super(util);
        setType(type);
        setUtil(util);
        switch (type) {
            case TYPE_STRONG:
                setPattern("\\*");
            break;
            case TYPE_ITALIC:
                setPattern("_");
            break;
            case TYPE_STRONGITALIC:
                setPattern("__");
                break;
            case TYPE_FIXED:
                setPattern("=");
                break;
            case TYPE_BOLDFIXED:
                setPattern("==");
            break;
        }
    }

    public String makePattern(String patternparam) {
        return prefixpattern + patternparam + validcharpattern  + patternparam + postfixpattern;
    }

    public String makeUnbreakingSpace(String text) {
        String result = getUtil().substitute("s/\\t/   /go", text);
        result = getUtil().substitute("s/\\s\\s/&nbsp; /go", result);
        result = getUtil().substitute("s/\\s\\s/ &nbsp;/go", result);
        return result;
    }

    public void prepareSubstitution(MatchResult matchResult) {
     switch (getType()) {
           case TYPE_STRONG:
                setSubstitution("$1<strong>$2</strong>$3");
            break;
            case TYPE_ITALIC:
                setSubstitution("$1<em>$2</em>$3");
            break;
            case TYPE_STRONGITALIC:
                setSubstitution("$1<strong><em>$2</em></strong>$3");
            break;
         case TYPE_FIXED:
                setSubstitution("$1<code>" + makeUnbreakingSpace(matchResult.group(2)) + "</code>$3");
             break;
         case TYPE_BOLDFIXED:
                setSubstitution("$1<code><b>" + makeUnbreakingSpace(matchResult.group(2)) + "</b></code>$3");
             break;
         default:
                setSubstitution("$&");
             break;
     }
    }

   public static String substitute(Util util, int type, String line) {
    return (new FormattingSubstitution(util, type)).substitute(line);
   }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}

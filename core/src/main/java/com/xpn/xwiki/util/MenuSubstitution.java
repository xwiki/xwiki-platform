/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author ludovic
 */
package com.xpn.xwiki.util;

import com.xpn.xwiki.render.WikiSubstitution;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;

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
        stringBuffer.append(page.replaceAll("/", "."));
        stringBuffer.append("\",\"view\")\"");
    }
}

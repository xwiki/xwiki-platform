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

import org.apache.commons.lang3.StringUtils;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;

import com.xpn.xwiki.util.Util;

public class ListSubstitution extends WikiSubstitution
{
    private static final int TYPE_DL = 1;

    private static final int TYPE_UL1 = 2;

    private static final int TYPE_UL2 = 3;

    private static final int TYPE_OL = 4;

    private static final String dlpattern = "^(\\t+)(\\S+?):\\s";

    private static final String ulpattern1 = "^(\\t+)\\* ";

    private static final String ulpattern2 = "^((\\s\\s\\s)+)\\* ";

    private static final String olpattern = "^(\\t+)\\d+\\.?";

    private boolean finished = false;

    private int currenttype = 0;

    private String currentline = null;

    private StringBuilder currentList = new StringBuilder();

    public ListSubstitution(Util util)
    {
        super(util);
    }

    @Override
    public void appendSubstitution(StringBuffer stringBuffer, MatchResult matchResult, int i,
        PatternMatcherInput minput, PatternMatcher patternMatcher, Pattern pattern)
    {
        setSubstitution(" ");
        super.appendSubstitution(stringBuffer, matchResult, i, minput, patternMatcher, pattern);
        int length;
        if (this.currenttype == TYPE_UL2) {
            length = matchResult.group(1).length() / 3;
        } else {
            length = matchResult.group(1).length();
        }

        String text = this.currentline.substring(matchResult.endOffset(0));
        String itemdelim = "li";
        String groupdelim = "ul";
        switch (this.currenttype) {
            case TYPE_DL:
                itemdelim = "dd";
                groupdelim = "dt";
                break;
            case TYPE_OL:
                groupdelim = "ol";
                break;
        }

        for (int nb = 0; nb < length; nb++) {
            this.currentList.append("<");
            this.currentList.append(groupdelim);
            this.currentList.append(">");
        }
        this.currentList.append("<");
        this.currentList.append(itemdelim);
        this.currentList.append("> ");
        this.currentList.append(text);
        this.currentList.append("</");
        this.currentList.append(itemdelim);
        this.currentList.append(">\n");
        for (int nb = 0; nb < length; nb++) {
            this.currentList.append("</");
            this.currentList.append(groupdelim);
            this.currentList.append(">");
        }
        this.finished = false;
        this.currentline = null;
    }

    public String substitute(String line, int type)
    {
        this.currenttype = type;
        switch (type) {
            case TYPE_DL:
                setPattern(dlpattern);
                break;
            case TYPE_UL1:
                setPattern(ulpattern1);
                break;
            case TYPE_UL2:
                setPattern(ulpattern2);
                break;
            case TYPE_OL:
                setPattern(olpattern);
                break;
        }
        return super.substitute(line);
    }

    public String handleList(String line)
    {
        if (StringUtils.isBlank(line)) {
            line = "<p />";
            this.finished = true;
        }
        if (line.matches("\\S++.*")) {
            this.finished = true;
        }

        // Handle the lists
        this.currentline = line;
        if (this.currentline != null) {
            substitute(this.currentline, TYPE_DL);
        }
        if (this.currentline != null) {
            substitute(this.currentline, TYPE_UL1);
        }
        if (this.currentline != null) {
            substitute(this.currentline, TYPE_UL2);
        }
        if (this.currentline != null) {
            substitute(this.currentline, TYPE_OL);
        }
        return this.currentline;
    }

    public String dumpCurrentList(StringBuffer output, boolean force)
    {
        if ((this.currentList.length() != 0) && (force || this.finished)) {
            String list = this.currentList.toString();
            list = list.replaceAll("</([dou]l)><\\1>", "");
            output.append(list);
            output.append("\n");
            this.currentList = new StringBuilder();
            this.finished = false;
            return list;
        } else {
            return "";
        }
    }
}

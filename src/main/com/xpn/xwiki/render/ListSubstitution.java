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
 * Date: 4 déc. 2003
 * Time: 20:53:14
 */
package com.xpn.xwiki.render;

import com.xpn.xwiki.util.Util;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;

public class ListSubstitution  extends WikiSubstitution {
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
    private StringBuffer currentList = new StringBuffer();

    public ListSubstitution(Util util) {
        super(util);
    }

    public void appendSubstitution(StringBuffer stringBuffer, MatchResult matchResult, int i, PatternMatcherInput minput, PatternMatcher patternMatcher, Pattern pattern) {
        setSubstitution(" ");
        super.appendSubstitution(stringBuffer,matchResult, i, minput, patternMatcher, pattern);
        int length;
        if (currenttype==TYPE_UL2)
           length = matchResult.group(1).length() / 3;
        else
           length = matchResult.group(1).length();

        String text = currentline.substring(matchResult.endOffset(0));
        String itemdelim = "li";
        String groupdelim = "ul";
        switch (currenttype) {
            case TYPE_DL:
                itemdelim = "dd";
                groupdelim = "dt";
                break;
            case TYPE_OL:
                groupdelim = "ol";
                break;
        }

        for (int nb=0;nb<length;nb++)  {
         currentList.append("<");
         currentList.append(groupdelim);
         currentList.append(">");
        }
        currentList.append("<");
        currentList.append(itemdelim);
        currentList.append("> ");
        currentList.append(text);
        currentList.append("</");
        currentList.append(itemdelim);
        currentList.append(">\n");
        for (int nb=0;nb<length;nb++)  {
            currentList.append("</");
            currentList.append(groupdelim);
            currentList.append(">");
        }
        finished = false;
        currentline = null;
    }

    public String substitute(String line, int type) {
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
        return super.substitute(line);    //To change body of overriden methods use Options | File Templates.
    }


    public String handleList(String line) {
       Util util = getUtil();
       line = util.substitute("s/^\\s*$/<p \\/> /o", line);
       if (util.matched())
           finished = true;
       if (util.match("m/^(\\S+?)/", line))
           finished = true;

       // Handle the lists
       currentline = line;
       if (currentline!=null) substitute(currentline, TYPE_DL);
       if (currentline!=null) substitute(currentline, TYPE_UL1);
       if (currentline!=null) substitute(currentline, TYPE_UL2);
       if (currentline!=null) substitute(currentline, TYPE_OL);
       return currentline;
    }

    public String dumpCurrentList(StringBuffer output, boolean force) {
        if ((currentList.length()!=0)&&(force||finished)) {
            Util util = getUtil();
            String list = currentList.toString();
            list = util.substitute("s/<\\/dl><dl>//go",list);
            list = util.substitute("s/<\\/ul><ul>//go",list);
            list = util.substitute("s/<\\/ol><ol>//go",list);
            output.append(list);
            output.append("\n");
            currentList = new StringBuffer();
            finished = false;
            return list;
        }
        else
            return "";
    }
}

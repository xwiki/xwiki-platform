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
 * Date: 24 nov. 2003
 * Time: 17:04:20
 */
package com.xpn.xwiki.util;

import com.xpn.xwiki.render.WikiSubstitution;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.oro.text.PatternCache;
import org.apache.oro.text.PatternCacheLRU;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.oro.text.regex.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;


public class Util {

    private static PatternCache patterns = new PatternCacheLRU(200);
    private Perl5Matcher matcher = new Perl5Matcher();
    private Perl5Util p5util = new Perl5Util(getPatterns());

    public String substitute(String pattern, String text) {
        return getP5util().substitute(pattern, text);
    }

    public boolean match(String pattern, String text) {
        return getP5util().match(pattern, text);
    }

    public boolean matched() {
     return (getP5util().getMatch()!=null);
    }

    public String substitute(String pattern, String substitution, String text) {
        WikiSubstitution subst = new WikiSubstitution(this, pattern);
        subst.setSubstitution(substitution);
        return subst.substitute(text);
    }

    public Perl5Matcher getMatcher() {
        return matcher;
    }

    public Perl5Util getP5util() {
        return p5util;
    }

    public List getMatches(String content, String spattern, int group) throws MalformedPatternException {
        List list = new ArrayList();
        PatternMatcherInput input = new PatternMatcherInput(content);
        Pattern pattern = patterns.addPattern(spattern);
        while (matcher.contains(input, pattern)) {
            MatchResult result = matcher.getMatch();
            String smatch = result.group(group);
            if (!list.contains(smatch))
             list.add(smatch);
        }
        return list;
    }


    public static String cleanValue(String value) {
      value = StringUtils.replace(value,"\r\r\n", "%_N_%");
      value = StringUtils.replace(value,"\r\n", "%_N_%");
      value = StringUtils.replace(value,"\n\r", "%_N_%");
      value = StringUtils.replace(value,"\r", "\n");
      value = StringUtils.replace(value,"\n", "%_N_%");
      value = StringUtils.replace(value,"\"", "%_Q_%");
      return value;
    }

    public static String restoreValue(String value) {
      value = StringUtils.replace(value,"%_N_%", "\n");
      value = StringUtils.replace(value,"%_Q_%", "\"");
      return value;
    }

    /*
       Treats lines of format name="value1" name2="value2"...
    */
    public static Hashtable keyValueToHashtable(String keyvalue) throws IOException {
        Hashtable hash = new Hashtable();
        StreamTokenizer st = new StreamTokenizer(new BufferedReader(new StringReader(keyvalue)));
        st.resetSyntax();
        st.quoteChar('"');
        st.wordChars('a','z');
        st.wordChars('A','Z');
        // st.wordChars(' ',' ');
        st.whitespaceChars(' ',' ');
        st.whitespaceChars('=','=');
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            String key = st.sval;
            st.nextToken();
            String value = (st.sval!=null) ? st.sval : "";
            hash.put(key,restoreValue(value));
        }
        return hash;
    }

    public static PatternCache getPatterns() {
        return patterns;
    }

    public static Map getObject(HttpServletRequest request, String prefix) {
        Map map = request.getParameterMap();
        HashMap map2 = new HashMap();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
          String name = (String) it.next();
          if (name.startsWith(prefix + "_")) {
              String newname = name.substring(prefix.length()+1);
              map2.put(newname, map.get(name));
          }
        }
        return map2;
    }

    public static String getWeb(String fullname) {
        int i = fullname.lastIndexOf(".");
        return fullname.substring(0, i);
    }

    public Vector split (String pattern,
			     String text) {
	Vector results = new Vector();
	getP5util().split ((Collection) results,pattern,text);
	return results;
    }

    public static String getFileContent(File file) throws IOException {
            StringBuffer content = new StringBuffer();
            BufferedReader fr = new BufferedReader(new FileReader(file));
            String line;
            line = fr.readLine();
            while (true) {
                 if (line==null) {
                        fr.close();
                        return content.toString();
                    }
                    content.append(line);
                    content.append("\n");
                    line = fr.readLine();
                }
    }

    public static boolean contains(String name, String list, String sep) {
          String[] sarray = StringUtils.split(list, sep);
          return ArrayUtils.contains(sarray, name);
    }
}

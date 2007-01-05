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
 * @author sdumitriu
 */

package com.xpn.xwiki.util;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.render.WikiSubstitution;
import com.xpn.xwiki.web.XWikiRequest;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.oro.text.PatternCache;
import org.apache.oro.text.PatternCacheLRU;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.oro.text.regex.*;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import javax.servlet.http.Cookie;
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

    public static Map getObject(XWikiRequest request, String prefix) {
        return getSubMap(request.getParameterMap(), prefix);
    }

    public static Map getSubMap(Map map, String prefix) {
        HashMap map2 = new HashMap();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            if (name.startsWith(prefix + "_")) {
                String newname = name.substring(prefix.length()+1);
                map2.put(newname, map.get(name));
            }
            if (name.equals(prefix)) {
                map2.put("", map.get(name));
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
        getP5util().split (results,pattern,text);
        return results;
    }

    public static String getFileContent(File file) throws IOException {
        return getFileContent(new FileReader(file));
    }

    public static String getFileContent(Reader reader) throws IOException {
        StringBuffer content = new StringBuffer();
        BufferedReader fr = new BufferedReader(reader);
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

    public static byte[] getFileContentAsBytes(File file) throws IOException {
        return getFileContentAsBytes(new FileInputStream(file));
    }

    public static byte[] getFileContentAsBytes(InputStream is) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = new byte[65536];
        int nb = 0;
        while((nb = bis.read(data))>0) {
            baos.write(data, 0, nb);
        }
        return baos.toByteArray();
    }

    public static boolean contains(String name, String list, String sep) {
        String[] sarray = StringUtils.split(list, sep);
        return ArrayUtils.contains(sarray, name);
    }

    public static String noaccents(String text) {
        String temp = text;
        temp = temp.replace('\u00c0', 'A');
        temp = temp.replace('\u00c1', 'A');
        temp = temp.replace('\u00c2', 'A');
        temp = temp.replace('\u00c3', 'A');
        temp = temp.replace('\u00c4', 'A');
        temp = temp.replace('\u00c5', 'A');
        temp = temp.replace('\u0100', 'A');
        temp = temp.replace('\u0102', 'A');
        temp = temp.replace('\u0104', 'A');
        temp = temp.replace('\u01cd', 'A');
        temp = temp.replace('\u01de', 'A');
        temp = temp.replace('\u01e0', 'A');
        temp = temp.replace('\u01fa', 'A');
        temp = temp.replace('\u0200', 'A');
        temp = temp.replace('\u0202', 'A');
        temp = temp.replace('\u0226', 'A');
        temp = temp.replace('\u00e0', 'a');
        temp = temp.replace('\u00e1', 'a');
        temp = temp.replace('\u00e2', 'a');
        temp = temp.replace('\u00e3', 'a');
        temp = temp.replace('\u00e4', 'a');
        temp = temp.replace('\u00e5', 'a');
        temp = temp.replace('\u0101', 'a');
        temp = temp.replace('\u0103', 'a');
        temp = temp.replace('\u0105', 'a');
        temp = temp.replace('\u01ce', 'a');
        temp = temp.replace('\u01df', 'a');
        temp = temp.replace('\u01e1', 'a');
        temp = temp.replace('\u01fb', 'a');
        temp = temp.replace('\u0201', 'a');
        temp = temp.replace('\u0203', 'a');
        temp = temp.replace('\u0227', 'a');
        temp = temp.replace("\u00c6", "AE");
        temp = temp.replace("\u01e2", "AE");
        temp = temp.replace("\u01fc", "AE");
        temp = temp.replace("\u00e6", "ae");
        temp = temp.replace("\u01e3", "ae");
        temp = temp.replace("\u01fd", "ae");
        temp = temp.replace("\u008c", "OE");
        temp = temp.replace("\u0152", "OE");
        temp = temp.replace("\u009c", "oe");
        temp = temp.replace("\u0153", "oe");
        temp = temp.replace('\u00c7', 'C');
        temp = temp.replace('\u0106', 'C');
        temp = temp.replace('\u0108', 'C');
        temp = temp.replace('\u010a', 'C');
        temp = temp.replace('\u010c', 'C');
        temp = temp.replace('\u00e7', 'c');
        temp = temp.replace('\u0107', 'c');
        temp = temp.replace('\u0109', 'c');
        temp = temp.replace('\u010b', 'c');
        temp = temp.replace('\u010d', 'c');
        temp = temp.replace('\u00d0', 'D');
        temp = temp.replace('\u010e', 'D');
        temp = temp.replace('\u0110', 'D');
        temp = temp.replace('\u00f0', 'd');
        temp = temp.replace('\u010f', 'd');
        temp = temp.replace('\u0111', 'd');
        temp = temp.replace('\u00c8', 'E');
        temp = temp.replace('\u00c9', 'E');
        temp = temp.replace('\u00ca', 'E');
        temp = temp.replace('\u00cb', 'E');
        temp = temp.replace('\u0112', 'E');
        temp = temp.replace('\u0114', 'E');
        temp = temp.replace('\u0116', 'E');
        temp = temp.replace('\u0118', 'E');
        temp = temp.replace('\u011a', 'E');
        temp = temp.replace('\u0204', 'E');
        temp = temp.replace('\u0206', 'E');
        temp = temp.replace('\u0228', 'E');
        temp = temp.replace('\u00e8', 'e');
        temp = temp.replace('\u00e9', 'e');
        temp = temp.replace('\u00ea', 'e');
        temp = temp.replace('\u00eb', 'e');
        temp = temp.replace('\u0113', 'e');
        temp = temp.replace('\u0115', 'e');
        temp = temp.replace('\u0117', 'e');
        temp = temp.replace('\u0119', 'e');
        temp = temp.replace('\u011b', 'e');
        temp = temp.replace('\u01dd', 'e');
        temp = temp.replace('\u0205', 'e');
        temp = temp.replace('\u0207', 'e');
        temp = temp.replace('\u0229', 'e');
        temp = temp.replace('\u011c', 'G');
        temp = temp.replace('\u011e', 'G');
        temp = temp.replace('\u0120', 'G');
        temp = temp.replace('\u0122', 'G');
        temp = temp.replace('\u01e4', 'G');
        temp = temp.replace('\u01e6', 'G');
        temp = temp.replace('\u01f4', 'G');
        temp = temp.replace('\u011d', 'g');
        temp = temp.replace('\u011f', 'g');
        temp = temp.replace('\u0121', 'g');
        temp = temp.replace('\u0123', 'g');
        temp = temp.replace('\u01e5', 'g');
        temp = temp.replace('\u01e7', 'g');
        temp = temp.replace('\u01f5', 'g');
        temp = temp.replace('\u0124', 'H');
        temp = temp.replace('\u0126', 'H');
        temp = temp.replace('\u021e', 'H');
        temp = temp.replace('\u0125', 'h');
        temp = temp.replace('\u0127', 'h');
        temp = temp.replace('\u021f', 'h');
        temp = temp.replace('\u00cc', 'I');
        temp = temp.replace('\u00cd', 'I');
        temp = temp.replace('\u00ce', 'I');
        temp = temp.replace('\u00cf', 'I');
        temp = temp.replace('\u0128', 'I');
        temp = temp.replace('\u012a', 'I');
        temp = temp.replace('\u012c', 'I');
        temp = temp.replace('\u012e', 'I');
        temp = temp.replace('\u0130', 'I');
        temp = temp.replace('\u01cf', 'I');
        temp = temp.replace('\u0208', 'I');
        temp = temp.replace('\u020a', 'I');
        temp = temp.replace('\u00ec', 'i');
        temp = temp.replace('\u00ed', 'i');
        temp = temp.replace('\u00ee', 'i');
        temp = temp.replace('\u00ef', 'i');
        temp = temp.replace('\u0129', 'i');
        temp = temp.replace('\u012b', 'i');
        temp = temp.replace('\u012d', 'i');
        temp = temp.replace('\u012f', 'i');
        temp = temp.replace('\u0131', 'i');
        temp = temp.replace('\u01d0', 'i');
        temp = temp.replace('\u0209', 'i');
        temp = temp.replace('\u020b', 'i');
        temp = temp.replace("\u0132", "IJ");
        temp = temp.replace("\u0133", "ij");
        temp = temp.replace('\u0134', 'J');
        temp = temp.replace('\u0135', 'j');
        temp = temp.replace('\u0136', 'K');
        temp = temp.replace('\u01e8', 'K');
        temp = temp.replace('\u0137', 'k');
        temp = temp.replace('\u0138', 'k');
        temp = temp.replace('\u01e9', 'k');
        temp = temp.replace('\u0139', 'L');
        temp = temp.replace('\u013b', 'L');
        temp = temp.replace('\u013d', 'L');
        temp = temp.replace('\u013f', 'L');
        temp = temp.replace('\u0141', 'L');
        temp = temp.replace('\u013a', 'l');
        temp = temp.replace('\u013c', 'l');
        temp = temp.replace('\u013e', 'l');
        temp = temp.replace('\u0140', 'l');
        temp = temp.replace('\u0142', 'l');
        temp = temp.replace('\u0234', 'l');
        temp = temp.replace('\u00d1', 'N');
        temp = temp.replace('\u0143', 'N');
        temp = temp.replace('\u0145', 'N');
        temp = temp.replace('\u0147', 'N');
        temp = temp.replace('\u014a', 'N');
        temp = temp.replace('\u01f8', 'N');
        temp = temp.replace('\u00f1', 'n');
        temp = temp.replace('\u0144', 'n');
        temp = temp.replace('\u0146', 'n');
        temp = temp.replace('\u0148', 'n');
        temp = temp.replace('\u0149', 'n');
        temp = temp.replace('\u014b', 'n');
        temp = temp.replace('\u01f9', 'n');
        temp = temp.replace('\u0235', 'n');
        temp = temp.replace('\u00d2', 'O');
        temp = temp.replace('\u00d3', 'O');
        temp = temp.replace('\u00d4', 'O');
        temp = temp.replace('\u00d5', 'O');
        temp = temp.replace('\u00d6', 'O');
        temp = temp.replace('\u00d8', 'O');
        temp = temp.replace('\u014c', 'O');
        temp = temp.replace('\u014e', 'O');
        temp = temp.replace('\u0150', 'O');
        temp = temp.replace('\u01d1', 'O');
        temp = temp.replace('\u01ea', 'O');
        temp = temp.replace('\u01ec', 'O');
        temp = temp.replace('\u01fe', 'O');
        temp = temp.replace('\u020c', 'O');
        temp = temp.replace('\u020e', 'O');
        temp = temp.replace('\u022a', 'O');
        temp = temp.replace('\u022c', 'O');
        temp = temp.replace('\u022e', 'O');
        temp = temp.replace('\u0230', 'O');
        temp = temp.replace('\u00f2', 'o');
        temp = temp.replace('\u00f3', 'o');
        temp = temp.replace('\u00f4', 'o');
        temp = temp.replace('\u00f5', 'o');
        temp = temp.replace('\u00f6', 'o');
        temp = temp.replace('\u00f8', 'o');
        temp = temp.replace('\u014d', 'o');
        temp = temp.replace('\u014f', 'o');
        temp = temp.replace('\u0151', 'o');
        temp = temp.replace('\u01d2', 'o');
        temp = temp.replace('\u01eb', 'o');
        temp = temp.replace('\u01ed', 'o');
        temp = temp.replace('\u01ff', 'o');
        temp = temp.replace('\u020d', 'o');
        temp = temp.replace('\u020f', 'o');
        temp = temp.replace('\u022b', 'o');
        temp = temp.replace('\u022d', 'o');
        temp = temp.replace('\u022f', 'o');
        temp = temp.replace('\u0231', 'o');
        temp = temp.replace('\u0156', 'R');
        temp = temp.replace('\u0158', 'R');
        temp = temp.replace('\u0210', 'R');
        temp = temp.replace('\u0212', 'R');
        temp = temp.replace('\u0157', 'r');
        temp = temp.replace('\u0159', 'r');
        temp = temp.replace('\u0211', 'r');
        temp = temp.replace('\u0213', 'r');
        temp = temp.replace('\u015a', 'S');
        temp = temp.replace('\u015c', 'S');
        temp = temp.replace('\u015e', 'S');
        temp = temp.replace('\u0160', 'S');
        temp = temp.replace('\u0218', 'S');
        temp = temp.replace('\u015b', 's');
        temp = temp.replace('\u015d', 's');
        temp = temp.replace('\u015f', 's');
        temp = temp.replace('\u0161', 's');
        temp = temp.replace('\u0219', 's');
        temp = temp.replace('\u00de', 'T');
        temp = temp.replace('\u0162', 'T');
        temp = temp.replace('\u0164', 'T');
        temp = temp.replace('\u0166', 'T');
        temp = temp.replace('\u021a', 'T');
        temp = temp.replace('\u00fe', 't');
        temp = temp.replace('\u0163', 't');
        temp = temp.replace('\u0165', 't');
        temp = temp.replace('\u0167', 't');
        temp = temp.replace('\u021b', 't');
        temp = temp.replace('\u0236', 't');
        temp = temp.replace('\u00d9', 'U');
        temp = temp.replace('\u00da', 'U');
        temp = temp.replace('\u00db', 'U');
        temp = temp.replace('\u00dc', 'U');
        temp = temp.replace('\u0168', 'U');
        temp = temp.replace('\u016a', 'U');
        temp = temp.replace('\u016c', 'U');
        temp = temp.replace('\u016e', 'U');
        temp = temp.replace('\u0170', 'U');
        temp = temp.replace('\u0172', 'U');
        temp = temp.replace('\u01d3', 'U');
        temp = temp.replace('\u01d5', 'U');
        temp = temp.replace('\u01d7', 'U');
        temp = temp.replace('\u01d9', 'U');
        temp = temp.replace('\u01db', 'U');
        temp = temp.replace('\u0214', 'U');
        temp = temp.replace('\u0216', 'U');
        temp = temp.replace('\u00f9', 'u');
        temp = temp.replace('\u00fa', 'u');
        temp = temp.replace('\u00fb', 'u');
        temp = temp.replace('\u00fc', 'u');
        temp = temp.replace('\u0169', 'u');
        temp = temp.replace('\u016b', 'u');
        temp = temp.replace('\u016d', 'u');
        temp = temp.replace('\u016f', 'u');
        temp = temp.replace('\u0171', 'u');
        temp = temp.replace('\u0173', 'u');
        temp = temp.replace('\u01d4', 'u');
        temp = temp.replace('\u01d6', 'u');
        temp = temp.replace('\u01d8', 'u');
        temp = temp.replace('\u01da', 'u');
        temp = temp.replace('\u01dc', 'u');
        temp = temp.replace('\u0215', 'u');
        temp = temp.replace('\u0217', 'u');
        temp = temp.replace('\u0174', 'W');
        temp = temp.replace('\u0175', 'w');
        temp = temp.replace('\u00dd', 'Y');
        temp = temp.replace('\u0176', 'Y');
        temp = temp.replace('\u0178', 'Y');
        temp = temp.replace('\u0232', 'Y');
        temp = temp.replace('\u00fd', 'y');
        temp = temp.replace('\u00ff', 'y');
        temp = temp.replace('\u0177', 'y');
        temp = temp.replace('\u0233', 'y');
        temp = temp.replace('\u0179', 'Z');
        temp = temp.replace('\u017b', 'Z');
        temp = temp.replace('\u017d', 'Z');
        temp = temp.replace('\u017a', 'z');
        temp = temp.replace('\u017c', 'z');
        temp = temp.replace('\u017e', 'z');
        temp = temp.replace("\u00df", "ss");
        return temp;
    }

    public static boolean isAlphaNumeric(String text) {
        return StringUtils.isAlphanumeric(text.replace('-','a').replace('.','a'));
    }

    public static String getName(String name) {
        int i0 = name.indexOf(":");
        if (i0!=-1) {
            name = name.substring(i0+1);
            return name;
        }

        if (name.indexOf(".") !=-1)
            return name;
        else
            return "XWiki." + name;
    }

    public static String getName(String name, XWikiContext context) {
        String database = null;
        int i0 = name.indexOf(":");
        if (i0!=-1) {
            database = name.substring(0,i0);
            name = name.substring(i0+1);
            context.setDatabase(database);
            return name;
        }
        // This does not make sense
        // context.setDatabase(context.getWiki().getDatabase());
        if (name.indexOf(".") !=-1)
            return name;
        else
            return "XWiki." + name;
    }

    public static Cookie getCookie(String cookieName, XWikiContext context) {
        Cookie[] cookies = context.getRequest().getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if (cookieName.equals(cookie.getName())) {
                    return (cookie);
                }
            }
        }
        return null;
    }

    public static String getHTMLExceptionMessage(XWikiException xe, XWikiContext context) {
        String title;
        String text;
        title = xe.getMessage();
        text = com.xpn.xwiki.XWiki.getFormEncoded(xe.getFullMessage());
        String id = (String)context.get("xwikierrorid");
        if (id==null)
         id = "1";
        else
         id = "" + (Integer.parseInt(id) + 1);

        return "<a href=\"\" onclick=\"document.getElementById('xwikierror" + id + "').style.display='block'; return false;\">"
                + title + "</a><div id=\"xwikierror" + id + "\" style=\"display: none;\"><pre>\n"
                + text + "</pre></div>";
    }

    public static String secureLaszloCode(String laszlocode) throws XWikiException {
        SAXReader reader = new SAXReader();
              Document domdoc;

              try {
                  StringReader in = new StringReader(laszlocode);
                  domdoc = reader.read(in);
              } catch (DocumentException e) {
                  throw new XWikiException(XWikiException.MODULE_PLUGIN_LASZLO, XWikiException.ERROR_LASZLO_INVALID_XML, "Invalid Laszlo XML", e);
              }

        String code = domdoc.asXML();
        if (code.indexOf("..")!=-1)
            throw new XWikiException(XWikiException.MODULE_PLUGIN_LASZLO, XWikiException.ERROR_LASZLO_INVALID_DOTDOT, "Invalid content in Laszlo XML");

        return laszlocode;
    }

    public static MonitorPlugin getMonitorPlugin(XWikiContext context) {
        try {
        if ((context==null)||(context.getWiki()==null))
            return null;

        return (MonitorPlugin) context.getWiki().getPlugin("monitor", context);
        } catch (Exception e) {
            return null;
        }
    }

}

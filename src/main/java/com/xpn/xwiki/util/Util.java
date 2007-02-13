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
        temp = temp.replaceAll("\u00c0", "A");
        temp = temp.replaceAll("\u00c1", "A");
        temp = temp.replaceAll("\u00c2", "A");
        temp = temp.replaceAll("\u00c3", "A");
        temp = temp.replaceAll("\u00c4", "A");
        temp = temp.replaceAll("\u00c5", "A");
        temp = temp.replaceAll("\u0100", "A");
        temp = temp.replaceAll("\u0102", "A");
        temp = temp.replaceAll("\u0104", "A");
        temp = temp.replaceAll("\u01cd", "A");
        temp = temp.replaceAll("\u01de", "A");
        temp = temp.replaceAll("\u01e0", "A");
        temp = temp.replaceAll("\u01fa", "A");
        temp = temp.replaceAll("\u0200", "A");
        temp = temp.replaceAll("\u0202", "A");
        temp = temp.replaceAll("\u0226", "A");
        temp = temp.replaceAll("\u00e0", "a");
        temp = temp.replaceAll("\u00e1", "a");
        temp = temp.replaceAll("\u00e2", "a");
        temp = temp.replaceAll("\u00e3", "a");
        temp = temp.replaceAll("\u00e4", "a");
        temp = temp.replaceAll("\u00e5", "a");
        temp = temp.replaceAll("\u0101", "a");
        temp = temp.replaceAll("\u0103", "a");
        temp = temp.replaceAll("\u0105", "a");
        temp = temp.replaceAll("\u01ce", "a");
        temp = temp.replaceAll("\u01df", "a");
        temp = temp.replaceAll("\u01e1", "a");
        temp = temp.replaceAll("\u01fb", "a");
        temp = temp.replaceAll("\u0201", "a");
        temp = temp.replaceAll("\u0203", "a");
        temp = temp.replaceAll("\u0227", "a");
        temp = temp.replaceAll("\u00c6", "AE");
        temp = temp.replaceAll("\u01e2", "AE");
        temp = temp.replaceAll("\u01fc", "AE");
        temp = temp.replaceAll("\u00e6", "ae");
        temp = temp.replaceAll("\u01e3", "ae");
        temp = temp.replaceAll("\u01fd", "ae");
        temp = temp.replaceAll("\u008c", "OE");
        temp = temp.replaceAll("\u0152", "OE");
        temp = temp.replaceAll("\u009c", "oe");
        temp = temp.replaceAll("\u0153", "oe");
        temp = temp.replaceAll("\u00c7", "C");
        temp = temp.replaceAll("\u0106", "C");
        temp = temp.replaceAll("\u0108", "C");
        temp = temp.replaceAll("\u010a", "C");
        temp = temp.replaceAll("\u010c", "C");
        temp = temp.replaceAll("\u00e7", "c");
        temp = temp.replaceAll("\u0107", "c");
        temp = temp.replaceAll("\u0109", "c");
        temp = temp.replaceAll("\u010b", "c");
        temp = temp.replaceAll("\u010d", "c");
        temp = temp.replaceAll("\u00d0", "D");
        temp = temp.replaceAll("\u010e", "D");
        temp = temp.replaceAll("\u0110", "D");
        temp = temp.replaceAll("\u00f0", "d");
        temp = temp.replaceAll("\u010f", "d");
        temp = temp.replaceAll("\u0111", "d");
        temp = temp.replaceAll("\u00c8", "E");
        temp = temp.replaceAll("\u00c9", "E");
        temp = temp.replaceAll("\u00ca", "E");
        temp = temp.replaceAll("\u00cb", "E");
        temp = temp.replaceAll("\u0112", "E");
        temp = temp.replaceAll("\u0114", "E");
        temp = temp.replaceAll("\u0116", "E");
        temp = temp.replaceAll("\u0118", "E");
        temp = temp.replaceAll("\u011a", "E");
        temp = temp.replaceAll("\u0204", "E");
        temp = temp.replaceAll("\u0206", "E");
        temp = temp.replaceAll("\u0228", "E");
        temp = temp.replaceAll("\u00e8", "e");
        temp = temp.replaceAll("\u00e9", "e");
        temp = temp.replaceAll("\u00ea", "e");
        temp = temp.replaceAll("\u00eb", "e");
        temp = temp.replaceAll("\u0113", "e");
        temp = temp.replaceAll("\u0115", "e");
        temp = temp.replaceAll("\u0117", "e");
        temp = temp.replaceAll("\u0119", "e");
        temp = temp.replaceAll("\u011b", "e");
        temp = temp.replaceAll("\u01dd", "e");
        temp = temp.replaceAll("\u0205", "e");
        temp = temp.replaceAll("\u0207", "e");
        temp = temp.replaceAll("\u0229", "e");
        temp = temp.replaceAll("\u011c", "G");
        temp = temp.replaceAll("\u011e", "G");
        temp = temp.replaceAll("\u0120", "G");
        temp = temp.replaceAll("\u0122", "G");
        temp = temp.replaceAll("\u01e4", "G");
        temp = temp.replaceAll("\u01e6", "G");
        temp = temp.replaceAll("\u01f4", "G");
        temp = temp.replaceAll("\u011d", "g");
        temp = temp.replaceAll("\u011f", "g");
        temp = temp.replaceAll("\u0121", "g");
        temp = temp.replaceAll("\u0123", "g");
        temp = temp.replaceAll("\u01e5", "g");
        temp = temp.replaceAll("\u01e7", "g");
        temp = temp.replaceAll("\u01f5", "g");
        temp = temp.replaceAll("\u0124", "H");
        temp = temp.replaceAll("\u0126", "H");
        temp = temp.replaceAll("\u021e", "H");
        temp = temp.replaceAll("\u0125", "h");
        temp = temp.replaceAll("\u0127", "h");
        temp = temp.replaceAll("\u021f", "h");
        temp = temp.replaceAll("\u00cc", "I");
        temp = temp.replaceAll("\u00cd", "I");
        temp = temp.replaceAll("\u00ce", "I");
        temp = temp.replaceAll("\u00cf", "I");
        temp = temp.replaceAll("\u0128", "I");
        temp = temp.replaceAll("\u012a", "I");
        temp = temp.replaceAll("\u012c", "I");
        temp = temp.replaceAll("\u012e", "I");
        temp = temp.replaceAll("\u0130", "I");
        temp = temp.replaceAll("\u01cf", "I");
        temp = temp.replaceAll("\u0208", "I");
        temp = temp.replaceAll("\u020a", "I");
        temp = temp.replaceAll("\u00ec", "i");
        temp = temp.replaceAll("\u00ed", "i");
        temp = temp.replaceAll("\u00ee", "i");
        temp = temp.replaceAll("\u00ef", "i");
        temp = temp.replaceAll("\u0129", "i");
        temp = temp.replaceAll("\u012b", "i");
        temp = temp.replaceAll("\u012d", "i");
        temp = temp.replaceAll("\u012f", "i");
        temp = temp.replaceAll("\u0131", "i");
        temp = temp.replaceAll("\u01d0", "i");
        temp = temp.replaceAll("\u0209", "i");
        temp = temp.replaceAll("\u020b", "i");
        temp = temp.replaceAll("\u0132", "IJ");
        temp = temp.replaceAll("\u0133", "ij");
        temp = temp.replaceAll("\u0134", "J");
        temp = temp.replaceAll("\u0135", "j");
        temp = temp.replaceAll("\u0136", "K");
        temp = temp.replaceAll("\u01e8", "K");
        temp = temp.replaceAll("\u0137", "k");
        temp = temp.replaceAll("\u0138", "k");
        temp = temp.replaceAll("\u01e9", "k");
        temp = temp.replaceAll("\u0139", "L");
        temp = temp.replaceAll("\u013b", "L");
        temp = temp.replaceAll("\u013d", "L");
        temp = temp.replaceAll("\u013f", "L");
        temp = temp.replaceAll("\u0141", "L");
        temp = temp.replaceAll("\u013a", "l");
        temp = temp.replaceAll("\u013c", "l");
        temp = temp.replaceAll("\u013e", "l");
        temp = temp.replaceAll("\u0140", "l");
        temp = temp.replaceAll("\u0142", "l");
        temp = temp.replaceAll("\u0234", "l");
        temp = temp.replaceAll("\u00d1", "N");
        temp = temp.replaceAll("\u0143", "N");
        temp = temp.replaceAll("\u0145", "N");
        temp = temp.replaceAll("\u0147", "N");
        temp = temp.replaceAll("\u014a", "N");
        temp = temp.replaceAll("\u01f8", "N");
        temp = temp.replaceAll("\u00f1", "n");
        temp = temp.replaceAll("\u0144", "n");
        temp = temp.replaceAll("\u0146", "n");
        temp = temp.replaceAll("\u0148", "n");
        temp = temp.replaceAll("\u0149", "n");
        temp = temp.replaceAll("\u014b", "n");
        temp = temp.replaceAll("\u01f9", "n");
        temp = temp.replaceAll("\u0235", "n");
        temp = temp.replaceAll("\u00d2", "O");
        temp = temp.replaceAll("\u00d3", "O");
        temp = temp.replaceAll("\u00d4", "O");
        temp = temp.replaceAll("\u00d5", "O");
        temp = temp.replaceAll("\u00d6", "O");
        temp = temp.replaceAll("\u00d8", "O");
        temp = temp.replaceAll("\u014c", "O");
        temp = temp.replaceAll("\u014e", "O");
        temp = temp.replaceAll("\u0150", "O");
        temp = temp.replaceAll("\u01d1", "O");
        temp = temp.replaceAll("\u01ea", "O");
        temp = temp.replaceAll("\u01ec", "O");
        temp = temp.replaceAll("\u01fe", "O");
        temp = temp.replaceAll("\u020c", "O");
        temp = temp.replaceAll("\u020e", "O");
        temp = temp.replaceAll("\u022a", "O");
        temp = temp.replaceAll("\u022c", "O");
        temp = temp.replaceAll("\u022e", "O");
        temp = temp.replaceAll("\u0230", "O");
        temp = temp.replaceAll("\u00f2", "o");
        temp = temp.replaceAll("\u00f3", "o");
        temp = temp.replaceAll("\u00f4", "o");
        temp = temp.replaceAll("\u00f5", "o");
        temp = temp.replaceAll("\u00f6", "o");
        temp = temp.replaceAll("\u00f8", "o");
        temp = temp.replaceAll("\u014d", "o");
        temp = temp.replaceAll("\u014f", "o");
        temp = temp.replaceAll("\u0151", "o");
        temp = temp.replaceAll("\u01d2", "o");
        temp = temp.replaceAll("\u01eb", "o");
        temp = temp.replaceAll("\u01ed", "o");
        temp = temp.replaceAll("\u01ff", "o");
        temp = temp.replaceAll("\u020d", "o");
        temp = temp.replaceAll("\u020f", "o");
        temp = temp.replaceAll("\u022b", "o");
        temp = temp.replaceAll("\u022d", "o");
        temp = temp.replaceAll("\u022f", "o");
        temp = temp.replaceAll("\u0231", "o");
        temp = temp.replaceAll("\u0156", "R");
        temp = temp.replaceAll("\u0158", "R");
        temp = temp.replaceAll("\u0210", "R");
        temp = temp.replaceAll("\u0212", "R");
        temp = temp.replaceAll("\u0157", "r");
        temp = temp.replaceAll("\u0159", "r");
        temp = temp.replaceAll("\u0211", "r");
        temp = temp.replaceAll("\u0213", "r");
        temp = temp.replaceAll("\u015a", "S");
        temp = temp.replaceAll("\u015c", "S");
        temp = temp.replaceAll("\u015e", "S");
        temp = temp.replaceAll("\u0160", "S");
        temp = temp.replaceAll("\u0218", "S");
        temp = temp.replaceAll("\u015b", "s");
        temp = temp.replaceAll("\u015d", "s");
        temp = temp.replaceAll("\u015f", "s");
        temp = temp.replaceAll("\u0161", "s");
        temp = temp.replaceAll("\u0219", "s");
        temp = temp.replaceAll("\u00de", "T");
        temp = temp.replaceAll("\u0162", "T");
        temp = temp.replaceAll("\u0164", "T");
        temp = temp.replaceAll("\u0166", "T");
        temp = temp.replaceAll("\u021a", "T");
        temp = temp.replaceAll("\u00fe", "t");
        temp = temp.replaceAll("\u0163", "t");
        temp = temp.replaceAll("\u0165", "t");
        temp = temp.replaceAll("\u0167", "t");
        temp = temp.replaceAll("\u021b", "t");
        temp = temp.replaceAll("\u0236", "t");
        temp = temp.replaceAll("\u00d9", "U");
        temp = temp.replaceAll("\u00da", "U");
        temp = temp.replaceAll("\u00db", "U");
        temp = temp.replaceAll("\u00dc", "U");
        temp = temp.replaceAll("\u0168", "U");
        temp = temp.replaceAll("\u016a", "U");
        temp = temp.replaceAll("\u016c", "U");
        temp = temp.replaceAll("\u016e", "U");
        temp = temp.replaceAll("\u0170", "U");
        temp = temp.replaceAll("\u0172", "U");
        temp = temp.replaceAll("\u01d3", "U");
        temp = temp.replaceAll("\u01d5", "U");
        temp = temp.replaceAll("\u01d7", "U");
        temp = temp.replaceAll("\u01d9", "U");
        temp = temp.replaceAll("\u01db", "U");
        temp = temp.replaceAll("\u0214", "U");
        temp = temp.replaceAll("\u0216", "U");
        temp = temp.replaceAll("\u00f9", "u");
        temp = temp.replaceAll("\u00fa", "u");
        temp = temp.replaceAll("\u00fb", "u");
        temp = temp.replaceAll("\u00fc", "u");
        temp = temp.replaceAll("\u0169", "u");
        temp = temp.replaceAll("\u016b", "u");
        temp = temp.replaceAll("\u016d", "u");
        temp = temp.replaceAll("\u016f", "u");
        temp = temp.replaceAll("\u0171", "u");
        temp = temp.replaceAll("\u0173", "u");
        temp = temp.replaceAll("\u01d4", "u");
        temp = temp.replaceAll("\u01d6", "u");
        temp = temp.replaceAll("\u01d8", "u");
        temp = temp.replaceAll("\u01da", "u");
        temp = temp.replaceAll("\u01dc", "u");
        temp = temp.replaceAll("\u0215", "u");
        temp = temp.replaceAll("\u0217", "u");
        temp = temp.replaceAll("\u0174", "W");
        temp = temp.replaceAll("\u0175", "w");
        temp = temp.replaceAll("\u00dd", "Y");
        temp = temp.replaceAll("\u0176", "Y");
        temp = temp.replaceAll("\u0178", "Y");
        temp = temp.replaceAll("\u0232", "Y");
        temp = temp.replaceAll("\u00fd", "y");
        temp = temp.replaceAll("\u00ff", "y");
        temp = temp.replaceAll("\u0177", "y");
        temp = temp.replaceAll("\u0233", "y");
        temp = temp.replaceAll("\u0179", "Z");
        temp = temp.replaceAll("\u017b", "Z");
        temp = temp.replaceAll("\u017d", "Z");
        temp = temp.replaceAll("\u017a", "z");
        temp = temp.replaceAll("\u017c", "z");
        temp = temp.replaceAll("\u017e", "z");
        temp = temp.replaceAll("\u00df", "ss");
        return temp;
    }

    public static boolean isAlphaNumeric(String text) {
        return StringUtils.isAlphanumeric(text.replaceAll("-", "a").replaceAll("\\.", "a"));
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

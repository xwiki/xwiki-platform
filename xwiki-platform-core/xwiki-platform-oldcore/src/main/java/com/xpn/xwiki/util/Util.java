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
package com.xpn.xwiki.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.oro.text.PatternCache;
import org.apache.oro.text.PatternCacheLRU;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xwiki.container.Container;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.render.WikiSubstitution;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;

public class Util
{
    /**
     * Encoding used for URL encoding/decoding. UTF-8 is the default encoding in RFC 3986, and is recommended by the W3
     * consortium.
     */
    private static final String URL_ENCODING = "UTF-8";

    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private static PatternCache patterns = new PatternCacheLRU(200);

    private Perl5Matcher matcher = new Perl5Matcher();

    private Perl5Util p5util = new Perl5Util(getPatterns());

    public String substitute(String pattern, String text)
    {
        return getP5util().substitute(pattern, text);
    }

    public boolean match(String pattern, String text)
    {
        return getP5util().match(pattern, text);
    }

    public boolean matched()
    {
        return (getP5util().getMatch() != null);
    }

    public String substitute(String pattern, String substitution, String text)
    {
        WikiSubstitution subst = new WikiSubstitution(this, pattern);
        subst.setSubstitution(substitution);
        return subst.substitute(text);
    }

    public Perl5Matcher getMatcher()
    {
        return this.matcher;
    }

    public Perl5Util getP5util()
    {
        return this.p5util;
    }

    public List<String> getAllMatches(String content, String spattern, int group) throws MalformedPatternException
    {
        List<String> list = new ArrayList<String>();
        PatternMatcherInput input = new PatternMatcherInput(content);
        Pattern pattern = patterns.addPattern(spattern);
        while (this.matcher.contains(input, pattern)) {
            MatchResult result = this.matcher.getMatch();
            String smatch = result.group(group);
            list.add(smatch);
        }

        return list;
    }

    public List<String> getUniqueMatches(String content, String spattern, int group) throws MalformedPatternException
    {
        // Remove duplicate entries
        Set<String> uniqueMatches = new HashSet<String>();
        uniqueMatches.addAll(getAllMatches(content, spattern, group));

        List<String> matches = new ArrayList<String>();
        matches.addAll(uniqueMatches);

        return matches;
    }

    public static String cleanValue(String value)
    {
        value = StringUtils.replace(value, "\r\r\n", "%_N_%");
        value = StringUtils.replace(value, "\r\n", "%_N_%");
        value = StringUtils.replace(value, "\n\r", "%_N_%");
        value = StringUtils.replace(value, "\r", "\n");
        value = StringUtils.replace(value, "\n", "%_N_%");
        value = StringUtils.replace(value, "\"", "%_Q_%");

        return value;
    }

    public static String restoreValue(String value)
    {
        value = StringUtils.replace(value, "%_N_%", "\n");
        value = StringUtils.replace(value, "%_Q_%", "\"");

        return value;
    }

    /**
     * Create a Map from a string holding a space separated list of key=value pairs. If keys or values must contain
     * spaces, they can be placed inside quotes, like <code>"this key"="a larger value"</code>. To use a quote as part
     * of a key/value, use <code>%_Q_%</code>.
     *
     * @param mapString The string that must be parsed.
     * @return A Map containing the keys and values. If a key is defined more than once, the last value is used.
     */
    public static Hashtable<String, String> keyValueToHashtable(String mapString) throws IOException
    {
        Hashtable<String, String> result = new Hashtable<String, String>();
        StreamTokenizer st = new StreamTokenizer(new BufferedReader(new StringReader(mapString)));
        st.resetSyntax();
        st.quoteChar('"');
        st.wordChars('a', 'z');
        st.wordChars('A', 'Z');
        st.whitespaceChars(' ', ' ');
        st.whitespaceChars('=', '=');
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            String key = st.sval;
            st.nextToken();
            String value = (st.sval != null) ? st.sval : "";
            result.put(key, restoreValue(value));
        }
        return result;
    }

    public static PatternCache getPatterns()
    {
        return patterns;
    }

    public static Map<String, String[]> getObject(XWikiRequest request, String prefix)
    {
        @SuppressWarnings("unchecked")
        Map<String, String[]> parameters = request.getParameterMap();
        return getSubMap(parameters, prefix);
    }

    public static <T> Map<String, T> getSubMap(Map<String, T> map, String prefix)
    {
        Map<String, T> result = new HashMap<String, T>();
        for (String name : map.keySet()) {
            if (name.startsWith(prefix + "_")) {
                String newname = name.substring(prefix.length() + 1);
                result.put(newname, map.get(name));
            } else if (name.equals(prefix)) {
                result.put("", map.get(name));
            }
        }

        return result;
    }

    public static String getWeb(String fullname)
    {
        int i = fullname.lastIndexOf(".");

        return fullname.substring(0, i);
    }

    public Vector<String> split(String pattern, String text)
    {
        Vector<String> results = new Vector<String>();
        getP5util().split(results, pattern, text);

        return results;
    }

    public static boolean contains(String name, String list, String sep)
    {
        String[] sarray = StringUtils.split(list, sep);

        return ArrayUtils.contains(sarray, name);
    }

    public static String noaccents(String text)
    {
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

    public static boolean isAlphaNumeric(String text)
    {
        return StringUtils.isAlphanumeric(text.replace('-', 'a').replace('.', 'a'));
    }

    public static String getName(String name)
    {
        int i0 = name.indexOf(":");
        if (i0 != -1) {
            name = name.substring(i0 + 1);
            return name;
        }

        if (name.indexOf(".") != -1) {
            return name;
        } else {
            return "XWiki." + name;
        }
    }

    public static String getName(String name, XWikiContext context)
    {
        String database = null;
        int i0 = name.indexOf(":");
        if (i0 != -1) {
            database = name.substring(0, i0);
            name = name.substring(i0 + 1);
            context.setWikiId(database);
            return name;
        }

        // This does not make sense
        // context.setWikiId(context.getWiki().getDatabase());
        if (name.indexOf(".") != -1) {
            return name;
        } else {
            return "XWiki." + name;
        }
    }

    public static Cookie getCookie(String cookieName, XWikiContext context)
    {
        return getCookie(cookieName, context.getRequest());
    }

    public static Cookie getCookie(String cookieName, HttpServletRequest request)
    {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return (cookie);
                }
            }
        }

        return null;
    }

    public static String getHTMLExceptionMessage(XWikiException xe, XWikiContext context)
    {
        String title = XMLUtils.escape(xe.getMessage());
        String text = XMLUtils.escape(xe.getFullMessage());

        return "<div class=\"xwikirenderingerror\" title=\"Read technical information related to this error\" "
            + "style=\"cursor: pointer;\">" + title + "</div>"
            + "<div class=\"xwikirenderingerrordescription hidden\"><pre>" + text + "</pre></div>";
    }

    public static MonitorPlugin getMonitorPlugin(XWikiContext context)
    {
        try {
            if ((context == null) || (context.getWiki() == null)) {
                return null;
            }

            return (MonitorPlugin) context.getWiki().getPlugin("monitor", context);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * API to obtain a DOM document for the specified string
     *
     * @param str The parsed text
     * @return A DOM document element corresponding to the string, or null on error
     */
    public org.w3c.dom.Document getDOMForString(String str)
    {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(str)));
        } catch (SAXException ex) {
            LOGGER.warn("Cannot parse string:" + str, ex);
        } catch (IOException ex) {
            LOGGER.warn("Cannot parse string:" + str, ex);
        } catch (ParserConfigurationException ex) {
            LOGGER.warn("Cannot parse string:" + str, ex);
        }

        return null;
    }

    /**
     * API to get a new DOM document
     *
     * @return a new DOM document element, or null on error
     */
    public org.w3c.dom.Document getDOMDocument()
    {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException ex) {
            LOGGER.warn("Cannot create DOM tree", ex);
        }

        return null;
    }

    /**
     * API to protect Text from Radeox transformation
     *
     * @param text
     * @return escaped text
     * @deprecated dedicated to Radeox which is deprecated since a long time
     */
    @Deprecated
    public static String escapeText(String text)
    {
        text = text.replaceAll("http://", "&#104;ttp://");
        text = text.replaceAll("ftp://", "&#102;tp://");
        text = text.replaceAll("\\-", "&#45;");
        text = text.replaceAll("\\*", "&#42;");
        text = text.replaceAll("\\~", "&#126;");
        text = text.replaceAll("\\[", "&#91;");
        text = text.replaceAll("\\]", "&#93;");
        text = text.replaceAll("\\{", "&#123;");
        text = text.replaceAll("\\}", "&#125;");
        text = text.replaceAll("\\1", "&#49;");

        return text;
    }

    /**
     * API to protect URLs from Radeox transformation
     *
     * @param url
     * @return encoded URL
     * @deprecated dedicated to Radeox which is deprecated since a long time
     */
    @Deprecated
    public static String escapeURL(String url)
    {
        url = url.replaceAll("\\~", "%7E");
        url = url.replaceAll("\\[", "%5B");
        url = url.replaceAll("\\]", "%5D");
        url = url.replaceAll("\\{", "%7B");
        url = url.replaceAll("\\}", "%7D");
        // We should not encode the following char for non local urls
        // since this might not be handle correctly by FF
        if (url.indexOf("//") == -1) {
            url = url.replaceAll("-", "%2D");
            url = url.replaceAll("\\*", "%2A");
        }

        return url;
    }

    /**
     * Translates a string into <code>application/x-www-form-urlencoded</code> format, so that it can be safely used in
     * URIs, as a parameter value in a query string or as a segment in the URI path. This uses the UTF-8 encoding, the
     * default encoding for URIs, as stated in <a href="http://tools.ietf.org/html/rfc3986#section-2.5">RFC 3986</a>.
     *
     * @param text the non encoded text
     * @param context the current context
     * @return encoded text
     * @see #decodeURI(String, XWikiContext)
     */
    public static String encodeURI(String text, XWikiContext context)
    {
        try {
            return URLEncoder.encode(text, URL_ENCODING);
        } catch (Exception e) {
            // Should not happen (UTF-8 is always available), but if so, fail securely
            return null;
        }
    }

    /**
     * Decodes a <code>application/x-www-form-urlencoded</code> string, the reverse of
     * {@link #encodeURI(String, XWikiContext)}. This uses the UTF-8 encoding, the default encoding for URIs, as stated
     * in <a href="http://tools.ietf.org/html/rfc3986#section-2.5">RFC 3986</a>.
     *
     * @param text the encoded text
     * @param context the current context
     * @return decoded text
     * @see #encodeURI(String, XWikiContext)
     */
    public static String decodeURI(String text, XWikiContext context)
    {
        try {
            return URLDecoder.decode(text, URL_ENCODING);
        } catch (Exception e) {
            // Should not happen (UTF-8 is always available)
            return text;
        }
    }

    /**
     * Removes all non alpha numerical characters from the passed text. First tries to convert accented chars to their
     * alpha numeric representation.
     *
     * @param text the text to convert
     * @return the alpha numeric equivalent
     */
    public static String convertToAlphaNumeric(String text)
    {
        // Start by removing accents
        String textNoAccents = Util.noaccents(text);

        // Now remove all non alphanumeric chars
        StringBuffer result = new StringBuffer(textNoAccents.length());
        char[] testChars = textNoAccents.toCharArray();
        for (char testChar : testChars) {
            if (Character.isLetterOrDigit(testChar) && testChar < 128) {
                result.append(testChar);
            }
        }

        return result.toString();
    }

    public static Date getFileLastModificationDate(String path)
    {
        try {
            File f = new File(path);

            return (new Date(f.lastModified()));
        } catch (Exception ex) {
            return new Date();
        }
    }

    /**
     * Validate a XML element name. XML elements must follow these naming rules :
     * <ul>
     * <li>Names can contain letters, numbers, and the following characters [., -, _].</li>
     * <li>Names must not start with a number or punctuation character.</li>
     * <li>Names must not start (case-insensitive) with the letters xml.</li>
     * <li>Names cannot contain spaces.</li>
     * </ul>
     *
     * @param elementName the XML element name to validate
     * @return true if the element name is valid, false if it is not
     */
    public static boolean isValidXMLElementName(String elementName)
    {
        if (elementName == null || elementName.equals("") || elementName.matches("(?i)^(xml).*")
            || !elementName.matches("(^[a-zA-Z\\_]+[\\w\\.\\-]*$)")) {
            return false;
        }

        return true;
    }

    /**
     * Load resources from: 1. FileSystem 2. ServletContext 3. ClassPath in this order.
     *
     * @param resource resource path to load
     * @return InputStream of resource or null if not found
     */
    public static InputStream getResourceAsStream(String resource)
    {
        File file = new File(resource);
        try {
            if (file.exists()) {
                return new FileInputStream(file);
            }
        } catch (Exception e) {
            // Probably running under -security, which prevents calling File.exists()
            LOGGER.debug("Failed load resource [" + resource + "] using a file path");
        }
        try {
            Container container = Utils.getComponent(Container.class);
            InputStream res = container.getApplicationContext().getResourceAsStream(resource);
            if (res != null) {
                return res;
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to load resource [" + resource + "] using the application context");
        }

        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    }

    /**
     * Normalize the given language code. Converts the given language code to lower case and checks its validity (i.e.
     * whether it is an ISO 639 language code or the string "default").
     *
     * <pre>
     * Util.normalizeLanguage(null)      = null
     * Util.normalizeLanguage("")        = ""
     * Util.normalizeLanguage("  ")      = ""
     * Util.normalizeLanguage("default") = "default"
     * Util.normalizeLanguage("DeFault") = "default"
     * Util.normalizeLanguage("invalid") = "default"
     * Util.normalizeLanguage("en")      = "en"
     * Util.normalizeLanguage("DE_at")   = "de_AT"
     * </pre>
     *
     * @param languageCode the language code to normalize
     * @return normalized language code or the string "default" if the code is invalid
     */
    public static String normalizeLanguage(String languageCode)
    {
        if (languageCode == null) {
            return null;
        }
        if (StringUtils.isBlank(languageCode)) {
            return "";
        }
        // handle language_COUNTRY case
        final String separator = "_";

        String[] parts = StringUtils.split(languageCode.toLowerCase(), "_-.");
        String result = parts[0];
        if (parts.length > 1) {
            parts[1] = parts[1].toUpperCase();
            // NOTE cannot use Locale#toString(), because it would change some language codes
            result = parts[0] + separator + parts[1];
        }
        // handle the "default" case
        final String defaultLanguage = "default";
        if (defaultLanguage.equals(result)) {
            return defaultLanguage;
        }
        try {
            Locale l = new Locale(parts[0], parts.length > 1 ? parts[1] : "");
            // Will throw an exception if the language code is not valid
            l.getISO3Language();
            return result;
        } catch (MissingResourceException ex) {
            LOGGER.warn("Invalid language: " + languageCode);
        }
        return defaultLanguage;
    }

    /**
     * Get a likely unique 64bit hash representing the provided uid string. Use the MD5 hashing algorithm.
     *
     * @param uid an uid string usually provided by
     *            {@link org.xwiki.model.internal.reference.LocalUidStringEntityReferenceSerializer} or
     *            {@link org.xwiki.model.internal.reference.UidStringEntityReferenceSerializer}
     * @return 64bit hash
     * @since 4.0M1
     */
    public static long getHash(String uid)
    {
        MessageDigest md5 = null;
        long hash = 0;

        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(uid.getBytes("UTF-8"));
            for (int l = digest.length, i = Math.max(0, digest.length - 9); i < l; i++) {
                hash = hash << 8 | ((long) digest[i] & 0xFF);
            }
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.error("Cannot retrieve MD5 provider for hashing", ex);
            throw new RuntimeException("MD5 hash is required for id hash");
        } catch (Exception ex) {
            LOGGER.error("Id computation failed during MD5 processing", ex);
            throw new RuntimeException("MD5 hash is required for id hash");
        }

        return hash;
    }
}

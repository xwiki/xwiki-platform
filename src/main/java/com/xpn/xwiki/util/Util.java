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

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.Cookie;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.oro.text.PatternCache;
import org.apache.oro.text.PatternCacheLRU;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Matcher;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.render.WikiSubstitution;
import com.xpn.xwiki.web.XWikiRequest;


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
        String orig = "אגהיטךכמןפצשûü";
        String targ = "aaaeeeeiioouuu";
        for (int i=0;i<orig.length();i++)
            temp = temp.replace(orig.charAt(i), targ.charAt(i));
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

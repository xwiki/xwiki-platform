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
package org.xwiki.xml;

import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;

import org.w3c.dom.Node;

/**
 * XML Utility methods.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public final class XMLUtils
{
    /** XML encoding of the "ampersand" character. */
    private static final String AMP = "&#38;";

    /** Regular expression recognizing XML-escaped "ampersand" characters. */
    private static final Pattern AMP_PATTERN = Pattern.compile("&(?:amp|#0*+38|#x0*+26);");

    /** XML encoding of the "single quote" character. */
    private static final String APOS = "&#39;";

    /** Regular expression recognizing XML-escaped "single quote" characters. */
    private static final Pattern APOS_PATTERN = Pattern.compile("&(?:apos|#0*+39|#x0*+27);");

    /** XML encoding of the "double quote" character. */
    private static final String QUOT = "&#34;";

    /** Regular expression recognizing XML-escaped "double quote" characters. */
    private static final Pattern QUOT_PATTERN = Pattern.compile("&(?:quot|#0*+34|#x0*+22);");

    /** XML encoding of the "less than" character. */
    private static final String LT = "&#60;";

    /** Regular expression recognizing XML-escaped "less than" characters. */
    private static final Pattern LT_PATTERN = Pattern.compile("&(?:lt|#0*+60|#x0*+3[cC]);");

    /** XML encoding of the "greater than" character. */
    private static final String GT = "&#62;";

    /** Regular expression recognizing XML-escaped "greater than" characters. */
    private static final Pattern GT_PATTERN = Pattern.compile("&(?:gt|#0*+62|#x0*+3[eE]);");

    /**
     * Private constructor since this is a utility class that shouldn't be instantiated (all methods are static).
     */
    private XMLUtils()
    {
        // Nothing to do
    }

    /**
     * Extracts a well-formed XML fragment from the given DOM tree.
     * 
     * @param node the root of the DOM tree where the extraction takes place
     * @param start the index of the first character
     * @param length the maximum number of characters in text nodes to include in the returned fragment
     * @return a well-formed XML fragment starting at the given character index and having up to the specified length,
     *         summing only the characters in text nodes
     * @since 1.6M2
     */
    public static String extractXML(Node node, int start, int length)
    {
        ExtractHandler handler = null;
        try {
            handler = new ExtractHandler(start, length);
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(new DOMSource(node), new SAXResult(handler));
            return handler.getResult();
        } catch (Throwable t) {
            if (handler != null && handler.isFinished()) {
                return handler.getResult();
            } else {
                throw new RuntimeException("Failed to extract XML", t);
            }
        }
    }

    /**
     * XML comment does not support some characters inside its content but there is no official escaping/unescaping for
     * it so we made our own.
     * <p>
     * <ul>
     * <li>1) Escape existing \</li>
     * <li>2) Escape --</li>
     * <li>3) Add "\" (unescaped as "") at the end if the last char is -</li>
     * </ul>
     * 
     * @param content the XML comment content to escape
     * @return the escaped content.
     * @since 1.9M2
     */
    public static String escapeXMLComment(String content)
    {
        StringBuffer str = new StringBuffer(content.length());

        char[] buff = content.toCharArray();
        char lastChar = 0;
        for (char c : buff) {
            if (c == '\\') {
                str.append('\\');
            } else if (c == '-' && lastChar == '-') {
                str.append('\\');
            }

            str.append(c);
            lastChar = c;
        }

        if (lastChar == '-') {
            str.append('\\');
        }

        return str.toString();
    }

    /**
     * XML comment does not support some characters inside its content but there is no official escaping/unescaping for
     * it so we made our own.
     * 
     * @param content the XML comment content to unescape
     * @return the unescaped content.
     * @see #escapeXMLComment(String)
     * @since 1.9M2
     */
    public static String unescapeXMLComment(String content)
    {
        StringBuffer str = new StringBuffer(content.length());

        char[] buff = content.toCharArray();
        boolean escaped = false;
        for (char c : buff) {
            if (!escaped && c == '\\') {
                escaped = true;
                continue;
            }

            str.append(c);
            escaped = false;
        }

        return str.toString();
    }

    /**
     * Escapes all the XML special characters in a <code>String</code> using numerical XML entities. Specifically,
     * escapes &lt;, &gt;, ", ' and &amp;.
     *
     * @param content the text to escape, may be {@code null}
     * @return a new escaped {@code String}, {@code null} if {@code null} input
     */
    public static String escape(Object content)
    {
        return escapeAttributeValue(content);
    }

    /**
     * Escapes all the XML special characters in a <code>String</code> using numerical XML entities, so that the
     * resulting string can safely be used as an XML attribute value. Specifically, escapes &lt;, &gt;, ", ' and &amp;.
     *
     * @param content the text to escape, may be {@code null}
     * @return a new escaped {@code String}, {@code null} if {@code null} input
     */
    public static String escapeAttributeValue(Object content)
    {
        if (content == null) {
            return null;
        }
        String str = String.valueOf(content);
        StringBuilder result = new StringBuilder((int) (str.length() * 1.1));
        int length = str.length();
        char c;
        for (int i = 0; i < length; ++i) {
            c = str.charAt(i);
            switch (c) {
                case '&':
                    result.append(AMP);
                    break;
                case '\'':
                    result.append(APOS);
                    break;
                case '"':
                    result.append(QUOT);
                    break;
                case '<':
                    result.append(LT);
                    break;
                case '>':
                    result.append(GT);
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Escapes the XML special characters in a <code>String</code> using numerical XML entities, so that the resulting
     * string can safely be used as an XML text node. Specifically, escapes &lt;, &gt;, and &amp;.
     *
     * @param content the text to escape, may be {@code null}
     * @return a new escaped {@code String}, {@code null} if {@code null} input
     */
    public static String escapeElementContent(Object content)
    {
        if (content == null) {
            return null;
        }
        String str = String.valueOf(content);
        StringBuilder result = new StringBuilder((int) (str.length() * 1.1));
        int length = str.length();
        char c;
        for (int i = 0; i < length; ++i) {
            c = str.charAt(i);
            switch (c) {
                case '&':
                    result.append(AMP);
                    break;
                case '<':
                    result.append(LT);
                    break;
                case '>':
                    result.append(GT);
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Unescape encoded special XML characters. Only &gt;, &lt; &amp;, " and ' are unescaped, since they are the only
     * ones that affect the resulting markup.
     *
     * @param content the text to decode, may be {@code null}
     * @return unescaped content, {@code null} if {@code null} input
     */
    public static String unescape(Object content)
    {
        if (content == null) {
            return null;
        }
        String str = String.valueOf(content);

        str = APOS_PATTERN.matcher(str).replaceAll("'");
        str = QUOT_PATTERN.matcher(str).replaceAll("\"");
        str = LT_PATTERN.matcher(str).replaceAll("<");
        str = GT_PATTERN.matcher(str).replaceAll(">");
        str = AMP_PATTERN.matcher(str).replaceAll("&");

        return str;
    }
}

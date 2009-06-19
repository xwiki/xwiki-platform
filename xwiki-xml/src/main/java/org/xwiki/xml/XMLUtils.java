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
     * XML content does not support some characters inside its content but there is no official escaping/unescaping for
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
     * XML content does not support some characters inside its content but there is no official escaping/unescaping for
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
}

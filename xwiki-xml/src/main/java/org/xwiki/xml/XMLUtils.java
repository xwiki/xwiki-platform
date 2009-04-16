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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;

import org.jdom.input.DOMBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XML Utility methods.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public final class XMLUtils
{
    /**
     * JDOM's XMLOutputter class converts reserved XML characters (<, >, ' , &, \r and \n) into their entity format
     * (&lt;, &gt; &apos; &amp; &#xD; and \r\n). However since we're using HTML Cleaner
     * (http://htmlcleaner.sourceforge.net/) and since it's buggy for character escapes we have turned off character
     * escaping for it and thus we need to perform selective escaping here.
     * 
     * @todo Remove this complex escaping code when SF HTML Cleaner will do proper escaping
     */
    public static class XWikiXMLOutputter extends XMLOutputter
    {
        /**
         * Regex to recognize a XML Entity.
         */
        private static final Pattern ENTITY = Pattern.compile("&[a-z]+;|&#[0-9a-zA-Z]+;");

        /**
         * Ampersand character.
         */
        private static final String AMPERSAND = "&";

        /**
         * {@inheritDoc}
         * 
         * @see XMLOutputter#XMLOutputter(Format)
         */
        public XWikiXMLOutputter(Format format)
        {
            super(format);
        }

        /**
         * {@inheritDoc}
         * 
         * @see XMLOutputter#escapeElementEntities(String)
         */
        @Override
        public String escapeElementEntities(String text)
        {
            if (text.length() == 0) {
                return text;
            }

            String result;
            int pos1 = text.indexOf("<![CDATA[");
            if (pos1 > -1) {
                int pos2 = text.indexOf("]]>", pos1 + 9);
                if (pos2 + 3 == text.length()) {
                    return text;
                }
                result = escapeElementEntities(text.substring(0, pos1));
                if (pos2 + 3 == text.length()) {
                    result = result + text.substring(pos1);
                } else {
                    result = result + text.substring(pos1, pos2 + 3) + escapeElementEntities(text.substring(pos2 + 3));
                }
            } else {
                result = escapeAmpersand(text);
                result = result.replaceAll("<", "&lt;");
                result = result.replaceAll(">", "&gt;");
            }

            return result;
        }

        /**
         * {@inheritDoc}
         * 
         * @see XMLOutputter#escapeAttributeEntities(String)
         */
        @Override
        public String escapeAttributeEntities(String text)
        {
            String result = escapeElementEntities(text);

            // Attribute values must have quotes escaped since attributes are defined with quotes...
            result = result.replaceAll("\"", "&quot;");

            return result;
        }

        /**
         * Escape ampersand when it's not defining an entity.
         * 
         * @param text the text to escape
         * @return the escaped text
         */
        private String escapeAmpersand(String text)
        {
            StringBuffer buffer = new StringBuffer(text);
            // find all occurrences of &
            int pos = buffer.indexOf(AMPERSAND);
            while (pos > -1 && pos < buffer.length()) {
                // Check if the & is an entity
                Matcher matcher = ENTITY.matcher(buffer.substring(pos));
                if (matcher.lookingAt()) {
                    // We've found an entity, don't do anything, just skip it
                    pos = pos + matcher.end() - matcher.start();
                } else {
                    // No entity, escape the &
                    buffer.replace(pos, pos + 1, "&amp;");
                    pos += 5;
                }
                pos = buffer.indexOf(AMPERSAND, pos);
            }
            return buffer.toString();
        }
    }

    /**
     * Private constructor since this is a utility class that shouldn't be instantiated (all methods are static).
     */
    private XMLUtils()
    {
        // Nothing to do
    }

    /**
     * @param document the W3C Document to transform into a String
     * @return the XML as a String
     */
    public static String toString(Document document)
    {
        // Note: We don't use javax.xml.transform.Transformer since it prints our valid XHTML as HTML which is not
        // XHTML compliant. For example it transforms our "<hr/>" into "<hr>.
        DOMBuilder builder = new DOMBuilder();
        org.jdom.Document jdomDoc = builder.build(document);

        Format format = Format.getRawFormat();
        // Force newlines to use \n since otherwise the default is \n\r.
        // See http://www.jdom.org/docs/apidocs/org/jdom/output/Format.html#setLineSeparator(java.lang.String)
        format.setLineSeparator("\n");

        XMLOutputter outputter = new XWikiXMLOutputter(format);
        return outputter.outputString(jdomDoc);
    }

    /**
     * Strip the HTML envelope if it exists. Precisely this means removing the head tag and move all tags in the body
     * tag directly under the html element. This is useful for example if you wish to insert an HTML fragment into an
     * existing HTML page.
     * 
     * @param document the w3c Document to strip
     */
    public static void stripHTMLEnvelope(Document document)
    {
        org.w3c.dom.Element root = document.getDocumentElement();
        if (root.getNodeName().equalsIgnoreCase("html")) {
            // Look for a head element below the root element and for a body element
            Node bodyNode = null;
            Node headNode = null;
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equalsIgnoreCase("head")) {
                    headNode = node;
                } else if (node.getNodeName().equalsIgnoreCase("body")) {
                    bodyNode = node;
                }
            }

            if (headNode != null) {
                root.removeChild(headNode);
            }

            if (bodyNode != null) {
                // Move all children of body node under the root element
                NodeList bodyChildrenNodes = bodyNode.getChildNodes();
                while (bodyChildrenNodes.getLength() > 0) {
                    root.insertBefore(bodyChildrenNodes.item(0), null);
                }
                root.removeChild(bodyNode);
            }
        }
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

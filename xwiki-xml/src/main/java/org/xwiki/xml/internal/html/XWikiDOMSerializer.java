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
 *
 */
package org.xwiki.xml.internal.html;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CommentToken;
import org.htmlcleaner.ContentToken;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.Utils;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Generate a W3C Document from a SF's HTML Cleaner TagNode.
 * Original implementation by Vladimir Nikic, under the BSD license
 * (see http://htmlcleaner.sourceforge.net/license.php).
 * 
 * Modified to bypass following bugs:
 * <ul>
 *   <li>https://sourceforge.net/tracker/?func=detail&aid=2691888&group_id=183053&atid=903696</li>
 *   <li>https://sourceforge.net/tracker/?func=detail&aid=2761963&group_id=183053&atid=903696</li>
 * </ul>
 * 
 * @version $Id$
 * @since 1.8.2
 */
public class XWikiDOMSerializer
{
    /**
     * The Regex Pattern to recognize a CDATA block.
     */
    private static final Pattern CDATA_PATTERN =
        Pattern.compile("<!\\[CDATA\\[.*(\\]\\]>|<!\\[CDATA\\[)", Pattern.DOTALL);

    /**
     * The HTML Cleaner properties set by the user to control the HTML cleaning.
     */
    private CleanerProperties props;

    /**
     * Whether XML entities should be escaped or not.
     */
    private boolean escapeXml;

    /**
     * @param props the HTML Cleaner properties set by the user to control the HTML cleaning.
     * @param escapeXml if true then escape XML entities
     */
    public XWikiDOMSerializer(CleanerProperties props, boolean escapeXml)
    {
        this.props = props;
        this.escapeXml = escapeXml;
    }

    /**
     * @param rootNode the HTML Cleaner root node to serialize
     * @return the W3C Document object
     * @throws ParserConfigurationException if there's an error during serialization
     */
    public Document createDOM(TagNode rootNode) throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        Document document = factory.newDocumentBuilder().newDocument();
        Element rootElement = document.createElement(rootNode.getName());
        document.appendChild(rootElement);

        createSubnodes(document, rootElement, rootNode.getChildren());

        return document;
    }

    /**
     * Perform CDATA transformations if the user has specified to use CDATA inside scripts and style elements.
     * 
     * @param document the W3C Document to use for creating new DOM elements
     * @param element the W3C element to which we'll add the text content to
     * @param bufferedContent the buffered text content on which we need to perform the CDATA transformations
     * @param item the current HTML Cleaner node being processed
     */
    private void flushContent(Document document, Element element, StringBuffer bufferedContent, Object item)
    {
        if (bufferedContent.length() > 0 && !(item instanceof ContentToken)) {
            // Flush the buffered content
            String nodeName = element.getNodeName();
            boolean specialCase = this.props.isUseCdataForScriptAndStyle()
                && ("script".equalsIgnoreCase(nodeName) || "style".equalsIgnoreCase(nodeName));
            String content = bufferedContent.toString();

            if (this.escapeXml && !specialCase) {
                content = Utils.escapeXml(content, this.props, true);
            } else if (specialCase) {
                content = processCDATABlocks(content);
            }

            // Generate a javascript comment in front on the CDATA block so that it works in IE6 and when
            // serving XHTML under a mimetype of HTML.
            if (specialCase) {
                element.appendChild(document.createTextNode("//"));
                element.appendChild(document.createCDATASection("\n" + content + "\n//"));
            } else {
                element.appendChild(document.createTextNode(content));
            }

            bufferedContent.setLength(0);
        }
    }

    /**
     * Remove any existing CDATA section and unencode HTML entities that are not inside a CDATA block.
     * 
     * @param content the text input to transform
     * @return the transformed content that will be wrapped inside a CDATA block
     */
    private String processCDATABlocks(String content)
    {
        StringBuffer result = new StringBuffer();
        Matcher matcher = CDATA_PATTERN.matcher(content);
        int cursor = 0;
        while (matcher.find()) {
            result.append(StringEscapeUtils.unescapeHtml(content.substring(cursor, matcher.start())));
            result.append(content.substring(matcher.start() + 9, matcher.end() - matcher.group(1).length()));
            cursor = matcher.end() - matcher.group(1).length() + 3;
        }
        // Copy the remaining text data in the result buffer
        if (cursor < content.length()) {
            result.append(StringEscapeUtils.unescapeHtml(content.substring(cursor)));
        }
        // Ensure ther's no invalid <![CDATA[ or ]]> remaining.
        String contentResult = result.toString().replace("<![CDATA[", "").replace("]]>", "");

        return contentResult;
    }

    /**
     * Serialize a given SF HTML Cleaner node.
     * 
     * @param document the W3C Document to use for creating new DOM elements
     * @param element the W3C element to which we'll add the subnodes to
     * @param tagChildren the SF HTML Cleaner nodes to serialize for that node
     */
    private void createSubnodes(Document document, Element element, List<Object> tagChildren)
    {
        // We've modified the original implementation based in SF's HTML Cleaner to better handle CDATA.
        // More specifically we want to handle the following 3 use cases:
        //
        // Use case 1: useCdata = true && input is:
        // <script>...<![CDATA[...]]>...</script>
        // In this case we must make sure to have only one CDATA block.
        //
        // Use case 2: useCdata = true && input is:
        // <script>...entities not encoded (e.g. "<")...</script>
        // We must generate a CDATA block around the whole content (the HTML Tokenizer split
        // ContentToken on "<" character so we need to join them before creating the CDATA block.
        // We must also unencode any entities (i.e. transform "&lt;" into "<") since we'll be
        // wrapping them in a CDATA section.
        //
        // Use case 3: useCData = false
        // Simply group all ContentToken together.

        StringBuffer bufferedContent = new StringBuffer();

        if (tagChildren != null) {
            for (Object item : tagChildren) {
                // Flush content tokens
                flushContent(document, element, bufferedContent, item);

                if (item instanceof CommentToken) {
                    CommentToken commentToken = (CommentToken) item;
                    Comment comment = document.createComment(commentToken.getContent());
                    element.appendChild(comment);
                } else if (item instanceof ContentToken) {
                    ContentToken contentToken = (ContentToken) item;
                    String content = contentToken.getContent();
                    bufferedContent.append(content);
                } else if (item instanceof TagNode) {
                    TagNode subTagNode = (TagNode) item;
                    Element subelement = document.createElement(subTagNode.getName());
                    @SuppressWarnings("unchecked")
                    Map<String, String> attributes = subTagNode.getAttributes();
                    for (Map.Entry<String, String> entry : attributes.entrySet()) {
                        String attrName = entry.getKey();
                        String attrValue = entry.getValue();
                        if (this.escapeXml) {
                            attrValue = Utils.escapeXml(attrValue, this.props, true);
                        }
                        subelement.setAttribute(attrName, attrValue);
                    }

                    // recursively create subnodes
                    createSubnodes(document, subelement, subTagNode.getChildren());

                    element.appendChild(subelement);
                } else if (item instanceof List< ? >) {
                    @SuppressWarnings("unchecked")
                    List<Object> sublist = (List<Object>) item;
                    createSubnodes(document, element, sublist);
                }
            }
            flushContent(document, element, bufferedContent, null);
        }
    }

}

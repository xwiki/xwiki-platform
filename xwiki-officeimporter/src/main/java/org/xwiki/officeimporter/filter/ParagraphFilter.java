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
 */package org.xwiki.officeimporter.filter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * Open Office server creates a new paragraph element for every line break (enter) in the original office document. For
 * an example: <br/><br/> {@code<P STYLE="margin-bottom: 0in">Line - 1</P>}<br/>
 * {@code<P STYLE="margin-bottom: 0in">Line - 2</P>}<br/> {@code<P STYLE="margin-bottom: 0in">Line - 3</P>}<br/><br/> Is
 * the output produced by open office for a simple document containing only three consecutive lines. Further, to
 * represent empty lines, Open Office uses following element: <br/><br/> {@code<P STYLE="margin-bottom: 0in"><BR></P>}
 * <br/><br/> These constructs when rendered on browsers doesn't resemble the original document at all, and when parsed
 * into xwiki/2.0 syntax the generated xwiki syntax is also invalid (obviously). The purpose of this filter is to clean
 * up such html content by merging consecutive paragraph sequences and appropriately inserting {@code<br/>} elements.
 */
public class ParagraphFilter implements HTMLFilter
{
    /**
     * {@inheritDoc}
     */
    public void filter(Document document)
    {
        // This is only a partial workaround, need to implement the correct scheme (mentioned in the class comment)
        // soon.
        NodeList paragraphs = document.getElementsByTagName("p");
        for (int i = 0; i < paragraphs.getLength(); i++) {
            Element paragraph = (Element) paragraphs.item(i);
            if (paragraph.getTextContent().trim().equals("")) {
                // We suspect this is an empty paragraph but it is possible that it contains other
                // non-textual tags like images. For the moment we'll only search for internal image
                // tags, we might have to refine this criterion later.
                NodeList internalImages = paragraph.getElementsByTagName("img");
                if (internalImages.getLength() == 0) {
                    NamedNodeMap attributes = paragraph.getAttributes();
                    while (attributes.getLength() > 0) {
                        attributes.removeNamedItem(attributes.item(0).getNodeName());
                    }
                }
            }
        }
    }
}

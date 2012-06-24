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
package org.xwiki.officeimporter.internal.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;
import org.xwiki.xml.html.filter.ElementSelector;

/**
 * Open Office server creates a new paragraph element for every line break (enter) in the original office document. For
 * an example: <br/>
 * {@code<P STYLE="margin-bottom: 0in">Line - 1</P>}<br/>
 * {@code<P STYLE="margin-bottom: 0in">Line - 2</P>}<br/>
 * {@code<P STYLE="margin-bottom: 0in">Line - 3</P>}<br/>
 * Is the output produced by open office for a simple document containing only three consecutive lines. Further, to
 * represent empty lines, Open Office uses following element: <br/>
 * {@code<P STYLE="margin-bottom: 0in"><BR></P>} <br/>
 * These constructs when rendered on browsers doesn't resemble the original document at all, and when parsed
 * into xwiki/2.0 syntax the generated xwiki syntax is also invalid (obviously). The purpose of this filter is to clean
 * up such html content by merging consecutive paragraph sequences and appropriately inserting {@code<br/>} elements.
 * 
 * @version $Id$
 */
@Component("officeimporter/paragraph")
public class ParagraphFilter extends AbstractHTMLFilter
{
    @Override
    public void filter(Document document, Map<String, String> cleaningParams)
    {
        for (Node p : findEmptyLineParagraphSequences(document)) {
            Node next = p.getNextSibling();
            // Remove the first empty paragraph.
            p.getParentNode().removeChild(p);
            // Replace the following ones by their children elements.
            while (isEmptyLineParagraph(next)) {
                Node following = next.getNextSibling();
                replaceWithChildren((Element) next);
                next = following;
            }
        }
    }

    /**
     * Finds all the empty paragraph sequences in the document.
     * 
     * @param document the {@link Document}
     * @return a list of nodes containing leading paragraph elements of each sequence found.
     */
    private List<Node> findEmptyLineParagraphSequences(Document document)
    {
        List<Element> emptyLineParagraphs =
            filterDescendants(document.getDocumentElement(), new String[] {TAG_P}, new ElementSelector()
            {
                @Override
                public boolean isSelected(Element element)
                {
                    return isEmptyLineParagraph(element);
                }
            });
        List<Node> sequences = new ArrayList<Node>();
        for (Element emptyLineParagraph : emptyLineParagraphs) {
            Node prev = emptyLineParagraph.getPreviousSibling();
            // Skip garbage.
            while (isEmptyTextNode(prev) || isCommentNode(prev)) {
                Node oneBefore = prev.getPreviousSibling();
                prev.getParentNode().removeChild(prev);
                prev = oneBefore;
            }
            if (!isEmptyLineParagraph(prev)) {
                // This is the beginning of a sequence of empty line paragraphs
                sequences.add(emptyLineParagraph);
            }
        }
        return sequences;
    }

    /**
     * Checks if a node represents a paragraph element.
     * 
     * @param node the {@link Node}.
     * @return True if the node represents a {@code <p/>} element.
     */
    private boolean isParagraph(Node node)
    {
        return null != node && node.getNodeName().equals(TAG_P);
    }

    /**
     * Checks if a node represents a {@code<p><br/></p>} element used by open office to represent an empty line.
     * 
     * @param node the {@link Node}
     * @return true if the node represents an empty line.
     */
    private boolean isEmptyLineParagraph(Node node)
    {
        boolean isEmptyLine = false;
        if (isParagraph(node)) {
            isEmptyLine = true;
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (!(isEmptyTextNode(child) || isCommentNode(child) || isLineBreak(child))) {
                    isEmptyLine = false;
                }
            }
        }
        return isEmptyLine;
    }

    /**
     * Checks if a node represents empty text content (white space).
     * 
     * @param node the {@link Node}.
     * @return true if the node represents white space.
     */
    private boolean isEmptyTextNode(Node node)
    {
        return null != node && node.getNodeType() == Node.TEXT_NODE && node.getTextContent().trim().equals("");
    }

    /**
     * Checks if a node represents an html comment.
     * 
     * @param node the {@link Node}.
     * @return true if the node is a comment node.
     */
    private boolean isCommentNode(Node node)
    {
        return null != node && node.getNodeType() == Node.COMMENT_NODE;
    }

    /**
     * Checks if a node represents an html line break.
     * 
     * @param node the {@link Node}
     * @return true of the node represents a line break.
     */
    private boolean isLineBreak(Node node)
    {
        return null != node && node.getNodeName().equals(TAG_BR);
    }
}

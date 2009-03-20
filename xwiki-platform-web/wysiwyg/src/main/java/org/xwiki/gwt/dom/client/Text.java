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
package org.xwiki.gwt.dom.client;

import com.google.gwt.dom.client.Node;

/**
 * Extends the text implementation provided by GWT to add useful methods. All of them should be removed as soon as they
 * make their way into GWT's API.
 * 
 * @version $Id$
 */
public final class Text extends com.google.gwt.dom.client.Text
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected Text()
    {
    }

    /**
     * Casts a {@link Node} to an instance of this type.
     * 
     * @param node the instance to be casted to this type.
     * @return the given object as an instance of {@link Text}.
     */
    public static Text as(Node node)
    {
        assert node.getNodeType() == Node.TEXT_NODE;
        return (Text) com.google.gwt.dom.client.Text.as(node);
    }

    /**
     * Merges all the neighbor text nodes of this text node and returns a text fragment specifying where is this text
     * placed in the final text node resulted after the merge.
     * 
     * @return a text fragment showing the place of this text in the node obtained after the merge.
     */
    public TextFragment normalize()
    {
        StringBuffer leftText = new StringBuffer();
        Node leftSibling = this.getPreviousSibling();
        while (leftSibling != null && leftSibling.getNodeType() == Node.TEXT_NODE) {
            leftText.insert(0, leftSibling.getNodeValue());
            leftSibling.getParentNode().removeChild(leftSibling);
            leftSibling = this.getPreviousSibling();
        }

        StringBuffer rightText = new StringBuffer();
        Node rightSibling = this.getNextSibling();
        while (rightSibling != null && rightSibling.getNodeType() == Node.TEXT_NODE) {
            rightText.append(rightSibling.getNodeValue());
            rightSibling.getParentNode().removeChild(rightSibling);
            rightSibling = this.getNextSibling();
        }

        int startIndex = leftText.length();
        int endIndex = startIndex + this.getLength();
        this.setData(leftText.toString() + this.getData() + rightText.toString());
        return new TextFragment(this, startIndex, endIndex);
    }

    /**
     * @return the offset of this text node relative to the left-most successive text node sibling. The offset is
     *         expressed as the number of characters between this text node and the reference point.
     */
    public int getOffset()
    {
        int offset = 0;
        Node leftSibling = this.getPreviousSibling();
        while (leftSibling != null) {
            if (leftSibling.getNodeType() == Node.TEXT_NODE) {
                offset += leftSibling.getNodeValue().length();
            } else if (DOMUtils.getInstance().isSerializable(leftSibling)) {
                break;
            }
            leftSibling = leftSibling.getPreviousSibling();
        }
        return offset;
    }

    /**
     * Keeps the text between the given indexes as the value of this node. The remaining text, if present, is placed in
     * sibling text nodes.
     * 
     * @param startIndex crop start
     * @param endIndex crop end
     */
    public void crop(int startIndex, int endIndex)
    {
        if (startIndex > 0) {
            String leftData = getData().substring(0, startIndex);
            Text left = getOwnerDocument().createTextNode(leftData).cast();
            getParentNode().insertBefore(left, this);
            setData(getData().substring(startIndex));
        }

        int length = endIndex - startIndex;
        if (length < getLength()) {
            String rightData = getData().substring(length);
            Text right = getOwnerDocument().createTextNode(rightData).cast();
            DOMUtils.getInstance().insertAfter(right, this);
            setData(getData().substring(0, length));
        }
    }
}

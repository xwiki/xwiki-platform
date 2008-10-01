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
package com.xpn.xwiki.wysiwyg.client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;

public abstract class DOMUtils
{
    private static DOMUtils instance = GWT.create(DOMUtils.class);

    public static synchronized DOMUtils getInstance()
    {
        if (instance == null) {
            instance = GWT.create(DOMUtils.class);
        }
        return instance;
    }

    public abstract String getComputedStyleProperty(Element el, String propertyName);

    public Node getNextLeaf(Node node)
    {
        while (node != null && node.getNextSibling() == null) {
            node = node.getParentNode();
        }
        if (node == null) {
            return null;
        } else {
            return getFirstLeaf(node.getNextSibling());
        }
    }

    public Node getFirstLeaf(Node node)
    {
        while (node.hasChildNodes()) {
            node = node.getFirstChild();
        }
        return node;
    }

    public int getNodeIndex(Node node)
    {
        int index = 0;
        while (node.getPreviousSibling() != null) {
            index++;
            node = node.getPreviousSibling();
        }
        return index;
    }

    public TextFragment normalize(Text text)
    {
        StringBuffer leftText = new StringBuffer();
        Node leftSibling = text.getPreviousSibling();
        while (leftSibling != null && leftSibling.getNodeType() == Node.TEXT_NODE) {
            leftText.insert(0, leftSibling.getNodeValue());
            leftSibling.getParentNode().removeChild(leftSibling);
            leftSibling = text.getPreviousSibling();
        }

        StringBuffer rightText = new StringBuffer();
        Node rightSibling = text.getNextSibling();
        while (rightSibling != null && rightSibling.getNodeType() == Node.TEXT_NODE) {
            rightText.append(rightSibling.getNodeValue());
            rightSibling.getParentNode().removeChild(rightSibling);
            rightSibling = text.getNextSibling();
        }

        int startIndex = leftText.length();
        int endIndex = startIndex + text.getLength();
        text.setData(leftText.toString() + text.getData() + rightText.toString());
        return new TextFragment(text, startIndex, endIndex);
    }

    public int getOffset(Text text)
    {
        int offset = 0;
        Node leftSibling = text.getPreviousSibling();
        while (leftSibling != null && leftSibling.getNodeType() == Node.TEXT_NODE) {
            offset += leftSibling.getNodeValue().length();
            leftSibling = leftSibling.getPreviousSibling();
        }
        return offset;
    }

    public boolean isInline(Node node)
    {
        switch (node.getNodeType()) {
            case Node.TEXT_NODE:
                return true;
            case Node.ELEMENT_NODE:
                return "inline".equalsIgnoreCase(getComputedStyleProperty((Element) node, "display"));
            default:
                return false;
        }
    }
}

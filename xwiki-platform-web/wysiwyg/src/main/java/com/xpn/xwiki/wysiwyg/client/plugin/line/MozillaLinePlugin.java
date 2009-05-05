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
package com.xpn.xwiki.wysiwyg.client.plugin.line;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Range;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;

/**
 * Mozilla specific implementation of the {@link LinePlugin}.
 * 
 * @version $Id$
 */
public class MozillaLinePlugin extends LinePlugin
{

    /**
     * {@inheritDoc}<br/>
     * We overwrite in order to fix a Mozilla bug which causes the caret to be rendered on the same line after you press
     * Enter, if the new line doesn't have any visible contents. Once you start typing the caret moves below, but it
     * looks strange before you type. We fixed the bug by adding a BR at the end of the new line.
     * 
     * @see LinePlugin#insertLineBreak(Node, Range)
     */
    protected void insertLineBreak(Node container, Range caret)
    {
        super.insertLineBreak(container, caret);

        // Start container should be a text node.
        Node lastLeaf;
        Node leaf = caret.getStartContainer();
        // Look if there is any visible element on the new line, taking care to remain in the current block container.
        do {
            if (needsSpace(leaf)) {
                return;
            }
            lastLeaf = leaf;
            leaf = domUtils.getNextLeaf(leaf);
        } while (leaf != null && container == domUtils.getNearestBlockContainer(leaf));

        // It seems there's no visible element on the new line. We should add one.
        domUtils.insertAfter(getTextArea().getDocument().xCreateBRElement(), lastLeaf);
    }

    /**
     * {@inheritDoc}<br/>
     * We overwrite in order to fix a Mozilla bug which makes empty paragraphs invisible. We add a BR to the created
     * paragraph if it's empty.
     * 
     * @see LinePlugin#splitLine(Node, Range)
     */
    protected void splitLine(Node container, Range caret)
    {
        super.splitLine(container, caret);

        // The caret should have been placed inside the new paragraph.
        Node paragraph = domUtils.getNearestBlockContainer(caret.getStartContainer());
        // Look if there is any visible element on the new line, taking care to remain in the current block container.
        Node leaf = domUtils.getFirstLeaf(paragraph);
        do {
            if (needsSpace(leaf)) {
                return;
            }
            leaf = domUtils.getNextLeaf(leaf);
        } while (leaf != null && paragraph == domUtils.getNearestBlockContainer(leaf));
        // It seems there's no visible element inside the newly created paragraph. We should add one.
        paragraph.appendChild(getTextArea().getDocument().xCreateBRElement());
    }

    /**
     * {@inheritDoc}<br/>
     * We overwrite in order to fix a Mozilla bug which makes empty lines invisible. We add a line break to the newly
     * created empty line.
     * 
     * @see LinePlugin#insertEmptyLine(Node, Range)
     */
    protected void insertEmptyLine(Node container, Range caret)
    {
        super.insertEmptyLine(container, caret);

        Node emptyLine;
        if (domUtils.isFlowContainer(container)) {
            emptyLine = container.getFirstChild();
        } else {
            emptyLine = container.getPreviousSibling();
        }
        emptyLine.appendChild(getTextArea().getDocument().xCreateBRElement());
    }

    /**
     * {@inheritDoc}<br/>
     * We overwrite in order to fix a Mozilla bug which makes empty paragraphs invisible. We add a BR to the newly
     * created paragraph.
     * 
     * @see LinePlugin#replaceEmptyDivsWithParagraphs()
     */
    protected void replaceEmptyDivsWithParagraphs()
    {
        super.replaceEmptyDivsWithParagraphs();

        Document document = getTextArea().getDocument();
        NodeList<com.google.gwt.dom.client.Element> paragraphs = document.getBody().getElementsByTagName("p");
        for (int i = 0; i < paragraphs.getLength(); i++) {
            Node paragraph = paragraphs.getItem(i);
            if (!paragraph.hasChildNodes()) {
                // The user cannot place the caret inside an empty paragraph in Firefox. The workaround to make an empty
                // paragraph editable is to append a BR.
                paragraph.appendChild(document.xCreateBRElement());
            }
        }
    }
}

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

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.InnerHTMLListener;
import org.xwiki.gwt.dom.client.Range;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * Internet Explorer specific implementation of the {@link LinePlugin}.
 * 
 * @version $Id$
 */
public class IELinePlugin extends LinePlugin implements InnerHTMLListener
{
    /**
     * {@inheritDoc}
     * 
     * @see LinePlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);
        getTextArea().getDocument().addInnerHTMLListener(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see LinePlugin#destroy()
     */
    public void destroy()
    {
        getTextArea().getDocument().removeInnerHTMLListener(this);
        super.destroy();
    }

    /**
     * {@inheritDoc}<br/>
     * We overwrite in order to fix a IE bug which makes empty lines invisible. Setting the inner HTML to the empty
     * string seems to do the trick.
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

            if (!container.hasChildNodes()) {
                // If the caret is inside an empty block level container and we insert an empty line before then the
                // caret doesn't remain in its place. We have to reset the caret.
                container.appendChild(container.getOwnerDocument().createTextNode(""));
                caret.selectNodeContents(container.getFirstChild());
            }
        }
        // Empty lines are not displayed in IE. Strangely, setting the inner HTML to the empty string
        // forces IE to render the empty lines. Appending an empty text node doesn't help.
        Element.as(emptyLine).setInnerHTML("");
    }

    /**
     * {@inheritDoc}<br/>
     * We overwrite in order to fix a IE bug which makes empty paragraphs invisible. Setting the inner HTML to the empty
     * string seems to do the trick.
     * 
     * @see LinePlugin#replaceEmptyDivsWithParagraphs()
     */
    protected void replaceEmptyDivsWithParagraphs()
    {
        super.replaceEmptyDivsWithParagraphs();
        ensureEmptyLinesAreEditable((Element) getTextArea().getDocument().getBody().cast());
    }

    /**
     * Ensures all the empty lines inside the given container are editable.
     * 
     * @param container the element where to look for empty lines
     */
    protected void ensureEmptyLinesAreEditable(Element container)
    {
        NodeList<com.google.gwt.dom.client.Element> paragraphs = container.getElementsByTagName("p");
        for (int i = 0; i < paragraphs.getLength(); i++) {
            Element paragraph = paragraphs.getItem(i).cast();
            if (!paragraph.hasChildNodes()) {
                // Empty paragraphs are not displayed in IE. Strangely, setting the inner HTML to the empty string
                // forces IE to render the empty paragraphs. Appending an empty text node doesn't help.
                paragraph.setInnerHTML("");
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InnerHTMLListener#onInnerHTMLChange(Element)
     */
    public void onInnerHTMLChange(Element element)
    {
        ensureEmptyLinesAreEditable(element);
    }
}

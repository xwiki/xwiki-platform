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
package org.xwiki.gwt.wysiwyg.client.plugin.line;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.InnerHTMLListener;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.dom.client.Node;

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
     * @see LinePlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);
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
     * If the caret is inside an empty block level container and we insert an empty line before then the caret doesn't
     * remain in its place. We have to reset the caret.
     * 
     * @see LinePlugin#insertEmptyLine(Node, Range)
     */
    protected void insertEmptyLine(Node container, Range caret)
    {
        super.insertEmptyLine(container, caret);

        if (!container.hasChildNodes()) {
            container.appendChild(container.getOwnerDocument().createTextNode(""));
            caret.selectNodeContents(container.getFirstChild());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InnerHTMLListener#onInnerHTMLChange(Element)
     */
    public void onInnerHTMLChange(Element element)
    {
        element.ensureEditable();
    }
}

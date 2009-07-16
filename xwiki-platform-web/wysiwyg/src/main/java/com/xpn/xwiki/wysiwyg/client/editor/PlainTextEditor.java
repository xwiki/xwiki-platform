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

package com.xpn.xwiki.wysiwyg.client.editor;

import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;

/**
 * Source editor.
 * 
 * @version $Id$
 */
public class PlainTextEditor extends Composite
{
    /**
     * Container panel.
     */
    private FlowPanel container;

    /**
     * The plain text area.
     */
    private TextArea textArea;

    /**
     * Creates a new plain text editor that wraps the given text area element.
     * 
     * @param textAreaElement the plain HTML text area element to wrap
     */
    public PlainTextEditor(TextAreaElement textAreaElement)
    {
        textArea = TextArea.wrap(textAreaElement);
        textArea.setStyleName("xPlainTextEditor");

        container = new FlowPanel();
        container.add(textArea);

        initWidget(container);
    }

    /**
     * @return the text area used by the editor
     */
    public TextArea getTextArea()
    {
        return textArea;
    }

    /**
     * Set focus on the editor.
     * 
     * @param focused {@code true} to set the focus on the editor, {@code false} blur the editor
     */
    public void setFocus(boolean focused)
    {
        textArea.setFocus(focused);
    }

    /**
     * Puts the editor in loading state. While in loading state a spinner will be displayed.
     * 
     * @param loading {@code true} to enter the loading state, {@code false} to leave the loading state
     */
    public void setLoading(boolean loading)
    {
        if (loading) {
            container.addStyleName(WysiwygEditor.STYLE_NAME_LOADING);
            textArea.addStyleName(WysiwygEditor.STYLE_NAME_INVISIBLE);
        } else {
            container.removeStyleName(WysiwygEditor.STYLE_NAME_LOADING);
            textArea.removeStyleName(WysiwygEditor.STYLE_NAME_INVISIBLE);
        }
    }
}

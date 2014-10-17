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

package org.xwiki.gwt.wysiwyg.client;

import org.xwiki.gwt.dom.client.Element;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;

/**
 * Source editor.
 * 
 * @version $Id$
 */
public class PlainTextEditor extends Composite implements BlurHandler, ClosingHandler
{
    /**
     * The CSS class name used to make an element invisible.
     */
    private static final String STYLE_NAME_INVISIBLE = "invisible";

    /**
     * The CSS class name used to display a spinner in the middle of an element.
     */
    private static final String STYLE_NAME_LOADING = "loading";

    /**
     * Container panel.
     */
    private final FlowPanel container;

    /**
     * The plain text area.
     */
    private final TextArea textArea;

    /**
     * The element replaced by the newly created plain text editor.
     */
    private final Element hook;

    /**
     * Flag indicating if this editor is in loading state.
     */
    private boolean loading;

    /**
     * Creates a new plain text editor that wraps the given text area element.
     * 
     * @param hook the element replaced by the newly created plain text editor
     */
    public PlainTextEditor(Element hook)
    {
        this.hook = hook;

        textArea = new TextArea();
        textArea.setEnabled(!hook.getPropertyBoolean("disabled"));
        textArea.setReadOnly(hook.getPropertyBoolean("readOnly"));
        textArea.setStyleName("xPlainTextEditor");
        textArea.setHeight(Math.max(hook.getOffsetHeight(), 100) + "px");
        textArea.addBlurHandler(this);

        Window.addWindowClosingHandler(this);

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
        if (loading != this.loading) {
            this.loading = loading;
            if (loading) {
                container.addStyleName(STYLE_NAME_LOADING);
                textArea.addStyleName(STYLE_NAME_INVISIBLE);
            } else {
                container.removeStyleName(STYLE_NAME_LOADING);
                textArea.removeStyleName(STYLE_NAME_INVISIBLE);
            }
        }
    }

    /**
     * @return {@code true} if this plain text editor is currently in loading state, {@code false} otherwise
     */
    public boolean isLoading()
    {
        return loading;
    }

    /**
     * {@inheritDoc}
     * 
     * @see BlurHandler#onBlur(BlurEvent)
     */
    public void onBlur(BlurEvent event)
    {
        // Update the value of the hook element in case it is submitted.
        submit();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClosingHandler#onWindowClosing(ClosingEvent)
     */
    public void onWindowClosing(ClosingEvent event)
    {
        // Allow the browser to cache the plain text when the user navigates away from the current page.
        submit();
    }

    /**
     * Stores the value of the plain text area in the hook's value property.
     */
    public void submit()
    {
        if (textArea.isAttached() && textArea.isEnabled()) {
            hook.setPropertyString("value", textArea.getText());
        }
    }
}

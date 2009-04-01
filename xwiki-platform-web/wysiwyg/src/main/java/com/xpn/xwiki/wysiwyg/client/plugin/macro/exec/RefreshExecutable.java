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
package com.xpn.xwiki.wysiwyg.client.plugin.macro.exec;

import org.xwiki.gwt.dom.client.Style;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.AbstractExecutable;

/**
 * Refreshes all the macros present on the edited document.
 * 
 * @version $Id$
 */
public class RefreshExecutable extends AbstractExecutable
{
    /**
     * The syntax used for storing the edited document.
     */
    private final String syntax;

    /**
     * Used to prevent typing in the rich text area while waiting for the updated content from the server.
     */
    private final FocusPanel waiting;

    /**
     * Creates a new refresh executable.
     * 
     * @param syntax the syntax used for storing the edited document
     */
    public RefreshExecutable(String syntax)
    {
        this.syntax = syntax;

        waiting = new FocusPanel();
        waiting.addStyleName("loading");
        waiting.getElement().getStyle().setProperty(Style.POSITION, Style.Position.ABSOLUTE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#execute(RichTextArea, String)
     */
    public boolean execute(final RichTextArea rta, String param)
    {
        // Check if there is a refresh in progress.
        // NOTE: We don't test the parent but the next sibling because in IE the parent of an orphan node is sometimes a
        // document fragment, thus not null.
        if (waiting.getElement().getNextSibling() != null) {
            return false;
        }

        // Prevent typing while waiting for the updated content.
        waiting.getElement().getStyle().setPropertyPx(Style.WIDTH, rta.getOffsetWidth());
        waiting.getElement().getStyle().setPropertyPx(Style.HEIGHT, rta.getOffsetHeight());
        waiting.getElement().getStyle().setPropertyPx(Style.LEFT, rta.getElement().getOffsetLeft());
        waiting.getElement().getStyle().setPropertyPx(Style.TOP, rta.getElement().getOffsetTop());
        rta.getElement().getParentNode().insertBefore(waiting.getElement(), rta.getElement());

        // We have to blur the rich text area first in order to trigger some client-side DOM clean-up. In consequence,
        // we wait to be notified of the blur event before refreshing the content of the rich text area.
        rta.addFocusListener(new FocusListener()
        {
            public void onFocus(Widget sender)
            {
                // ignore
            }

            public void onLostFocus(Widget sender)
            {
                rta.removeFocusListener(this);
                refresh(rta);
            }
        });
        waiting.setFocus(true);

        return true;
    }

    /**
     * Sends a request to the server to parse and re-render the current content of the given rich text area.
     * 
     * @param rta the rich text area whose content will be refreshed
     */
    private void refresh(final RichTextArea rta)
    {
        WysiwygService.Singleton.getInstance().parseAndRender(rta.getHTML(), syntax, new AsyncCallback<String>()
        {
            public void onFailure(Throwable caught)
            {
                waiting.getElement().getParentNode().removeChild(waiting.getElement());
            }

            public void onSuccess(String result)
            {
                rta.setHTML(result);
                waiting.getElement().getParentNode().removeChild(waiting.getElement());
            }
        });
    }
}

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

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Style;
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
    private final Element waiting;

    /**
     * Creates a new refresh executable.
     * 
     * @param syntax the syntax used for storing the edited document
     */
    public RefreshExecutable(String syntax)
    {
        this.syntax = syntax;

        waiting = (Element) Document.get().createDivElement().cast();
        waiting.getStyle().setProperty(Style.POSITION, Style.Position.ABSOLUTE);
        waiting.setClassName("loading");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#execute(RichTextArea, String)
     */
    public boolean execute(final RichTextArea rta, String param)
    {
        // Check if there is a refresh in progress.
        if (waiting.getParentNode() != null) {
            return false;
        }

        // We have to blur the rich text area first in order to trigger some client-side DOM clean-up.
        rta.setFocus(false);

        // Prevent typing while waiting for the updated content.
        waiting.getStyle().setPropertyPx(Style.WIDTH, rta.getOffsetWidth());
        waiting.getStyle().setPropertyPx(Style.HEIGHT, rta.getOffsetHeight());
        waiting.getStyle().setPropertyPx(Style.LEFT, rta.getElement().getOffsetLeft());
        waiting.getStyle().setPropertyPx(Style.TOP, rta.getElement().getOffsetTop());
        rta.getElement().getParentNode().insertBefore(waiting, rta.getElement());

        // Request the updated content.
        WysiwygService.Singleton.getInstance().parseAndRender(rta.getHTML(), syntax, new AsyncCallback<String>()
        {
            public void onFailure(Throwable caught)
            {
                waiting.getParentNode().removeChild(waiting);
            }

            public void onSuccess(String result)
            {
                rta.setHTML(result);
                waiting.getParentNode().removeChild(waiting);
            }
        });

        // Give the focus back to the rich text area.
        rta.setFocus(true);
        return true;
    }
}

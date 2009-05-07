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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.util.Timer;
import com.xpn.xwiki.wysiwyg.client.util.TimerListener;

/**
 * Debugger for the {@link WysiwygEditor}. It displays the current HTML content of the debugged rich text area, the
 * cleaned HTML, the corresponding wiki syntax as well as the rendering events triggered during the conversion to wiki
 * syntax.
 * 
 * @version $Id$
 */
public class WysiwygEditorDebugger extends Composite implements TimerListener
{
    /**
     * The editor being debugged.
     */
    private WysiwygEditor editor;

    /**
     * Displays the corresponding wiki syntax.
     */
    private TextArea wikiTextArea;

    /**
     * Displays the current HTML content.
     */
    private TextArea dirtyHTMLTextArea;

    /**
     * Displays the corresponding XHTML content.
     */
    private TextArea cleanHTMLTextArea;

    /**
     * Displays the rendering events triggered during the conversion to wiki syntax.
     */
    private TextArea eventsTextArea;

    /**
     * Timer used to schedule periodically updates.
     */
    private Timer timer;

    /**
     * The previous HTML content of the rich text area. We store it to detect when the content of the rich text area has
     * change in order to refresh the debug text areas.
     */
    private String previousHTML = "";

    /**
     * Creates a new debugger for the given editor.
     * 
     * @param editor The WYSIWYG editor being debugged.
     */
    public WysiwygEditorDebugger(WysiwygEditor editor)
    {
        this.editor = editor;

        FlowPanel panel = new FlowPanel();
        panel.setWidth("100%");
        panel.add(editor.getRichTextEditor());

        String width = "400px";
        String height = "220px";

        dirtyHTMLTextArea = new TextArea();
        dirtyHTMLTextArea.setWidth(width);
        dirtyHTMLTextArea.setHeight(height);
        panel.add(dirtyHTMLTextArea);

        cleanHTMLTextArea = new TextArea();
        cleanHTMLTextArea.setWidth(width);
        cleanHTMLTextArea.setHeight(height);
        panel.add(cleanHTMLTextArea);

        wikiTextArea = new TextArea();
        wikiTextArea.setWidth(width);
        wikiTextArea.setHeight(height);
        panel.add(wikiTextArea);

        eventsTextArea = new TextArea();
        eventsTextArea.setWidth(width);
        eventsTextArea.setHeight(height);
        panel.add(eventsTextArea);

        // get the transformed HTML Content
        dirtyHTMLTextArea.setText(editor.getRichTextEditor().getTextArea().getHTML());

        initWidget(panel);

        timer = new Timer();
        timer.addTimerListener(this);
        timer.scheduleRepeating(4000);
    }

    /**
     * Refreshes the debug text areas if the text in the rich text area has been changed.
     */
    public void refreshData()
    {
        String currentHTML = editor.getRichTextEditor().getTextArea().getHTML();
        if (previousHTML.equals(currentHTML)) {
            return;
        }
        previousHTML = currentHTML;

        dirtyHTMLTextArea.setText(currentHTML);

        WysiwygService.Singleton.getInstance().cleanHTML(currentHTML, new AsyncCallback<String>()
        {
            public void onFailure(Throwable caught)
            {
                cleanHTMLTextArea.setText(caught.toString());
            }

            public void onSuccess(String result)
            {
                cleanHTMLTextArea.setText(result);
            }
        });

        String syntax = editor.getConfig().getParameter("syntax");
        WysiwygService.Singleton.getInstance().fromHTML(currentHTML, syntax, new AsyncCallback<String>()
        {
            public void onFailure(Throwable caught)
            {
                wikiTextArea.setText(caught.toString());
            }

            public void onSuccess(String result)
            {
                wikiTextArea.setText(result);
            }
        });

        WysiwygService.Singleton.getInstance().fromHTML(currentHTML, "events", new AsyncCallback<String>()
        {
            public void onFailure(Throwable caught)
            {
                eventsTextArea.setText(caught.toString());
            }

            public void onSuccess(String result)
            {
                eventsTextArea.setText(result);
            }
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @see TimerListener#onElapsed(Timer)
     */
    public void onElapsed(Timer sender)
    {
        if (sender == timer) {
            refreshData();
        }
    }
}

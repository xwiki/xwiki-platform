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

public class WysiwygEditorDebugger extends Composite implements TimerListener
{
    private WysiwygEditor editor;

    private TextArea wikiTextArea;

    private TextArea dirtyHTMLTextArea;

    private TextArea cleanHTMLTextArea;

    private TextArea eventsTextArea;

    private Timer timer;

    private String previousHTML = "";

    public WysiwygEditorDebugger(WysiwygEditor editor)
    {
        this.editor = editor;

        FlowPanel panel = new FlowPanel();
        panel.setWidth("100%");
        panel.add(editor.getUI());

        dirtyHTMLTextArea = new TextArea();
        dirtyHTMLTextArea.setWidth("400px");
        dirtyHTMLTextArea.setHeight("220px");
        panel.add(dirtyHTMLTextArea);

        cleanHTMLTextArea = new TextArea();
        cleanHTMLTextArea.setWidth("400px");
        cleanHTMLTextArea.setHeight("220px");
        panel.add(cleanHTMLTextArea);

        wikiTextArea = new TextArea();
        wikiTextArea.setWidth("400px");
        wikiTextArea.setHeight("220px");
        panel.add(wikiTextArea);

        eventsTextArea = new TextArea();
        eventsTextArea.setWidth("400px");
        eventsTextArea.setHeight("220px");
        panel.add(eventsTextArea);

        // get the transformed HTML Content
        dirtyHTMLTextArea.setText(editor.getUI().getTextArea().getHTML());

        initWidget(panel);

        timer = new Timer();
        timer.addTimerListener(this);
        timer.scheduleRepeating(4000);
    }

    public void refreshData()
    {
        String currentHTML = editor.getUI().getTextArea().getHTML();
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

        WysiwygService.Singleton.getInstance().fromHTML(currentHTML, editor.getSyntax(), new AsyncCallback<String>()
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

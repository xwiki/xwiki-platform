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
package com.xpn.xwiki.wysiwyg.client.widget.rta;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Event;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.IFrameElement;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandManager;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.DefaultCommandManager;
import com.xpn.xwiki.wysiwyg.client.widget.rta.internal.BehaviorAdjuster;

/**
 * Extends the rich text area provided by GWT to add support for advanced editing.
 * 
 * @version $Id$
 */
public class RichTextArea extends com.google.gwt.user.client.ui.RichTextArea
{
    /**
     * @see #setHTML(String)
     */
    public static final String DIRTY = "dirty";

    /**
     * The command manager that executes commands on this rich text area.
     */
    private final CommandManager cm;

    /**
     * Overwrites the default behavior of the rich text area when DOM events are triggered by user actions and that
     * default behavior is either incomplete, unnatural, browser specific or buggy. This custom behavior can still be
     * prevented from a listener by calling {@link Event#preventDefault()} on the {@link #getCurrentEvent()}.
     */
    private final BehaviorAdjuster adjuster = (BehaviorAdjuster) GWT.create(BehaviorAdjuster.class);

    /**
     * The current event triggered on this rich text area. We need to store it because DOM.eventGetCurrentEvent() and
     * Event.getCurrentEvent() return null for RichTextArea events.
     * 
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3133
     */
    private Event currentEvent;

    /**
     * Creates a new rich text area.
     */
    public RichTextArea()
    {
        cm = new DefaultCommandManager(this);
        adjuster.setTextArea(this);
    }

    /**
     * Custom constructor allowing us to inject a mock command manager and a mock history. It was mainly added to be
     * used in unit tests.
     * 
     * @param cm Custom command manager
     */
    public RichTextArea(CommandManager cm)
    {
        this.cm = cm;
        adjuster.setTextArea(this);
    }

    /**
     * NOTE: If the current browser doesn't support rich text editing this method returns <code>null</code>. You should
     * test the returned value and fail save to an appropriate behavior!<br/>
     * The appropriate test would be: <code><pre>
     * if (rta.isAttached() && rta.getDocument() == null) {
     *   // The current browser doesn't support rich text editing.
     * }
     * </pre></code>
     * 
     * @return The DOM document being edited with this rich text area.
     */
    public Document getDocument()
    {
        if (getElement().getTagName().equalsIgnoreCase("iframe")) {
            return IFrameElement.as(getElement()).getContentDocument().cast();
        } else {
            return null;
        }
    }

    /**
     * @return the {@link CommandManager} associated with this instance.
     */
    public CommandManager getCommandManager()
    {
        return cm;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.RichTextArea#setHTML(String)
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3147
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3156
     */
    public void setHTML(String html)
    {
        // We add a dirty attribute and set its value to true in order to be able to overcome the Issue 3156. Precisely,
        // we test this attribute in the setHTMLImpl to avoid overwriting the contents when setHTML haven't been called.
        getElement().setAttribute(DIRTY, String.valueOf(true));
        super.setHTML(html);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.RichTextArea#onBrowserEvent(com.google.gwt.user.client.Event)
     */
    public void onBrowserEvent(com.google.gwt.user.client.Event event)
    {
        // We need to preview the event due to a GWT bug.
        // @see http://code.google.com/p/google-web-toolkit/issues/detail?id=729
        if (!previewEvent(event)) {
            return;
        }
        currentEvent = event.cast();
        adjuster.onBeforeBrowserEvent();
        super.onBrowserEvent(event);
        adjuster.onBrowserEvent();
        currentEvent = null;
    }

    /**
     * We need to call DOM.previewEvent because there is a bug in GWT that prevents PopupPanel from previewing events
     * generated in in-line frames like the one in behind of this rich text area.
     * 
     * @param event a handle to the event being previewed.
     * @return <code>false</code> to cancel the event.
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=729
     */
    private native boolean previewEvent(com.google.gwt.user.client.Event event)
    /*-{
        return @com.google.gwt.user.client.DOM::previewEvent(Lcom/google/gwt/user/client/Event;)(event);
    }-*/;

    /**
     * @return the current event triggered on this rich text area.
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3133
     */
    public Event getCurrentEvent()
    {
        return currentEvent;
    }
}

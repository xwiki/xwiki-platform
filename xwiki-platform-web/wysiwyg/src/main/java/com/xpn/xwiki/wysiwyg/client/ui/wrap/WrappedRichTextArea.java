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
package com.xpn.xwiki.wysiwyg.client.ui.wrap;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.ui.XShortcutKey;
import com.xpn.xwiki.wysiwyg.client.ui.XShortcutKeyFactory;

public class WrappedRichTextArea extends RichTextArea
{
    public static final String STYLESHEET;

    static {
        String baseURL = GWT.getModuleBaseURL();
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }
        STYLESHEET = baseURL + "RichTextArea.css";
    }

    private List<XShortcutKey> shortcutKeys = new ArrayList<XShortcutKey>();

    public void addShortcutKey(XShortcutKey shortcutKey)
    {
        if (!shortcutKeys.contains(shortcutKey)) {
            shortcutKeys.add(shortcutKey);
        }
    }

    public void removeShortcutKey(XShortcutKey shortcutKey)
    {
        shortcutKeys.remove(shortcutKey);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.RichTextArea#onBrowserEvent(Event)
     */
    public void onBrowserEvent(Event event)
    {
        if (DOM.eventGetType(event) == Event.ONKEYDOWN
            && shortcutKeys.contains(XShortcutKeyFactory.createShortcutKey(event))) {
            DOM.eventPreventDefault(event);
        }
        super.onBrowserEvent(event);
    }
}

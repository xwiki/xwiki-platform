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
package org.xwiki.gwt.user.client;

import org.xwiki.gwt.dom.client.Document;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasClickHandlers;

/**
 * A command that simulates a click when executed. Click commands can be associated with shortcut keys.
 * 
 * @version $Id$
 */
public final class ClickCommand implements com.google.gwt.user.client.Command
{
    /**
     * The object sending the click events.
     */
    private final HasClickHandlers clickable;

    /**
     * The native event used to simulate a click on {@link #clickable}.
     */
    private final NativeEvent nativeClickEvent;

    /**
     * Creates a new click command that will simulate a click on the specified object when executed.
     * 
     * @param clickable an object that can be clicked
     */
    public ClickCommand(HasClickHandlers clickable)
    {
        this.clickable = clickable;
        nativeClickEvent = Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, false, false);
    }

    @Override
    public void execute()
    {
        try {
            DomEvent.fireNativeEvent(nativeClickEvent, clickable);
        } catch (Exception e) {
            Console.getInstance().error("Click command failed!", e);
        }
    }
}

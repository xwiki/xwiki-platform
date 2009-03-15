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
package com.xpn.xwiki.wysiwyg.client.util;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * A command that simulates a click when executed. Click commands can be associated with shortcut keys.
 * 
 * @version $Id$
 */
public final class ClickCommand implements com.google.gwt.user.client.Command
{
    /**
     * The object listening to click events.
     */
    private final ClickListener listener;

    /**
     * The object sending the click events.
     */
    private final Widget sender;

    /**
     * Creates a new click command that will simulate a click on the specified widget for the given listener, when
     * executed.
     * 
     * @param listener the object listening to click events
     * @param sender the object sending the click events
     */
    public ClickCommand(ClickListener listener, Widget sender)
    {
        this.listener = listener;
        this.sender = sender;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.Command#execute()
     */
    public void execute()
    {
        try {
            listener.onClick(sender);
        } catch (Throwable t) {
            Console.getInstance().error(t, ClickCommand.class.getName(), listener.getClass().getName());
        }
    }
}

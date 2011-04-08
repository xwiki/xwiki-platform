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
package org.xwiki.gwt.wysiwyg.client.plugin.list.internal;

import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.wysiwyg.client.plugin.list.ListBehaviorAdjuster;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.user.client.ui.Widget;

/**
 * Internet Explorer implementation of the {@link ListBehaviorAdjuster}.
 * 
 * @version $Id$
 */
public class IEListBehaviorAdjuster extends ListBehaviorAdjuster
{
    /**
     * {@inheritDoc}. Internet explorer receives the special keys on key down, not on key pressed, so we'll override the
     * key press handling and move the handling in this function.
     * 
     * @see ListBehaviorAdjuster#onKeyDown(KeyDownEvent)
     */
    public void onKeyDown(KeyDownEvent event)
    {
        dispatchKey((Widget) event.getSource(), event.getNativeKeyCode(), (Event) event.getNativeEvent());
    }

    /**
     * {@inheritDoc}
     * 
     * @see ListBehaviorAdjuster#onKeyPress(KeyPressEvent)
     */
    public void onKeyPress(KeyPressEvent event)
    {
        // Prevent the behavior from the base class.
    }
}

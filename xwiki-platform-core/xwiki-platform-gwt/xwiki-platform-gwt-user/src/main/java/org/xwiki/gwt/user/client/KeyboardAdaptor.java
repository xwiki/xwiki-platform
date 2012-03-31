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

import org.xwiki.gwt.dom.client.Event;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;

/**
 * Cross-browser keyboard handling.
 * 
 * @version $Id$
 */
public class KeyboardAdaptor implements KeyDownHandler, KeyPressHandler, KeyUpHandler
{
    /**
     * Flag used to avoid handling both KeyDown and KeyPress events. This flag is needed because of the inconsistencies
     * between browsers regarding keyboard events. For instance IE doesn't generate the KeyPress event for backspace key
     * and generates multiple KeyDown events while a key is hold down. On the contrary, FF generates the KeyPress event
     * for the backspace key and generates just one KeyDown event while a key is hold down. FF generates multiple
     * KeyPress events when a key is hold down.
     */
    private boolean ignoreNextKeyPress;

    /**
     * Flag used to prevent the default browser behavior for the KeyPress event when the KeyDown event has been
     * canceled. This is needed only in functional tests where keyboard events (KeyDown, KeyPress, KeyUp) are triggered
     * independently and thus canceling KeyDown doesn't prevent the default KeyPress behavior. Without this flag, and
     * because we have to handle the KeyDown event besides the KeyPress in order to overcome cross-browser
     * inconsistencies, simulating keyboard typing in functional tests would trigger our custom behavior but also the
     * default browser behavior.
     */
    private boolean cancelNextKeyPress;

    @Override
    public void onKeyDown(KeyDownEvent event)
    {
        ignoreNextKeyPress = true;
        Event nativeEvent = (Event) event.getNativeEvent();
        if (!nativeEvent.isCancelled()) {
            handleRepeatableKey(nativeEvent);
        }
        cancelNextKeyPress = nativeEvent.isCancelled();
    }

    @Override
    public void onKeyPress(KeyPressEvent event)
    {
        Event nativeEvent = (Event) event.getNativeEvent();
        if (!ignoreNextKeyPress) {
            if (!nativeEvent.isCancelled()) {
                handleRepeatableKey(nativeEvent);
            }
        } else if (cancelNextKeyPress) {
            nativeEvent.xPreventDefault();
        }
        ignoreNextKeyPress = false;
        cancelNextKeyPress = false;
    }

    @Override
    public void onKeyUp(KeyUpEvent event)
    {
        ignoreNextKeyPress = false;
        cancelNextKeyPress = false;
        Event nativeEvent = (Event) event.getNativeEvent();
        if (!nativeEvent.isCancelled()) {
            handleKeyRelease(nativeEvent);
        }
    }

    /**
     * Handles a repeatable key press.
     * 
     * @param event the native event that was fired
     */
    protected void handleRepeatableKey(Event event)
    {
    }

    /**
     * Handles a key release.
     * 
     * @param event the native event that was fired
     */
    protected void handleKeyRelease(Event event)
    {
    }
}

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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.user.client.ShortcutKey.ModifierKey;

import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

/**
 * Associates {@link Command}s to shortcut keys.<br/>
 * 
 * @version $Id$
 */
public class ShortcutKeyManager extends HashMap<ShortcutKey, Command> implements KeyDownHandler, KeyPressHandler,
    KeyUpHandler
{
    /**
     * Field required by all {@link java.io.Serializable} classes.
     */
    private static final long serialVersionUID = -1888376929779432766L;

    /**
     * The last command executed. The command is reset on each KeyDown event because only then we have the right key
     * code and key modifiers. Also, KeyPress and KeyUp events shouldn't be triggered without a KeyDown event.
     */
    private Command command;

    /**
     * Flag used to avoid handling the shortcut key on both KeyDown and KeyPress events. This flag is needed because of
     * the inconsistencies between browsers regarding keyboard events. For instance IE doesn't generate the KeyPress
     * event for navigation (arrow) keys and generates multiple KeyDown events while a key is hold down. On the
     * contrary, FF generates the KeyPress event for navigation (arrow) keys and generates just one KeyDown event while
     * a key is hold down. FF generates multiple KeyPress events when a key is hold down.
     */
    private boolean ignoreNextKeyPress;

    /**
     * Adds the necessary key handlers to be able to catch shortcut keys.
     * 
     * @param source and object that fires keyboard events
     * @return a list of handler registrations that can be used to remove the added key handlers
     */
    public List<HandlerRegistration> addHandlers(HasAllKeyHandlers source)
    {
        List<HandlerRegistration> registrations = new ArrayList<HandlerRegistration>();
        registrations.add(source.addKeyDownHandler(this));
        registrations.add(source.addKeyPressHandler(this));
        registrations.add(source.addKeyUpHandler(this));
        return registrations;
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyDownHandler#onKeyDown(KeyDownEvent)
     */
    public void onKeyDown(KeyDownEvent event)
    {
        ignoreNextKeyPress = true;
        // We reset the command here because:
        // * Key modifiers are not properly detected on key press.
        // * Meta key is not detected on apple keyboards (where it is mapped to apple command key) for key up events.
        command = get(new ShortcutKey(event.getNativeKeyCode(), getModifiers(event)));
        if (command != null) {
            // Prevent default browser behavior.
            ((Event) event.getNativeEvent()).xPreventDefault();
            // Schedule command.
            DeferredCommand.addCommand(command);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyPressHandler#onKeyPress(KeyPressEvent)
     */
    public void onKeyPress(KeyPressEvent event)
    {
        if (command != null) {
            // Prevent default browser behavior (apparently it's not enough to cancel the KeyDown event).
            ((Event) event.getNativeEvent()).xPreventDefault();
            if (!ignoreNextKeyPress) {
                // Schedule command.
                DeferredCommand.addCommand(command);
            }
            ignoreNextKeyPress = false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyUpHandler#onKeyUp(KeyUpEvent)
     */
    public void onKeyUp(KeyUpEvent event)
    {
        if (command != null) {
            // Prevent default browser behavior (apparently it's not enough to cancel the KeyDown event).
            ((Event) event.getNativeEvent()).xPreventDefault();
        }
    }

    /**
     * @param event a key down event
     * @return the set of modifier keys pressed when the event was triggered
     */
    public static EnumSet<ModifierKey> getModifiers(KeyDownEvent event)
    {
        EnumSet<ModifierKey> modifiers = EnumSet.noneOf(ModifierKey.class);
        if (event.isAltKeyDown()) {
            modifiers.add(ModifierKey.ALT);
        }
        if (event.isControlKeyDown()) {
            modifiers.add(ModifierKey.CTRL);
        }
        if (event.isMetaKeyDown()) {
            modifiers.add(ModifierKey.META);
        }
        if (event.isShiftKeyDown()) {
            modifiers.add(ModifierKey.SHIFT);
        }
        return modifiers;
    }
}

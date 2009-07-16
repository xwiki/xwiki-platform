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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import org.xwiki.gwt.dom.client.Event;

import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.xpn.xwiki.wysiwyg.client.util.ShortcutKey.ModifierKey;

/**
 * Associates {@link Command}s to shortcut keys.<br/>
 * 
 * @version $Id$
 */
public class ShortcutKeyManager extends HashMap<ShortcutKey, Command> implements KeyDownHandler
{
    /**
     * Field required by all {@link java.io.Serializable} classes.
     */
    private static final long serialVersionUID = -1888376929779432766L;

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
        return registrations;
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyDownHandler#onKeyDown(KeyDownEvent)
     */
    public void onKeyDown(KeyDownEvent event)
    {
        // We listen to key down events because:
        // * It seems the key modifiers are not detected on key press.
        // * It seems that the Meta key is not detected on apple keyboards (where it is mapped to apple command key) for
        // key up events.
        Command command = get(new ShortcutKey(event.getNativeKeyCode(), getModifiers(event)));
        if (command != null) {
            // Prevent default browser behavior.
            ((Event) event.getNativeEvent()).xPreventDefault();
            // Schedule command.
            DeferredCommand.addCommand(command);
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

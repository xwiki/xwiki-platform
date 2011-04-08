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
package org.xwiki.gwt.wysiwyg.client.plugin.macro;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.dom.client.Event;

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * Listens to rich text area events and takes actions on existing macros.
 * 
 * @version $Id$
 */
public class MacroController implements DoubleClickHandler, KeyDownHandler
{
    /**
     * The object used to access the rich text area and to perform actions on existing macros.
     */
    private final MacroPlugin plugin;

    /**
     * Creates a new macro controller.
     * 
     * @param plugin the object used to access the rich text area and to perform actions on existing macros
     */
    public MacroController(MacroPlugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Adds the necessary event handlers.
     * 
     * @return a list of handler registrations that can be used to remove the added event handlers
     */
    List<HandlerRegistration> addHadlers()
    {
        List<HandlerRegistration> registrations = new ArrayList<HandlerRegistration>();
        registrations.add(plugin.getTextArea().addDoubleClickHandler(this));
        registrations.add(plugin.getTextArea().addKeyDownHandler(this));
        return registrations;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DoubleClickHandler#onDoubleClick(DoubleClickEvent)
     */
    public void onDoubleClick(DoubleClickEvent event)
    {
        if (event.getSource() == plugin.getTextArea() && plugin.getSelector().getMacroCount() == 1) {
            plugin.edit();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyDownHandler#onKeyDown(KeyDownEvent)
     */
    public void onKeyDown(KeyDownEvent event)
    {
        if (event.getSource() == plugin.getTextArea() && plugin.getSelector().getMacroCount() == 1) {
            switch (event.getNativeKeyCode()) {
                case KeyCodes.KEY_ENTER:
                    plugin.edit();
                    ((Event) event.getNativeEvent()).xPreventDefault();
                    break;
                // Space
                case 32:
                    // Toggle between collapsed and expanded state.
                    boolean expanded = plugin.getTextArea().getCommandManager().isExecuted(MacroPlugin.EXPAND);
                    plugin.getTextArea().getCommandManager().execute(
                        expanded ? MacroPlugin.COLLAPSE : MacroPlugin.EXPAND);
                    ((Event) event.getNativeEvent()).xPreventDefault();
                    break;
                default:
                    break;
            }
        }
    }
}

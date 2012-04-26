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
package org.xwiki.gwt.wysiwyg.client.plugin.internal;

import org.xwiki.gwt.user.client.DeferredUpdater;
import org.xwiki.gwt.user.client.Updatable;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;

import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;

/**
 * An abstract kind of plug-in that listens to the changes in the state of the editor's text area.
 * 
 * @version $Id$
 */
public abstract class AbstractStatefulPlugin extends AbstractPlugin implements Updatable, MouseUpHandler, KeyUpHandler,
    CommandListener
{
    /**
     * Schedules updates and executes only the most recent one.
     */
    private final DeferredUpdater updater = new DeferredUpdater(this);

    @Override
    public void onMouseUp(MouseUpEvent event)
    {
        // We listen to mouse up events instead of clicks because if the user selects text and the end points of the
        // selection are in different DOM nodes the click events are not triggered.
        if (event.getSource() == getTextArea()) {
            updater.deferUpdate();
        }
    }

    @Override
    public void onKeyUp(KeyUpEvent event)
    {
        if (event.getSource() == getTextArea()) {
            updater.deferUpdate();
        }
    }

    @Override
    public boolean onBeforeCommand(CommandManager sender, Command command, String param)
    {
        // ignore
        return false;
    }

    @Override
    public void onCommand(CommandManager sender, Command command, String param)
    {
        if (sender == getTextArea().getCommandManager()) {
            updater.deferUpdate();
        }
    }

    @Override
    public boolean canUpdate()
    {
        // Always return true. Derived classes should check if their UI is enabled before updating it.
        return true;
    }

    /**
     * Registers the rich text area handlers required to update the state of the plug-in.
     */
    protected void registerTextAreaHandlers()
    {
        saveRegistration(getTextArea().addMouseUpHandler(this));
        saveRegistration(getTextArea().addKeyUpHandler(this));
        getTextArea().getCommandManager().addCommandListener(this);
    }

    @Override
    public void destroy()
    {
        // This should fail silently if we weren't listening to command events.
        getTextArea().getCommandManager().removeCommandListener(this);

        super.destroy();
    }
}

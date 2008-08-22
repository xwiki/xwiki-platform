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
package com.xpn.xwiki.wysiwyg.client.ui.cmd.internal;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FocusWidget;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.CommandListener;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.CommandListenerCollection;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.CommandManager;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.SourcesCommandEvents;

public abstract class AbstractCommandManager implements CommandManager
{
    protected FocusWidget widget;

    protected ParamFilter filter;

    private final CommandListenerCollection commandListeners;

    public AbstractCommandManager(FocusWidget widget)
    {
        this.widget = widget;
        filter = (ParamFilter) GWT.create(ParamFilter.class);
        commandListeners = new CommandListenerCollection();
    }

    protected abstract boolean execCommandAssumingFocus(String cmd, String param);

    protected abstract boolean queryCommandEnabledAssumingFocus(String cmd);

    protected abstract boolean queryCommandIndetermAssumingFocus(String cmd);

    protected abstract boolean queryCommandStateAssumingFocus(String cmd);

    protected abstract boolean queryCommandSupportedAssumingFocus(String cmd);

    protected abstract String queryCommandValueAssumingFocus(String cmd);

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#execCommand(Command, String)
     */
    public boolean execCommand(Command cmd, String param)
    {
        widget.setFocus(true);
        boolean success = execCommandAssumingFocus(cmd.toString(), filter.encode(cmd, param));
        if (success) {
            commandListeners.fireCommand(this, cmd, param);
        }
        return success;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#execCommand(Command, int)
     */
    public boolean execCommand(Command cmd, int param)
    {
        return execCommand(cmd, String.valueOf(param));
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#execCommand(Command, boolean)
     */
    public boolean execCommand(Command cmd, boolean param)
    {
        return execCommand(cmd, String.valueOf(param));
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#execCommand(Command)
     */
    public boolean execCommand(Command cmd)
    {
        return execCommand(cmd, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#queryCommandEnabled(Command)
     */
    public boolean queryCommandEnabled(Command cmd)
    {
        widget.setFocus(true);
        return queryCommandEnabledAssumingFocus(cmd.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#queryCommandIndeterm(Command)
     */
    public boolean queryCommandIndeterm(Command cmd)
    {
        widget.setFocus(true);
        return queryCommandIndetermAssumingFocus(cmd.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#queryCommandState(Command)
     */
    public boolean queryCommandState(Command cmd)
    {
        widget.setFocus(true);
        return queryCommandStateAssumingFocus(cmd.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#queryCommandSupported(Command)
     */
    public boolean queryCommandSupported(Command cmd)
    {
        widget.setFocus(true);
        return queryCommandSupportedAssumingFocus(cmd.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#queryCommandValue(Command)
     */
    public String queryCommandStringValue(Command cmd)
    {
        widget.setFocus(true);
        return filter.decode(cmd, queryCommandValueAssumingFocus(cmd.toString()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#queryCommandIntValue(Command)
     */
    public Integer queryCommandIntegerValue(Command cmd)
    {
        String sValue = queryCommandStringValue(cmd);
        if (sValue != null) {
            try {
                return Integer.valueOf(sValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#queryCommandBooleanValue(Command)
     */
    public Boolean queryCommandBooleanValue(Command cmd)
    {
        String sValue = queryCommandStringValue(cmd);
        if (sValue != null) {
            return Boolean.valueOf(sValue);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesCommandEvents#addCommandListener(CommandListener)
     */
    public void addCommandListener(CommandListener listener)
    {
        commandListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesCommandEvents#removeCommandListener(CommandListener)
     */
    public void removeCommandListener(CommandListener listener)
    {
        commandListeners.remove(listener);
    }
}

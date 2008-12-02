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
package com.xpn.xwiki.wysiwyg.client.plugin.table.feature;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PushButton;
import com.xpn.xwiki.wysiwyg.client.plugin.table.TableFeature;
import com.xpn.xwiki.wysiwyg.client.plugin.table.TablePlugin;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.AbstractExecutable;

/**
 * This class partly implements the Executable and TableFeature interfaces. Table features must extend this class.
 * 
 * @version $Id$
 */
public abstract class AbstractTableFeature extends AbstractExecutable implements TableFeature
{
    /**
     * Feature name (examples: inserttable, insertrowbefore).
     */
    private final String name;

    /**
     * Feature toolbar push-button.
     */
    private final PushButton button;

    /**
     * Feature command.
     */
    private final Command command;

    /**
     * Table plug-in.
     */
    private final TablePlugin plugin;

    /**
     * Default constructor.
     * 
     * @param name feature name.
     * @param command feature command.
     * @param button feature button.
     * @param title feature button title.
     * @param plugin table plug-in.
     */
    public AbstractTableFeature(String name, Command command, PushButton button, String title, TablePlugin plugin)
    {
        this.name = name;
        this.command = command;
        this.button = button;
        button.setTitle(title);
        this.plugin = plugin;
    }

    /**
     * {@inheritDoc}
     * 
     * @see TableFeature#getName()
     */
    public String getName()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see TableFeature#getButton()
     */
    public PushButton getButton()
    {
        return button;
    }

    /**
     * {@inheritDoc}
     * 
     * @see TableFeature#getCommand()
     */
    public Command getCommand()
    {
        return command;
    }

    /**
     * {@inheritDoc}
     * 
     * @see TableFeature#getPlugin()
     */
    public TablePlugin getPlugin()
    {
        return plugin;
    }

    /**
     * {@inheritDoc}
     * 
     * @see TableFeature#destroy()
     */
    public void destroy()
    {
        getButton().removeFromParent();
        getButton().removeClickListener((ClickListener) getPlugin());
    }
}

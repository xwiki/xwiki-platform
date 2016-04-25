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
package org.xwiki.gwt.wysiwyg.client.plugin.table.feature;

import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractSelectionExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.table.TableFeature;
import org.xwiki.gwt.wysiwyg.client.plugin.table.TablePlugin;


/**
 * This class partly implements the Executable and TableFeature interfaces. Table features must extend this class.
 * 
 * @version $Id$
 */
public abstract class AbstractTableFeature extends AbstractSelectionExecutable implements TableFeature
{
    /**
     * Feature name (examples: inserttable, insertrowbefore).
     */
    private final String name;

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
     * @param title feature button title.
     * @param plugin table plug-in.
     */
    public AbstractTableFeature(String name, Command command, String title, TablePlugin plugin)
    {
        super(plugin.getTextArea());
        this.name = name;
        this.command = command;
        this.plugin = plugin;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Command getCommand()
    {
        return command;
    }

    @Override
    public TablePlugin getPlugin()
    {
        return plugin;
    }

    @Override
    public void destroy()
    {
    }
}

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

import com.google.gwt.user.client.ui.PushButton;
import com.xpn.xwiki.wysiwyg.client.plugin.table.TableFeature;
import com.xpn.xwiki.wysiwyg.client.plugin.table.util.TableUtils;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * This class partly implements the Executable and TableFeature interfaces. Table features must extend this class.
 * 
 * @version $Id$
 */
public abstract class AbstractTableFeature implements TableFeature
{
    /**
     * TableUtils instance.
     */
    protected TableUtils utils = TableUtils.getInstance();

    /**
     * Feature name (examples: inserttable, insertrowbefore).
     */
    protected String name;

    /**
     * Feature toolbar push-button.
     */
    protected PushButton button;

    /**
     * Feature command.
     */
    protected Command command;

    /**
     * Get feature name.
     * 
     * @return feature name (examples: inserttable, insertrowbefore).
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get feature button.
     * 
     * @return feature button.
     */
    public PushButton getButton()
    {
        return button;
    }

    /**
     * Get feature command.
     * 
     * @return feature command.
     */
    public Command getCommand()
    {
        return command;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#getParameter(RichTextArea)
     */
    public String getParameter(RichTextArea rta)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isExecuted(RichTextArea)
     */
    public boolean isExecuted(RichTextArea rta)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isSupported(RichTextArea)
     */
    public boolean isSupported(RichTextArea rta)
    {
        return true;
    }
}

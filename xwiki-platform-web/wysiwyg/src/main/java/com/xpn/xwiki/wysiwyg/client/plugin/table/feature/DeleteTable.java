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

import org.xwiki.gwt.user.client.ui.rta.cmd.Command;

import com.google.gwt.dom.client.TableElement;
import com.xpn.xwiki.wysiwyg.client.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.table.TablePlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.table.util.TableUtils;

/**
 * Feature allowing to remove a table from the editor. It is disabled when the caret is positioned outside of a table
 * element.
 * 
 * @version $Id$
 */
public class DeleteTable extends AbstractTableFeature
{
    /**
     * Feature name.
     */
    public static final String NAME = "deletetable";

    /**
     * Initialize the feature. Table features needs to be aware of the plug-in (here the ClickListener) since they hold
     * their own PushButton.
     * 
     * @param plugin table plug-in.
     */
    public DeleteTable(TablePlugin plugin)
    {
        super(NAME, new Command(NAME), Strings.INSTANCE.deleteTable(), plugin);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractTableFeature#execute(String)
     */
    public boolean execute(String parameter)
    {
        TableElement table =
            TableUtils.getInstance().getTable(TableUtils.getInstance().getCaretNode(rta.getDocument()));
        table.getParentNode().removeChild(table);
        // FIXME : the table editor rulers are still visible, find a way to clean midas state after deletion.
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractTableFeature#isEnabled()
     */
    public boolean isEnabled()
    {
        return super.isEnabled()
            && TableUtils.getInstance().getTable(TableUtils.getInstance().getCaretNode(rta.getDocument())) != null;
    }
}

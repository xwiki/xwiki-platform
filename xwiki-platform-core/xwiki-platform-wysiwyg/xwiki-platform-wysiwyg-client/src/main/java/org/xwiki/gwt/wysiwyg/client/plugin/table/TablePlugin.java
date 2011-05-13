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
package org.xwiki.gwt.wysiwyg.client.plugin.table;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.DeleteCol;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.DeleteRow;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.DeleteTable;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.InsertColAfter;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.InsertColBefore;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.InsertRowAfter;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.InsertRowBefore;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.InsertTable;
import org.xwiki.gwt.wysiwyg.client.plugin.table.ui.TableMenuExtension;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * Plug-in allowing to manipulate tables in the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class TablePlugin extends AbstractPlugin implements CommandListener
{
    /**
     * List of table features (example : InsertTable, DeleteCol).
     */
    private final List<TableFeature> features = new ArrayList<TableFeature>();

    /**
     * The menu extension of this plugin.
     */
    private TableMenuExtension menuExtension;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(XRichTextArea, Config)
     */
    public void init(RichTextArea rta, Config config)
    {
        super.init(rta, config);

        addFeature(rta, new InsertTable(this));
        addFeature(rta, new InsertRowBefore(this));
        addFeature(rta, new InsertRowAfter(this));
        addFeature(rta, new DeleteRow(this));
        addFeature(rta, new InsertColBefore(this));
        addFeature(rta, new InsertColAfter(this));
        addFeature(rta, new DeleteCol(this));
        addFeature(rta, new DeleteTable(this));

        menuExtension = new TableMenuExtension(this);
        getUIExtensionList().add(menuExtension);
        // Hack: We can access the menus where each menu item was placed only after the main menu bar is initialized,
        // which happens after all the plugins are loaded.
        Scheduler.get().scheduleDeferred(new ScheduledCommand()
        {
            public void execute()
            {
                menuExtension.registerAttachHandlers();
            }
        });

        // Listen to the "enable" command and disable the browser built-in table editing feature.
        rta.getCommandManager().addCommandListener(this);
        // At this point the rich text area may be already enabled so let's make sure the built-in table editing feature
        // is disabled.
        disableInlineTableEditing();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        for (TableFeature feature : features) {
            feature.destroy();
        }
        features.clear();
        menuExtension.clearFeatures();
        getTextArea().getCommandManager().removeCommandListener(this);
        super.destroy();
    }

    /**
     * Make a feature available.
     * 
     * @param rta WYSIWYG RichTextArea.
     * @param feature feature to enable.
     */
    private void addFeature(RichTextArea rta, TableFeature feature)
    {
        rta.getCommandManager().registerCommand(feature.getCommand(), feature);
        features.add(feature);
    }

    /**
     * @return The list of the features exposed by the plugin.
     */
    public List<TableFeature> getFeatures()
    {
        return features;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onBeforeCommand(CommandManager, Command, String)
     */
    public boolean onBeforeCommand(CommandManager sender, Command command, String param)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onCommand(CommandManager, Command, String)
     */
    public void onCommand(CommandManager sender, Command command, String param)
    {
        if ("enable".equalsIgnoreCase(command.toString())) {
            // Disable the built-in table editing feature because it doesn't handle the table header correctly.
            disableInlineTableEditing();
        }
    }

    /**
     * Disable the built-in table editing feature.
     */
    private void disableInlineTableEditing()
    {
        if (getTextArea().isEnabled()) {
            getTextArea().getDocument().execCommand("enableInlineTableEditing", "false");
        }
    }
}

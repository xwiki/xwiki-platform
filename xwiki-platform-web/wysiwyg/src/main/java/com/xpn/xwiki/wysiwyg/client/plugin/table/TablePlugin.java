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
package com.xpn.xwiki.wysiwyg.client.plugin.table;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.table.feature.DeleteCol;
import com.xpn.xwiki.wysiwyg.client.plugin.table.feature.DeleteRow;
import com.xpn.xwiki.wysiwyg.client.plugin.table.feature.DeleteTable;
import com.xpn.xwiki.wysiwyg.client.plugin.table.feature.InsertColAfter;
import com.xpn.xwiki.wysiwyg.client.plugin.table.feature.InsertColBefore;
import com.xpn.xwiki.wysiwyg.client.plugin.table.feature.InsertRowAfter;
import com.xpn.xwiki.wysiwyg.client.plugin.table.feature.InsertRowBefore;
import com.xpn.xwiki.wysiwyg.client.plugin.table.feature.InsertTable;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * Plug-in allowing to manipulate tables in the WYSIWYG editor. 
 * 
 * @version $Id$
 */
public class TablePlugin extends AbstractPlugin implements ClickListener
{
    /**
     * List of table features (example : InsertTable, DeleteCol).
     */
    private final List<TableFeature> features = new ArrayList<TableFeature>();

    /**
     * The plug-in toolbar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");   

    /**
     * Make a feature available.
     * 
     * @param rta WYSIWYG RichTextArea.
     * @param feature feature to enable.
     */
    private void addFeature(RichTextArea rta, TableFeature feature)
    {
        rta.getCommandManager().registerCommand(feature.getCommand(), feature);
        toolBarExtension.addFeature(feature.getName(), feature.getButton());
        features.add(feature);        
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, XRichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea rta, Config config)
    {
        super.init(wysiwyg, rta, config);

        addFeature(rta, new InsertTable(this));
        addFeature(rta, new InsertRowBefore(this));
        addFeature(rta, new InsertRowAfter(this));
        addFeature(rta, new DeleteRow(this));
        addFeature(rta, new InsertColBefore(this));
        addFeature(rta, new InsertColAfter(this));
        addFeature(rta, new DeleteCol(this));
        addFeature(rta, new DeleteTable(this));
        
        // Disable the standard table editing features of Firefox since they don't take
        // table headings (th) into account.
        rta.getDocument().execCommand("enableInlineTableEditing", "false");          

        getUIExtensionList().add(toolBarExtension);
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
            features.remove(feature);
        }
        toolBarExtension.clearFeatures();
        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        for (TableFeature feature : features) {
            if (sender == feature.getButton()) {
                // Insert table feature opens a dialog so we shouldn't focus the rich text are in this case.
                if (!(feature instanceof InsertTable)) {
                    getTextArea().setFocus(true);
                }
                getTextArea().getCommandManager().execute(feature.getCommand());
            }
        }
    }
}

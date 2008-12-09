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

import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.ui.PushButton;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.table.TablePlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.table.ui.TableConfigDialog;
import com.xpn.xwiki.wysiwyg.client.plugin.table.util.TableConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.table.util.TableUtils;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.widget.PopupListener;
import com.xpn.xwiki.wysiwyg.client.widget.SourcesPopupEvents;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.SelectionPreserver;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Feature allowing to insert a table in the editor. It is disabled when the caret is positioned in a table.
 * 
 * @version $Id$
 */
public class InsertTable extends AbstractTableFeature implements PopupListener
{
    /**
     * Feature name.
     */
    private static final String NAME = "inserttable"; 
    
    /**
     * Table dialog.
     */
    private TableConfigDialog dialog;

    /**
     * RTA Selection preserver.
     */
    private SelectionPreserver selectionPreserver;

    /**
     * Initialize the feature. Table features needs to be aware of the plug-in (here the ClickListener) since they hold
     * their own PushButton.
     * 
     * @param plugin table plug-in.
     */
    public InsertTable(TablePlugin plugin)
    {
        super(NAME, new Command(NAME), new PushButton(Images.INSTANCE.insertTable().createImage(),
            plugin), Strings.INSTANCE.insertTable(), plugin);
        selectionPreserver = new SelectionPreserver(plugin.getTextArea());
    }

    /**
     * Get table wizard pop-up.
     * 
     * @return the table wizard pop-up instance.
     */
    public TableConfigDialog getDialog()
    {
        if (dialog == null) {
            dialog = new TableConfigDialog();
            dialog.addPopupListener(this);
        }
        return dialog;
    }

    /**
     * Display the table creation wizard.
     * 
     * @param rta WYSIWYG RichTextArea
     */
    public void showDialog(RichTextArea rta)
    {
        if (getButton().isEnabled()) {
            // We save the selection because in some browsers, including Internet Explorer, by clicking on the
            // table wizard we loose the selection in the rich text area and the symbol gets inserted at the
            // beginning of the text.
            selectionPreserver.saveSelection();
            getDialog().center();
        }
    }
    
    /**
     * Create a table from TableConfig configuration.
     *
     * @param doc currently edited document.
     * @param config table configuration (row number, etc).
     * @return the newly created table.
     */
    public Element createTable(Document doc, TableConfig config) 
    {
        Element table = doc.xCreateElement(TableUtils.TABLE_NODENAME);       
        Element tbody = doc.xCreateElement(TableUtils.TBODY_NODENAME);
        Element row;
        Element cell;
                        
        table.appendChild(tbody);        

        // Set table border
        // table.setBorder(config.getBorderSize());
        
        // Create a table with rows and columns according to the configuration
        for (int i = 0; i < config.getRowNumber(); i++) {
            row = doc.xCreateElement(TableUtils.ROW_NODENAME);            
            tbody.appendChild(row);                 
            for (int j = 0; j < config.getColNumber(); j++) {
                if (i == 0 && config.hasHeader()) {
                    cell = doc.xCreateElement(TableUtils.COL_HNODENAME);
                } else {
                    cell = doc.xCreateElement(TableUtils.COL_NODENAME);                    
                }                
                cell.setInnerHTML(TableUtils.CELL_DEFAULTHTML);            
                row.appendChild(cell);                
            }
        }
        
        return table;
    }

    /**
     * Insert a HTML table in the editor.
     * 
     * @param rta WYSIWYG RichTextArea
     * @param config creation parameters.
     */
    public void insertTable(RichTextArea rta, TableConfig config)
    {
        Range range = TableUtils.getInstance().getRange(rta.getDocument());
        Selection selection = rta.getDocument().getSelection();
        Node start = range.getStartContainer();
        int offset = range.getStartOffset();
        Node flowContainer = DOMUtils.getInstance().getNearestFlowContainer(start);
        Element table = createTable(rta.getDocument(), config);
        Node nodeToSelect;

        // Leave the rest of the ranges intact.
        selection.removeAllRanges();

        // Delete the contents of the first range. The table will be inserted in place of the deleted text.
        range.deleteContents();

        // Insert the table in the DOM.
        if (flowContainer == start) {
            DOMUtils.getInstance().insertAt(flowContainer, table, offset);
        } else {
            DOMUtils.getInstance().splitNode(flowContainer, start, offset);
            DOMUtils.getInstance().insertAfter(table, DOMUtils.getInstance().getChild(flowContainer, start));
        }
        
        // Find the first table cell to put the caret within.
        if (config.hasHeader()) {            
            nodeToSelect = DOMUtils.getInstance().getFirstDescendant(table, TableUtils.COL_HNODENAME);
        } else {
            nodeToSelect = DOMUtils.getInstance().getFirstDescendant(table, TableUtils.COL_NODENAME);
        }
        
        range.selectNodeContents(nodeToSelect);
        range.collapse(false);
        selection.addRange(range);                
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String parameter)
    {
        if (StringUtils.isEmpty(parameter)) {
            // The command has been executed without insertion configuration, display the configuration dialog.
            showDialog(rta);
        } else {
            // Insert the table element.
            insertTable(rta, (TableConfig) TableConfig.fromJson(parameter));
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        return TableUtils.getInstance().getTable(TableUtils.getInstance().getCaretNode(rta.getDocument())) == null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupListener#onPopupClosed(PopupPanel, boolean)
     */
    public void onPopupClosed(SourcesPopupEvents sender, boolean autoClosed)
    {
        // We restore the selection on the text area because it may have been lost when the dialog was opened.
        selectionPreserver.restoreSelection();
        if (!autoClosed && !getDialog().isCanceled()) {
            // Call the command again, passing the insertion configuration as a JSON object.
            getPlugin().getTextArea().getCommandManager().execute(getCommand(),
                "{ rows:" + getDialog().getRowNumber()
                + ", cols: " + getDialog().getColNumber()
                + ", header: " + getDialog().hasHeader()
                + " }");
        } else {
            // We get here if the dialog has been closed by clicking the close button.
            // In this case we return the focus to the text area.
            getPlugin().getTextArea().setFocus(true);
        }
    }
}

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
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
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
     * Initialize the feature. Table features needs to be aware of the plug-in (here the ClickListener) since they hold
     * their own PushButton.
     * 
     * @param plugin table plug-in.
     */
    public InsertTable(TablePlugin plugin)
    {
        super(NAME, new Command(NAME), new PushButton(Images.INSTANCE.insertTable().createImage(), plugin),
            Strings.INSTANCE.insertTable(), plugin);
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
            getDialog().center();
        }
    }

    /**
     * Create a table from TableConfig configuration.
     * <p>
     * We create the table using innerHTML instead of creating each DOM node in order to improve the speed. In most of
     * the browsers setting the innerHTML is faster than creating the DOM nodes and appending them.
     * 
     * @param doc currently edited document.
     * @param config table configuration (row number, etc).
     * @return the newly created table.
     */
    public Element createTable(Document doc, TableConfig config)
    {
        StringBuffer table = new StringBuffer("<table>");

        StringBuffer row = new StringBuffer("<tr>");
        for (int i = 0; i < config.getColNumber(); i++) {
            row.append("<td>");
            // The default cell content depends on the browser. In Firefox the best option is to use a BR. Firefox
            // itself uses BRs in order to allow the user to place the caret inside empty block elements. In Internet
            // Explorer the best option is to set the inner HTML of each cell to the empty string, after creation. For
            // now lets keep the non-breaking space. At some point we should have browser specific implementations for
            // FF and IE. Each will overwrite this method and add specific initialization.
            row.append(TableUtils.CELL_DEFAULTHTML);
            row.append("</td>");
        }
        row.append("</tr>");

        if (config.hasHeader()) {
            table.append("<thead>");
            if (config.getRowNumber() > 0) {
                table.append(row.toString().replace("td", "th"));
            }
            table.append("</thead>");
        }

        table.append("<tbody>");
        for (int i = config.hasHeader() ? 1 : 0; i < config.getRowNumber(); i++) {
            table.append(row.toString());
        }
        table.append("</tbody></table>");

        Element container = doc.xCreateDivElement().cast();
        container.setInnerHTML(table.toString());
        Element tableElement = (Element) container.getFirstChild();
        container.removeChild(tableElement);
        return tableElement;
    }

    /**
     * Insert a HTML table in the editor.
     * 
     * @param rta WYSIWYG RichTextArea
     * @param config creation parameters.
     */
    public void insertTable(RichTextArea rta, TableConfig config)
    {
        Selection selection = rta.getDocument().getSelection();
        if (!selection.isCollapsed()) {
            // Delete the selected contents. The table will be inserted in place of the deleted text.
            // NOTE: We cannot use Range#deleteContents because it may lead to DTD-invalid HTML. That's because it
            // operates on any DOM tree without taking care of the underlying XML syntax, (X)HTML in our case. Let's use
            // the Delete command instead which is HTML-aware. Moreover, others could listen to this command and adjust
            // the DOM before we insert the table.
            rta.getCommandManager().execute(Command.DELETE);
        }

        // At this point the selection should be collapsed.
        // Split the DOM tree up to the nearest flow container and insert the table.
        Range range = selection.getRangeAt(0);
        Node start = range.getStartContainer();
        int offset = range.getStartOffset();
        Node flowContainer = DOMUtils.getInstance().getNearestFlowContainer(start);
        if (flowContainer == null) {
            return;
        }
        Element table = createTable(rta.getDocument(), config);
        if (flowContainer == start) {
            DOMUtils.getInstance().insertAt(flowContainer, table, offset);
        } else {
            DOMUtils.getInstance().splitHTMLNode(flowContainer, start, offset);
            DOMUtils.getInstance().insertAfter(table, DOMUtils.getInstance().getChild(flowContainer, start));
        }

        // Place the caret at the beginning of the first cell.
        range.selectNodeContents(DOMUtils.getInstance().getFirstDescendant(table,
            config.hasHeader() ? TableUtils.COL_HNODENAME : TableUtils.COL_NODENAME));
        range.collapse(false);
        selection.removeAllRanges();
        selection.addRange(range);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractTableFeature#execute(RichTextArea, String)
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
     * @see AbstractTableFeature#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        return super.isEnabled(rta)
            && TableUtils.getInstance().getTable(TableUtils.getInstance().getCaretNode(rta.getDocument())) == null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupListener#onPopupClosed(SourcesPopupEvents, boolean)
     */
    public void onPopupClosed(SourcesPopupEvents sender, boolean autoClosed)
    {
        if (!autoClosed && !getDialog().isCanceled()) {
            // Call the command again, passing the insertion configuration as a JSON object.
            getPlugin().getTextArea().getCommandManager().execute(
                getCommand(),
                "{ rows:" + getDialog().getRowNumber() + ", cols: " + getDialog().getColNumber() + ", header: "
                    + getDialog().hasHeader() + " }");
        } else {
            // We get here if the dialog has been closed by clicking the close button.
            // In this case we return the focus to the text area.
            getPlugin().getTextArea().setFocus(true);
        }
    }
}

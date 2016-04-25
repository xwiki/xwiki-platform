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

import java.util.EnumSet;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.InsertBlockHTMLExecutable;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardListener;
import org.xwiki.gwt.user.client.ui.wizard.WizardStepMap;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.table.TableDescriptor;
import org.xwiki.gwt.wysiwyg.client.plugin.table.TablePlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.table.ui.TableConfigWizardStep;
import org.xwiki.gwt.wysiwyg.client.plugin.table.util.TableConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.table.util.TableUtils;

import com.google.gwt.user.client.ui.Image;

/**
 * Feature allowing to insert a table in the editor. It is disabled when the caret is positioned in a table.
 * 
 * @version $Id$
 */
public class InsertTable extends AbstractTableFeature implements WizardListener
{
    /**
     * Feature name.
     */
    public static final String NAME = "inserttable";

    /**
     * The name of the wizard step that configures a table before inserting it.
     */
    private static final String CONFIG_STEP_NAME = "config";

    /**
     * Insert table wizard.
     */
    private Wizard wizard;

    /**
     * The object used to insert the table.
     * <p>
     * NOTE: This class should extend {@link InsertBlockHTMLExecutable} instead of aggregating it.
     */
    private final InsertBlockHTMLExecutable insertBlockHTMLExecutable;

    /**
     * Initialize the feature. Table features needs to be aware of the plug-in (here the ClickListener) since they hold
     * their own PushButton.
     * 
     * @param plugin table plug-in.
     */
    public InsertTable(TablePlugin plugin)
    {
        super(NAME, new Command(NAME), Strings.INSTANCE.insertTable(), plugin);
        insertBlockHTMLExecutable = new InsertBlockHTMLExecutable(rta);
    }

    /**
     * Get table wizard pop-up.
     * 
     * @return the table wizard pop-up instance.
     */
    public Wizard getWizard()
    {
        if (wizard == null) {
            TableConfigWizardStep configStep = new TableConfigWizardStep();
            configStep.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.tableInsertButton());
            configStep.setValidDirections(EnumSet.of(NavigationDirection.FINISH));

            WizardStepMap insertSteps = new WizardStepMap();
            insertSteps.put(CONFIG_STEP_NAME, configStep);

            wizard = new Wizard(Strings.INSTANCE.tableInsertDialogCaption(), new Image(Images.INSTANCE.insertTable()));
            wizard.setProvider(insertSteps);
            wizard.addWizardListener(this);
        }
        return wizard;
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

        Element container = doc.createDivElement().cast();
        container.setInnerHTML(table.toString());
        Element tableElement = (Element) container.getFirstChild();
        container.removeChild(tableElement);
        return tableElement;
    }

    /**
     * Insert a HTML table in the editor.
     * 
     * @param config creation parameters.
     */
    public void insertTable(TableConfig config)
    {
        Element table = createTable(rta.getDocument(), config);
        insertBlockHTMLExecutable.execute(table);

        // Place the caret at the beginning of the first cell.
        Range range = rta.getDocument().createRange();
        range.selectNodeContents(domUtils.getFirstDescendant(table, config.hasHeader() ? TableUtils.COL_HNODENAME
            : TableUtils.COL_NODENAME));
        range.collapse(true);

        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
    }

    @Override
    public boolean execute(String parameter)
    {
        if (StringUtils.isEmpty(parameter)) {
            // The command has been executed without insertion configuration, start the insert table wizard.
            getWizard().start(CONFIG_STEP_NAME, null);
        } else {
            // Insert the table element.
            insertTable((TableConfig) TableConfig.fromJson(parameter));
        }

        return true;
    }

    @Override
    public boolean isEnabled()
    {
        return super.isEnabled()
            && TableUtils.getInstance().getTable(TableUtils.getInstance().getCaretNode(rta.getDocument())) == null;
    }

    @Override
    public void onCancel(Wizard sender)
    {
        if (sender == getWizard()) {
            getPlugin().getTextArea().setFocus(true);
        }
    }

    @Override
    public void onFinish(Wizard sender, Object result)
    {
        if (sender == getWizard()) {
            getPlugin().getTextArea().setFocus(true);
            TableDescriptor descriptor = (TableDescriptor) result;
            // Call the command again, passing the insertion configuration as a JSON object.
            getPlugin().getTextArea().getCommandManager().execute(
                getCommand(),
                "{ rows:" + descriptor.getRowCount() + ", cols: " + descriptor.getColumnCount() + ", header: "
                    + descriptor.isWithHeader() + " }");
        }
    }
}

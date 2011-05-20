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
package org.xwiki.gwt.wysiwyg.client.plugin.table.ui;

import org.xwiki.gwt.user.client.FocusCommand;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.TextBoxNumberFilter;
import org.xwiki.gwt.user.client.ui.wizard.AbstractInteractiveWizardStep;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListenerCollection;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.table.TableDescriptor;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Wizard step for configuring a table before inserting it.
 * 
 * @version $Id$
 */
public class TableConfigWizardStep extends AbstractInteractiveWizardStep implements KeyUpHandler,
    SourcesNavigationEvents
{
    /**
     * Default panel style.
     */
    private static final String DEFAULT_PANEL_STYLE = "xTablePanel";

    /**
     * Style of the information labels.
     */
    private static final String INFO_LABEL_STYLE = "xInfoLabel";

    /**
     * Style used to signal mandatory elements.
     */
    private static final String MANDATORY_STYLE = "xMandatory";

    /**
     * Style of the information labels.
     */
    private static final String HELP_LABEL_STYLE = "xHelpLabel";

    /**
     * The style applied to the validation messages.
     */
    private static final String ERROR_STYLE = "xErrorMsg";

    /**
     * The style for an field in error.
     */
    private static final String FIELD_ERROR_STYLE = "xErrorField";

    /**
     * Row number text input.
     */
    private TextBox rows;

    /**
     * Displays a validation message if the {@link #rows} field has an invalid value.
     */
    private Label rowsValidationMessage;

    /**
     * Column number text input.
     */
    private TextBox columns;

    /**
     * Displays a validation message if the {@link #columns} field has an invalid value.
     */
    private Label columnsValidationMessage;

    /**
     * Border size text input.
     */
    private TextBox border;

    /**
     * Table has a heading row.
     */
    private CheckBox header;

    /**
     * Describes the table to be inserted.
     */
    private final TableDescriptor descriptor;

    /**
     * The list of navigation listeners. This wizard step generates navigation events when the user presses the Enter
     * key in one of the fields.
     */
    private final NavigationListenerCollection navigationListeners = new NavigationListenerCollection();

    /**
     * Creates a new wizard step for configuring a table before inserting it.
     */
    public TableConfigWizardStep()
    {
        setStepTitle(Strings.INSTANCE.tableInsertDialogTitle());

        descriptor = new TableDescriptor();
        descriptor.setRowCount(2);
        descriptor.setColumnCount(2);
        descriptor.setWithHeader(true);

        display().addStyleName("xTableMainPanel");
        display().add(getRowsPanel());
        display().add(getColumnsPanel());
        // getPanel().add(getBorderPanel());
        display().add(getHeaderPanel());
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyUpHandler#onKeyUp(KeyUpEvent)
     */
    public void onKeyUp(KeyUpEvent event)
    {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            navigationListeners.fireNavigationEvent(NavigationDirection.NEXT);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInteractiveWizardStep#getResult()
     */
    public Object getResult()
    {
        return descriptor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInteractiveWizardStep#init(Object, AsyncCallback)
     */
    public void init(Object data, AsyncCallback< ? > cb)
    {
        cb.onSuccess(null);
        // Focus the first input field.
        Scheduler.get().scheduleDeferred(new FocusCommand(rows));
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInteractiveWizardStep#onCancel()
     */
    public void onCancel()
    {
        hideValidationMessages();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInteractiveWizardStep#onSubmit(AsyncCallback)
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        if (validate()) {
            updateTableDescriptor();
            async.onSuccess(true);
        } else {
            async.onSuccess(false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesNavigationEvents#addNavigationListener(NavigationListener)
     */
    public void addNavigationListener(NavigationListener listener)
    {
        navigationListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesNavigationEvents#removeNavigationListener(NavigationListener)
     */
    public void removeNavigationListener(NavigationListener listener)
    {
        navigationListeners.remove(listener);
    }

    /**
     * @return the panel holding the rows settings for the table
     */
    private Panel getRowsPanel()
    {
        FlowPanel panel = new FlowPanel();
        panel.addStyleName(DEFAULT_PANEL_STYLE);

        Panel rowsLabel = new FlowPanel();
        rowsLabel.setStyleName(INFO_LABEL_STYLE);
        rowsLabel.add(new InlineLabel(Strings.INSTANCE.tableRowsLabel()));
        InlineLabel mandatoryLabel = new InlineLabel(Strings.INSTANCE.mandatory());
        mandatoryLabel.addStyleName(MANDATORY_STYLE);
        rowsLabel.add(mandatoryLabel);
        panel.add(rowsLabel);

        Label rowsHelpLabel = new Label(Strings.INSTANCE.tableRowsHelpLabel());
        rowsHelpLabel.setStyleName(HELP_LABEL_STYLE);
        panel.add(rowsHelpLabel);

        rowsValidationMessage = new Label(Strings.INSTANCE.tableInsertStrictPositiveIntegerRequired());
        rowsValidationMessage.setVisible(false);
        rowsValidationMessage.addStyleName(ERROR_STYLE);
        panel.add(rowsValidationMessage);

        rows = new TextBox();
        rows.setText(String.valueOf(descriptor.getRowCount()));
        rows.setTitle(Strings.INSTANCE.tableRowsToolTip());
        rows.addKeyPressHandler(new TextBoxNumberFilter());
        rows.addKeyUpHandler(this);
        panel.add(rows);

        return panel;
    }

    /**
     * @return the panel holding the columns settings for the table
     */
    private Panel getColumnsPanel()
    {
        FlowPanel panel = new FlowPanel();
        panel.addStyleName(DEFAULT_PANEL_STYLE);

        Panel colsLabel = new FlowPanel();
        colsLabel.setStyleName(INFO_LABEL_STYLE);
        colsLabel.add(new InlineLabel(Strings.INSTANCE.tableColsLabel()));
        InlineLabel mandatoryLabel = new InlineLabel(Strings.INSTANCE.mandatory());
        mandatoryLabel.addStyleName(MANDATORY_STYLE);
        colsLabel.add(mandatoryLabel);
        panel.add(colsLabel);

        Label colsHelpLabel = new Label(Strings.INSTANCE.tableColsHelpLabel());
        colsHelpLabel.setStyleName(HELP_LABEL_STYLE);
        panel.add(colsHelpLabel);

        columnsValidationMessage = new Label(Strings.INSTANCE.tableInsertStrictPositiveIntegerRequired());
        columnsValidationMessage.setVisible(false);
        columnsValidationMessage.addStyleName(ERROR_STYLE);
        panel.add(columnsValidationMessage);

        columns = new TextBox();
        columns.setText(String.valueOf(descriptor.getColumnCount()));
        columns.setTitle(Strings.INSTANCE.tableColsToolTip());
        columns.addKeyPressHandler(new TextBoxNumberFilter());
        columns.addKeyUpHandler(this);
        panel.add(columns);

        return panel;
    }

    /**
     * @return the panel holding the border settings for the table
     */
    private Panel getBorderPanel()
    {
        FlowPanel panel = new FlowPanel();
        panel.addStyleName(DEFAULT_PANEL_STYLE);

        Label borderSizeLabel = new Label(Strings.INSTANCE.tableBorderLabel());
        borderSizeLabel.setStyleName(INFO_LABEL_STYLE);
        panel.add(borderSizeLabel);

        Label borderSizeHelpLabel = new Label(Strings.INSTANCE.tableBorderHelpLabel());
        borderSizeHelpLabel.setStyleName(HELP_LABEL_STYLE);
        panel.add(borderSizeHelpLabel);

        border = new TextBox();
        border.addStyleName("xBorderInput");
        border.addKeyPressHandler(new TextBoxNumberFilter());
        border.addKeyUpHandler(this);
        panel.add(border);

        Label borderPixelLabel = new Label(Strings.INSTANCE.tablePixel());
        borderPixelLabel.setStyleName("xTablePixel");
        panel.add(borderPixelLabel);

        return panel;
    }

    /**
     * @return the panel holding the border settings for the table
     */
    private Panel getHeaderPanel()
    {
        FlowPanel panel = new FlowPanel();
        panel.addStyleName(DEFAULT_PANEL_STYLE);

        header = new CheckBox(Strings.INSTANCE.tableHeaderLabel());
        header.addStyleName(INFO_LABEL_STYLE);
        header.addKeyUpHandler(this);
        header.setValue(descriptor.isWithHeader());
        panel.add(header);

        Label headerHelpLabel = new Label(Strings.INSTANCE.tableHeaderHelpLabel());
        headerHelpLabel.setStyleName(HELP_LABEL_STYLE);
        panel.add(headerHelpLabel);

        return panel;
    }

    /**
     * @return {@code true} if this dialog can be submitted, {@code false} otherwise
     */
    private boolean validate()
    {
        hideValidationMessages();

        Focusable failed = null;
        if (!validateRequiredStrictPositiveIntger(columns.getText())) {
            columnsValidationMessage.setVisible(true);
            columns.addStyleName(FIELD_ERROR_STYLE);
            failed = columns;
        }
        if (!validateRequiredStrictPositiveIntger(rows.getText())) {
            rowsValidationMessage.setVisible(true);
            rows.addStyleName(FIELD_ERROR_STYLE);
            failed = rows;
        }

        if (failed != null) {
            Scheduler.get().scheduleDeferred(new FocusCommand(failed));
            return false;
        }
        return true;
    }

    /**
     * Checks if the given string is a strict positive integer.
     * 
     * @param str the string to be validated
     * @return {@code true} if the given string is valid, {@code false} otherwise
     */
    private boolean validateRequiredStrictPositiveIntger(String str)
    {
        if (StringUtils.isEmpty(str)) {
            // Required
            return false;
        } else {
            try {
                int rowCount = Integer.parseInt(str);
                if (rowCount <= 0) {
                    // Positive
                    return false;
                }
            } catch (NumberFormatException e) {
                // Number
                return false;
            }
        }
        return true;
    }

    /**
     * Hides the validation messages generated by the last call to {@link #validate()}.
     */
    private void hideValidationMessages()
    {
        rowsValidationMessage.setVisible(false);
        rows.removeStyleName(FIELD_ERROR_STYLE);
        columnsValidationMessage.setVisible(false);
        columns.removeStyleName(FIELD_ERROR_STYLE);
    }

    /**
     * Updates the table descriptor with the values from the input fields.
     */
    private void updateTableDescriptor()
    {
        descriptor.setRowCount(Integer.parseInt(rows.getText()));
        descriptor.setColumnCount(Integer.parseInt(columns.getText()));
        descriptor.setWithHeader(header.getValue());
    }
}

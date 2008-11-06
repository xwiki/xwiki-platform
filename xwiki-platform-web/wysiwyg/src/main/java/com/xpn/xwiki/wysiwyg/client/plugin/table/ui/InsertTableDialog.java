package com.xpn.xwiki.wysiwyg.client.plugin.table.ui;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.widget.CompositeDialogBox;
import com.xpn.xwiki.wysiwyg.client.util.TextBoxNumberFilter;

/**
 * Table creation dialog.
 * 
 * @version $Id$
 */
public class InsertTableDialog extends CompositeDialogBox implements ClickListener
{
    /**
     * Insert button.
     */
    private Button insertButton;

    /**
     * Row number text input.
     */
    private TextBox rows;

    /**
     * Column number text input.
     */
    private TextBox cols;

    /**
     * Is the insertion action canceled.
     */
    private boolean canceled = true;

    /**
     * Default constructor.
     */
    public InsertTableDialog()
    {
        super(false, true);

        // Enable display/hide special effects
        getDialog().setAnimationEnabled(true);

        // FIXME : improve dialog layout

        // Row number
        rows = new TextBox();
        rows.addKeyboardListener(new TextBoxNumberFilter());
        rows.setText("2");

        // Column number
        cols = new TextBox();
        cols.addKeyboardListener(new TextBoxNumberFilter());
        cols.setText("2");
        insertButton = new Button("Insert", this);

        // Container panel
        FlowPanel panel = new FlowPanel();
        panel.add(rows);
        panel.add(cols);
        panel.add(insertButton);

        // Container panel initialization
        initWidget(panel);
    }

    /**
     * Get requested row number.
     * 
     * @return number of row to create.
     */
    public String getRowNumber()
    {
        return rows.getText();
    }

    /**
     * Get requested column number.
     * 
     * @return number of columns to create.
     */
    public String getColNumber()
    {
        return cols.getText();
    }

    /**
     * Check if the insertion action been canceled by the user.
     * 
     * @return true if the insertion action has been canceled.
     */
    public boolean isCanceled()
    {
        return canceled;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == insertButton) {
            // FIXME : check that there's no 0 or "" in one of the 2 inputs.
            canceled = false;
            hide();
        }
    }
}

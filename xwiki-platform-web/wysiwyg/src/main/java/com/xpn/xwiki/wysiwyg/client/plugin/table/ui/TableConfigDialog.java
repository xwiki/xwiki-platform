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
package com.xpn.xwiki.wysiwyg.client.plugin.table.ui;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.util.TextBoxNumberFilter;
import com.xpn.xwiki.wysiwyg.client.widget.CompositeDialogBox;

/**
 * Table creation dialog.
 * 
 * @version $Id$
 */
public class TableConfigDialog extends CompositeDialogBox implements ClickListener
{
    /**
     * Default panel style.
     */
    private static final String DEFAULT_PANEL_STYLE = "xTablePanel";
    
    /**
     * Row number text input.
     */
    private TextBox rows;

    /**
     * Column number text input.
     */
    private TextBox cols;
    
    /**
     * Border size text input.
     */
    private TextBox borderSize;
    
    /**
     * Table has a heading row.
     */
    private CheckBox header;
    
    /**
     * Insert button.
     */
    private Button insertButton;

    /**
     * Is the insertion action canceled.
     */
    private boolean canceled = true;
    
    /**
     * Default constructor.
     */
    public TableConfigDialog()
    {
        super(false, true);

        // Initialize insert button
        insertButton = new Button(Strings.INSTANCE.tableInsertButton(), this);
        
        // Miscellaneous settings
        getDialog().setIcon(Images.INSTANCE.insertTable().createImage());
        getDialog().setCaption(Strings.INSTANCE.table());     
        
        // Create main panel
        FlowPanel mainPanel = new FlowPanel();
        mainPanel.addStyleName("xTableMainPanel");
        mainPanel.add(getRowsPanel());
        mainPanel.add(getColsPanel());
        // FIXME : activate table border option when possible
        // mainPanel.add(getBorderPanel());
        mainPanel.add(getHeaderPanel());
        mainPanel.add(insertButton);

        // Main panel initialization
        initWidget(mainPanel);
    }
    
    /**
     * @return the panel holding the rows settings for the table
     */
    private Panel getRowsPanel()
    {
        rows = new TextBox();               
        FlowPanel panel = new FlowPanel();        
        
        rows.setMaxLength(2);
        rows.setVisibleLength(2);
        rows.setText(Strings.INSTANCE.tableRowsDefault());
        rows.addKeyboardListener(new TextBoxNumberFilter());
        panel.addStyleName(DEFAULT_PANEL_STYLE);
        panel.add(new Label(Strings.INSTANCE.tableRowsLabel()));
        panel.add(rows);
        
        return panel;
    }
    
    /**
     * @return the panel holding the columns settings for the table
     */
    private Panel getColsPanel()
    {
        cols = new TextBox();                
        FlowPanel panel = new FlowPanel();        

        cols.setMaxLength(2);
        cols.setVisibleLength(2);
        cols.setText(Strings.INSTANCE.tableColsDefault());
        cols.addKeyboardListener(new TextBoxNumberFilter());
        panel.addStyleName(DEFAULT_PANEL_STYLE);
        panel.add(new Label(Strings.INSTANCE.tableColsLabel()));
        panel.add(cols);
        
        return panel;
    }
    
    /**
     * @return the panel holding the border settings for the table
     */
    private Panel getBorderPanel()
    {
        borderSize = new TextBox();                       
        FlowPanel panel = new FlowPanel();        
        
        borderSize.setMaxLength(2);
        borderSize.setVisibleLength(2);
        borderSize.setText(Strings.INSTANCE.tableBorderDefault());
        borderSize.addKeyboardListener(new TextBoxNumberFilter());
        panel.addStyleName(DEFAULT_PANEL_STYLE);
        panel.add(new Label(Strings.INSTANCE.tableBorderLabel()));
        panel.add(borderSize);
        panel.add(new Label(Strings.INSTANCE.tablePixel()));
        
        return panel;
    }
    
    /**
     * @return the panel holding the border settings for the table
     */
    private Panel getHeaderPanel()
    {
        header = new CheckBox();                       
        FlowPanel panel = new FlowPanel();        
        
        header.setChecked(true);
        panel.addStyleName(DEFAULT_PANEL_STYLE);        
        panel.add(header);
        panel.add(new Label(Strings.INSTANCE.tableHeaderLabel()));
        
        return panel;
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
     * Get requested border size.
     * 
     * @return table border size in pixels.
     */
    public String getBorderSize()
    {
        return borderSize.getText();
    }
    
    /**
     * Does the table include a header row.
     * 
     * @return true is the table contains a header row.
     */
    public boolean hasHeader()
    {
        return header.isChecked();
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

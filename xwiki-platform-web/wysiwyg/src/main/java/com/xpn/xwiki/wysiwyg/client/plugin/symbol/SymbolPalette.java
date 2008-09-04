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
package com.xpn.xwiki.wysiwyg.client.plugin.symbol;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ClickListenerCollection;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;

public class SymbolPalette extends Composite implements SourcesClickEvents, TableListener
{
    private final ClickListenerCollection clickListeners = new ClickListenerCollection();

    private SymbolCell selectedCell;

    private final Grid symbolGrid;

    public SymbolPalette(Object[][] symbols, int rows, int columns)
    {
        symbolGrid = new Grid(columns, rows);
        symbolGrid.addStyleName("xSymbolPalette");
        symbolGrid.setBorderWidth(0);
        symbolGrid.setCellPadding(0);
        symbolGrid.setCellSpacing(1);
        symbolGrid.addTableListener(this);

        SymbolCell charCell;
        int j = 0, k = 0;
        for (int i = 0; i < symbols.length; i++) {
            if ((Boolean) symbols[i][2]) {
                charCell = new SymbolCell(symbols[i][0].toString());
                charCell.setTitle(symbols[i][3].toString());
                symbolGrid.setWidget(k, j, charCell);
                symbolGrid.getCellFormatter().setHorizontalAlignment(k, j, HasHorizontalAlignment.ALIGN_CENTER);
                j++;
                if (j == rows) {
                    j = 0;
                    k++;
                }
            }
        }
        initWidget(symbolGrid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesClickEvents#addClickListener(ClickListener)
     */
    public void addClickListener(ClickListener listener)
    {
        clickListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesClickEvents#removeClickListener(ClickListener)
     */
    public void removeClickListener(ClickListener listener)
    {
        clickListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see TableListener#onCellClicked(SourcesTableEvents, int, int)
     */
    public void onCellClicked(SourcesTableEvents sender, int row, int column)
    {
        if (sender == symbolGrid) {
            setSelectedCell(row, column);
            clickListeners.fireClick(this);
        }
    }

    private void setSelectedCell(int row, int column)
    {
        SymbolCell selectedCell = (SymbolCell) symbolGrid.getWidget(row, column);
        if (this.selectedCell != selectedCell) {
            if (this.selectedCell != null) {
                this.selectedCell.setSelected(false);
            }
            this.selectedCell = selectedCell;
            this.selectedCell.setSelected(true);
        }
    }

    public String getSelectedSymbol()
    {
        if (selectedCell != null) {
            return selectedCell.getSymbol();
        } else {
            return null;
        }
    }
}

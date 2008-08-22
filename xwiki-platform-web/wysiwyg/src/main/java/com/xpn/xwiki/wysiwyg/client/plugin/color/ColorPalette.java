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
package com.xpn.xwiki.wysiwyg.client.plugin.color;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ClickListenerCollection;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;

public class ColorPalette extends Composite implements SourcesClickEvents, TableListener
{
    private final ClickListenerCollection clickListeners = new ClickListenerCollection();

    private ColorCell selectedCell;

    private final Grid colorGrid;

    private final String[][] colors;

    public ColorPalette(String[][] colors)
    {
        colorGrid = new Grid(colors.length, colors[0].length);
        colorGrid.addStyleName("xColorPalette");
        colorGrid.setBorderWidth(0);
        colorGrid.setCellPadding(0);
        colorGrid.setCellSpacing(0);
        colorGrid.addTableListener(this);

        this.colors = colors;
        for (int i = 0; i < colors.length; i++) {
            for (int j = 0; j < colors[0].length; j++) {
                colorGrid.setWidget(i, j, new ColorCell(colors[i][j]));
            }
        }

        initWidget(colorGrid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesClickEvents#addClickListener(com.google.gwt.user.client.ui.ClickListener)
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
        if (sender == colorGrid) {
            setSelectedCell(row, column);
            clickListeners.fireClick(this);
        }
    }

    private void setSelectedCell(int row, int column)
    {
        ColorCell selectedCell = (ColorCell) colorGrid.getWidget(row, column);
        if (this.selectedCell != selectedCell) {
            if (this.selectedCell != null) {
                this.selectedCell.setSelected(false);
            }
            this.selectedCell = selectedCell;
            this.selectedCell.setSelected(true);
        }
    }

    public String getSelectedColor()
    {
        if (selectedCell != null) {
            return selectedCell.getColor();
        } else {
            return null;
        }
    }

    public void setSelectedColor(String color)
    {
        for (int i = 0; i < colors.length; i++) {
            for (int j = 0; j < colors[0].length; j++) {
                if (colors[i][j].equals(color)) {
                    setSelectedCell(i, j);
                    return;
                }
            }
        }
        if (selectedCell != null) {
            selectedCell.setSelected(false);
            selectedCell = null;
        }
    }
}
